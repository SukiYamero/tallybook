package com.kurobello.tallybook.core.ui.feedback

import com.kurobello.tallybook.core.model.BudgetNotice

internal enum class ManagementSubject {
  Movimiento,
  Transferencia,
  Recurrencia,
  Presupuesto,
}

internal enum class ManagementFeedbackKind {
  Saved,
  Deleted,
  Missing,
  Confirmed,
  Skipped,
  Restored,
  RestoreFailed,
  RestoreConflict,
  RestoreMissingCuenta,
  BudgetNotice,
}

/**
 * A committed mutation held by its management sheet until that sheet is hidden. [budget] rides on
 * the same outcome so one save never queues a second snackbar.
 */
internal data class ManagementOutcome(
    val subject: ManagementSubject,
    val kind: ManagementFeedbackKind,
    val undo: UndoSnapshot? = null,
    val budget: BudgetNotice? = null,
)

internal data class ManagementFeedback(
    val token: Long,
    val revision: Int = 0,
    val subject: ManagementSubject,
    val kind: ManagementFeedbackKind,
    val isRestoring: Boolean = false,
    val undo: UndoSnapshot? = null,
    val budget: BudgetNotice? = null,
) {
  /** Undo keeps priority: undoing the save also takes its budget line away. */
  val opensBudgets: Boolean
    get() = undo == null && budget != null
}
