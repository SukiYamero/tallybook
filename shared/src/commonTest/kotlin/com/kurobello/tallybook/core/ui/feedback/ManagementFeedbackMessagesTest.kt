package com.kurobello.tallybook.core.ui.feedback

import com.kurobello.tallybook.core.model.OccurrenceUndoResult
import com.kurobello.tallybook.core.model.RestoreResult
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ManagementFeedbackMessagesTest {
  @Test
  fun everySubjectHasItsOwnMessageForEveryKindItProducesAndForRestoring() {
    val occurrenceKinds = setOf(ManagementFeedbackKind.Confirmed, ManagementFeedbackKind.Skipped)
    val recordKinds = ManagementFeedbackKind.entries.toSet() - occurrenceKinds
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
    assertEquals(ManagementSubject.entries.toSet(), managementFeedbackMessages.keys)
    recordKinds.forEach { kind ->
      assertNotEquals(
          managementFeedbackMessages.getValue(ManagementSubject.Movimiento).getValue(kind),
          managementFeedbackMessages.getValue(ManagementSubject.Transferencia).getValue(kind),
      )
    }
    assertEquals(ManagementSubject.entries.toSet(), managementFeedbackRestoring.keys)
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
