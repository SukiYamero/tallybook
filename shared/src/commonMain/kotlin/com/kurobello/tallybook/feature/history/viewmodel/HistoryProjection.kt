package com.kurobello.tallybook.feature.history.viewmodel

import com.kurobello.tallybook.core.model.Actividad
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.movimiento.rowTitle
import com.kurobello.tallybook.feature.history.ui.HistoryDayGroupUi
import com.kurobello.tallybook.feature.history.ui.HistoryMovimientoRowUi
import com.kurobello.tallybook.feature.history.ui.HistoryRowUi
import com.kurobello.tallybook.feature.history.ui.HistoryTransferenciaRowUi

internal sealed interface HistoryProjection {
  /** Transfers are rows only: [summary] and [movementCount] describe movimientos. */
  data class Content(val summary: MovimientoSummary, val groups: List<HistoryDayGroupUi>) :
      HistoryProjection {
    val movementCount: Int
      get() = groups.sumOf { group -> group.rows.count { it is HistoryMovimientoRowUi } }

    val isEmpty: Boolean
      get() = groups.isEmpty()
  }

  data class Failure(val failure: HistoryFailure) : HistoryProjection
}

@Suppress("LongParameterList")
internal fun projectHistory(
    moneda: Moneda,
    range: MovimientoDateRange,
    actividades: List<Actividad>,
    categoriasById: Map<String, Categoria>,
    unknownCategoryLabel: String,
    cuentasById: Map<String, Cuenta> = emptyMap(),
): HistoryProjection {
  var previous: Actividad? = null
  for (actividad in actividades) {
    val monto = actividad.monto
    require(monto.moneda == moneda) { "History snapshot currency must match its request" }
    require(actividad.fecha >= range.start && actividad.fecha <= range.end) {
      "History snapshot dates must be inside its requested range"
    }
    require(monto.minorUnits > 0) { "History snapshot amounts must be positive" }
    require(previous == null || previous.fecha >= actividad.fecha) {
      "History snapshot dates must be in descending order"
    }
    previous = actividad
  }
  val summary =
      try {
        var ingresos = Money(0, moneda)
        var gastos = Money(0, moneda)
        for (actividad in actividades) {
          val movimiento = (actividad as? Actividad.DeMovimiento)?.movimiento ?: continue
          when (movimiento.tipo) {
            TipoMovimiento.INGRESO -> ingresos += movimiento.monto
            TipoMovimiento.GASTO -> gastos += movimiento.monto
          }
        }
        MovimientoSummary(ingresos, gastos)
      } catch (_: ArithmeticException) {
        return HistoryProjection.Failure(HistoryFailure.CalculationOverflow)
      }
  val groups =
      actividades
          .groupBy { it.fecha }
          .map { (fecha, rows) ->
            HistoryDayGroupUi(
                fecha,
                rows.map { it.toRow(categoriasById, cuentasById, unknownCategoryLabel) },
            )
          }
  return HistoryProjection.Content(summary, groups)
}

private val Actividad.monto: Money
  get() =
      when (this) {
        is Actividad.DeMovimiento -> movimiento.monto
        is Actividad.DeTransferencia -> monto
      }

private fun Actividad.toRow(
    categoriasById: Map<String, Categoria>,
    cuentasById: Map<String, Cuenta>,
    unknownCategoryLabel: String,
): HistoryRowUi =
    when (this) {
      is Actividad.DeMovimiento -> {
        val categoria = categoriasById[movimiento.categoriaId]
        val categoryLabel = categoria?.nombre ?: unknownCategoryLabel
        HistoryMovimientoRowUi(
            movimiento = movimiento,
            categoria = categoria,
            title = movimiento.rowTitle(categoryLabel),
            categoryLabel = categoryLabel,
        )
      }
      is Actividad.DeTransferencia ->
          HistoryTransferenciaRowUi(
              this,
              cuentasById[transferencia.cuentaOrigenId],
              cuentasById[transferencia.cuentaDestinoId],
          )
    }
