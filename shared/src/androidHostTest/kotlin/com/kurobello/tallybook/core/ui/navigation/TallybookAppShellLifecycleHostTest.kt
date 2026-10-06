package com.kurobello.tallybook.core.ui.navigation

import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.NavEntry
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.savedstate.savedState
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.di.DispatcherQualifier
import com.kurobello.tallybook.di.viewModelModule
import com.kurobello.tallybook.feature.history.ui.rememberHistoryRouteState
import com.kurobello.tallybook.feature.history.viewmodel.HistoryLoadState
import com.kurobello.tallybook.feature.history.viewmodel.HistoryScope
import com.kurobello.tallybook.feature.history.viewmodel.HistoryUiState
import com.kurobello.tallybook.feature.history.viewmodel.HistoryViewModel
import com.kurobello.tallybook.feature.home.HomeViewModel
import com.kurobello.tallybook.feature.home.rememberHomeRouteState
import java.io.Serializable as JavaSerializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.runner.RunWith
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.KoinApplication
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class TallybookAppShellLifecycleHostTest {
  @Test
  fun `actual primary owners retain VMs and inactive checkpoint restores in new owners`() =
      runTest {
        withShell {
          val first = create()
          val home = first.home
          assertFalse(first.homeLoading.last())
          assertEquals(1, repository.activeRecent)
          first.select(History)
          val history = first.history
          assertNotSame(first.owners.getValue(Home), first.owners.getValue(History))
          history.onYearSelected(2024)
          history.onScopeChanged(HistoryScope.Month)
          history.onPeriodSelected(
              MovimientoDateRange(LocalDate(2024, 2, 1), LocalDate(2024, 2, 29))
          )
          history.onScopeChanged(HistoryScope.Week)
          history.onPeriodSelected(
              MovimientoDateRange(LocalDate(2024, 2, 22), LocalDate(2024, 2, 28))
          )
          first.settle()
          first.scroll.getValue(History).requestScrollToItem(12, 7)
          first.select(Home)
          advanceTimeBy(5_000)
          runCurrent()
          assertEquals(0, repository.activeRanges)
          assertEquals(1, repository.activeRecent)
          val saved = first.saveAndParcel()
          first.close()
          assertEquals(0, repository.activeRecent)
          val restored = create(saved)
          assertEquals(Home, restored.navigation.value.selected)
          assertTrue(restored.navigation.value.historyVisited)
          assertNotSame(home, restored.home)
          restored.select(History)
          assertNotSame(history, restored.history)
          assertEquals(history.uiState.value.selection, restored.history.uiState.value.selection)
          assertEquals(12, restored.scroll.getValue(History).firstVisibleItemIndex)
          assertEquals(7, restored.scroll.getValue(History).firstVisibleItemScrollOffset)
          repeat(3) {
            restored.select(Home)
            restored.select(History)
          }
          assertEquals(2, restored.owners.size)
          assertSame(restored.history, restored.historyInstances.last())
          assertIs<HistoryLoadState.Loading<*>>(restored.firstHistoryStates.last().period)
          assertTrue(repository.rangeStarts > 1)
        }
      }

  @Test
  fun `Activity Intent extras retain actual entry stores across repeated tab recomposition`() =
      runTest {
        val controller =
            Robolectric.buildActivity(
                    ComponentActivity::class.java,
                    Intent().putExtra("ui_catalog", true),
                )
                .setup()
        try {
          withShell {
            val fixture = create(activity = controller.get())
            fixture.select(History)
            val history = fixture.history
            val home = fixture.home
            repeat(3) {
              fixture.select(Home)
              fixture.select(History)
            }
            assertSame(history, fixture.history)
            assertSame(home, fixture.home)
            assertNotSame(fixture.owners.getValue(Home), fixture.owners.getValue(History))
          }
        } finally {
          controller.pause().stop().destroy()
        }
      }

  @Test
  fun `removing actual History entry clears it once while Home survives`() = runTest {
    withShell {
      val fixture = create()
      assertEquals(setOf<Destination>(Home), fixture.owners.keys)
      val home = fixture.home
      fixture.select(History)
      val history = fixture.history
      val tracker = fixture.clearTrackers.getValue(fixture.owners.getValue(History))
      fixture.select(Home)
      fixture.navigation.value = fixture.navigation.value.copy(historyVisited = false)
      fixture.settle()
      assertEquals(1, tracker.cleared)
      assertEquals(0, repository.activeRanges)
      assertSame(home, fixture.home)
      fixture.select(History)
      assertNotSame(history, fixture.history)
      assertEquals(HistoryScope.Day, fixture.history.uiState.value.selection.scope)
      assertEquals(1, tracker.cleared)
    }
  }

  @Test
  fun `hidden write resume and locale change never first render cached success`() = runTest {
    withShell {
      val fixture = create()
      fixture.select(History)
      val selected = fixture.history.uiState.value.selection
      fixture.select(Home)
      advanceTimeBy(5_000)
      runCurrent()
      repository.rows.value = repository.rows.value + shellRow("hidden", LocalDate(2026, 10, 5))
      fixture.select(History)
      assertEquals(selected, fixture.firstHistoryStates.last().selection)
      assertIs<HistoryLoadState.Loading<*>>(fixture.firstHistoryStates.last().period)
      fixture.pause()
      assertIs<HistoryLoadState.Loading<*>>(fixture.renderedHistory.last().period)
      fixture.resume()
      fixture.label.value = "Sin categoría"
      fixture.settle()
      assertIs<HistoryLoadState.Loading<*>>(fixture.firstHistoryStates.last().period)
      assertEquals(
          "Sin categoría",
          (fixture.history.uiState.value.period as HistoryLoadState.Ready)
              .value
              .groups
              .first()
              .rows
              .first()
              .title,
      )
    }
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class ShellSuite(private val scope: TestScope) {
  val repository = ShellMovimientos()
  private val fixtures = mutableListOf<ShellHarness>()
  private val dispatcher = StandardTestDispatcher(scope.testScheduler)
  private val context: KoinApplication

  init {
    Dispatchers.setMain(dispatcher)
    context = koinApplication {
      modules(
          viewModelModule,
          module {
            single<AppPreferencesRepository> { ShellPreferences() }
            single<MovimientoRepository> { repository }
            single<CategoriaRepository> { ShellCategories() }
            single<Clock> {
              object : Clock {
                override fun now() = Instant.parse("2026-10-05T12:00:00Z")
              }
            }
            single<TimeZoneProvider> { TimeZoneProvider { TimeZone.UTC } }
            single<CoroutineDispatcher>(named(DispatcherQualifier.DEFAULT)) { dispatcher }
          },
      )
    }
  }

  fun create(snapshot: ShellSnapshot? = null, activity: ComponentActivity? = null) =
      ShellHarness(scope, context, snapshot, activity).also {
        fixtures += it
        it.settle()
      }

  suspend fun close() {
    try {
      fixtures.forEach { it.close() }
      assertEquals(0, repository.activeRanges)
      assertEquals(0, repository.activeRecent)
    } finally {
      context.close()
      Dispatchers.resetMain()
    }
  }
}

private suspend fun TestScope.withShell(block: suspend ShellSuite.() -> Unit) {
  val suite = ShellSuite(this)
  try {
    suite.block()
  } finally {
    suite.close()
  }
}

private data class ShellSnapshot(val owner: SavedState, val compose: Map<String, List<Any?>>)

private class ShellParent(restored: SavedState?) : SavedStateRegistryOwner, ViewModelStoreOwner {
  override val viewModelStore = ViewModelStore()
  override val lifecycle = LifecycleRegistry.createUnsafe(this)
  private val controller = SavedStateRegistryController.create(this)
  override val savedStateRegistry = controller.savedStateRegistry
  val viewModelOwner: ViewModelStoreOwner

  init {
    controller.performRestore(restored)
    viewModelOwner = androidx.lifecycle.viewmodel.ViewModelStoreOwner(viewModelStore, this)
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
  }

  fun save(): SavedState = savedState().also(controller::performSave)

  fun close() {
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    viewModelStore.clear()
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class ShellHarness(
    private val scope: TestScope,
    context: KoinApplication,
    snapshot: ShellSnapshot?,
    private val activity: ComponentActivity?,
) {
  private val parent = ShellParent(snapshot?.owner)
  private val registry =
      SaveableStateRegistry(snapshot?.compose) { it is Parcelable || it is JavaSerializable }
  lateinit var navigation: MutableState<PrimaryNavigationState>
  lateinit var home: HomeViewModel
  lateinit var history: HistoryViewModel
  val owners = mutableMapOf<Destination, ViewModelStoreOwner>()
  val clearTrackers = mutableMapOf<ViewModelStoreOwner, ShellClearTracker>()
  val scroll = mutableMapOf<Destination, LazyListState>()
  val historyInstances = mutableListOf<HistoryViewModel>()
  val firstHistoryStates = mutableListOf<HistoryUiState>()
  val renderedHistory = mutableListOf<HistoryUiState>()
  val homeLoading = mutableListOf<Boolean>()
  val label = androidx.compose.runtime.mutableStateOf("Missing category")
  private val clock = ShellFrameClock()
  private val recomposer = Recomposer(scope.coroutineContext + clock)
  private val composition = Composition(ShellUnitApplier(), recomposer)
  private val recomposerJob = scope.launch(clock) { recomposer.runRecomposeAndApplyChanges() }
  private var closed = false

  init {
    composition.setContent {
      KoinIsolatedContext(context) {
        CompositionLocalProvider(
            LocalLifecycleOwner provides (activity ?: parent),
            LocalViewModelStoreOwner provides (activity ?: parent.viewModelOwner),
            LocalSavedStateRegistryOwner provides (activity ?: parent),
            LocalSaveableStateRegistry provides registry,
        ) {
          Content()
        }
      }
    }
  }

  @Composable
  private fun Content() {
    val state = rememberPrimaryNavigation(Home)
    val entries =
        rememberPrimaryEntries(state.value) { destination ->
          NavEntry(destination, contentKey = primaryContentKey(destination)) {
            val owner = requireNotNull(LocalViewModelStoreOwner.current)
            androidx.compose.runtime.remember(owner) {
              ShellClearTracker().also {
                owner.viewModelStore.put("shell-cleanup", it)
                clearTrackers[owner] = it
              }
            }
            val list = rememberLazyListState()
            when (destination) {
              Home -> {
                val vm: HomeViewModel = koinViewModel()
                val output = rememberHomeRouteState(vm)
                val rendered = output.value
                SideEffect {
                  home = vm
                  homeLoading += rendered.isLoading
                }
              }
              History -> {
                val vm: HistoryViewModel = koinViewModel { parametersOf(label.value) }
                val output = rememberHistoryRouteState(vm, label.value)
                val rendered = output.value
                val initial = androidx.compose.runtime.remember(vm, label.value) { rendered }
                SideEffect {
                  history = vm
                  if (
                      historyInstances.lastOrNull() !== vm ||
                          firstHistoryStates.lastOrNull() !== initial
                  ) {
                    historyInstances += vm
                    firstHistoryStates += initial
                  }
                  renderedHistory += rendered
                }
              }
              UiCatalog -> Unit
            }
            SideEffect {
              owners[destination] = owner
              scroll[destination] = list
            }
          }
        }
    SideEffect { navigation = state }
    key(state.value.selected) {
      entries[state.value.retainedKeys.indexOf(state.value.selected)].Content()
    }
  }

  fun select(destination: Destination) {
    navigation.value = navigation.value.select(destination)
    settle()
  }

  fun settle() {
    repeat(3) {
      Snapshot.sendApplyNotifications()
      scope.runCurrent()
    }
  }

  fun pause() {
    parent.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    settle()
  }

  fun resume() {
    parent.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    settle()
  }

  fun saveAndParcel(): ShellSnapshot {
    settle()
    val parcel = Parcel.obtain()
    try {
      parcel.writeBundle(parent.save())
      parcel.writeValue(registry.performSave())
      parcel.setDataPosition(0)
      val owner = requireNotNull(parcel.readBundle(javaClass.classLoader))
      val raw = parcel.readValue(javaClass.classLoader)
      require(raw is Map<*, *>)
      val compose =
          raw.entries.associate { (key, value) ->
            require(key is String && value is List<*>)
            key to value.toList()
          }
      return ShellSnapshot(owner, compose)
    } finally {
      parcel.recycle()
    }
  }

  suspend fun close() {
    if (closed) return
    closed = true
    composition.dispose()
    settle()
    parent.close()
    settle()
    recomposer.close()
    recomposerJob.join()
    assertTrue(clearTrackers.values.all { it.cleared == 1 })
  }
}

private class ShellMovimientos : MovimientoRepository {
  val rows =
      MutableStateFlow(
          listOf(
              shellRow("past", LocalDate(2024, 2, 22)),
              shellRow("today", LocalDate(2026, 10, 5)),
          )
      )
  var activeRanges = 0
  var activeRecent = 0
  var rangeStarts = 0

  override fun observeRange(
      moneda: Moneda,
      range: MovimientoDateRange,
  ): Flow<DataOutcome<List<Movimiento>>> = flow {
    activeRanges += 1
    rangeStarts += 1
    try {
      emitAll(
          rows.map { values ->
            DataOutcome.Success(
                values
                    .filter { it.fecha in range.start..range.end && it.monto.moneda == moneda }
                    .sortedWith(shellStorageOrder)
            )
          }
      )
    } finally {
      activeRanges -= 1
    }
  }

  override fun observeRecent(moneda: Moneda, limit: Int): Flow<DataOutcome<List<Movimiento>>> =
      flow {
        activeRecent += 1
        try {
          emitAll(
              rows.map { values ->
                DataOutcome.Success(
                    values
                        .filter { it.monto.moneda == moneda }
                        .sortedWith(shellStorageOrder)
                        .take(limit)
                )
              }
          )
        } finally {
          activeRecent -= 1
        }
      }

  override fun observeRecent(limit: Int): Flow<DataOutcome<List<Movimiento>>> = flow {
    activeRecent += 1
    try {
      emitAll(rows.map { DataOutcome.Success(it.sortedWith(shellStorageOrder).take(limit)) })
    } finally {
      activeRecent -= 1
    }
  }

  override fun observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>> = rows.map {
    DataOutcome.Success(it.map { row -> row.fecha.year }.distinct())
  }

  override fun observeSummary(moneda: Moneda): Flow<DataOutcome<MovimientoSummary>> = rows.map {
    DataOutcome.Success(
        MovimientoSummary(Money(0, moneda), Money(it.sumOf { row -> row.monto.minorUnits }, moneda))
    )
  }

  override suspend fun insert(movimiento: Movimiento): DataOutcome<Unit> {
    rows.value += movimiento
    return DataOutcome.Success(Unit)
  }

  override suspend fun delete(id: String): DataOutcome<Unit> {
    rows.value = rows.value.filter { it.id != id }
    return DataOutcome.Success(Unit)
  }

  override suspend fun getAll(): DataOutcome<List<Movimiento>> = DataOutcome.Success(rows.value)
}

private val shellStorageOrder =
    compareByDescending<Movimiento> { it.fecha }
        .thenByDescending { it.createdAt }
        .thenByDescending { it.id }

private class ShellPreferences : AppPreferencesRepository {
  override val preferences: Flow<DataOutcome<AppPreferencesState>> =
      MutableStateFlow(DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(Moneda.USD))))

  override suspend fun initialize(monedaPrincipal: Moneda): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun setMonedaPrincipal(moneda: Moneda): DataOutcome<Unit> =
      DataOutcome.Success(Unit)
}

private class ShellCategories : CategoriaRepository {
  override fun observeAll(): Flow<DataOutcome<List<Categoria>>> =
      MutableStateFlow(DataOutcome.Success(emptyList()))

  override suspend fun getAll(): DataOutcome<List<Categoria>> = DataOutcome.Success(emptyList())

  override suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
      DataOutcome.Success(Unit)
}

private fun shellRow(id: String, date: LocalDate) =
    Movimiento(
        id,
        TipoMovimiento.GASTO,
        Money(125, Moneda.USD),
        date,
        "missing",
        null,
        Instant.parse("2026-10-05T12:00:00Z"),
    )

private class ShellUnitApplier : AbstractApplier<Unit>(Unit) {
  override fun insertTopDown(index: Int, instance: Unit) = Unit

  override fun insertBottomUp(index: Int, instance: Unit) = Unit

  override fun remove(index: Int, count: Int) = Unit

  override fun move(from: Int, to: Int, count: Int) = Unit

  override fun onClear() = Unit
}

private class ShellFrameClock : MonotonicFrameClock {
  override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(0L)
}

private class ShellClearTracker : ViewModel() {
  var cleared = 0

  override fun onCleared() {
    cleared += 1
  }
}
