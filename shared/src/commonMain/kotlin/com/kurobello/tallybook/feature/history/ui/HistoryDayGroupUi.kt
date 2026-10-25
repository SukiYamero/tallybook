package com.kurobello.tallybook.feature.history.ui

import com.kurobello.tallybook.core.model.Actividad
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.datetime.LocalDate

internal data class HistoryDayGroupUi(
    val fecha: LocalDate,
    val rows: List<HistoryRowUi>,
) {
  val key: String
    get() = "history-day:$fecha"
}

/** One row of a day group; [monto] is in the viewed currency. */
internal sealed interface HistoryRowUi {
  val id: String
  val fecha: LocalDate
  val monto: Money
  val key: String
}

internal data class HistoryMovimientoRowUi(
    val movimiento: Movimiento,
    val categoria: Categoria?,
    val title: String,
    val categoryLabel: String,
) : HistoryRowUi {
  override val id: String
    get() = movimiento.id

  override val fecha: LocalDate
    get() = movimiento.fecha

  val tipo: TipoMovimiento
    get() = movimiento.tipo

  override val monto: Money
    get() = movimiento.monto

  override val key: String
    get() = "history-movimiento:$id"
}

/** A null [origen] or [destino] is an account the catalog no longer lists. */
internal data class HistoryTransferenciaRowUi(
    val actividad: Actividad.DeTransferencia,
    val origen: Cuenta?,
    val destino: Cuenta?,
) : HistoryRowUi {
  override val id: String
    get() = actividad.id

  override val fecha: LocalDate
    get() = actividad.fecha

  override val monto: Money
    get() = actividad.monto

  override val key: String
    get() = "history-transferencia:$id"
}
