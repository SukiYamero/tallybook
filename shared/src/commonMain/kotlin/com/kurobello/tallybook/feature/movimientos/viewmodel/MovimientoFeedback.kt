package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.Movimiento

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
    val deletedMovimiento: Movimiento? = null,
) {
  val hasAction: Boolean
    get() = kind == MovimientoFeedbackKind.Deleted || kind == MovimientoFeedbackKind.RestoreFailed
}
