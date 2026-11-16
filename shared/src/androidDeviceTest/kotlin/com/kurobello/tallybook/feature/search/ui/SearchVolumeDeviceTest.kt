package com.kurobello.tallybook.feature.search.ui

import android.content.Context
import android.os.Debug
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.kurobello.tallybook.core.data.ActividadRepository
import com.kurobello.tallybook.core.data.ActividadRepositoryImpl
import com.kurobello.tallybook.core.data.AppPreferencesRepositoryImpl
import com.kurobello.tallybook.core.data.CategoriaRepositoryImpl
import com.kurobello.tallybook.core.data.CategorySeedProvider
import com.kurobello.tallybook.core.data.FakeCuentaRepository
import com.kurobello.tallybook.core.data.FakeEtiquetaRepository
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.PreferencesDataStoreFactory
import com.kurobello.tallybook.core.data.PreferencesFile
import com.kurobello.tallybook.core.data.PreferencesPathProvider
import com.kurobello.tallybook.core.data.TestChangeClock
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.data.insertCuentaRow
import com.kurobello.tallybook.core.data.insertTransferenciaRow
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.Actividad
import com.kurobello.tallybook.core.model.ActividadRecentOrder
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import com.kurobello.tallybook.core.model.SearchTipo
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.Transferencia
import com.kurobello.tallybook.core.ui.layout.TallybookAppShell
import com.kurobello.tallybook.core.ui.navigation.Search
import com.kurobello.tallybook.core.ui.navigation.TallybookBottomNavigation
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.search.viewmodel.SearchDatePreset
import com.kurobello.tallybook.feature.search.viewmodel.SearchDateProvider
import com.kurobello.tallybook.feature.search.viewmodel.SearchFilters
import com.kurobello.tallybook.feature.search.viewmodel.SearchRepositories
import com.kurobello.tallybook.feature.search.viewmodel.SearchViewModel
import com.kurobello.tallybook.feature.search.viewmodel.buildSearchCandidates
import com.kurobello.tallybook.feature.search.viewmodel.matchSearchCandidates
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.TimeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.search_title
import tallybook.shared.generated.resources.unknown_category

