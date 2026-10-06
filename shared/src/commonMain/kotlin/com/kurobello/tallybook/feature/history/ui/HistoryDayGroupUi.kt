package com.kurobello.tallybook.feature.history.ui

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.datetime.LocalDate

internal data class HistoryDayGroupUi(
    val fecha: LocalDate,
    val rows: List<HistoryMovimientoRowUi>,
) {
  val key: String
    get() = "history-day:$fecha"
}

internal data class HistoryMovimientoRowUi(
    val movimiento: Movimiento,
    val categoria: Categoria?,
    val title: String,
    val categoryLabel: String,
) {
  val id: String
    get() = movimiento.id

  val fecha: LocalDate
    get() = movimiento.fecha

  val tipo: TipoMovimiento
    get() = movimiento.tipo

  val monto: Money
    get() = movimiento.monto

  val key: String
    get() = "history-movimiento:$id"
}
