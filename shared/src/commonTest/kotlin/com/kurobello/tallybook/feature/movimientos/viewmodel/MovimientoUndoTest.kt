package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.RestoreResult
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
    val feedback = assertNotNull(f.viewModel.uiState.value.feedback)
    val creation = AddMovimientoSheetSession()
    val modals = PrimaryModalCoordinator(creation) { false }
    modals.attachManagement(
        object : MovimientoManagementController {
          override val hasActiveSession
            get() = f.viewModel.uiState.value.session != null

          override val isRestoring
            get() = f.viewModel.uiState.value.isRestoring

          override fun open(id: String) = f.viewModel.open(id)
        }
    )
    assertTrue(modals.tryOpenCreation())
    repeat(2) {
      assertFalse(
          modals.dispatchFeedback(true) {
            f.viewModel.onFeedbackDismiss(feedback.token, feedback.revision)
          }
      )
    }
    assertFalse(
        modals.dispatchFeedback(true) {
          f.viewModel.onFeedbackAction(feedback.token, feedback.revision)
        }
    )
    assertEquals(feedback, f.viewModel.uiState.value.feedback)
    assertEquals(0, f.repository.restores)
    creation.dismiss()
    assertFalse(
        modals.dispatchFeedback(true) {
          f.viewModel.onFeedbackDismiss(feedback.token, feedback.revision)
        }
    )
    creation.onSheetHidden(creation.token)
    assertFalse(
        modals.dispatchFeedback(false) {
          f.viewModel.onFeedbackDismiss(feedback.token, feedback.revision)
        }
    )
    assertTrue(
        modals.dispatchFeedback(true) {
          f.viewModel.onFeedbackAction(feedback.token, feedback.revision)
        }
    )
    runCurrent()
    assertEquals(1, f.repository.restores)
    assertEquals(MovimientoFeedbackKind.Restored, f.viewModel.uiState.value.feedback?.kind)
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
    assertNull(f.viewModel.uiState.value.feedback)
    f.viewModel.onSheetHidden(assertNotNull(f.viewModel.uiState.value.session).token)
    val feedback = assertNotNull(f.viewModel.uiState.value.feedback)
    f.repository.restoreGate = CompletableDeferred()
    f.viewModel.onFeedbackAction(feedback.token, feedback.revision)
    f.viewModel.onFeedbackAction(feedback.token, feedback.revision)
    f.viewModel.onFeedbackDismiss(feedback.token, feedback.revision)
    assertTrue(f.viewModel.uiState.value.isRestoring)
    assertFalse(f.viewModel.open("movement"))
    runCurrent()
    assertEquals(1, f.repository.restores)
    assertEquals(captured, f.repository.restored)
    f.repository.restoreGate?.complete(Unit)
    runCurrent()
    assertEquals(MovimientoFeedbackKind.Restored, f.viewModel.uiState.value.feedback?.kind)
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
    val old = assertNotNull(f.viewModel.uiState.value.feedback)
    f.repository.current = managementMovimiento().copy(id = "next")
    f.viewModel.open("next")
    runCurrent()
    f.viewModel.onRequestDismiss()
    f.viewModel.onSheetHidden(assertNotNull(f.viewModel.uiState.value.session).token)
    assertEquals(old, f.viewModel.uiState.value.feedback)
    f.viewModel.open("next")
    runCurrent()
    f.viewModel.onAmountChanged("20.00")
    f.viewModel.onSubmit()
    runCurrent()
    val newerOrigin = assertNotNull(f.viewModel.uiState.value.session).token
    f.viewModel.onSheetHidden(deletedOrigin)
    assertEquals(newerOrigin, f.viewModel.uiState.value.session?.token)
    f.viewModel.onSheetHidden(newerOrigin)
    assertEquals(old, f.viewModel.uiState.value.feedback)
    f.viewModel.onFeedbackDismiss(old.token, old.revision)
    val saved = assertNotNull(f.viewModel.uiState.value.feedback)
    assertEquals(MovimientoFeedbackKind.Saved, saved.kind)
    f.viewModel.onFeedbackAction(old.token, old.revision)
    f.viewModel.onFeedbackDismiss(old.token, old.revision)
    runCurrent()
    assertEquals(0, f.repository.restores)
    assertEquals(saved, f.viewModel.uiState.value.feedback)
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
    val feedback = assertNotNull(f.viewModel.uiState.value.feedback)
    f.viewModel.onFeedbackDismiss(feedback.token, feedback.revision)
    f.viewModel.onFeedbackAction(feedback.token, feedback.revision)
    runCurrent()
    assertNull(f.viewModel.uiState.value.feedback)
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
    val first = assertNotNull(f.viewModel.uiState.value.feedback)
    f.repository.restoreResult = managementFailure()
    f.viewModel.onFeedbackAction(first.token, first.revision)
    runCurrent()
    val failed = assertNotNull(f.viewModel.uiState.value.feedback)
    assertEquals(MovimientoFeedbackKind.RestoreFailed, failed.kind)
    assertTrue(failed.revision > first.revision)
    f.viewModel.onFeedbackDismiss(first.token, first.revision)
    f.viewModel.onFeedbackAction(first.token, first.revision)
    runCurrent()
    assertEquals(1, f.repository.restores)
    assertEquals(failed, f.viewModel.uiState.value.feedback)
    f.repository.restoreResult = DataOutcome.Success(RestoreResult.AlreadyPresent)
    f.viewModel.onFeedbackAction(failed.token, failed.revision)
    runCurrent()
    assertEquals(2, f.repository.restores)
    assertEquals(MovimientoFeedbackKind.Restored, f.viewModel.uiState.value.feedback?.kind)
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
    val feedback = assertNotNull(f.viewModel.uiState.value.feedback)
    f.repository.restoreResult = DataOutcome.Success(RestoreResult.Conflict)
    f.viewModel.onFeedbackAction(feedback.token, feedback.revision)
    runCurrent()
    val conflict = assertNotNull(f.viewModel.uiState.value.feedback)
    assertEquals(MovimientoFeedbackKind.RestoreConflict, conflict.kind)
    assertFalse(conflict.hasAction)
    f.viewModel.onFeedbackAction(conflict.token, conflict.revision)
    runCurrent()
    assertEquals(1, f.repository.restores)
  }

  @Test
  fun restoreWithoutItsCuentaReleasesSnapshotAsInformationalFailure() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onRequestDelete()
    f.viewModel.onConfirmDelete()
    runCurrent()
    f.viewModel.onSheetHidden(assertNotNull(f.viewModel.uiState.value.session).token)
    val feedback = assertNotNull(f.viewModel.uiState.value.feedback)
    f.repository.restoreResult = DataOutcome.Success(RestoreResult.MissingCuenta)
    f.viewModel.onFeedbackAction(feedback.token, feedback.revision)
    runCurrent()
    val failed = assertNotNull(f.viewModel.uiState.value.feedback)
    assertEquals(MovimientoFeedbackKind.RestoreFailed, failed.kind)
    assertNull(failed.deletedMovimiento)
    assertFalse(failed.hasAction)
    f.viewModel.onFeedbackAction(failed.token, failed.revision)
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
    val feedback = assertNotNull(f.viewModel.uiState.value.feedback)
    assertEquals(MovimientoFeedbackKind.Missing, feedback.kind)
    assertFalse(feedback.hasAction)
  }
}
