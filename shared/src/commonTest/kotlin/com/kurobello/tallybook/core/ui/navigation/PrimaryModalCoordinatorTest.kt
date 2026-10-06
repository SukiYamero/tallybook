package com.kurobello.tallybook.core.ui.navigation

import com.kurobello.tallybook.core.ui.movimiento.AddMovimientoSheetSession
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class PrimaryModalCoordinatorTest {
  @Test
  fun searchAttachmentAndClosingExcludeOtherModalAndNavigationActions() {
    val creation = AddMovimientoSheetSession()
    var selectedSearch = true
    val coordinator = PrimaryModalCoordinator(creation) { selectedSearch }
    assertFalse(coordinator.canNavigate)
    assertFalse(coordinator.tryOpenCreation())
    val controller = FilterController()
    coordinator.attach(controller)
    assertTrue(coordinator.canNavigate)
    assertTrue(coordinator.tryOpenFilters())
    assertFalse(coordinator.tryOpenCreation())
    assertFalse(coordinator.canNavigate)
    controller.closing = true
    assertFalse(coordinator.tryOpenFilters())
    controller.active = false
    controller.closing = false
    assertTrue(coordinator.tryOpenCreation())
    creation.dismiss()
    assertFalse(coordinator.tryOpenFilters())
    assertFalse(coordinator.canNavigate)
    creation.onSheetHidden(creation.token)
    assertTrue(coordinator.canNavigate)
    selectedSearch = false
    assertFalse(coordinator.tryOpenFilters())
    coordinator.detach(controller)
    assertTrue(coordinator.canNavigate)
  }

  @Test
  fun oldDetachCannotReleaseNewControllerAndReadinessRejectsOpening() {
    val creation = AddMovimientoSheetSession()
    val coordinator = PrimaryModalCoordinator(creation) { true }
    val old = FilterController()
    val current = FilterController()
    current.ready = false
    coordinator.attach(old)
    coordinator.attach(current)
    coordinator.detach(old)
    assertFalse(coordinator.tryOpenFilters())
    current.ready = true
    assertTrue(coordinator.tryOpenFilters())
    assertFalse(coordinator.canNavigate)
  }

  private class FilterController : SearchFilterController {
    var ready = true
    var active = false
    var closing = false
    override val hasActiveSession: Boolean
      get() = active || closing

    override fun open(): Boolean {
      if (!ready || hasActiveSession) return false
      active = true
      return true
    }
  }
}
