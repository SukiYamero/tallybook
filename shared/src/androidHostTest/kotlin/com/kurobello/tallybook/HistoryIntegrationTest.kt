package com.kurobello.tallybook

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.AppPreferencesRepositoryImpl
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategoriaRepositoryImpl
import com.kurobello.tallybook.core.data.CategorySeedProvider
import com.kurobello.tallybook.core.data.DeviceLocaleProvider
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.data.EtiquetaRepositoryImpl
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.LocalDataBootstrapper
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.MovimientoRepositoryImpl
import com.kurobello.tallybook.core.data.PreferencesDataStoreFactory
import com.kurobello.tallybook.core.data.PreferencesFile
import com.kurobello.tallybook.core.data.PreferencesPathProvider
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.data.insertLegacy
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.di.DispatcherQualifier
import com.kurobello.tallybook.di.entryModule
import com.kurobello.tallybook.di.viewModelModule
import com.kurobello.tallybook.feature.history.viewmodel.HistoryFailure
import com.kurobello.tallybook.feature.history.viewmodel.HistoryProjection
import com.kurobello.tallybook.feature.history.viewmodel.HistoryScope
import com.kurobello.tallybook.feature.history.viewmodel.HistorySelection
import com.kurobello.tallybook.feature.history.viewmodel.HistoryViewModel
import com.kurobello.tallybook.feature.home.HomeViewModel
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoEffect
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoViewModel
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormError
import java.nio.file.Path
import java.sql.Driver
import java.sql.DriverManager
import kotlin.io.path.createTempDirectory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.KoinApplication
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.viewmodel.defaultExtras
import org.koin.viewmodel.resolveViewModel
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class, KoinInternalApi::class)
@RunWith(RobolectricTestRunner::class)
class HistoryIntegrationTest {
  @Test
  fun `real forms invalidate both screens and survive a closed file reopen`() = runTest {
    withDiskFixture {
      val first = open(createSchema = true)
      first.start()
      val selection = first.history.uiState.value.selection
      assertEquals(HistorySelection(HistoryScope.Day, TODAY), selection)
      assertEquals(0, first.period.movementCount)
      assertTrue(first.home.uiState.value.isEmpty)
      first.submit("expense", "10,50", TipoMovimiento.GASTO, TODAY)
      first.submit("income", "20,00", TipoMovimiento.INGRESO, TODAY)
      assertEquals(summary(2000, 1050), first.home.uiState.value.summary)
      assertEquals(summary(2000, 1050), first.period.summary)
      assertEquals(listOf("income", "expense"), first.period.groups.single().rows.map { it.id })
      assertEquals(TODAY, first.period.groups.single().fecha)
      assertEquals(2, first.period.movementCount)
      first.submit("outside", "5,00", TipoMovimiento.GASTO, LocalDate(2026, 9, 14))
      assertEquals(summary(2000, 1550), first.home.uiState.value.summary)
      assertEquals(summary(2000, 1050), first.period.summary)
      assertEquals(2, first.period.movementCount)
      assertEquals(selection, first.history.uiState.value.selection)
      val firstHome = first.home.uiState.value
      val firstHistory = first.history.uiState.value
      first.close()
      assertEquals(0, first.driver.listeners)
      assertTrue(first.collectors.all { it.isCompleted })

      val reopened = open()
      reopened.start()
      assertNotSame(first.driver, reopened.driver)
      assertNotSame(first.home, reopened.home)
      assertNotSame(first.history, reopened.history)
      assertEquals(
          setOf("expense", "income", "outside"),
          assertSuccess(reopened.movimientos.getAll()).map { it.id }.toSet(),
      )
      assertEquals(summary(2000, 1550), reopened.home.uiState.value.summary)
      assertEquals(summary(2000, 1050), reopened.period.summary)
      assertEquals(2, reopened.period.movementCount)
      reopened.submit("after-reopen", "1,00", TipoMovimiento.INGRESO, TODAY)
      assertEquals(summary(2100, 1550), reopened.home.uiState.value.summary)
      assertEquals(3, reopened.period.movementCount)
      assertEquals(firstHome, first.home.uiState.value)
      assertEquals(firstHistory, first.history.uiState.value)
    }
  }

