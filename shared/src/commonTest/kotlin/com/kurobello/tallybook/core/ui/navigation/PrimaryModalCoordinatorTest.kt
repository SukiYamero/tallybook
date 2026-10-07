package com.kurobello.tallybook.core.ui.navigation

import com.kurobello.tallybook.core.ui.movimiento.AddMovimientoSheetSession
import kotlin.test.Test
import kotlin.test.assertEquals
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

  @Test
  fun managementClosingBlocksTabsAndEveryModalWhileUndoOnlyBlocksModalAdmission() {
    val creation = AddMovimientoSheetSession()
    val coordinator = PrimaryModalCoordinator(creation) { false }
    val management = ManagementController()
    coordinator.attachManagement(management)
    assertTrue(coordinator.tryOpenManagement("movement"))
    assertFalse(coordinator.canNavigate)
    assertFalse(coordinator.tryOpenCreation())
    assertFalse(coordinator.tryOpenManagement("other"))
    management.active = false
    management.restoring = true
    assertTrue(coordinator.canNavigate)
    assertFalse(coordinator.tryOpenCreation())
    assertFalse(coordinator.tryOpenManagement("other"))
    management.restoring = false
    assertTrue(coordinator.tryOpenCreation())
  }

  @Test
  fun managementAttachmentAndFiltersUseTheSameConservativeAdmission() {
    val creation = AddMovimientoSheetSession()
    val coordinator = PrimaryModalCoordinator(creation) { true }
    val management = ManagementController()
    coordinator.attachManagement(management)
    assertFalse(coordinator.tryOpenManagement("movement"))
    val filters = FilterController()
    coordinator.attach(filters)
    assertTrue(coordinator.tryOpenFilters())
    assertFalse(coordinator.tryOpenManagement("movement"))
    filters.active = false
    management.restoring = true
    assertFalse(coordinator.tryOpenFilters())
    management.restoring = false
    val replacement = ManagementController()
    coordinator.attachManagement(replacement)
    coordinator.detachManagement(management)
    assertTrue(coordinator.tryOpenManagement("movement"))
    assertFalse(coordinator.tryOpenFilters())
  }

  @Test
  fun pausedCurrentFeedbackCallbacksCannotExpireUndoOrClaimRestoration() {
    val creation = AddMovimientoSheetSession()
    val coordinator = PrimaryModalCoordinator(creation) { false }
    val management = ManagementController()
    coordinator.attachManagement(management)
    var deliveries = 0
    val deliver = {
      deliveries++
      Unit
    }
    assertFalse(coordinator.dispatchFeedback(false, deliver))
    assertTrue(coordinator.tryOpenCreation())
    assertFalse(coordinator.dispatchFeedback(true, deliver))
    creation.dismiss()
    assertFalse(coordinator.dispatchFeedback(true, deliver))
    creation.onSheetHidden(creation.token)
    assertTrue(coordinator.tryOpenManagement("movement"))
    assertFalse(coordinator.dispatchFeedback(true, deliver))
    management.active = false
    management.restoring = true
    assertFalse(coordinator.dispatchFeedback(true, deliver))
    management.restoring = false
    assertEquals(0, deliveries)
    assertTrue(coordinator.dispatchFeedback(true, deliver))
    assertEquals(1, deliveries)
  }

  private class ManagementController : MovimientoManagementController {
    var active = false
    var restoring = false
    override val hasActiveSession
      get() = active

    override val isRestoring
      get() = restoring

    override fun open(id: String): Boolean {
      if (active || restoring) return false
      active = true
      return true
    }
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
