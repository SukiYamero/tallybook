package com.kurobello.tallybook.core.ui.feedback

import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.Transferencia

/** The exact row a delete transaction captured; only this value is eligible for Undo. */
internal sealed interface DeletedSnapshot {
  data class OfMovimiento(val movimiento: Movimiento) : DeletedSnapshot

  data class OfTransferencia(val transferencia: Transferencia) : DeletedSnapshot
}
