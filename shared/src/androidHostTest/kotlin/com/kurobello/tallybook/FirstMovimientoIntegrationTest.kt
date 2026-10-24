package com.kurobello.tallybook

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.data.AppPreferencesRepositoryImpl
import com.kurobello.tallybook.core.data.CategoriaRepositoryImpl
import com.kurobello.tallybook.core.data.CategorySeedProvider
import com.kurobello.tallybook.core.data.DeviceLocaleProvider
import com.kurobello.tallybook.core.data.EtiquetaRepositoryImpl
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.LocalDataBootstrapper
import com.kurobello.tallybook.core.data.MovimientoRepositoryImpl
import com.kurobello.tallybook.core.data.PreferencesDataStoreFactory
import com.kurobello.tallybook.core.data.PreferencesFile
import com.kurobello.tallybook.core.data.PreferencesPathProvider
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.data.entry.CategoryKeywordDictionary
import com.kurobello.tallybook.core.data.entry.CategorySuggester
import com.kurobello.tallybook.core.data.entry.EtiquetaMatcher
import com.kurobello.tallybook.core.data.entry.MovimientoPhraseParser
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.feature.home.HomeViewModel
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoRepositories
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoViewModel
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoEntryEngine
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class FirstMovimientoIntegrationTest {

  @Test
  fun `first movimiento flows from bootstrap to home and persists across repository recreation`() =
      runTest(timeout = ZERO) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val databaseDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TallybookDatabase.Schema.create(databaseDriver)
        val database = TallybookDatabase(databaseDriver)
        val preferencesDirectory = createTempDirectory("tallybook-first-movimiento-")
        val preferencesPath = TestPreferencesPathProvider(preferencesDirectory.toString())
        val scopes = mutableListOf<CoroutineScope>()
        var firstPreferencesFactory: PreferencesDataStoreFactory? = null
        var recreatedPreferencesFactory: PreferencesDataStoreFactory? = null

        try {
          firstPreferencesFactory = PreferencesDataStoreFactory(preferencesPath, dispatcher)
          val preferences = AppPreferencesRepositoryImpl(firstPreferencesFactory.create())
          val categorias =
              CategoriaRepositoryImpl(
                  database,
                  dispatcher,
                  IdGenerator { error("Not used by this test") },
              )
          val movimientos = MovimientoRepositoryImpl(database, dispatcher)
          val etiquetas =
              EtiquetaRepositoryImpl(
                  database,
                  dispatcher,
                  IdGenerator { error("Not used by this test") },
                  FixedClock(FIXED_INSTANT),
              )
          val bootstrapper =
              LocalDataBootstrapper(
                  localeProvider = FixedDeviceLocaleProvider("es-CO"),
                  preferencesRepository = preferences,
                  categoriaRepository = categorias,
                  categorySeedProvider =
                      CategorySeedProvider { resource ->
                        // Resource resolution is covered on device; this graph test needs
                        // deterministic names.
                        resource.key
                      },
              )

          assertEquals(DataOutcome.Success(Unit), bootstrapper.initialize())
          assertEquals(
              DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(Moneda.COP))),
              preferences.preferences.first(),
          )
          assertEquals(59, assertSuccess(categorias.getAll()).size)

          val home =
              HomeViewModel(
                  preferencesRepository = preferences,
                  movimientoRepository = movimientos,
                  categoriaRepository = categorias,
                  scope = scopes.newScope(dispatcher),
              )
          backgroundScope.launch { home.uiState.collect {} }
          testScheduler.advanceUntilIdle()

          assertFalse(home.uiState.value.isLoading)
          assertTrue(home.uiState.value.isEmpty)
          assertEquals(emptySummary(Moneda.COP), home.uiState.value.summary)

          val expenseForm =
              createMovimientoViewModel(
                  repositories =
                      AddMovimientoRepositories(preferences, categorias, movimientos, etiquetas),
                  id = "expense",
                  scope = scopes.newScope(dispatcher),
                  dispatcher = dispatcher,
              )
          testScheduler.advanceUntilIdle()
          expenseForm.apply {
            onAmountChanged("10,50")
            onCategoriaSelected("cat_comida")
            onSubmit()
          }
          testScheduler.advanceUntilIdle()

          assertEquals(Money(-1_050, Moneda.COP), home.uiState.value.summary.balance)
          assertEquals(listOf("expense"), home.uiState.value.recent.map(Movimiento::id))

          val incomeForm =
              createMovimientoViewModel(
                  repositories =
                      AddMovimientoRepositories(preferences, categorias, movimientos, etiquetas),
                  id = "income",
                  scope = scopes.newScope(dispatcher),
                  dispatcher = dispatcher,
              )
          testScheduler.advanceUntilIdle()
          incomeForm.apply {
            onTipoChanged(TipoMovimiento.INGRESO)
            onAmountChanged("20,00")
            onCategoriaSelected("cat_ingresos")
            onSubmit()
          }
          testScheduler.advanceUntilIdle()

          assertEquals(
              MovimientoSummary(
                  ingresos = Money(2_000, Moneda.COP),
                  gastos = Money(1_050, Moneda.COP),
              ),
              home.uiState.value.summary,
          )
          assertEquals(Money(950, Moneda.COP), home.uiState.value.summary.balance)

          assertEquals(
              DataOutcome.Success(Unit),
              movimientos.insert(
                  Movimiento(
                      id = "foreign-income",
                      tipo = TipoMovimiento.INGRESO,
                      monto = Money(9_999, Moneda.USD),
                      fecha = FIXED_DATE,
                      categoriaId = "cat_ingresos",
                      titulo = null,
                      descripcion = null,
                      createdAt = FIXED_INSTANT,
                  )
              ),
          )
          testScheduler.advanceUntilIdle()

          assertEquals(Money(950, Moneda.COP), home.uiState.value.summary.balance)

          scopes.forEach(CoroutineScope::cancel)
          testScheduler.advanceUntilIdle()
          firstPreferencesFactory.close()
          firstPreferencesFactory = null

          recreatedPreferencesFactory = PreferencesDataStoreFactory(preferencesPath, dispatcher)
          val recreatedPreferences =
              AppPreferencesRepositoryImpl(recreatedPreferencesFactory.create())
          val recreatedCategorias =
              CategoriaRepositoryImpl(
                  TallybookDatabase(databaseDriver),
                  dispatcher,
                  IdGenerator { error("Not used by this test") },
              )
          val recreatedMovimientos =
              MovimientoRepositoryImpl(TallybookDatabase(databaseDriver), dispatcher)

          assertEquals(
              DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(Moneda.COP))),
              recreatedPreferences.preferences.first(),
          )
          assertEquals(59, assertSuccess(recreatedCategorias.getAll()).size)
          assertEquals(
              setOf("expense", "income", "foreign-income"),
              assertSuccess(recreatedMovimientos.getAll()).map(Movimiento::id).toSet(),
          )
        } finally {
          scopes.forEach(CoroutineScope::cancel)
          firstPreferencesFactory?.close()
          recreatedPreferencesFactory?.close()
          databaseDriver.close()
          preferencesDirectory.toFile().deleteRecursively()
        }
      }

  @Test
  fun `a sentence becomes a persisted row and a saved correction teaches the next one`() =
      runTest(timeout = ZERO) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val databaseDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TallybookDatabase.Schema.create(databaseDriver)
        val database = TallybookDatabase(databaseDriver)
        val preferencesDirectory = createTempDirectory("tallybook-sentence-entry-")
        val scopes = mutableListOf<CoroutineScope>()
        val preferencesFactory =
            PreferencesDataStoreFactory(
                TestPreferencesPathProvider(preferencesDirectory.toString()),
                dispatcher,
            )

        try {
          val preferences = AppPreferencesRepositoryImpl(preferencesFactory.create())
          val categorias =
              CategoriaRepositoryImpl(
                  database,
                  dispatcher,
                  IdGenerator { error("Not used by this test") },
              )
          val movimientos = MovimientoRepositoryImpl(database, dispatcher)
          val etiquetas =
              EtiquetaRepositoryImpl(
                  database,
                  dispatcher,
                  IdGenerator { error("Not used by this test") },
                  FixedClock(FIXED_INSTANT),
              )
          val bootstrapper =
              LocalDataBootstrapper(
                  localeProvider = FixedDeviceLocaleProvider("es-CO"),
                  preferencesRepository = preferences,
                  categoriaRepository = categorias,
                  categorySeedProvider = CategorySeedProvider { resource -> resource.key },
              )
          assertEquals(DataOutcome.Success(Unit), bootstrapper.initialize())

          val repositories =
              AddMovimientoRepositories(preferences, categorias, movimientos, etiquetas)
          val first =
              createMovimientoViewModel(
                  repositories,
                  "first",
                  scopes.newScope(dispatcher),
                  dispatcher,
              )
          testScheduler.advanceUntilIdle()
          first.onPhraseChanged("Aceite 30 mil")
          first.onPhraseContinue()
          assertNull(first.uiState.value.categoriaId)
          first.onSuggestionSelected("cat_supermercado")
          first.onSubmit()
          testScheduler.advanceUntilIdle()

          val saved = assertSuccess(movimientos.getAll()).single()
          assertEquals("first", saved.id)
          assertEquals("Aceite 30 mil", saved.titulo)
          assertNull(saved.descripcion)
          assertEquals("cat_supermercado", saved.categoriaId)
          assertEquals(Money(3_000_000, Moneda.COP), saved.monto)
          assertEquals(FIXED_DATE, saved.fecha)

          val second =
              createMovimientoViewModel(
                  repositories,
                  "second",
                  scopes.newScope(dispatcher),
                  dispatcher,
              )
          testScheduler.advanceUntilIdle()
          second.onPhraseChanged("Aceite 15 mil")
          second.onPhraseContinue()

          assertEquals("cat_supermercado", second.uiState.value.categoriaId)
          assertEquals(Money(1_500_000, Moneda.COP), second.uiState.value.summary.monto)

          second.onDiscardChanges()
          second.onPhraseChanged("Gasté 5.000 en algo raro")
          second.onPhraseContinue()
          second.onSubmit()
          testScheduler.advanceUntilIdle()

          assertEquals(
              CATEGORIA_OTROS_ID,
              assertSuccess(movimientos.getAll()).single { it.id == "second" }.categoriaId,
          )
        } finally {
          scopes.forEach(CoroutineScope::cancel)
          preferencesFactory.close()
          databaseDriver.close()
          preferencesDirectory.toFile().deleteRecursively()
        }
      }

  private fun createMovimientoViewModel(
      repositories: AddMovimientoRepositories,
      id: String,
      scope: CoroutineScope,
      dispatcher: TestDispatcher,
  ) =
      AddMovimientoViewModel(
          repositories = repositories,
          idGenerator = IdGenerator { id },
          clock = FixedClock(FIXED_INSTANT),
          timeZoneProvider = TimeZoneProvider { TimeZone.UTC },
          entryEngine =
              MovimientoEntryEngine(
                  parser = MovimientoPhraseParser(),
                  suggester = CategorySuggester(CategoryKeywordDictionary.builtIn),
                  etiquetaMatcher = EtiquetaMatcher(),
                  localeProvider = FixedDeviceLocaleProvider("es-CO"),
                  computationDispatcher = dispatcher,
              ),
          scope = scope,
      )

  private fun emptySummary(moneda: Moneda) =
      MovimientoSummary(ingresos = Money(0, moneda), gastos = Money(0, moneda))

  private fun <T> assertSuccess(outcome: DataOutcome<T>): T =
      (outcome as? DataOutcome.Success)?.data ?: error("Expected a successful data outcome")

  private fun MutableList<CoroutineScope>.newScope(
      dispatcher: TestDispatcher,
  ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher).also(::add)

  private class FixedDeviceLocaleProvider(
      private val languageTag: String,
  ) : DeviceLocaleProvider {
    override fun currentLanguageTag(): String = languageTag
  }

  private class FixedClock(
      private val instant: Instant,
  ) : Clock {
    override fun now(): Instant = instant
  }

  private class TestPreferencesPathProvider(
      private val directory: String,
  ) : PreferencesPathProvider {
    override fun path(file: PreferencesFile): String = "$directory/${file.fileName}"
  }

  private companion object {
    val FIXED_INSTANT: Instant = Instant.parse("2026-09-15T12:00:00Z")
    val FIXED_DATE: LocalDate = LocalDate(2026, 9, 15)
  }
}
