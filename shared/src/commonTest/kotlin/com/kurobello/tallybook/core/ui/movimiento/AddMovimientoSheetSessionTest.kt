package com.kurobello.tallybook.core.ui.movimiento

import androidx.lifecycle.ViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AddMovimientoSheetSessionTest {

  @Test
  fun dismissalAdvancesOnceAndReopenUsesANewViewModelKey() {
    val session = AddMovimientoSheetSession()
    val tracker = ClearedTracker()

    assertFalse(session.hasBeenOpened)
    session.open()
    session.viewModelStoreOwner.viewModelStore.put("tracker", TrackingViewModel(tracker))
    assertTrue(session.isVisible)
    assertEquals("add-movimiento-0", session.viewModelKey)

    session.dismiss()
    session.dismiss()
    assertFalse(session.isVisible)
    assertEquals("add-movimiento-0", session.viewModelKey)

    session.onSheetHidden(session.token)
    session.onSheetHidden(session.token)
    assertEquals(1, tracker.count)
    session.open()
    assertTrue(session.isVisible)
    assertEquals("add-movimiento-1", session.viewModelKey)
  }

  @Test
  fun closingSessionRejectsAnotherOpenUntilTheSheetIsHidden() {
    val session = AddMovimientoSheetSession()

    session.open()
    session.dismiss()
    session.open()

    assertFalse(session.isVisible)
    assertTrue(session.hasBeenOpened)

    session.onSheetHidden(session.token)
    session.open()

    assertTrue(session.isVisible)
    assertEquals("add-movimiento-1", session.viewModelKey)
  }

  @Test
  fun oldHiddenCompletionCannotReleaseANewerClosingSession() {
    val session = AddMovimientoSheetSession()
    session.open()
    val first = session.token
    session.dismiss()
    session.onSheetHidden(first)
    session.open()
    val second = session.token
    val tracker = ClearedTracker()
    session.viewModelStoreOwner.viewModelStore.put("tracker", TrackingViewModel(tracker))
    session.dismiss()
    session.onSheetHidden(first)
    assertTrue(session.hasBeenOpened)
    assertEquals(0, tracker.count)
    session.onSheetHidden(second)
    assertFalse(session.hasBeenOpened)
    assertEquals(1, tracker.count)
  }

  @Test
  fun repeatedDisposalAndHiddenNotificationsReleaseTheSessionOnlyOnce() {
    val session = AddMovimientoSheetSession()
    val tracker = ClearedTracker()

    session.open()
    session.viewModelStoreOwner.viewModelStore.put("tracker", TrackingViewModel(tracker))
    session.dispose()
    session.dispose()
    session.onSheetHidden(session.token)

    assertFalse(session.isVisible)
    assertFalse(session.hasBeenOpened)
    assertEquals(1, tracker.count)
    assertEquals("add-movimiento-0", session.viewModelKey)
  }
}

private class ClearedTracker {
  var count = 0
}

private class TrackingViewModel(private val tracker: ClearedTracker) : ViewModel() {
  override fun onCleared() {
    tracker.count += 1
  }
}
