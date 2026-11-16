package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.RestoreResult
import com.kurobello.tallybook.core.ui.feedback.ManagementFeedbackKind
import com.kurobello.tallybook.core.ui.movimiento.AddMovimientoSheetSession
import com.kurobello.tallybook.core.ui.navigation.MovimientoManagementController
import com.kurobello.tallybook.core.ui.navigation.PrimaryModalCoordinator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class MovimientoUndoTest {
  @Test
  fun pausedCurrentTokenTimerCloseAndActionKeepUndoUntilTheModalIsHidden() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onRequestDelete()
    f.viewModel.onConfirmDelete()
    runCurrent()
    f.viewModel.onSheetHidden(assertNotNull(f.viewModel.uiState.value.session).token)
    val feedback = assertNotNull(f.feedback.state.value.feedback)
    val creation = AddMovimientoSheetSession()
    val modals = PrimaryModalCoordinator(creation) { false }
    modals.attachManagement(
        object : MovimientoManagementController {
          override val hasActiveSession
            get() = f.viewModel.uiState.value.session != null

          override val isRestoring
            get() = f.feedback.state.value.isRestoring

          override fun open(id: String) = f.viewModel.open(id)
        }
    )
    assertTrue(modals.tryOpenCreation())
    repeat(2) {
      assertFalse(
          modals.dispatchFeedback(true) {
            f.feedback.onFeedbackDismiss(feedback.token, feedback.revision)
          }
      )
    }
    assertFalse(
        modals.dispatchFeedback(true) {
          f.feedback.onFeedbackAction(feedback.token, feedback.revision)
        }
    )
    assertEquals(feedback, f.feedback.state.value.feedback)
    assertEquals(0, f.repository.restores)
    creation.dismiss()
    assertFalse(
        modals.dispatchFeedback(true) {
          f.feedback.onFeedbackDismiss(feedback.token, feedback.revision)
        }
    )
    creation.onSheetHidden(creation.token)
    assertFalse(
        modals.dispatchFeedback(false) {
          f.feedback.onFeedbackDismiss(feedback.token, feedback.revision)
        }
    )
    assertTrue(
        modals.dispatchFeedback(true) {
          f.feedback.onFeedbackAction(feedback.token, feedback.revision)
        }
    )
    runCurrent()
    assertEquals(1, f.repository.restores)
    assertEquals(ManagementFeedbackKind.Restored, f.feedback.state.value.feedback?.kind)
  }

  @Test
  fun undoUsesTransactionalCaptureAndAtomicallyClaimsBeforeTimeout() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.viewModel.open("movement")
    runCurrent()
    val captured = managementMovimiento().copy(descripcion = "Committed version")
    f.repository.deleteResult = DataOutcome.Success(captured)
    f.viewModel.onRequestDelete()
    f.viewModel.onConfirmDelete()
    f.viewModel.onConfirmDelete()
    runCurrent()
    assertEquals(1, f.repository.deletes)
    assertNull(f.feedback.state.value.feedback)
    f.viewModel.onSheetHidden(assertNotNull(f.viewModel.uiState.value.session).token)
    val feedback = assertNotNull(f.feedback.state.value.feedback)
    f.repository.restoreGate = CompletableDeferred()
    f.feedback.onFeedbackAction(feedback.token, feedback.revision)
    f.feedback.onFeedbackAction(feedback.token, feedback.revision)
    f.feedback.onFeedbackDismiss(feedback.token, feedback.revision)
    assertTrue(f.feedback.state.value.isRestoring)
    assertFalse(f.viewModel.open("movement"))
    runCurrent()
    assertEquals(1, f.repository.restores)
    assertEquals(captured, f.repository.restored)
    f.repository.restoreGate?.complete(Unit)
    runCurrent()
    assertEquals(ManagementFeedbackKind.Restored, f.feedback.state.value.feedback?.kind)
  }

  @Test
  fun cancelledSessionAndNewCommittedFeedbackPreserveCurrentUndoUntilDismissed() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onRequestDelete()
    f.viewModel.onConfirmDelete()
    runCurrent()
    val deletedOrigin = assertNotNull(f.viewModel.uiState.value.session).token
    f.viewModel.onSheetHidden(deletedOrigin)
    val old = assertNotNull(f.feedback.state.value.feedback)
    f.repository.current = managementMovimiento().copy(id = "next")
    f.viewModel.open("next")
    runCurrent()
    f.viewModel.onRequestDismiss()
    f.viewModel.onSheetHidden(assertNotNull(f.viewModel.uiState.value.session).token)
    assertEquals(old, f.feedback.state.value.feedback)
    f.viewModel.open("next")
    runCurrent()
    f.viewModel.onAmountChanged("20.00")
    f.viewModel.onSubmit()
    runCurrent()
    val newerOrigin = assertNotNull(f.viewModel.uiState.value.session).token
    f.viewModel.onSheetHidden(deletedOrigin)
    assertEquals(newerOrigin, f.viewModel.uiState.value.session?.token)
    f.viewModel.onSheetHidden(newerOrigin)
    assertEquals(old, f.feedback.state.value.feedback)
    f.feedback.onFeedbackDismiss(old.token, old.revision)
    val saved = assertNotNull(f.feedback.state.value.feedback)
    assertEquals(ManagementFeedbackKind.Saved, saved.kind)
    f.feedback.onFeedbackAction(old.token, old.revision)
    f.feedback.onFeedbackDismiss(old.token, old.revision)
    runCurrent()
    assertEquals(0, f.repository.restores)
    assertEquals(saved, f.feedback.state.value.feedback)
  }

  @Test
  fun timeoutBeforeClaimReleasesUndoAndRejectsLateAction() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onRequestDelete()
    f.viewModel.onConfirmDelete()
    runCurrent()
    f.viewModel.onSheetHidden(assertNotNull(f.viewModel.uiState.value.session).token)
    val feedback = assertNotNull(f.feedback.state.value.feedback)
    f.feedback.onFeedbackDismiss(feedback.token, feedback.revision)
    f.feedback.onFeedbackAction(feedback.token, feedback.revision)
    runCurrent()
    assertNull(f.feedback.state.value.feedback)
    assertEquals(0, f.repository.restores)
  }

  @Test
  fun failedUndoOffersFreshRevisionAndRejectsOldRetryAndTimeout() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onRequestDelete()
    f.viewModel.onConfirmDelete()
    runCurrent()
    f.viewModel.onSheetHidden(assertNotNull(f.viewModel.uiState.value.session).token)
    val first = assertNotNull(f.feedback.state.value.feedback)
    f.repository.restoreResult = managementFailure()
    f.feedback.onFeedbackAction(first.token, first.revision)
    runCurrent()
    val failed = assertNotNull(f.feedback.state.value.feedback)
    assertEquals(ManagementFeedbackKind.RestoreFailed, failed.kind)
    assertTrue(failed.revision > first.revision)
    f.feedback.onFeedbackDismiss(first.token, first.revision)
    f.feedback.onFeedbackAction(first.token, first.revision)
    runCurrent()
    assertEquals(1, f.repository.restores)
    assertEquals(failed, f.feedback.state.value.feedback)
    f.repository.restoreResult = DataOutcome.Success(RestoreResult.AlreadyPresent)
    f.feedback.onFeedbackAction(failed.token, failed.revision)
    runCurrent()
    assertEquals(2, f.repository.restores)
    assertEquals(ManagementFeedbackKind.Restored, f.feedback.state.value.feedback?.kind)
  }

  @Test
  fun restoreConflictReleasesSnapshotAndHasNoRetry() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onRequestDelete()
    f.viewModel.onConfirmDelete()
    runCurrent()
    f.viewModel.onSheetHidden(assertNotNull(f.viewModel.uiState.value.session).token)
    val feedback = assertNotNull(f.feedback.state.value.feedback)
    f.repository.restoreResult = DataOutcome.Success(RestoreResult.Conflict)
    f.feedback.onFeedbackAction(feedback.token, feedback.revision)
    runCurrent()
    val conflict = assertNotNull(f.feedback.state.value.feedback)
    assertEquals(ManagementFeedbackKind.RestoreConflict, conflict.kind)
    assertFalse(conflict.hasAction)
    f.feedback.onFeedbackAction(conflict.token, conflict.revision)
    runCurrent()
    assertEquals(1, f.repository.restores)
  }

  @Test
  fun restoreWithoutItsCuentaReleasesSnapshotWithItsOwnFeedback() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onRequestDelete()
    f.viewModel.onConfirmDelete()
    runCurrent()
    f.viewModel.onSheetHidden(assertNotNull(f.viewModel.uiState.value.session).token)
    val feedback = assertNotNull(f.feedback.state.value.feedback)
    f.repository.restoreResult = DataOutcome.Success(RestoreResult.MissingCuenta)
    f.feedback.onFeedbackAction(feedback.token, feedback.revision)
    runCurrent()
    val failed = assertNotNull(f.feedback.state.value.feedback)
    assertEquals(ManagementFeedbackKind.RestoreMissingCuenta, failed.kind)
    assertNull(failed.undo)
    assertFalse(failed.hasAction)
    f.feedback.onFeedbackAction(failed.token, failed.revision)
    runCurrent()
    assertEquals(1, f.repository.restores)
  }

  @Test
  fun missingDeletePublishesInformationalFeedbackWithoutUndo() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.viewModel.open("movement")
    runCurrent()
    f.repository.deleteResult = DataOutcome.Success(null)
    f.viewModel.onRequestDelete()
    f.viewModel.onConfirmDelete()
    runCurrent()
    f.viewModel.onSheetHidden(assertNotNull(f.viewModel.uiState.value.session).token)
    val feedback = assertNotNull(f.feedback.state.value.feedback)
    assertEquals(ManagementFeedbackKind.Missing, feedback.kind)
    assertFalse(feedback.hasAction)
  }
}