  @Test
  fun `six newer foreign rows cannot hide Home primary currency movements`() = runTest {
    withDiskFixture {
      clock.instant = Instant.parse("2026-09-20T12:00:00Z")
      val graph = open(createSchema = true)
      graph.start()
      graph.history.onPeriodSelected(MovimientoDateRange(TODAY, TODAY))
      runCurrent()
      graph.submit("expense", "10,50", TipoMovimiento.GASTO, TODAY)
      graph.submit("income", "20,00", TipoMovimiento.INGRESO, TODAY)
      val selected = graph.history.uiState.value.selection
      val period = graph.period
      repeat(6) { index ->
        assertEquals(
            DataOutcome.Success(Unit),
            graph.movimientos.insert(row("foreign-$index", LocalDate(2026, 9, 20), Moneda.USD)),
        )
      }
      runCurrent()
      assertEquals(listOf("income", "expense"), graph.home.uiState.value.recent.map { it.id })
      assertFalse(graph.home.uiState.value.isEmpty)
      assertEquals(summary(2000, 1050), graph.home.uiState.value.summary)
      assertEquals(period, graph.period)
      assertEquals(selected, graph.history.uiState.value.selection)
      val sql = requireNotNull(graph.driver.scopedRecentSql)
      val plan =
          graph.driver
              .executeQuery(
                  null,
                  "EXPLAIN QUERY PLAN $sql",
                  { cursor ->
                    val details = mutableListOf<String>()
                    while (cursor.next().value) details += requireNotNull(cursor.getString(3))
                    QueryResult.Value(details)
                  },
                  2,
              ) {
                bindString(0, "COP")
                bindLong(1, 6L)
              }
              .value
      assertTrue(plan.any { "movimiento_recent" in it })
      assertFalse(plan.any { "TEMP B-TREE FOR ORDER BY" in it })
      println("Scoped Home recent actual query plan: $plan")
    }
  }

  @Test
  fun `real metadata category invalidation and resume preserve historical selection`() = runTest {
    withDiskFixture {
      val graph = open(createSchema = true)
      graph.start()
      graph.insertLegacy(row("missing", TODAY, category = "later-category", title = null))
      runCurrent()
      assertEquals("Unknown category", graph.period.groups.single().rows.single().title)
      val originalSummary = graph.period.summary
      graph.history.onPresentationChanged("Sin categoría")
      runCurrent()
      assertEquals("Sin categoría", graph.period.groups.single().rows.single().title)
      assertEquals(
          DataOutcome.Success(Unit),
          graph.categorias.ensureSeeded(
              listOf(
                  Categoria(
                      "later-category",
                      "Recovered category",
                      null,
                      CategoryIcon.UTENSILS,
                      CategoryTint.AMBER,
                  )
              )
          ),
      )
      runCurrent()
      assertEquals("Recovered category", graph.period.groups.single().rows.single().title)
      assertEquals(originalSummary, graph.period.summary)
      assertEquals(
          DataOutcome.Success(Unit),
          graph.movimientos.insert(row("prior-year", LocalDate(2024, 9, 15))),
      )
      runCurrent()
      assertEquals(
          listOf(2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019),
          graph.history.uiState.value.availableYears,
      )
      graph.history.onYearSelected(2024)
      runCurrent()
      val historical = graph.history.uiState.value.selection
      assertEquals(1, graph.period.movementCount)
      assertEquals(DataOutcome.Success(Unit), graph.movimientos.delete("prior-year"))
      runCurrent()
      assertEquals(0, graph.period.movementCount)
      assertEquals(historical, graph.history.uiState.value.selection)
      assertTrue(2024 in graph.history.uiState.value.availableYears)
      graph.history.onYearSelected(2026)
      runCurrent()
      assertTrue(2024 in graph.history.uiState.value.availableYears)
      assertEquals(DataOutcome.Success(Unit), graph.movimientos.delete("missing"))
      runCurrent()
      assertEquals(emptyList(), graph.history.uiState.value.years.persisted)
      assertEquals(
          listOf(2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019),
          graph.history.uiState.value.availableYears,
      )
      graph.history.onYearSelected(2024)
      runCurrent()
      clock.instant = Instant.parse("2027-01-01T00:30:00Z")
      clock.zone = TimeZone.of("America/Bogota")
      graph.history.onResume()
      runCurrent()
      assertEquals(LocalDate(2026, 12, 31), graph.history.uiState.value.today)
      assertEquals(historical, graph.history.uiState.value.selection)
      clock.zone = TimeZone.UTC
      graph.history.onResume()
      runCurrent()
      assertEquals(LocalDate(2027, 1, 1), graph.history.uiState.value.today)
      assertEquals(historical, graph.history.uiState.value.selection)
      assertEquals(
          listOf(2027, 2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019),
          graph.history.uiState.value.availableYears,
      )
    }
  }

