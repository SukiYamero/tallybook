package com.kurobello.tallybook.core.ui.feedback

import com.kurobello.tallybook.core.data.FakeRecurrenciaRepository
import com.kurobello.tallybook.core.data.FakeTransferenciaRepository
import com.kurobello.tallybook.core.data.TransferenciaRepository
import com.kurobello.tallybook.core.model.BudgetNotice
import com.kurobello.tallybook.core.model.BudgetState
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.RestoreResult
import com.kurobello.tallybook.core.model.Transferencia
import com.kurobello.tallybook.feature.movimientos.viewmodel.ManagementRepository
import com.kurobello.tallybook.feature.movimientos.viewmodel.managementFailure
import com.kurobello.tallybook.feature.movimientos.viewmodel.managementMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ManagementFeedbackQueueTest {
  @Test
  fun mixedSnapshotsWaitInArrivalOrderAndRestoreThroughTheirOwnRepository() = runTest {
    val f = QueueFixture(backgroundScope)
    val movimiento = UndoSnapshot.OfMovimiento(managementMovimiento())
    val transferencia = UndoSnapshot.OfTransferencia(queueTransferencia())
    f.enqueue(
        ManagementOutcome(ManagementSubject.Movimiento, ManagementFeedbackKind.Deleted, movimiento)
    )
    f.enqueue(
        ManagementOutcome(
            ManagementSubject.Transferencia,
            ManagementFeedbackKind.Deleted,
            transferencia,
        )
    )
    f.enqueue(ManagementOutcome(ManagementSubject.Transferencia, ManagementFeedbackKind.Saved))

    val first = assertNotNull(f.queue.state.value.feedback)
    assertEquals(movimiento, first.undo)
    assertEquals(3, f.queue.state.value.feedbackQueue.size)
    f.queue.onFeedbackAction(first.token, first.revision)
    runCurrent()
    assertEquals(managementMovimiento(), f.movimientos.restored)
    assertEquals(0, f.transferencias.restores)
    val restored = assertNotNull(f.queue.state.value.feedback)
    assertEquals(ManagementFeedbackKind.Restored, restored.kind)
    f.queue.onFeedbackDismiss(restored.token, restored.revision)

    val second = assertNotNull(f.queue.state.value.feedback)
    assertEquals(transferencia, second.undo)
    assertTrue(second.token > first.token)
    f.queue.onFeedbackAction(second.token, second.revision)
    runCurrent()
    assertEquals(1, f.movimientos.restores)
    assertEquals(1, f.transferencias.restores)
    assertEquals(ManagementFeedbackKind.Restored, f.queue.state.value.feedback?.kind)
    f.queue.onFeedbackDismiss(
        assertNotNull(f.queue.state.value.feedback).token,
        assertNotNull(f.queue.state.value.feedback).revision,
    )

    val third = assertNotNull(f.queue.state.value.feedback)
    assertEquals(ManagementFeedbackKind.Saved, third.kind)
    assertFalse(third.presentation().action != null)
    assertTrue(third.token > second.token)
  }

  @Test
  fun onlyTheVisibleTokenClaimsItsSnapshotOnce() = runTest {
    val f = QueueFixture(backgroundScope)
    f.enqueue(
        ManagementOutcome(
            ManagementSubject.Transferencia,
            ManagementFeedbackKind.Deleted,
            f.transferSnapshot,
        )
    )
    f.enqueue(
        ManagementOutcome(
            ManagementSubject.Movimiento,
            ManagementFeedbackKind.Deleted,
            UndoSnapshot.OfMovimiento(managementMovimiento()),
        )
    )
    val head = assertNotNull(f.queue.state.value.feedback)
    val waiting = f.queue.state.value.feedbackQueue[1]

    f.queue.onFeedbackAction(waiting.token, waiting.revision)
    runCurrent()
    assertEquals(0, f.movimientos.restores)

    f.transferencias.restoreGate = CompletableDeferred()
    f.queue.onFeedbackAction(head.token, head.revision)
    val claimed = assertNotNull(f.queue.state.value.feedback)
    assertTrue(claimed.isRestoring)
    assertTrue(f.queue.state.value.isRestoring)
    f.queue.onFeedbackAction(head.token, head.revision)
    f.queue.onFeedbackAction(claimed.token, claimed.revision)
    f.queue.onFeedbackDismiss(head.token, head.revision)
    f.queue.onFeedbackDismiss(claimed.token, claimed.revision)
    runCurrent()
    assertEquals(1, f.transferencias.restores)
    assertEquals(claimed, f.queue.state.value.feedback)
    assertEquals(waiting, f.queue.state.value.feedbackQueue[1])

    f.transferencias.restoreGate?.complete(Unit)
    runCurrent()
    val restored = assertNotNull(f.queue.state.value.feedback)
    assertEquals(ManagementFeedbackKind.Restored, restored.kind)
    assertFalse(restored.presentation().action != null)
    assertEquals(waiting, f.queue.state.value.feedbackQueue[1])
  }

  @Test
  fun failedTransferRestoreKeepsItsSnapshotForRetryUnderAFreshRevision() = runTest {
    val f = QueueFixture(backgroundScope)
    f.enqueue(
        ManagementOutcome(
            ManagementSubject.Transferencia,
            ManagementFeedbackKind.Deleted,
            f.transferSnapshot,
        )
    )
    val first = assertNotNull(f.queue.state.value.feedback)
    f.transferencias.restoreResult = managementFailure()
    f.queue.onFeedbackAction(first.token, first.revision)
    runCurrent()

    val failed = assertNotNull(f.queue.state.value.feedback)
    assertEquals(ManagementFeedbackKind.RestoreFailed, failed.kind)
    assertEquals(first.token, failed.token)
    assertTrue(failed.revision > first.revision)
    assertEquals(f.transferSnapshot, failed.undo)
    f.queue.onFeedbackAction(first.token, first.revision)
    f.queue.onFeedbackDismiss(first.token, first.revision)
    runCurrent()
    assertEquals(1, f.transferencias.restores)
    assertEquals(failed, f.queue.state.value.feedback)

    f.transferencias.restoreResult = DataOutcome.Success(RestoreResult.AlreadyPresent)
    f.queue.onFeedbackAction(failed.token, failed.revision)
    runCurrent()
    assertEquals(2, f.transferencias.restores)
    assertEquals(ManagementFeedbackKind.Restored, f.queue.state.value.feedback?.kind)
  }

  @Test
  fun missingCuentaAndConflictReleaseTheTransferSnapshotWithoutRetry() = runTest {
    val expectations =
        mapOf(
            RestoreResult.MissingCuenta to ManagementFeedbackKind.RestoreMissingCuenta,
            RestoreResult.Conflict to ManagementFeedbackKind.RestoreConflict,
        )
    expectations.forEach { (result, kind) ->
      val f = QueueFixture(backgroundScope)
      f.enqueue(
          ManagementOutcome(
              ManagementSubject.Transferencia,
              ManagementFeedbackKind.Deleted,
              f.transferSnapshot,
          )
      )
      val head = assertNotNull(f.queue.state.value.feedback)
      f.transferencias.restoreResult = DataOutcome.Success(result)
      f.queue.onFeedbackAction(head.token, head.revision)
      runCurrent()

      val released = assertNotNull(f.queue.state.value.feedback)
      assertEquals(kind, released.kind)
      assertNull(released.undo)
      f.queue.onFeedbackAction(released.token, released.revision)
      runCurrent()
      assertEquals(1, f.transferencias.restores)
    }
  }

  @Test
  fun closeOrExpiryReleasesOnlyTheVisibleTokenAndAdvances() = runTest {
    val f = QueueFixture(backgroundScope)
    f.enqueue(
        ManagementOutcome(
            ManagementSubject.Transferencia,
            ManagementFeedbackKind.Deleted,
            f.transferSnapshot,
        )
    )
    f.enqueue(ManagementOutcome(ManagementSubject.Transferencia, ManagementFeedbackKind.Missing))
    val head = assertNotNull(f.queue.state.value.feedback)
    val waiting = f.queue.state.value.feedbackQueue[1]

    f.queue.onFeedbackDismiss(waiting.token, waiting.revision)
    assertEquals(head, f.queue.state.value.feedback)
    f.queue.onFeedbackDismiss(head.token, head.revision)
    assertEquals(waiting, f.queue.state.value.feedback)
    f.queue.onFeedbackAction(head.token, head.revision)
    runCurrent()
    assertEquals(0, f.transferencias.restores)
    f.queue.onFeedbackDismiss(waiting.token, waiting.revision)
    assertNull(f.queue.state.value.feedback)
  }

  @Test
  fun openSessionBlocksClaimsAndAClaimedRestoreBlocksNewSessions() = runTest {
    val f = QueueFixture(backgroundScope)
    f.enqueue(
        ManagementOutcome(
            ManagementSubject.Transferencia,
            ManagementFeedbackKind.Deleted,
            f.transferSnapshot,
        )
    )
    val head = assertNotNull(f.queue.state.value.feedback)

    val session = assertNotNull(f.queue.openSession())
    f.queue.onFeedbackAction(head.token, head.revision)
    runCurrent()
    assertEquals(0, f.transferencias.restores)
    assertEquals(head, f.queue.state.value.feedback)
    session.close()

    f.transferencias.restoreGate = CompletableDeferred()
    f.queue.onFeedbackAction(head.token, head.revision)
    assertNull(f.queue.openSession())
    f.transferencias.restoreGate?.complete(Unit)
    runCurrent()
    assertNotNull(f.queue.openSession())
  }

  @Test
  fun subjectSurvivesReleaseOfTheSnapshot() = runTest {
    val f = QueueFixture(backgroundScope)
    f.enqueue(
        ManagementOutcome(
            ManagementSubject.Transferencia,
            ManagementFeedbackKind.Deleted,
            f.transferSnapshot,
        )
    )
    val head = assertNotNull(f.queue.state.value.feedback)
    assertEquals(ManagementSubject.Transferencia, head.subject)
    f.transferencias.restoreGate = CompletableDeferred()
    f.queue.onFeedbackAction(head.token, head.revision)
    assertEquals(ManagementSubject.Transferencia, f.queue.state.value.feedback?.subject)
    f.transferencias.restoreResult = DataOutcome.Success(RestoreResult.Conflict)
    f.transferencias.restoreGate?.complete(Unit)
    runCurrent()

    val released = assertNotNull(f.queue.state.value.feedback)
    assertNull(released.undo)
    assertEquals(ManagementSubject.Transferencia, released.subject)
  }

  @Test
  fun everyOpenSessionHoldsItsOwnSlotAndClosesOnlyOnce() = runTest {
    val f = QueueFixture(backgroundScope)
    f.enqueue(
        ManagementOutcome(
            ManagementSubject.Transferencia,
            ManagementFeedbackKind.Deleted,
            f.transferSnapshot,
        )
    )
    val head = assertNotNull(f.queue.state.value.feedback)
    val first = assertNotNull(f.queue.openSession())
    val second = assertNotNull(f.queue.openSession())
    assertEquals(2, f.queue.state.value.openSessions)

    first.close(ManagementOutcome(ManagementSubject.Movimiento, ManagementFeedbackKind.Saved))
    first.close(ManagementOutcome(ManagementSubject.Movimiento, ManagementFeedbackKind.Saved))
    first.close()
    assertEquals(1, f.queue.state.value.openSessions)
    assertEquals(2, f.queue.state.value.feedbackQueue.size)
    f.queue.onFeedbackAction(head.token, head.revision)
    runCurrent()
    assertEquals(0, f.transferencias.restores)

    second.close()
    assertEquals(0, f.queue.state.value.openSessions)
    f.queue.onFeedbackAction(head.token, head.revision)
    runCurrent()
    assertEquals(1, f.transferencias.restores)
  }

  @Test
  fun clearReleasesTheWholeQueue() = runTest {
    val f = QueueFixture(backgroundScope)
    f.enqueue(
        ManagementOutcome(
            ManagementSubject.Transferencia,
            ManagementFeedbackKind.Deleted,
            f.transferSnapshot,
        )
    )
    f.enqueue(ManagementOutcome(ManagementSubject.Transferencia, ManagementFeedbackKind.Saved))
    val head = assertNotNull(f.queue.state.value.feedback)

    f.queue.clear()
    assertEquals(ManagementFeedbackState(), f.queue.state.value)
    f.queue.onFeedbackAction(head.token, head.revision)
    runCurrent()
    assertEquals(0, f.transferencias.restores)
  }

  @Test
  fun viewDismissesOnlyTheVisibleNoticeAndAdvances() = runTest {
    val f = QueueFixture(backgroundScope)
    f.queue.enqueue(
        ManagementOutcome(
            ManagementSubject.Presupuesto,
            ManagementFeedbackKind.BudgetNotice,
            budget = QUEUE_NOTICE,
        )
    )
    f.enqueue(ManagementOutcome(ManagementSubject.Transferencia, ManagementFeedbackKind.Saved))
    val head = assertNotNull(f.queue.state.value.feedback)
    val waiting = f.queue.state.value.feedbackQueue[1]
    assertEquals(QUEUE_NOTICE, head.budget)
    assertTrue(head.presentation().action != null)

    assertFalse(f.queue.onBudgetAction(waiting.token, waiting.revision))
    assertFalse(f.queue.onBudgetAction(head.token, head.revision + 1))
    assertTrue(f.queue.onBudgetAction(head.token, head.revision))

    assertEquals(listOf(waiting), f.queue.state.value.feedbackQueue)
    assertFalse(f.queue.onBudgetAction(waiting.token, waiting.revision))
  }

  @Test
  fun undoTakesTheBudgetLineAwayWithTheSave() = runTest {
    val f = QueueFixture(backgroundScope)
    f.enqueue(
        ManagementOutcome(
            ManagementSubject.Movimiento,
            ManagementFeedbackKind.Deleted,
            UndoSnapshot.OfMovimiento(managementMovimiento()),
            budget = QUEUE_NOTICE,
        )
    )
    val head = assertNotNull(f.queue.state.value.feedback)
    assertFalse(f.queue.onBudgetAction(head.token, head.revision))

    f.queue.onFeedbackAction(head.token, head.revision)
    assertNull(f.queue.state.value.feedback?.budget)
    runCurrent()

    val restored = assertNotNull(f.queue.state.value.feedback)
    assertEquals(ManagementFeedbackKind.Restored, restored.kind)
    assertNull(restored.budget)
    assertFalse(restored.presentation().action != null)
  }
}

