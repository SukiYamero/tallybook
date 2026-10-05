package com.kurobello.tallybook.core.ui.navigation

import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.defaultViewModelCreationExtras
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import androidx.lifecycle.viewmodel.navigation3.ViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.savedstate.savedState
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import com.kurobello.tallybook.feature.history.viewmodel.HistorySavedInput
import com.kurobello.tallybook.feature.history.viewmodel.HistoryScope
import com.kurobello.tallybook.feature.history.viewmodel.HistorySelection
import java.io.Serializable as JavaSerializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.junit.runner.RunWith
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.KoinApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class HistorySavedOwnerHostTest {
  @Test
  fun `Activity intent extras preserve entry stores across recomposition`() = runTest {
    val controller =
        Robolectric.buildActivity(
                ComponentActivity::class.java,
                Intent().putExtra("ui_catalog", true),
            )
            .setup()
    try {
      withProbe {
        val fixture = create(activity = controller.get())
        val home = fixture.activeViewModel
        fixture.select(ProbeRoute.History)
        val history = fixture.activeViewModel
        repeat(3) {
          fixture.select(ProbeRoute.Home)
          assertSame(home, fixture.activeViewModel)
          fixture.select(ProbeRoute.History)
          assertSame(history, fixture.activeViewModel)
        }
        assertEquals(2, records.size)
        assertEquals(0, home.record.cleared)
        assertEquals(0, history.record.cleared)
      }
    } finally {
      controller.pause().stop().destroy()
    }
  }

  @Test
  fun `inactive History restores from new owners while Home remains selected`() = runTest {
    withProbe {
      val original = create()
      val home = original.activeViewModel
      assertEquals(ProbeRoute.Home, original.selectedRoute)
      assertEquals(1, records.size)
      original.select(ProbeRoute.History)
      val history = original.activeViewModel
      val homeStore = original.entryOwners.getValue(ProbeRoute.Home).viewModelStore
      val historyStore = original.entryOwners.getValue(ProbeRoute.History).viewModelStore
      assertNotSame(home, history)
      assertNotSame(homeStore, historyStore)
      assertNotSame(
          original.entryOwners.getValue(ProbeRoute.Home),
          original.entryOwners.getValue(ProbeRoute.History),
      )
      assertNotSame(
          original.parent.viewModelOwner,
          original.entryOwners.getValue(ProbeRoute.History),
      )
      history.select(HistorySelection(HistoryScope.Week, LocalDate(2024, 2, 22)))
      repeat(3) {
        original.select(ProbeRoute.Home)
        assertSame(home, original.activeViewModel)
        assertSame(homeStore, original.entryOwners.getValue(ProbeRoute.Home).viewModelStore)
        original.select(ProbeRoute.History)
        assertSame(history, original.activeViewModel)
        assertSame(historyStore, original.entryOwners.getValue(ProbeRoute.History).viewModelStore)
      }
      assertEquals(2, records.size)
      assertEquals(0, history.record.cleared)
      assertEquals(HistorySelection(HistoryScope.Day, TODAY), home.selection.value)
      original.select(ProbeRoute.Home)
      val snapshot = original.saveAndParcel()
      original.close()
      assertEquals(1, home.record.cleared)
      assertEquals(1, history.record.cleared)
      assertEquals(history.record.readStarts, history.record.readStops)

      val restored = create(snapshot)
      assertNotSame(original.parent, restored.parent)
      assertNotSame(original.parent.viewModelStore, restored.parent.viewModelStore)
      assertNotSame(original.parent.savedStateRegistry, restored.parent.savedStateRegistry)
      assertNotSame(original.composeRegistry, restored.composeRegistry)
      assertEquals(ProbeRoute.Home, restored.selectedRoute)
      assertEquals(listOf(ProbeRoute.Home, ProbeRoute.History), restored.visitedRoutes)
      assertNotSame(home, restored.activeViewModel)
      val restoredHome = restored.activeViewModel
      assertEquals(3, records.size)
      restored.select(ProbeRoute.History)
      val restoredHistory = restored.activeViewModel
      assertNotSame(history, restoredHistory)
      assertNotSame(historyStore, restored.entryOwners.getValue(ProbeRoute.History).viewModelStore)
      assertEquals(
          HistorySelection(HistoryScope.Week, LocalDate(2024, 2, 22)),
          restoredHistory.selection.value,
      )
      assertEquals(1, restoredHistory.record.readStarts)
      restored.removeHistory()
      assertSame(restoredHome, restored.activeViewModel)
      assertEquals(0, restoredHome.record.cleared)
      assertEquals(1, restoredHistory.record.cleared)
      assertEquals(1, restoredHistory.record.readStops)
      restored.select(ProbeRoute.History)
      assertNotSame(restoredHistory, restored.activeViewModel)
      assertEquals(
          HistorySelection(HistoryScope.Day, TODAY),
          restored.activeViewModel.selection.value,
      )
    }
  }

  @Test
  fun `active History checkpoints the latest typed selection and clears each owner once`() =
      runTest {
        withProbe {
          val original = create()
          original.select(ProbeRoute.History)
          val history = original.activeViewModel
          history.select(HistorySelection(HistoryScope.Week, LocalDate(2024, 2, 22)))
          original.saveAndParcel()
          history.select(HistorySelection(HistoryScope.Month, LocalDate(2024, 2, 27)))
          val snapshot = original.saveAndParcel()
          original.close()
          val restored = create(snapshot)
          assertEquals(ProbeRoute.History, restored.selectedRoute)
          assertNotSame(history, restored.activeViewModel)
          assertEquals(
              HistorySelection(HistoryScope.Month, LocalDate(2024, 2, 27)),
              restored.activeViewModel.selection.value,
          )
          assertEquals(1, restored.activeViewModel.record.readStarts)
          restored.close()
          assertTrue(records.all { it.cleared == 1 })
          assertTrue(records.all { it.readStarts == it.readStops })
        }
      }

  @Test
  fun `malformed checkpoint recovers through the real restored entry handle`() = runTest {
    withProbe {
      val original = create()
      original.select(ProbeRoute.History)
      original.activeViewModel.select(HistorySelection(HistoryScope.Week, LocalDate(2024, 2, 22)))
      original.activeViewModel.replaceCheckpointWithMalformedState()
      val snapshot = original.saveAndParcel()
      original.close()
      val restored = create(snapshot)
      assertEquals(ProbeRoute.History, restored.selectedRoute)
      assertEquals(
          HistorySelection(HistoryScope.Day, TODAY),
          restored.activeViewModel.selection.value,
      )
      assertEquals(1, restored.activeViewModel.record.decodeFailures)
    }
  }
}