@OptIn(ExperimentalResourceApi::class)
class SearchVolumeDeviceTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun allYearAndSparseTextRemainCompleteAndScrollableInTestOwnedStorage() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    check(context.packageName == "com.kurobello.tallybook.shared.test") {
      "Volume storage must belong to the self-targeted test APK"
    }
    setResourceReaderAndroidContext(context)
    val metrics = JSONObject().put("fixtureRows", FIXTURE_ROWS).put("complete", false)
    val samples = JSONArray()
    val scenarios = JSONArray()
    metrics.put("memorySamples", samples).put("scenarios", scenarios)
    samples.put(memorySample("beforeFixture"))
    val fixture = VolumeFixture(context)
    val visible = mutableStateOf(true)
    var composed = false
    try {
      bounded { fixture.startAndSeed() }
      samples.put(memorySample("afterSeed"))
      var clearancePixels = 0f
      composeRule.setContent {
        if (visible.value) {
          val viewModel = requireNotNull(fixture.viewModel)
          val state by viewModel.uiState.collectAsState()
          TallybookTheme {
            TallybookAppShell(
                modifier = Modifier.fillMaxSize(),
                bottomBar = { TallybookBottomNavigation({}, selectedDestination = Search) },
            ) { clearance ->
              clearancePixels = with(LocalDensity.current) { clearance.toPx() }
              SearchScreen(state, viewModel, {}, {}, clearance)
            }
          }
        }
      }
      composed = true
      val initial = SearchFilters.initial(today)
      val all = bounded { fixture.measure(initial, "") }
      assertEquals(1500 + FIXTURE_TRANSFERS, all.rows.size)
      scenarios.put(all.metrics.put("case", "all"))
      scrollCheckpoints(all.rows, clearancePixels)
      samples.put(memorySample("afterAllScroll"))

      val year = initial.copy(preset = SearchDatePreset.ThisYear)
      val yearSettled = bounded { fixture.apply(year, "") }
      val thisYear = bounded { fixture.measure(year, "") }
      assertEquals(1200 + FIXTURE_TRANSFERS / 2, thisYear.rows.size)
      scenarios.put(thisYear.metrics.put("case", "thisYear").put("settleMs", yearSettled))
      scrollCheckpoints(thisYear.rows, clearancePixels)

      val sparse = year.copy(tipo = SearchTipo.GASTO, categoryIds = setOf("cat_comida"))
      val sparseSettled = bounded { fixture.apply(sparse, "needle") }
      val selected = bounded { fixture.measure(sparse, "needle") }
      assertEquals(19, selected.rows.size)
      scenarios.put(selected.metrics.put("case", "sparseText").put("settleMs", sparseSettled))
      scrollCheckpoints(selected.rows, clearancePixels)
      val startsBeforeText = fixture.subscriptions.starts.get()
      metrics.put("rapidTextMainQueueMs", bounded { fixture.rapidText() })
      assertEquals(startsBeforeText, fixture.subscriptions.starts.get())
      assertEquals(3, startsBeforeText)
      assertEquals(1, fixture.subscriptions.active.get())
      metrics.put("subscriptionStarts", startsBeforeText)
      metrics.put("subscriptionStopsBeforeClose", fixture.subscriptions.stops.get())
      metrics.put("subscriptionsBeforeClose", fixture.subscriptions.active.get())
      samples.put(memorySample("afterSparseScroll"))
    } finally {
      if (composed) detach(visible)
      try {
        fixture.close()
      } finally {
        metrics.put("subscriptionsAfterClose", fixture.subscriptions.active.get())
        metrics.put("subscriptionStopsAfterClose", fixture.subscriptions.stops.get())
        samples.put(memorySample("afterClose"))
        context.filesDir.resolve("search-volume.json").writeText(metrics.toString())
      }
    }
    assertEquals(0, fixture.subscriptions.active.get())
    metrics.put("complete", true)
    context.filesDir.resolve("search-volume.json").writeText(metrics.toString())
  }

  private fun detach(visible: MutableState<Boolean>) {
    composeRule.runOnIdle { visible.value = false }
    composeRule.waitForIdle()
  }

  // Checkpoints land on movimiento rows, whose titles are unique; transfers share their route.
  private fun scrollCheckpoints(results: List<SearchActividadRowUi>, clearancePixels: Float) {
    val rows = results.filterIsInstance<SearchResultRowUi>()
    assertTrue(rows.isNotEmpty())
    val title = bounded { getString(Res.string.search_title) }
    val list = composeRule.onNode(hasContentDescription(title) and hasScrollAction())
    for (row in listOf(rows.first(), rows[rows.size / 2], rows.last()).distinctBy { it.id }) {
      list.performScrollToKey("search-row:${row.id}")
      composeRule.waitForIdle()
      composeRule.onNodeWithText(row.title).assertIsDisplayed()
    }
    list.performTouchInput { swipeUp() }
    // Transfers can sort after the last movimiento, so the list's own last item is the one that
    // must clear the bottom bar once the end is reached.
    val range = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
    assertEquals(range.maxValue(), range.value())
    val lastItem = list.onChildren().onLast().assertIsDisplayed()
    assertTrue(clearancePixels > 0f)
    val bottom = lastItem.fetchSemanticsNode().boundsInRoot.bottom
    assertTrue(list.fetchSemanticsNode().boundsInRoot.bottom - bottom >= clearancePixels)
  }

  private class VolumeFixture(private val context: Context) {
    private val name = "search-volume-${UUID.randomUUID()}.db"
    private val directory =
        context.cacheDir.resolve(name.removeSuffix(".db")).also { check(it.mkdirs()) }
    private val driver = AndroidSqliteDriver(TallybookDatabase.Schema, context, name)
    private val database = TallybookDatabase(driver)
    private val factory =
        PreferencesDataStoreFactory(
            object : PreferencesPathProvider {
              override fun path(file: PreferencesFile) =
                  directory.resolve(file.fileName).absolutePath
            },
            Dispatchers.IO,
        )
    private val preferences = AppPreferencesRepositoryImpl(factory.create())
    private val actividades = ActividadRepositoryImpl(database, Dispatchers.IO)
    private val categorias =
        CategoriaRepositoryImpl(
            database,
            Dispatchers.IO,
            IdGenerator { error("Not used by this test") },
            TestChangeClock(),
        )
    val subscriptions = CountingActividades(actividades)
    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + Dispatchers.Main.immediate)
    private val store = ViewModelStore()
    private var collector: Job? = null
    private var fixtureRows = (0 until FIXTURE_ROWS).map(::fixtureRow)
    private var fixtureTransfers = (0 until FIXTURE_TRANSFERS).map(::fixtureTransfer)
    var viewModel: SearchViewModel? = null
      private set

    suspend fun startAndSeed() {
      assertEquals(DataOutcome.Success(Unit), preferences.initialize(Moneda.COP))
      assertEquals(
          DataOutcome.Success(Unit),
          categorias.ensureSeeded(CategorySeedProvider().getCategories()),
      )
      withContext(Dispatchers.IO) {
        database.transaction {
          fixtureCuentas.forEach { database.insertCuentaRow(it, ChangeStamp.SEED) }
          fixtureTransfers.forEach { database.insertTransferenciaRow(it, ChangeStamp.SEED) }
          for (row in fixtureRows) database.movimientoQueries.insertMovimiento(
              row.id,
              row.tipo.name,
              row.monto.minorUnits,
              row.fecha.toString(),
              row.titulo,
              row.descripcion,
              row.createdAt.toString(),
              row.categoriaId,
              Cuenta.defaultCashId(row.monto.moneda),
              "EFECTIVO",
              ChangeStamp.SEED.value,
          )
        }
      }
      val fallback = getString(Res.string.unknown_category)
      InstrumentationRegistry.getInstrumentation().runOnMainSync {
        viewModel =
            SearchViewModel(
                SearchRepositories(
                    preferences,
                    subscriptions,
                    categorias,
                    FakeEtiquetaRepository(),
                    FakeCuentaRepository(fixtureCuentas),
                ),
                SavedStateHandle(),
                fallback,
                SearchDateProvider(
                    object : Clock {
                      override fun now() = Instant.parse("2026-10-06T12:00:00Z")
                    },
                    TimeZoneProvider { TimeZone.UTC },
                ),
                Dispatchers.Default,
                scope,
            )
        store.put("volume", requireNotNull(viewModel))
        collector = scope.launch { requireNotNull(viewModel).uiState.collect {} }
      }
      await(SearchFilters.initial(today), "")
    }

    suspend fun apply(filters: SearchFilters, query: String): Double {
      val started = TimeSource.Monotonic.markNow()
      InstrumentationRegistry.getInstrumentation().runOnMainSync {
        val viewModel = requireNotNull(viewModel)
        val session = requireNotNull(viewModel.onOpenFilters())
        viewModel.onFilterDraftChanged(session, filters)
        viewModel.onQueryChanged(query)
        viewModel.onApplyFilters(session)
        viewModel.onFiltersHidden(session)
      }
      await(filters, query)
      return started.elapsedNow().inWholeNanoseconds / 1_000_000.0
    }

    private suspend fun await(filters: SearchFilters, query: String): SearchResultsUi {
      val state =
          requireNotNull(viewModel).uiState.first {
            it.filters == filters && it.rawQuery == query && it.isCurrent
          }
      val content = requireNotNull(state.displayResults)
      assertEquals(expected(filters, query).map { it.id }, content.rows.map { it.id })
      assertEquals(content.rows.size, state.currentCount)
      return content
    }

    suspend fun measure(filters: SearchFilters, query: String): Measurement {
      val state = requireNotNull(viewModel).uiState.value
      assertEquals(filters, state.filters)
      val criteria = requireNotNull(state.criteria)
      val started = TimeSource.Monotonic.markNow()
      val snapshot =
          assertIs<DataOutcome.Success<MovimientoSearchSnapshot>>(
                  actividades.observeFiltered(criteria).first()
              )
              .data
      val readMs = started.elapsedNow().inWholeNanoseconds / 1_000_000.0
      assertEquals(setOf(2026, 2024), snapshot.persistedYears.toSet())
      val categories =
          assertIs<DataOutcome.Success<List<com.kurobello.tallybook.core.model.Categoria>>>(
                  categorias.getAll()
              )
              .data
      val fallback = getString(Res.string.unknown_category)
      val candidatesStarted = TimeSource.Monotonic.markNow()
      val candidates =
          withContext(Dispatchers.Default) {
            buildSearchCandidates(
                snapshot.actividades,
                categories,
                emptyList(),
                fallback,
                fixtureCuentas,
            )
          }
      val candidateMs = candidatesStarted.elapsedNow().inWholeNanoseconds / 1_000_000.0
      val matchStarted = TimeSource.Monotonic.markNow()
      val rows = withContext(Dispatchers.Default) { matchSearchCandidates(candidates, query) }
      val matchMs = matchStarted.elapsedNow().inWholeNanoseconds / 1_000_000.0
      assertEquals(expected(filters, query).map { it.id }, rows.map { it.id })
      assertEquals(rows.map { it.id }, await(filters, query).rows.map { it.id })
      return Measurement(
          rows,
          JSONObject()
              .put("structuralRows", snapshot.actividades.size)
              .put("transfers", snapshot.actividades.size - snapshot.movimientos.size)
              .put("count", rows.size)
              .put("readMs", readMs)
              .put("candidateMs", candidateMs)
              .put("matchMs", matchMs)
              .put("scrollCheckpoints", 3),
      )
    }

    suspend fun rapidText(): JSONArray {
      val timings = JSONArray()
      val viewModel = requireNotNull(viewModel)
      val starts = subscriptions.starts.get()
      val marker = CompletableDeferred<Double>()
      InstrumentationRegistry.getInstrumentation().runOnMainSync {
        val started = TimeSource.Monotonic.markNow()
        for (query in listOf("n", "ne", "need", "needle volume")) {
          viewModel.onQueryChanged(query)
          assertEquals(null, viewModel.uiState.value.currentCount)
        }
        Handler(Looper.getMainLooper()).post {
          marker.complete(started.elapsedNow().inWholeNanoseconds / 1_000_000.0)
        }
      }
      timings.put(marker.await())
      await(viewModel.uiState.value.filters, "needle volume")
      assertEquals(starts, subscriptions.starts.get())
      return timings
    }

    private fun expected(filters: SearchFilters, query: String): List<Actividad> {
      val inDates = { fecha: LocalDate ->
        filters.preset == SearchDatePreset.All || fecha.year == today.year
      }
      val movimientos = fixtureRows.filter { row ->
        row.monto.moneda == Moneda.COP &&
            inDates(row.fecha) &&
            (filters.tipo == null || row.tipo == filters.tipo?.tipoMovimiento) &&
            (filters.categoryIds.isEmpty() || row.categoriaId in filters.categoryIds) &&
            (query.isEmpty() || requireNotNull(row.titulo).contains("Needle"))
      }
      val transfers = fixtureTransfers.filter {
        filters.categoryIds.isEmpty() &&
            (filters.tipo == null || filters.tipo == SearchTipo.TRANSFERENCIA) &&
            inDates(it.fecha) &&
            query.isEmpty()
      }
      return (movimientos.map(Actividad::DeMovimiento) +
              transfers.mapNotNull { Actividad.DeTransferencia.of(it, Moneda.COP) })
          .sortedWith(ActividadRecentOrder)
    }

    fun close() {
      bounded {
        collector?.cancelAndJoin()
        scopeJob.cancelAndJoin()
      }
      InstrumentationRegistry.getInstrumentation().runOnMainSync {
        store.clear()
        viewModel = null
      }
      factory.close()
      driver.close()
      fixtureRows = emptyList()
      fixtureTransfers = emptyList()
      check(name.startsWith("search-volume-"))
      if (context.getDatabasePath(name).exists()) check(context.deleteDatabase(name))
      check(directory.deleteRecursively())
    }
  }

  private class CountingActividades(private val delegate: ActividadRepository) :
      ActividadRepository by delegate {
    val starts = AtomicInteger()
    val stops = AtomicInteger()
    val active = AtomicInteger()

    override fun observeFiltered(
        criteria: MovimientoReadCriteria
    ): Flow<DataOutcome<MovimientoSearchSnapshot>> = flow {
      starts.incrementAndGet()
      active.incrementAndGet()
      try {
        emitAll(delegate.observeFiltered(criteria))
      } finally {
        active.decrementAndGet()
        stops.incrementAndGet()
      }
    }
  }

  private data class Measurement(val rows: List<SearchActividadRowUi>, val metrics: JSONObject)

  private companion object {
    const val FIXTURE_ROWS = 2000
    const val FIXTURE_TRANSFERS = 200
    const val FIXTURE_BANK = "volume-bank"
    val fixtureCuentas =
        listOf(
            fixtureCuenta(Cuenta.defaultCashId(Moneda.COP), TipoCuenta.EFECTIVO, Moneda.COP),
            fixtureCuenta(Cuenta.defaultCashId(Moneda.USD), TipoCuenta.EFECTIVO, Moneda.USD),
            fixtureCuenta(FIXTURE_BANK, TipoCuenta.BANCO, Moneda.COP),
        )

    fun fixtureCuenta(id: String, tipo: TipoCuenta, moneda: Moneda) =
        Cuenta(
            id,
            id,
            tipo,
            Money(0, moneda),
            null,
            false,
            Instant.parse("2026-01-01T00:00:00Z"),
            color = tipo.defaultColor,
        )

    // Half in each year, all between the COP accounts, so every one belongs to the COP results.
    fun fixtureTransfer(index: Int): Transferencia =
        Transferencia(
            "transfer-${index.toString().padStart(4, '0')}",
            Cuenta.defaultCashId(Moneda.COP),
            FIXTURE_BANK,
            Money(500, Moneda.COP),
            Money(500, Moneda.COP),
            LocalDate(if (index % 2 == 0) 2026 else 2024, 9, index % 28 + 1),
            "Ahorro $index",
            Instant.parse("2026-09-15T10:00:00Z"),
        )

    val today = LocalDate(2026, 10, 6)

    fun fixtureRow(index: Int): Movimiento {
      val id = "volume-${index.toString().padStart(4, '0')}"
      return Movimiento(
          id,
          if (index % 2 == 0) TipoMovimiento.GASTO else TipoMovimiento.INGRESO,
          Money(100, if (index % 4 == 0) Moneda.USD else Moneda.COP),
          LocalDate(if (index % 5 == 0) 2024 else 2026, 9, index % 30 + 1),
          if (index % 7 == 0) "cat_comida" else "cat_transporte",
          if (index % 3 == 0) "Needle volume $id" else "Volume $id",
          null,
          Instant.parse(if (index % 2 == 0) "2026-09-15T12:00:00Z" else "2026-09-15T11:00:00Z"),
          Cuenta.defaultCashId(Moneda.USD),
          MedioPago.EFECTIVO,
      )
    }

    fun memorySample(phase: String): JSONObject {
      val runtime = Runtime.getRuntime()
      return JSONObject()
          .put("phase", phase)
          .put("javaHeapBytes", runtime.totalMemory() - runtime.freeMemory())
          .put("pssKb", Debug.MemoryInfo().also(Debug::getMemoryInfo).totalPss)
    }

    fun <T> bounded(block: suspend () -> T): T = runBlocking { withTimeout(30_000) { block() } }
  }
}