private val QUEUE_NOTICE =
    BudgetNotice("Comida", BudgetState.OVER, Money(2_000_000, Moneda.COP), paceDay = null)

private class QueueFixture(scope: CoroutineScope) {
  val movimientos = ManagementRepository()
  val transferencias =
      GatedTransferencias().apply { restoreResult = DataOutcome.Success(RestoreResult.Restored) }
  val queue =
      ManagementFeedbackViewModel(movimientos, transferencias, FakeRecurrenciaRepository(), scope)
          .queue
  val transferSnapshot = UndoSnapshot.OfTransferencia(queueTransferencia())

  fun enqueue(outcome: ManagementOutcome) {
    assertNotNull(queue.openSession()).close(outcome)
  }
}

private class GatedTransferencias(
    private val delegate: FakeTransferenciaRepository = FakeTransferenciaRepository()
) : TransferenciaRepository by delegate {
  var restoreGate: CompletableDeferred<Unit>? = null
  var restoreResult: DataOutcome<RestoreResult>?
    get() = delegate.restoreResult
    set(value) {
      delegate.restoreResult = value
    }

  var restores = 0
    private set

  override suspend fun restoreDeleted(transferencia: Transferencia): DataOutcome<RestoreResult> {
    restores++
    restoreGate?.await()
    return delegate.restoreDeleted(transferencia)
  }
}

private fun queueTransferencia() =
    Transferencia(
        id = "transfer",
        cuentaOrigenId = Cuenta.defaultCashId(Moneda.USD),
        cuentaDestinoId = "card",
        montoOrigen = Money(5_000, Moneda.USD),
        montoDestino = Money(5_000, Moneda.USD),
        fecha = LocalDate(2026, 10, 16),
        descripcion = null,
        createdAt = Instant.parse("2026-10-16T10:00:00Z"),
    )
