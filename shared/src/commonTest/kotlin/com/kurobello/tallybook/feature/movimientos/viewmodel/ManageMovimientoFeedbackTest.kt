package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoRestoreResult
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarDuration
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarMessage
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarTimer
import com.kurobello.tallybook.core.ui.component.tallybookSnackbarTimeoutMillis
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
    val deleted = assertNotNull(f.viewModel.uiState.value.feedback)
    deleteAndHide(f, managementMovimiento().copy(id = "second"))
    assertEquals(deleted, f.viewModel.uiState.value.feedback)
    deleteAndHide(f, managementMovimiento().copy(id = "third"), missing = true)
    assertEquals(deleted, f.viewModel.uiState.value.feedback)

    f.viewModel.onFeedbackDismiss(deleted.token, deleted.revision)
    val second = assertNotNull(f.viewModel.uiState.value.feedback)
    assertEquals(MovimientoFeedbackKind.Deleted, second.kind)
    assertTrue(second.token > deleted.token)
    f.viewModel.onFeedbackDismiss(second.token, second.revision)
    val third = assertNotNull(f.viewModel.uiState.value.feedback)
    assertEquals(MovimientoFeedbackKind.Missing, third.kind)
    assertTrue(third.token > second.token)
    f.viewModel.onFeedbackDismiss(third.token, third.revision)
    assertNull(f.viewModel.uiState.value.feedback)
  }

  @Test
  fun backlogCompactsActiveAndLastQueuedFeedbackThenAnIsolatedMessageGetsItsFullWindow() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    deleteAndHide(f, managementMovimiento())
    val first = assertNotNull(f.viewModel.uiState.value.feedback)
    val timer =
        TallybookSnackbarTimer(this, testScheduler.timeSource, f.viewModel::onFeedbackDismiss)
    updateFeedbackTimer(timer, f)
    advanceTimeBy(3_000)
    deleteAndHide(f, managementMovimiento().copy(id = "second"), missing = true)
    assertEquals(first, f.viewModel.uiState.value.feedback)
    updateFeedbackTimer(timer, f)
    advanceTimeBy(1_999)
    runCurrent()
    assertEquals(first, f.viewModel.uiState.value.feedback)
    advanceTimeBy(1)
    runCurrent()
    val second = assertNotNull(f.viewModel.uiState.value.feedback)
    assertEquals(MovimientoFeedbackKind.Missing, second.kind)
    updateFeedbackTimer(timer, f)
    advanceTimeBy(1_999)
    runCurrent()
    assertEquals(second, f.viewModel.uiState.value.feedback)
    advanceTimeBy(1)
    runCurrent()
    assertNull(f.viewModel.uiState.value.feedback)
    updateFeedbackTimer(timer, f)
    deleteAndHide(f, managementMovimiento().copy(id = "third"), missing = true)
    val isolated = assertNotNull(f.viewModel.uiState.value.feedback)
    updateFeedbackTimer(timer, f)
    advanceTimeBy(3_999)
    runCurrent()
    assertEquals(isolated, f.viewModel.uiState.value.feedback)
    advanceTimeBy(1)
    runCurrent()
    assertNull(f.viewModel.uiState.value.feedback)
    timer.dispose()
  }

  @Test
  fun eachQueuedDeletionUndoesItsOwnSnapshotAndRejectsStaleIdentity() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    val firstSnapshot = managementMovimiento().copy(descripcion = "First committed version")
    val secondSnapshot = managementMovimiento().copy(id = "second", descripcion = "Second version")
    deleteAndHide(f, firstSnapshot)
    val first = assertNotNull(f.viewModel.uiState.value.feedback)
    deleteAndHide(f, secondSnapshot)

    f.viewModel.onFeedbackAction(first.token, first.revision)
    runCurrent()
    assertEquals(firstSnapshot, f.repository.restored)
    val restored = assertNotNull(f.viewModel.uiState.value.feedback)
    assertEquals(MovimientoFeedbackKind.Restored, restored.kind)
    f.viewModel.onFeedbackDismiss(first.token, first.revision)
    assertEquals(restored, f.viewModel.uiState.value.feedback)
    f.viewModel.onFeedbackDismiss(restored.token, restored.revision)
    val second = assertNotNull(f.viewModel.uiState.value.feedback)

    f.viewModel.onFeedbackAction(first.token, first.revision)
    f.viewModel.onFeedbackDismiss(restored.token, restored.revision)
    runCurrent()
    assertEquals(1, f.repository.restores)
    assertEquals(second, f.viewModel.uiState.value.feedback)
    f.viewModel.onFeedbackAction(second.token, second.revision)
    runCurrent()
    assertEquals(2, f.repository.restores)
    assertEquals(secondSnapshot, f.repository.restored)
  }

  @Test
  fun failedUndoAndRetryKeepWaitingFeedbackUntilTheCurrentResultIsDismissed() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    val snapshot = managementMovimiento()
    deleteAndHide(f, snapshot)
    val first = assertNotNull(f.viewModel.uiState.value.feedback)
    deleteAndHide(f, managementMovimiento().copy(id = "second"), missing = true)
    f.repository.restoreResult = managementFailure()
    f.viewModel.onFeedbackAction(first.token, first.revision)
    runCurrent()
    val failed = assertNotNull(f.viewModel.uiState.value.feedback)
    assertEquals(MovimientoFeedbackKind.RestoreFailed, failed.kind)
    assertEquals(first.token, failed.token)
    f.viewModel.onFeedbackDismiss(first.token, first.revision)
    f.viewModel.onFeedbackAction(first.token, first.revision)
    assertEquals(failed, f.viewModel.uiState.value.feedback)

    f.repository.restoreResult = DataOutcome.Success(MovimientoRestoreResult.Restored)
    f.viewModel.onFeedbackAction(failed.token, failed.revision)
    runCurrent()
    val restored = assertNotNull(f.viewModel.uiState.value.feedback)
    assertEquals(MovimientoFeedbackKind.Restored, restored.kind)
    assertEquals(first.token, restored.token)
    assertEquals(snapshot, f.repository.restored)
    assertEquals(2, f.repository.restores)
    f.viewModel.onFeedbackDismiss(restored.token, restored.revision)
    assertEquals(MovimientoFeedbackKind.Missing, f.viewModel.uiState.value.feedback?.kind)
  }

  @Test
  fun busyHeadAndOpenSessionRejectActionsWithoutDroppingWaitingFeedback() = runTest {
    val f = ManageMovimientoFixture(backgroundScope)
    deleteAndHide(f, managementMovimiento())
    val first = assertNotNull(f.viewModel.uiState.value.feedback)
    f.repository.current = managementMovimiento().copy(id = "second")
    assertTrue(f.viewModel.open("second"))
    runCurrent()
    f.viewModel.onFeedbackAction(first.token, first.revision)
    runCurrent()
    assertEquals(0, f.repository.restores)
    f.viewModel.onRequestDelete()
    f.viewModel.onConfirmDelete()
    runCurrent()
    f.viewModel.onSheetHidden(assertNotNull(f.viewModel.uiState.value.session).token)

    f.repository.restoreGate = CompletableDeferred()
    f.viewModel.onFeedbackAction(first.token, first.revision)
    val busy = assertNotNull(f.viewModel.uiState.value.feedback)
    assertTrue(busy.isRestoring)
    assertFalse(f.viewModel.open("third"))
    f.viewModel.onFeedbackAction(busy.token, busy.revision)
    f.viewModel.onFeedbackDismiss(busy.token, busy.revision)
    runCurrent()
    assertEquals(1, f.repository.restores)
    assertEquals(busy, f.viewModel.uiState.value.feedback)
    f.repository.restoreGate?.complete(Unit)
    runCurrent()
    val restored = assertNotNull(f.viewModel.uiState.value.feedback)
    f.viewModel.onFeedbackDismiss(restored.token, restored.revision)
    assertEquals(MovimientoFeedbackKind.Deleted, f.viewModel.uiState.value.feedback?.kind)
  }
}

private fun updateFeedbackTimer(timer: TallybookSnackbarTimer, fixture: ManageMovimientoFixture) {
  val state = fixture.viewModel.uiState.value
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
