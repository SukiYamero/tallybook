package com.kurobello.tallybook.core.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.ThemeMode
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.map
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class LocalDataBootstrapperTest {

  @Test
  fun `initialize resolves locale once then initializes preferences and categories`() = runTest {
    val localeProvider = FakeDeviceLocaleProvider("en-US")
    val preferencesRepository = FakePreferencesRepository(DataOutcome.Success(Unit))
    val categoriaRepository = FakeCategoriaRepository(DataOutcome.Success(Unit))
    var resolvedResourceCount = 0
    val bootstrapper =
        LocalDataBootstrapper(
            localeProvider = localeProvider,
            preferencesRepository = preferencesRepository,
            categoriaRepository = categoriaRepository,
            categorySeedProvider =
                CategorySeedProvider { resource ->
                  resolvedResourceCount += 1
                  resource.key
                },
            changeClock = TestChangeClock(),
            storedMaxChangeStamp = { DataOutcome.Success(null) },
        )

    assertEquals(DataOutcome.Success(Unit), bootstrapper.initialize())
    assertEquals(1, localeProvider.calls)
    assertEquals(listOf(Moneda.USD), preferencesRepository.initializedWith)
    assertEquals(1, categoriaRepository.seedCalls)
    assertEquals(59, categoriaRepository.seededCategorias.single().size)
    assertEquals(59, resolvedResourceCount)
  }

  @Test
  fun `initialize stops before category seeding when preferences initialization fails`() = runTest {
    val failure = DataOutcome.Failure(DataError.Local(IllegalStateException("preferences failed")))
    val categoriaRepository = FakeCategoriaRepository(DataOutcome.Success(Unit))
    val bootstrapper =
        LocalDataBootstrapper(
            localeProvider = FakeDeviceLocaleProvider("es-CO"),
            preferencesRepository = FakePreferencesRepository(failure),
            categoriaRepository = categoriaRepository,
            categorySeedProvider = CategorySeedProvider { resource -> resource.key },
            changeClock = TestChangeClock(),
            storedMaxChangeStamp = { DataOutcome.Success(null) },
        )

    assertEquals(failure, bootstrapper.initialize())
    assertEquals(0, categoriaRepository.seedCalls)
  }

  @Test
  fun `initialize returns a category seed failure`() = runTest {
    val failure = DataOutcome.Failure(DataError.Local(IllegalStateException("categories failed")))
    val categoriaRepository = FakeCategoriaRepository(failure)
    val changeClock = TestChangeClock()
    val bootstrapper =
        LocalDataBootstrapper(
            localeProvider = FakeDeviceLocaleProvider("es-CO"),
            preferencesRepository = FakePreferencesRepository(DataOutcome.Success(Unit)),
            categoriaRepository = categoriaRepository,
            categorySeedProvider = CategorySeedProvider { resource -> resource.key },
            changeClock = changeClock,
            storedMaxChangeStamp = { DataOutcome.Success(null) },
        )

    assertEquals(failure, bootstrapper.initialize())
    assertEquals(1, categoriaRepository.seedCalls)
    assertEquals(0, changeClock.prepareCalls)
  }

  @Test
  fun `initialize prepares the clock with the stored maximum after seeding`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val database = createSchemaWithReferences(driver, etiquetaIds = listOf("tag"))
      val tombstoneStamp = ChangeStamp.of(epochMillis = 9_000, counter = 2, node = NODE)
      database.insertLegacy(
          movimiento("live"),
          stamp = ChangeStamp.of(epochMillis = 9_000, counter = 1, node = NODE),
      )
      database.insertLegacy(movimiento("deleted"), stamp = tombstoneStamp)
      driver.execute(
          null,
          "UPDATE movimiento SET deleted_at = updated_at WHERE id = 'deleted'",
          0,
      )
      val events = mutableListOf<String>()
      val changeClock = RecordingChangeClock(events)
      val bootstrapper =
          LocalDataBootstrapper(
              localeProvider = FakeDeviceLocaleProvider("es-CO"),
              preferencesRepository = FakePreferencesRepository(DataOutcome.Success(Unit), events),
              categoriaRepository = FakeCategoriaRepository(DataOutcome.Success(Unit), events),
              categorySeedProvider = CategorySeedProvider { resource -> resource.key },
              changeClock = changeClock,
              storedMaxChangeStamp = {
                database.maxStoredChangeStamp(UnconfinedTestDispatcher(testScheduler))
              },
          )

      assertEquals(DataOutcome.Success(Unit), bootstrapper.initialize())
      assertEquals(listOf("preferences", "seed", "prepare"), events)
      assertEquals(
          listOf<DataOutcome<ChangeStamp?>>(DataOutcome.Success(tombstoneStamp)),
          changeClock.storedMaxima,
      )
    }
  }

  @Test
  fun `initialize fails when the stored maximum is not a change stamp`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val database = createSchemaWithReferences(driver)
      driver.execute(null, "UPDATE categoria SET updated_at = 'not a stamp'", 0)
      val changeClock = TestChangeClock()
      val bootstrapper =
          LocalDataBootstrapper(
              localeProvider = FakeDeviceLocaleProvider("es-CO"),
              preferencesRepository = FakePreferencesRepository(DataOutcome.Success(Unit)),
              categoriaRepository = FakeCategoriaRepository(DataOutcome.Success(Unit)),
              categorySeedProvider = CategorySeedProvider { resource -> resource.key },
              changeClock = changeClock,
              storedMaxChangeStamp = {
                database.maxStoredChangeStamp(UnconfinedTestDispatcher(testScheduler))
              },
          )

      val outcome = bootstrapper.initialize()

      assertIs<DataError.Local>(assertIs<DataOutcome.Failure>(outcome).error)
      assertEquals(1, changeClock.prepareCalls)
    }
  }

  private fun movimiento(id: String) =
      Movimiento(
          id = id,
          tipo = TipoMovimiento.GASTO,
          monto = Money(100, Moneda.COP),
          fecha = LocalDate(2026, 10, 1),
          categoriaId = CATEGORIA_OTROS_ID,
          titulo = null,
          descripcion = null,
          etiquetaIds = listOf("tag"),
          createdAt = Instant.parse("2026-10-01T12:00:00Z"),
      )

  private class RecordingChangeClock(private val events: MutableList<String>) : ChangeClock {
    val storedMaxima = mutableListOf<DataOutcome<ChangeStamp?>>()

    override suspend fun prepare(
        storedMax: suspend () -> DataOutcome<ChangeStamp?>
    ): DataOutcome<Unit> {
      events += "prepare"
      val outcome = storedMax()
      storedMaxima += outcome
      return outcome.map {}
    }

    override fun next(): ChangeStamp = error("Bootstrap never issues a stamp")
  }

  private companion object {
    const val NODE = "0123456789abcdef"
  }

  private class FakeDeviceLocaleProvider(
      private val languageTag: String,
  ) : DeviceLocaleProvider {
    var calls = 0

    override fun currentLanguageTag(): String {
      calls += 1
      return languageTag
    }
  }

  private class FakePreferencesRepository(
      private val initializeResult: DataOutcome<Unit>,
      private val events: MutableList<String> = mutableListOf(),
  ) : AppPreferencesRepository {
    override val preferences: Flow<DataOutcome<AppPreferencesState>> = emptyFlow()
    val initializedWith = mutableListOf<Moneda>()

    override suspend fun initialize(monedaPrincipal: Moneda): DataOutcome<Unit> {
      events += "preferences"
      initializedWith += monedaPrincipal
      return initializeResult
    }

    override suspend fun setThemeMode(themeMode: ThemeMode): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    override suspend fun setAppLocale(appLocale: AppLocale?): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    override suspend fun setMonedaPrincipal(moneda: Moneda): DataOutcome<Unit> =
        DataOutcome.Success(Unit)
  }

  private class FakeCategoriaRepository(
      private val seedResult: DataOutcome<Unit>,
      private val events: MutableList<String> = mutableListOf(),
  ) : CategoriaRepository by UnsupportedCategoriaRepository {
    var seedCalls = 0
    val seededCategorias = mutableListOf<List<Categoria>>()

    override fun observeAll(): Flow<DataOutcome<List<Categoria>>> =
        flowOf(DataOutcome.Success(emptyList()))

    override suspend fun getAll(): DataOutcome<List<Categoria>> = DataOutcome.Success(emptyList())

    override suspend fun localizeSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    override suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit> {
      events += "seed"
      seedCalls += 1
      seededCategorias += categorias
      return seedResult
    }
  }
}
