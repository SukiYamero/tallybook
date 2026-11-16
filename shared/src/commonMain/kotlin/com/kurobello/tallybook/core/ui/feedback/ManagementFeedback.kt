package com.kurobello.tallybook.core.ui.feedback

internal enum class ManagementSubject {
  Movimiento,
  Transferencia,
  Recurrencia,
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
}

/** A committed mutation held by its management sheet until that sheet is hidden. */
internal data class ManagementOutcome(
    val subject: ManagementSubject,
    val kind: ManagementFeedbackKind,
    val undo: UndoSnapshot? = null,
)

internal data class ManagementFeedback(
    val token: Long,
    val revision: Int = 0,
    val subject: ManagementSubject,
    val kind: ManagementFeedbackKind,
    val isRestoring: Boolean = false,
    val undo: UndoSnapshot? = null,
) {
  /** Undo or retry, offered only while the snapshot is held. */
  val hasAction: Boolean
    get() = undo != null
}
