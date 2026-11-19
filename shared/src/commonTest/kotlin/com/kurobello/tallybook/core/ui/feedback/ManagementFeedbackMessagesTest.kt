package com.kurobello.tallybook.core.ui.feedback

import com.kurobello.tallybook.core.model.BudgetNotice
import com.kurobello.tallybook.core.model.BudgetState
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.OccurrenceUndoResult
import com.kurobello.tallybook.core.model.RestoreResult
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarDuration
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarStatus
import com.kurobello.tallybook.feature.movimientos.viewmodel.managementMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.budget_notice_view
import tallybook.shared.generated.resources.movimiento_undo
import tallybook.shared.generated.resources.movimiento_updated
import tallybook.shared.generated.resources.recurrencia_confirmed

class ManagementFeedbackMessagesTest {
  @Test
  fun everySubjectHasItsOwnMessageForEveryKindItProducesAndForRestoring() {
    val occurrenceKinds = setOf(ManagementFeedbackKind.Confirmed, ManagementFeedbackKind.Skipped)
    val recordKinds =
        ManagementFeedbackKind.entries.toSet() -
            occurrenceKinds -
            ManagementFeedbackKind.BudgetNotice
    assertEquals(
        recordKinds,
        managementFeedbackMessages.getValue(ManagementSubject.Movimiento).keys,
    )
    assertEquals(
        recordKinds,
        managementFeedbackMessages.getValue(ManagementSubject.Transferencia).keys,
    )
    assertEquals(
        setOf(
            ManagementFeedbackKind.Saved,
            ManagementFeedbackKind.Deleted,
            ManagementFeedbackKind.Restored,
            ManagementFeedbackKind.RestoreFailed,
            ManagementFeedbackKind.RestoreConflict,
        ) + occurrenceKinds,
        managementFeedbackMessages.getValue(ManagementSubject.Recurrencia).keys,
    )
    val recordSubjects = ManagementSubject.entries.toSet() - ManagementSubject.Presupuesto
    assertEquals(recordSubjects, managementFeedbackMessages.keys)
    recordKinds.forEach { kind ->
      assertNotEquals(
          managementFeedbackMessages.getValue(ManagementSubject.Movimiento).getValue(kind),
          managementFeedbackMessages.getValue(ManagementSubject.Transferencia).getValue(kind),
      )
    }
    assertEquals(recordSubjects, managementFeedbackRestoring.keys)
    assertNotEquals(
        managementFeedbackRestoring.getValue(ManagementSubject.Movimiento),
        managementFeedbackRestoring.getValue(ManagementSubject.Transferencia),
    )
  }

  @Test
  fun everyKindHasAStatusAndAMissingCuentaIsAWarning() {
    assertEquals(ManagementFeedbackKind.entries.toSet(), managementFeedbackStatuses.keys)
    assertEquals(
        TallybookSnackbarStatus.Warning,
        managementFeedbackStatuses.getValue(ManagementFeedbackKind.RestoreMissingCuenta),
    )
  }

  @Test
  fun everyRestoreResultMapsToItsFeedbackKind() {
    assertEquals(
        mapOf(
            RestoreResult.Restored to ManagementFeedbackKind.Restored,
            RestoreResult.AlreadyPresent to ManagementFeedbackKind.Restored,
            RestoreResult.Conflict to ManagementFeedbackKind.RestoreConflict,
            RestoreResult.MissingCuenta to ManagementFeedbackKind.RestoreMissingCuenta,
        ),
        restoreFeedbackKinds,
    )
  }

  @Test
  fun onlyUndoableWritesAndRetryableFailuresOfferAnAction() {
    assertEquals(
        setOf(
            ManagementFeedbackKind.Deleted,
            ManagementFeedbackKind.Confirmed,
            ManagementFeedbackKind.Skipped,
            ManagementFeedbackKind.RestoreFailed,
        ),
        managementFeedbackActions.keys,
    )
  }

  @Test
  fun standaloneNoticeIsShortWithViewAction() {
    val presentation =
        feedback(ManagementSubject.Presupuesto, ManagementFeedbackKind.BudgetNotice, budget = NEAR)
            .presentation()

    assertNull(presentation.message)
    assertEquals(NEAR, presentation.budget)
    assertEquals(Res.string.budget_notice_view, presentation.action)
    assertEquals(TallybookSnackbarDuration.Short, presentation.duration)
    assertEquals(TallybookSnackbarStatus.Info, presentation.status)
  }

  @Test
  fun mergedNoticeWithoutUndoKeepsTheBaseMessageAndOffersViewForLong() {
    val presentation =
        feedback(ManagementSubject.Movimiento, ManagementFeedbackKind.Saved, budget = NEAR)
            .presentation()

    assertEquals(Res.string.movimiento_updated, presentation.message)
    assertEquals(NEAR, presentation.budget)
    assertEquals(Res.string.budget_notice_view, presentation.action)
    assertEquals(TallybookSnackbarDuration.Long, presentation.duration)
    assertEquals(TallybookSnackbarStatus.Success, presentation.status)
  }

  @Test
  fun undoActionWinsOverView() {
    val withUndo =
        feedback(
            ManagementSubject.Recurrencia,
            ManagementFeedbackKind.Confirmed,
            budget = NEAR,
            undo = UndoSnapshot.OfMovimiento(managementMovimiento()),
        )

    val presentation = withUndo.presentation()

    assertFalse(withUndo.opensBudgets)
    assertEquals(Res.string.recurrencia_confirmed, presentation.message)
    assertEquals(NEAR, presentation.budget)
    assertEquals(Res.string.movimiento_undo, presentation.action)
    assertEquals(TallybookSnackbarDuration.Long, presentation.duration)
  }

  @Test
  fun feedbackWithoutABudgetKeepsItsPresentation() {
    val presentation =
        feedback(ManagementSubject.Movimiento, ManagementFeedbackKind.Saved).presentation()

    assertEquals(Res.string.movimiento_updated, presentation.message)
    assertNull(presentation.budget)
    assertNull(presentation.action)
    assertEquals(TallybookSnackbarDuration.Short, presentation.duration)
  }

  @Test
  fun nearNoticeWithNothingLeftSaysTheBudgetIsUsed() {
    assertEquals(BudgetNoticeLine.Reached, NEAR.copy(amount = Money(0, Moneda.COP)).line())
    assertEquals(BudgetNoticeLine.Left(NEAR.amount), NEAR.line())
    assertEquals(
        BudgetNoticeLine.Over,
        NEAR.copy(state = BudgetState.OVER, amount = Money(0, Moneda.COP)).line(),
    )
    assertEquals(
        BudgetNoticeLine.Pace(22),
        NEAR.copy(state = BudgetState.PACE, paceDay = 22).line(),
    )
  }

  @Test
  fun everyOccurrenceUndoResultMapsToARestoreResult() {
    assertEquals(
        mapOf(
            OccurrenceUndoResult.Undone to RestoreResult.Restored,
            OccurrenceUndoResult.Conflict to RestoreResult.Conflict,
        ),
        occurrenceRestoreResults,
    )
  }
}

private val NEAR = BudgetNotice("Comida", BudgetState.NEAR, Money(3_000_000, Moneda.COP), null)

private fun feedback(
    subject: ManagementSubject,
    kind: ManagementFeedbackKind,
    budget: BudgetNotice? = null,
    undo: UndoSnapshot? = null,
) = ManagementFeedback(token = 1, subject = subject, kind = kind, undo = undo, budget = budget)
