package com.kurobello.tallybook.feature.movimientos.viewmodel

internal enum class MovimientoFeedbackKind {
  Saved,
  Deleted,
  Missing,
  Restored,
  RestoreFailed,
  RestoreConflict,
}

internal data class MovimientoFeedback(
    val token: Long,
    val revision: Int = 0,
    val kind: MovimientoFeedbackKind,
    val isRestoring: Boolean = false,
) {
  val hasAction: Boolean
    get() = kind == MovimientoFeedbackKind.Deleted || kind == MovimientoFeedbackKind.RestoreFailed
}
