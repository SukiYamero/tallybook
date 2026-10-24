package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.RestoreResult
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarDuration
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarMessage
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarTimer
import com.kurobello.tallybook.core.ui.component.tallybookSnackbarTimeoutMillis
import com.kurobello.tallybook.core.ui.feedback.ManagementFeedbackKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ManageMovimientoFeedbackTest {
  @Test
  fun completedOperationsWaitInOrderWithoutReplacingTheCurrentFeedback() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    deleteAndHide(f, managementMovimiento())
    val deleted = assertNotNull(f.feedback.state.value.feedback)
    deleteAndHide(f, managementMovimiento().copy(id = "second"))
    assertEquals(deleted, f.feedback.state.value.feedback)
    deleteAndHide(f, managementMovimiento().copy(id = "third"), missing = true)
    assertEquals(deleted, f.feedback.state.value.feedback)

    f.feedback.onFeedbackDismiss(deleted.token, deleted.revision)
    val second = assertNotNull(f.feedback.state.value.feedback)
    assertEquals(ManagementFeedbackKind.Deleted, second.kind)
    assertTrue(second.token > deleted.token)
    f.feedback.onFeedbackDismiss(second.token, second.revision)
    val third = assertNotNull(f.feedback.state.value.feedback)
    assertEquals(ManagementFeedbackKind.Missing, third.kind)
    assertTrue(third.token > second.token)
    f.feedback.onFeedbackDismiss(third.token, third.revision)
    assertNull(f.feedback.state.value.feedback)
  }

  @Test
  fun backlogCompactsActiveAndLastQueuedFeedbackThenAnIsolatedMessageGetsItsFullWindow() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    deleteAndHide(f, managementMovimiento())
    val first = assertNotNull(f.feedback.state.value.feedback)
    val timer =
        TallybookSnackbarTimer(this, testScheduler.timeSource) { token, revision ->
          f.feedback.onFeedbackDismiss(token, revision)
          true
        }
    updateFeedbackTimer(timer, f)
    advanceTimeBy(3_000)
    deleteAndHide(f, managementMovimiento().copy(id = "second"), missing = true)
    assertEquals(first, f.feedback.state.value.feedback)
    updateFeedbackTimer(timer, f)
    advanceTimeBy(1_849)
    runCurrent()
    assertEquals(first, f.feedback.state.value.feedback)
    advanceTimeBy(1)
    runCurrent()
    val second = assertNotNull(f.feedback.state.value.feedback)
    assertEquals(ManagementFeedbackKind.Missing, second.kind)
    updateFeedbackTimer(timer, f)
    advanceTimeBy(1_849)
    runCurrent()
    assertEquals(second, f.feedback.state.value.feedback)
    advanceTimeBy(1)
    runCurrent()
    assertNull(f.feedback.state.value.feedback)
    updateFeedbackTimer(timer, f)
    deleteAndHide(f, managementMovimiento().copy(id = "third"), missing = true)
    val isolated = assertNotNull(f.feedback.state.value.feedback)
    updateFeedbackTimer(timer, f)
    advanceTimeBy(3_699)
    runCurrent()
    assertEquals(isolated, f.feedback.state.value.feedback)
    advanceTimeBy(1)
    runCurrent()
    assertNull(f.feedback.state.value.feedback)
    timer.dispose()
  }

  @Test
  fun eachQueuedDeletionUndoesItsOwnSnapshotAndRejectsStaleIdentity() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    val firstSnapshot = managementMovimiento().copy(descripcion = "First committed version")
    val secondSnapshot = managementMovimiento().copy(id = "second", descripcion = "Second version")
    deleteAndHide(f, firstSnapshot)
    val first = assertNotNull(f.feedback.state.value.feedback)
    deleteAndHide(f, secondSnapshot)

    f.feedback.onFeedbackAction(first.token, first.revision)
    runCurrent()
    assertEquals(firstSnapshot, f.repository.restored)
    val restored = assertNotNull(f.feedback.state.value.feedback)
    assertEquals(ManagementFeedbackKind.Restored, restored.kind)
    f.feedback.onFeedbackDismiss(first.token, first.revision)
    assertEquals(restored, f.feedback.state.value.feedback)
    f.feedback.onFeedbackDismiss(restored.token, restored.revision)
    val second = assertNotNull(f.feedback.state.value.feedback)

    f.feedback.onFeedbackAction(first.token, first.revision)
    f.feedback.onFeedbackDismiss(restored.token, restored.revision)
    runCurrent()
    assertEquals(1, f.repository.restores)
    assertEquals(second, f.feedback.state.value.feedback)
    f.feedback.onFeedbackAction(second.token, second.revision)
    runCurrent()
    assertEquals(2, f.repository.restores)
    assertEquals(secondSnapshot, f.repository.restored)
  }

  @Test
  fun failedUndoAndRetryKeepWaitingFeedbackUntilTheCurrentResultIsDismissed() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    val snapshot = managementMovimiento()
    deleteAndHide(f, snapshot)
    val first = assertNotNull(f.feedback.state.value.feedback)
    deleteAndHide(f, managementMovimiento().copy(id = "second"), missing = true)
    f.repository.restoreResult = managementFailure()
    f.feedback.onFeedbackAction(first.token, first.revision)
    runCurrent()
    val failed = assertNotNull(f.feedback.state.value.feedback)
    assertEquals(ManagementFeedbackKind.RestoreFailed, failed.kind)
    assertEquals(first.token, failed.token)
    f.feedback.onFeedbackDismiss(first.token, first.revision)
    f.feedback.onFeedbackAction(first.token, first.revision)
    assertEquals(failed, f.feedback.state.value.feedback)

    f.repository.restoreResult = DataOutcome.Success(RestoreResult.Restored)
    f.feedback.onFeedbackAction(failed.token, failed.revision)
    runCurrent()
    val restored = assertNotNull(f.feedback.state.value.feedback)
    assertEquals(ManagementFeedbackKind.Restored, restored.kind)
    assertEquals(first.token, restored.token)
    assertEquals(snapshot, f.repository.restored)
    assertEquals(2, f.repository.restores)
    f.feedback.onFeedbackDismiss(restored.token, restored.revision)
    assertEquals(ManagementFeedbackKind.Missing, f.feedback.state.value.feedback?.kind)
  }

  @Test
  fun busyHeadAndOpenSessionRejectActionsWithoutDroppingWaitingFeedback() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    deleteAndHide(f, managementMovimiento())
    val first = assertNotNull(f.feedback.state.value.feedback)
    f.repository.current = managementMovimiento().copy(id = "second")
    assertTrue(f.viewModel.open("second"))
    runCurrent()
    f.feedback.onFeedbackAction(first.token, first.revision)
    runCurrent()
    assertEquals(0, f.repository.restores)
    f.viewModel.onRequestDelete()
    f.viewModel.onConfirmDelete()
    runCurrent()
    f.viewModel.onSheetHidden(assertNotNull(f.viewModel.uiState.value.session).token)

    f.repository.restoreGate = CompletableDeferred()
    f.feedback.onFeedbackAction(first.token, first.revision)
    val busy = assertNotNull(f.feedback.state.value.feedback)
    assertTrue(busy.isRestoring)
    assertFalse(f.viewModel.open("third"))
    f.feedback.onFeedbackAction(busy.token, busy.revision)
    f.feedback.onFeedbackDismiss(busy.token, busy.revision)
    runCurrent()
    assertEquals(1, f.repository.restores)
    assertEquals(busy, f.feedback.state.value.feedback)
    f.repository.restoreGate?.complete(Unit)
    runCurrent()
    val restored = assertNotNull(f.feedback.state.value.feedback)
    f.feedback.onFeedbackDismiss(restored.token, restored.revision)
    assertEquals(ManagementFeedbackKind.Deleted, f.feedback.state.value.feedback?.kind)
  }

  @Test
  fun missingCuentaFailureIsShortAndHandsOverToTheWaitingFeedback() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    deleteAndHide(f, managementMovimiento())
    val first = assertNotNull(f.feedback.state.value.feedback)
    deleteAndHide(f, managementMovimiento().copy(id = "second"), missing = true)
    f.repository.restoreResult = DataOutcome.Success(RestoreResult.MissingCuenta)
    f.feedback.onFeedbackAction(first.token, first.revision)
    runCurrent()
    val failed = assertNotNull(f.feedback.state.value.feedback)
    assertEquals(ManagementFeedbackKind.RestoreFailed, failed.kind)
    assertFalse(failed.hasAction)

    f.feedback.onFeedbackDismiss(failed.token, failed.revision)
    assertEquals(ManagementFeedbackKind.Missing, f.feedback.state.value.feedback?.kind)
  }
}

