package com.kurobello.tallybook.core.ui.feedback

import com.kurobello.tallybook.core.model.RestoreResult
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ManagementFeedbackMessagesTest {
  @Test
  fun everySubjectHasItsOwnMessageForEveryKindAndForRestoring() {
    ManagementSubject.entries.forEach { subject ->
      assertEquals(
          ManagementFeedbackKind.entries.toSet(),
          managementFeedbackMessages.getValue(subject).keys,
      )
    }
    ManagementFeedbackKind.entries.forEach { kind ->
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
  fun onlyDeletedAndRetryableFailuresOfferAnAction() {
    assertEquals(
        setOf(ManagementFeedbackKind.Deleted, ManagementFeedbackKind.RestoreFailed),
        managementFeedbackActions.keys,
    )
  }
}
