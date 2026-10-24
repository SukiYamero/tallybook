package com.kurobello.tallybook.core.ui.feedback

internal enum class ManagementFeedbackKind {
  Saved,
  Deleted,
  Missing,
  Restored,
  RestoreFailed,
  RestoreConflict,
}

/** A committed mutation held by its management sheet until that sheet is hidden. */
internal data class ManagementOutcome(
    val kind: ManagementFeedbackKind,
    val deleted: DeletedSnapshot? = null,
)

internal data class ManagementFeedback(
    val token: Long,
    val revision: Int = 0,
    val kind: ManagementFeedbackKind,
    val isRestoring: Boolean = false,
    val deleted: DeletedSnapshot? = null,
) {
  /** Undo or retry, offered only while the snapshot is held. */
  val hasAction: Boolean
    get() = deleted != null
}
