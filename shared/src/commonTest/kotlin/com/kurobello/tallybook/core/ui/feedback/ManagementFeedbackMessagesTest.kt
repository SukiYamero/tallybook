package com.kurobello.tallybook.core.ui.feedback

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
}
