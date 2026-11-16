package com.kurobello.tallybook.core.ui.feedback

import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.OccurrenceUndo
import com.kurobello.tallybook.core.model.Transferencia

/** Exactly what a committed write captured; only this value is eligible for Undo. */
internal sealed interface UndoSnapshot {
  data class OfMovimiento(val movimiento: Movimiento) : UndoSnapshot

  data class OfTransferencia(val transferencia: Transferencia) : UndoSnapshot

  data class OfOccurrence(val undo: OccurrenceUndo) : UndoSnapshot
}
