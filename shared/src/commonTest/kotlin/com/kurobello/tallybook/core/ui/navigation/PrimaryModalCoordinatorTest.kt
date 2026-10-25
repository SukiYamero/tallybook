package com.kurobello.tallybook.core.ui.navigation

import com.kurobello.tallybook.core.ui.movimiento.AddMovimientoSheetSession
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferEntry
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
    assertTrue(coordinator.tryOpenDestinationModal(controller))
    assertFalse(coordinator.tryOpenCreation())
    assertFalse(coordinator.canNavigate)
    controller.closing = true
    assertFalse(coordinator.tryOpenDestinationModal(controller))
    controller.active = false
    controller.closing = false
    assertTrue(coordinator.tryOpenCreation())
    creation.dismiss()
    assertFalse(coordinator.tryOpenDestinationModal(controller))
    assertFalse(coordinator.canNavigate)
    creation.onSheetHidden(creation.token)
    assertTrue(coordinator.canNavigate)
    selectedSearch = false
    coordinator.detach(controller)
    assertFalse(coordinator.tryOpenDestinationModal(controller))
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
    assertFalse(coordinator.tryOpenDestinationModal(old))
    assertFalse(coordinator.tryOpenDestinationModal(current))
    current.ready = true
    assertTrue(coordinator.tryOpenDestinationModal(current))
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
    assertTrue(coordinator.tryOpenDestinationModal(filters))
    assertFalse(coordinator.tryOpenManagement("movement"))
    filters.active = false
    management.restoring = true
    assertFalse(coordinator.tryOpenDestinationModal(filters))
    management.restoring = false
    val replacement = ManagementController()
    coordinator.attachManagement(replacement)
    coordinator.detachManagement(management)
    assertTrue(coordinator.tryOpenManagement("movement"))
    assertFalse(coordinator.tryOpenDestinationModal(filters))
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

  @Test
  fun anyDestinationModalPausesFeedbackAndWaitsForUndoRestoration() {
    val creation = AddMovimientoSheetSession()
    val coordinator = PrimaryModalCoordinator(creation) { false }
    val management = ManagementController()
    coordinator.attachManagement(management)
    val profileSheet = FilterController()
    coordinator.attach(profileSheet)
    management.restoring = true
    assertFalse(coordinator.tryOpenDestinationModal(profileSheet))
    management.restoring = false
    assertTrue(coordinator.tryOpenDestinationModal(profileSheet))
    assertFalse(coordinator.canNavigate)
    assertFalse(coordinator.dispatchFeedback(true) {})
    profileSheet.active = false
    assertTrue(coordinator.canNavigate)
  }

  @Test
  fun unattachedSearchBlocksNavigationWithoutCoveringContent() {
    val coordinator = PrimaryModalCoordinator(AddMovimientoSheetSession()) { true }
    assertFalse(coordinator.canNavigate)
    assertFalse(coordinator.tryOpenCreation())
    assertFalse(coordinator.isContentCovered)
    coordinator.attach(FilterController())
    assertTrue(coordinator.canNavigate)
    assertFalse(coordinator.isContentCovered)
  }

  @Test
  fun destinationSessionCoversContentUntilItIsFullyHidden() {
    val coordinator = PrimaryModalCoordinator(AddMovimientoSheetSession()) { true }
    val filters = FilterController()
    coordinator.attach(filters)
    assertTrue(coordinator.tryOpenDestinationModal(filters))
    assertTrue(coordinator.isContentCovered)
    filters.active = false
    filters.closing = true
    assertTrue(coordinator.isContentCovered)
    filters.closing = false
    assertFalse(coordinator.isContentCovered)
    coordinator.detach(filters)
    assertFalse(coordinator.isContentCovered)
  }

  @Test
  fun creationAndManagementSessionsCoverContentUntilHidden() {
    val creation = AddMovimientoSheetSession()
    val coordinator = PrimaryModalCoordinator(creation) { false }
    val management = ManagementController()
    coordinator.attachManagement(management)
    assertFalse(coordinator.isContentCovered)
    assertTrue(coordinator.tryOpenCreation())
    assertTrue(coordinator.isContentCovered)
    creation.dismiss()
    assertTrue(coordinator.isContentCovered)
    creation.onSheetHidden(creation.token)
    assertFalse(coordinator.isContentCovered)
    assertTrue(coordinator.tryOpenManagement("movement"))
    assertTrue(coordinator.isContentCovered)
    management.active = false
    management.restoring = true
    assertFalse(coordinator.isContentCovered)
    assertFalse(coordinator.tryOpenCreation())
  }

  @Test
  fun transferManagementIsOneMoreExclusiveModalThatWaitsForUndo() {
    val creation = AddMovimientoSheetSession()
    val coordinator = PrimaryModalCoordinator(creation) { false }
    val management = ManagementController()
    val transfer = TransferController()
    coordinator.attachManagement(management)
    assertFalse(coordinator.tryOpenTransfer(TransferEntry.Existing("t")))
    coordinator.attachTransfer(transfer)

    assertTrue(coordinator.tryOpenTransfer(TransferEntry.Existing("t")))
    assertTrue(coordinator.isContentCovered)
    assertFalse(coordinator.canNavigate)
    assertFalse(coordinator.tryOpenCreation())
    assertFalse(coordinator.tryOpenManagement("movement"))
    assertFalse(coordinator.dispatchFeedback(true) {})
    transfer.active = false
    assertTrue(coordinator.tryOpenManagement("movement"))
    assertFalse(coordinator.tryOpenTransfer(TransferEntry.New(null, null, null)))
    management.active = false
    management.restoring = true
    assertFalse(coordinator.tryOpenTransfer(TransferEntry.New(null, null, null)))
    management.restoring = false
    coordinator.detachTransfer(TransferController())
    assertTrue(coordinator.tryOpenTransfer(TransferEntry.New(null, null, null)))
    transfer.active = false
    coordinator.detachTransfer(transfer)
    assertFalse(coordinator.tryOpenTransfer(TransferEntry.New(null, null, null)))
  }

  private class TransferController : TransferManagementController {
    var active = false
    override val hasActiveSession
      get() = active

    override fun open(entry: TransferEntry): Boolean {
      if (active) return false
      active = true
      return true
    }
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

  private class FilterController : DestinationModalController {
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