  @Test
  fun `storage failure preserves form draft and History selection until explicit retry`() =
      runTest {
        withDiskFixture {
          val graph = open(createSchema = true)
          graph.start()
          val form = graph.form("retry")
          form.viewModel.onAmountChanged("10,50")
          form.viewModel.onCategoriaSelected("cat_comida")
          form.viewModel.onTituloChanged("Lunch")
          graph.driver.failWrites = true
          form.viewModel.onSubmit()
          runCurrent()
          assertEquals(MovimientoFormError.SAVE_FAILED, form.viewModel.uiState.value.error)
          assertEquals("10,50", form.viewModel.uiState.value.amountInput)
          assertEquals(TODAY, form.viewModel.uiState.value.fecha)
          assertEquals("cat_comida", form.viewModel.uiState.value.categoriaId)
          assertEquals("Lunch", form.viewModel.uiState.value.titulo)
          assertEquals(emptyList(), form.effects)
          assertEquals(0, graph.period.movementCount)
          graph.driver.failWrites = false
          form.viewModel.onSubmit()
          form.viewModel.onSubmit()
          runCurrent()
          assertEquals(listOf(AddMovimientoEffect.Saved), form.effects)
          assertEquals(listOf("retry"), assertSuccess(graph.movimientos.getAll()).map { it.id })
          val selected = graph.history.uiState.value.selection
          val previousPeriod = graph.period
          graph.driver.failRangeReads = true
          graph.history.onRetry()
          runCurrent()
          val failed =
              assertIs<RetainedLoadState.Failed<HistoryProjection.Content, HistoryFailure>>(
                  graph.history.uiState.value.period
              )
          assertIs<DataError.Local>(assertIs<HistoryFailure.Repository>(failed.failure).error)
          assertEquals(previousPeriod, failed.previous)
          assertEquals(selected, graph.history.uiState.value.selection)
          assertEquals(summary(0, 1050), graph.home.uiState.value.summary)
          graph.driver.failRangeReads = false
          graph.history.onRetry()
          runCurrent()
          assertEquals(1, graph.period.movementCount)
          assertEquals(summary(0, 1050), graph.period.summary)
          assertEquals(selected, graph.history.uiState.value.selection)
          assertEquals(listOf(AddMovimientoEffect.Saved), form.effects)
        }
      }

