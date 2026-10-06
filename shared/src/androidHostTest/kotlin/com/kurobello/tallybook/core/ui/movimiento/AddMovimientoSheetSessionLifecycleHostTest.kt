package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.ViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddMovimientoSheetSessionLifecycleHostTest {

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun compositionReplacementAndTeardownReleaseOnlyTheOwnedSession() = runTest {
    val first = AddMovimientoSheetSession().apply { open() }
    val firstTracker = LifecycleClearedTracker()
    first.viewModelStoreOwner.viewModelStore.put(
        "tracker",
        LifecycleTrackingViewModel(firstTracker),
    )
    val replacement = AddMovimientoSheetSession().apply { open() }
    val replacementTracker = LifecycleClearedTracker()
    replacement.viewModelStoreOwner.viewModelStore.put(
        "tracker",
        LifecycleTrackingViewModel(replacementTracker),
    )
    val activeSession = mutableStateOf<AddMovimientoSheetSession?>(first)
    val frameClock = ImmediateFrameClock()
    val recomposer = Recomposer(coroutineContext + frameClock)
    val composition = Composition(UnitApplier(), recomposer)
    val recomposerJob = launch(frameClock) { recomposer.runRecomposeAndApplyChanges() }

    try {
      composition.setContent {
        activeSession.value?.let { session -> AddMovimientoSheetSessionLifecycle(session) }
      }
      runCurrent()

      activeSession.value = replacement
      Snapshot.sendApplyNotifications()
      runCurrent()

      assertEquals(1, firstTracker.count)
      assertEquals(0, replacementTracker.count)
      assertTrue(replacement.hasBeenOpened)

      activeSession.value = null
      Snapshot.sendApplyNotifications()
      runCurrent()

      assertEquals(1, replacementTracker.count)
      assertFalse(replacement.hasBeenOpened)
    } finally {
      activeSession.value = null
      Snapshot.sendApplyNotifications()
      runCurrent()
      recomposer.close()
      recomposerJob.join()
    }
  }
}

private class LifecycleClearedTracker {
  var count = 0
}

private class LifecycleTrackingViewModel(private val tracker: LifecycleClearedTracker) :
    ViewModel() {
  override fun onCleared() {
    tracker.count += 1
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