private val TODAY = LocalDate(2026, 10, 5)
private const val HISTORY_INPUT_KEY = "history-input"

@Serializable
private enum class ProbeRoute : NavKey {
  Home,
  History,
}

private class ProbeRecord {
  var cleared = 0
  var readStarts = 0
  var readStops = 0
  var decodeFailures = 0
}

private class ProbeViewModel(
    private val handle: SavedStateHandle,
    val record: ProbeRecord,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
  private val editableSelection = MutableStateFlow(restoreSelection())
  val selection: StateFlow<HistorySelection> = editableSelection.asStateFlow()

  init {
    handle.setSavedStateProvider(HISTORY_INPUT_KEY) {
      encodeToSavedState(HistorySavedInput.serializer(), editableSelection.value.savedInput())
    }
  }

  fun select(value: HistorySelection) {
    editableSelection.value = value
  }

  fun observe(): Job =
      viewModelScope.launch(dispatcher) {
        flow {
              record.readStarts += 1
              try {
                emit(Unit)
                awaitCancellation()
              } finally {
                record.readStops += 1
              }
            }
            .collect {}
      }

  fun replaceCheckpointWithMalformedState() {
    handle.setSavedStateProvider(HISTORY_INPUT_KEY) { savedState() }
  }

  private fun restoreSelection(): HistorySelection {
    val saved = handle.get<SavedState>(HISTORY_INPUT_KEY) ?: return HistorySelection.initial(TODAY)
    val input =
        try {
          decodeFromSavedState(HistorySavedInput.serializer(), saved)
        } catch (_: SerializationException) {
          record.decodeFailures += 1
          null
        } catch (_: IllegalArgumentException) {
          record.decodeFailures += 1
          null
        }
    return HistorySelection.restore(input, TODAY)
  }

  override fun onCleared() {
    record.cleared += 1
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class ProbeSuite(private val scope: TestScope) {
  val records = mutableListOf<ProbeRecord>()
  private val fixtures = mutableListOf<ProbeHarness>()
  private val dispatcher = StandardTestDispatcher(scope.testScheduler)
  private val context = koinApplication {
    modules(
        module { viewModel { ProbeViewModel(get(), ProbeRecord().also(records::add), dispatcher) } }
    )
  }

  fun create(snapshot: ProbeSnapshot? = null, activity: ComponentActivity? = null): ProbeHarness =
      ProbeHarness(scope, context, snapshot, activity).also {
        fixtures.add(it)
        it.settle()
      }

  suspend fun close() {
    try {
      fixtures.forEach { it.close() }
      assertTrue(records.all { it.cleared == 1 })
      assertTrue(records.all { it.readStarts == it.readStops })
    } finally {
      context.close()
    }
  }
}

private suspend fun TestScope.withProbe(block: suspend ProbeSuite.() -> Unit) {
  val suite = ProbeSuite(this)
  try {
    suite.block()
  } finally {
    suite.close()
  }
}

private data class ProbeSnapshot(
    val ownerState: SavedState,
    val composeState: Map<String, List<Any?>>,
)

private class ProbeParentOwner(restoredState: SavedState?) :
    SavedStateRegistryOwner, ViewModelStoreOwner {
  override val viewModelStore = ViewModelStore()
  override val lifecycle = LifecycleRegistry.createUnsafe(this)
  private val controller = SavedStateRegistryController.create(this)
  override val savedStateRegistry = controller.savedStateRegistry
  val viewModelOwner: ViewModelStoreOwner

  init {
    controller.performRestore(restoredState)
    viewModelOwner = androidx.lifecycle.viewmodel.ViewModelStoreOwner(viewModelStore, this)
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
  }

  fun save(): SavedState = savedState().also(controller::performSave)

  fun close() {
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    viewModelStore.clear()
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class ProbeHarness(
    private val scope: TestScope,
    context: KoinApplication,
    snapshot: ProbeSnapshot?,
    private val activity: ComponentActivity?,
) {
  val parent = ProbeParentOwner(snapshot?.ownerState)
  val composeRegistry =
      SaveableStateRegistry(snapshot?.composeState) { it is Parcelable || it is JavaSerializable }
  val entryOwners = mutableMapOf<ProbeRoute, ViewModelStoreOwner>()
  lateinit var activeViewModel: ProbeViewModel
    private set

  private lateinit var routes: NavBackStack<ProbeRoute>
  private lateinit var selected: MutableState<ProbeRoute>
  private val frameClock = ImmediateFrameClock()
  private val recomposer = Recomposer(scope.coroutineContext + frameClock)
  private val composition = Composition(UnitApplier(), recomposer)
  private val recomposerJob = scope.launch(frameClock) { recomposer.runRecomposeAndApplyChanges() }
  private var closed = false

  val selectedRoute: ProbeRoute
    get() = selected.value

  val visitedRoutes: List<ProbeRoute>
    get() = routes.toList()

  init {
    composition.setContent {
      KoinIsolatedContext(context) {
        CompositionLocalProvider(
            LocalLifecycleOwner provides (activity ?: parent),
            LocalViewModelStoreOwner provides (activity ?: parent.viewModelOwner),
            LocalSavedStateRegistryOwner provides (activity ?: parent),
            LocalSaveableStateRegistry provides composeRegistry,
        ) {
          Content()
        }
      }
    }
  }

  @Composable
  private fun Content() {
    val parentOwner = requireNotNull(LocalViewModelStoreOwner.current)
    // Intent extras are copied Bundles, so snapshot creation extras for the parent's lifetime.
    val creationExtras = remember(parentOwner) { parentOwner.defaultViewModelCreationExtras }
    val provider =
        rememberViewModelStoreProvider(parent = parentOwner, defaultCreationExtras = creationExtras)
    val viewModelDecorator =
        remember(provider) { ViewModelStoreNavEntryDecorator<ProbeRoute>(provider) }
    val visited =
        rememberSerializable(serializer = NavBackStackSerializer(ProbeRoute.serializer())) {
          NavBackStack(ProbeRoute.Home)
        }
    val active =
        rememberSerializable(stateSerializer = ProbeRoute.serializer()) {
          mutableStateOf(ProbeRoute.Home)
        }
    val entries =
        rememberDecoratedNavEntries(
            backStack = visited,
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    viewModelDecorator,
                ),
            entryProvider = { route ->
              NavEntry(route, contentKey = "probe-${route.name}") {
                val owner = requireNotNull(LocalViewModelStoreOwner.current)
                val viewModel: ProbeViewModel = koinViewModel()
                DisposableEffect(viewModel) {
                  val read = viewModel.observe()
                  onDispose { read.cancel() }
                }
                SideEffect {
                  entryOwners[route] = owner
                  activeViewModel = viewModel
                }
              }
            },
        )
    SideEffect {
      routes = visited
      selected = active
    }
    key(active.value) { entries[visited.indexOf(active.value)].Content() }
  }

  fun select(route: ProbeRoute) {
    if (route !in routes) routes.add(route)
    selected.value = route
    settle()
  }

  fun removeHistory() {
    selected.value = ProbeRoute.Home
    routes.remove(ProbeRoute.History)
    settle()
  }

  fun settle() {
    Snapshot.sendApplyNotifications()
    scope.runCurrent()
  }

  fun saveAndParcel(): ProbeSnapshot {
    require(activity == null) {
      "Activity restoration is verified through the later app-shell gate"
    }
    settle()
    val parcel = Parcel.obtain()
    try {
      parcel.writeBundle(parent.save())
      parcel.writeValue(composeRegistry.performSave())
      parcel.setDataPosition(0)
      val ownerState = requireNotNull(parcel.readBundle(javaClass.classLoader))
      val rawValues = parcel.readValue(javaClass.classLoader)
      require(rawValues is Map<*, *>)
      val values =
          rawValues.entries.associate { (key, value) ->
            require(key is String && value is List<*>)
            key to value.toList()
          }
      return ProbeSnapshot(ownerState, values)
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
  }
}

private class UnitApplier : AbstractApplier<Unit>(Unit) {
  override fun insertTopDown(index: Int, instance: Unit) = Unit

  override fun insertBottomUp(index: Int, instance: Unit) = Unit

  override fun remove(index: Int, count: Int) = Unit

  override fun move(from: Int, to: Int, count: Int) = Unit

  override fun onClear() = Unit
}

private class ImmediateFrameClock : MonotonicFrameClock {
  override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(0L)
}