  private suspend fun TestScope.withDiskFixture(block: suspend DiskFixture.() -> Unit) {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)
    // DriverManager discovery must also retain the parent loader's driver for later plain JVM
    // tests.
    val sandboxLoader = requireNotNull(javaClass.classLoader)
    val parentLoader = requireNotNull(sandboxLoader.parent)
    val parentJdbc = Class.forName("org.sqlite.JDBC", true, parentLoader)
    println(
        "History JDBC fixture loader=${sandboxLoader.javaClass.name} parentDriver=${requireNotNull(parentJdbc.classLoader).javaClass.name}"
    )
    val jdbc =
        Class.forName("org.sqlite.JDBC")
            .asSubclass(Driver::class.java)
            .getDeclaredConstructor()
            .newInstance()
    DriverManager.registerDriver(jdbc)
    val fixture =
        DiskFixture(this, createTempDirectory("tallybook-history-integration-"), dispatcher)
    try {
      fixture.block()
    } finally {
      try {
        fixture.graphs.forEach { it.close() }
      } finally {
        fixture.directory.toFile().deleteRecursively()
        DriverManager.deregisterDriver(jdbc)
        Dispatchers.resetMain()
      }
    }
  }

  private class DiskFixture(
      val test: TestScope,
      val directory: Path,
      val dispatcher: CoroutineDispatcher,
  ) {
    val clock = MutableClock()
    val graphs = mutableListOf<Graph>()

    fun open(createSchema: Boolean = false) = Graph(this, createSchema).also(graphs::add)
  }

  private class Graph(private val fixture: DiskFixture, createSchema: Boolean) {
    val driver =
        ControlledDriver(JdbcSqliteDriver("jdbc:sqlite:${fixture.directory.resolve("history.db")}"))
    private val database = TallybookDatabase(driver)

    fun insertLegacy(movimiento: Movimiento) = database.insertLegacy(movimiento)

    private val factory =
        PreferencesDataStoreFactory(
            object : PreferencesPathProvider {
              override fun path(file: PreferencesFile) =
                  fixture.directory.resolve(file.fileName).toString()
            },
            fixture.dispatcher,
        )
    private val preferences = AppPreferencesRepositoryImpl(factory.create())
    val categorias =
        CategoriaRepositoryImpl(
            database,
            fixture.dispatcher,
            IdGenerator { error("Not used by this test") },
        )
    val movimientos = MovimientoRepositoryImpl(database, fixture.dispatcher)
    private val etiquetas =
        EtiquetaRepositoryImpl(
            database,
            fixture.dispatcher,
            IdGenerator { error("Not used by this test") },
            fixture.clock,
        )
    private var nextId = "unset"
    private val owners = mutableListOf<GraphOwner>()
    val collectors = mutableListOf<Job>()
    private var closed = false
    private val context: KoinApplication
    val home: HomeViewModel
    val history: HistoryViewModel
    val period
      get() =
          assertIs<RetainedLoadState.Ready<HistoryProjection.Content>>(history.uiState.value.period)
              .value

    init {
      if (createSchema) TallybookDatabase.Schema.create(driver)
      context = koinApplication {
        modules(
            viewModelModule,
            entryModule,
            module {
              single<DeviceLocaleProvider> {
                object : DeviceLocaleProvider {
                  override fun currentLanguageTag() = "es-CO"
                }
              }
              single<AppPreferencesRepository> { preferences }
              single<MovimientoRepository> { movimientos }
              single<CategoriaRepository> { categorias }
              single<EtiquetaRepository> { etiquetas }
              single<Clock> { fixture.clock }
              single<TimeZoneProvider> { TimeZoneProvider { fixture.clock.zone } }
              single<IdGenerator> { IdGenerator { nextId } }
              single<CoroutineDispatcher>(named(DispatcherQualifier.DEFAULT)) { fixture.dispatcher }
            },
        )
      }
      home = model()
      history = model()
    }

    suspend fun start() {
      val bootstrap =
          LocalDataBootstrapper(
              object : DeviceLocaleProvider {
                override fun currentLanguageTag() = "es-CO"
              },
              preferences,
              categorias,
              CategorySeedProvider { it.key },
          )
      assertEquals(DataOutcome.Success(Unit), bootstrap.initialize())
      assertEquals(
          DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(Moneda.COP))),
          preferences.preferences.first(),
      )
      assertEquals(59, assertSuccess(categorias.getAll()).size)
      history.onPresentationChanged("Unknown category")
      history.onResume()
      collectors += fixture.test.backgroundScope.launch { home.uiState.collect {} }
      collectors += fixture.test.backgroundScope.launch { history.uiState.collect {} }
      fixture.test.runCurrent()
      assertFalse(home.uiState.value.isLoading)
      assertIs<RetainedLoadState.Ready<*>>(history.uiState.value.period)
    }

    private inline fun <reified T : ViewModel> model(): T {
      val owner = GraphOwner().also(owners::add)
      return resolveViewModel(
          T::class,
          owner.viewModelStore,
          extras = defaultExtras(owner.viewModelOwner),
          scope = context.koin.scopeRegistry.rootScope,
          parameters = { parametersOf("Unknown category") },
      )
    }

    fun form(id: String): Form {
      nextId = id
      val vm: AddMovimientoViewModel = model()
      val effects = mutableListOf<AddMovimientoEffect>()
      collectors += fixture.test.backgroundScope.launch { vm.effects.collect(effects::add) }
      fixture.test.runCurrent()
      return Form(vm, effects)
    }

    fun submit(id: String, input: String, type: TipoMovimiento, date: LocalDate) {
      val form = form(id)
      form.viewModel.apply {
        onTipoChanged(type)
        onAmountChanged(input)
        onCategoriaSelected(if (type == TipoMovimiento.INGRESO) "cat_ingresos" else "cat_comida")
        onFechaChanged(date)
        onTituloChanged("Title $id")
        onSubmit()
      }
      fixture.test.runCurrent()
      assertEquals(listOf(AddMovimientoEffect.Saved), form.effects)
    }

    suspend fun close() {
      if (closed) return
      closed = true
      collectors.forEach { it.cancelAndJoin() }
      owners.forEach { it.close() }
      fixture.test.runCurrent()
      context.close()
      factory.close()
      fixture.test.runCurrent()
      try {
        assertEquals(0, driver.listeners)
      } finally {
        driver.close()
      }
    }
  }

  private data class Form(
      val viewModel: AddMovimientoViewModel,
      val effects: List<AddMovimientoEffect>,
  )

  private class GraphOwner : SavedStateRegistryOwner, ViewModelStoreOwner {
    override val lifecycle = LifecycleRegistry.createUnsafe(this)
    override val viewModelStore = ViewModelStore()
    private val controller = SavedStateRegistryController.create(this)
    override val savedStateRegistry = controller.savedStateRegistry
    val viewModelOwner: ViewModelStoreOwner

    init {
      controller.performRestore(null)
      viewModelOwner = androidx.lifecycle.viewmodel.ViewModelStoreOwner(viewModelStore, this)
      lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun close() {
      lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
      viewModelStore.clear()
    }
  }

  private class ControlledDriver(private val delegate: SqlDriver) : SqlDriver by delegate {
    var failRangeReads = false
    var failWrites = false
    var listeners = 0
      private set

    var scopedRecentSql: String? = null
      private set

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> {
      if (
          sql.trimStart().startsWith("SELECT") &&
              sql.contains("monto_moneda = ?") &&
              sql.contains("LIMIT")
      )
          scopedRecentSql = sql
      check(!failRangeReads || !sql.contains("fecha >=")) { "Test-owned range read failure" }
      return delegate.executeQuery(identifier, sql, mapper, parameters, binders)
    }

    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<Long> {
      check(!failWrites || !sql.startsWith("INSERT INTO movimiento")) { "Test-owned save failure" }
      return delegate.execute(identifier, sql, parameters, binders)
    }

    override fun addListener(vararg queryKeys: String, listener: Query.Listener) {
      delegate.addListener(*queryKeys, listener = listener)
      listeners++
    }

    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {
      delegate.removeListener(*queryKeys, listener = listener)
      listeners--
    }
  }

  private class MutableClock : Clock {
    var instant = Instant.parse("2026-09-15T12:00:00Z")
    var zone: TimeZone = TimeZone.UTC

    override fun now() = instant
  }

  private companion object {
    val TODAY = LocalDate(2026, 9, 15)

    fun summary(income: Long, expense: Long) =
        MovimientoSummary(Money(income, Moneda.COP), Money(expense, Moneda.COP))

    fun row(
        id: String,
        date: LocalDate,
        currency: Moneda = Moneda.COP,
        category: String = "cat_comida",
        title: String? = "Fixture row",
    ) =
        Movimiento(
            id = id,
            tipo = TipoMovimiento.GASTO,
            monto = Money(100, currency),
            fecha = date,
            categoriaId = category,
            titulo = title,
            descripcion = null,
            createdAt = Instant.parse("2026-09-20T12:00:00Z"),
        )

    fun <T> assertSuccess(outcome: DataOutcome<T>): T =
        assertIs<DataOutcome.Success<T>>(outcome).data
  }
}