private fun updateFeedbackTimer(timer: TallybookSnackbarTimer, fixture: ManageMovimientoFixture) {
  val state = fixture.feedback.state.value
  val message =
      state.feedback?.let {
        TallybookSnackbarMessage(
            it.token,
            it.revision,
            "Operation complete",
            if (it.hasAction) TallybookSnackbarDuration.Long else TallybookSnackbarDuration.Short,
            isBusy = it.isRestoring,
        )
      }
  val compacted = timer.shouldCompact(message, state.feedbackQueue.size > 1)
  timer.update(
      message,
      isActive = true,
      timeoutMillis = message?.let { tallybookSnackbarTimeoutMillis(it.duration, null, compacted) },
      isCompacted = compacted,
  )
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun TestScope.deleteAndHide(
    fixture: ManageMovimientoFixture,
    movimiento: Movimiento,
    missing: Boolean = false,
) {
  fixture.repository.current = movimiento
  fixture.repository.deleteResult = if (missing) DataOutcome.Success(null) else null
  assertTrue(fixture.viewModel.open(movimiento.id))
  runCurrent()
  fixture.viewModel.onRequestDelete()
  fixture.viewModel.onConfirmDelete()
  runCurrent()
  fixture.viewModel.onSheetHidden(assertNotNull(fixture.viewModel.uiState.value.session).token)
}
