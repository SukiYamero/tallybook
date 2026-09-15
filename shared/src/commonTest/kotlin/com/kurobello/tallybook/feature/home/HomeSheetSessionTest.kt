package com.kurobello.tallybook.feature.home

import androidx.lifecycle.ViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeSheetSessionTest {

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

    session.onSheetHidden()
    session.onSheetHidden()
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

    session.onSheetHidden()
    session.open()

    assertTrue(session.isVisible)
    assertEquals("add-movimiento-1", session.viewModelKey)
  }

  @Test
  fun repeatedDisposalAndHiddenNotificationsReleaseTheSessionOnlyOnce() {
    val session = AddMovimientoSheetSession()
    val tracker = ClearedTracker()

    session.open()
    session.viewModelStoreOwner.viewModelStore.put("tracker", TrackingViewModel(tracker))
    session.dispose()
    session.dispose()
    session.onSheetHidden()

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
