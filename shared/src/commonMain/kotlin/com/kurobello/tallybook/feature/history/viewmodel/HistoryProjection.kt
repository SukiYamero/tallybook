package com.kurobello.tallybook.feature.history.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.feature.history.ui.HistoryDayGroupUi
import com.kurobello.tallybook.feature.history.ui.HistoryMovimientoRowUi

internal sealed interface HistoryProjection {
  data class Content(val summary: MovimientoSummary, val groups: List<HistoryDayGroupUi>) :
      HistoryProjection {
    val movementCount: Int
      get() = groups.sumOf { it.rows.size }
  }

  data class Failure(val failure: HistoryFailure) : HistoryProjection
}

internal fun projectHistory(
    moneda: Moneda,
    range: MovimientoDateRange,
    movimientos: List<Movimiento>,
    categoriasById: Map<String, Categoria>,
    unknownCategoryLabel: String,
): HistoryProjection {
  var previous: Movimiento? = null
  for (movimiento in movimientos) {
    require(movimiento.monto.moneda == moneda) {
      "History snapshot currency must match its request"
    }
    require(movimiento.fecha >= range.start && movimiento.fecha <= range.end) {
      "History snapshot dates must be inside its requested range"
    }
    require(movimiento.monto.minorUnits > 0) {
      "History snapshot movimiento amounts must be positive"
    }
    require(previous == null || previous.fecha >= movimiento.fecha) {
      "History snapshot dates must be in descending order"
    }
    previous = movimiento
  }
  val summary =
      try {
        var ingresos = Money(0, moneda)
        var gastos = Money(0, moneda)
        for (movimiento in movimientos) {
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
      movimientos
          .groupBy { it.fecha }
          .map { (fecha, rows) ->
            HistoryDayGroupUi(
                fecha,
                rows.map { movimiento ->
                  val categoria = categoriasById[movimiento.categoriaIds.first()]
                  val categoryLabel = categoria?.nombre ?: unknownCategoryLabel
                  HistoryMovimientoRowUi(
                      movimiento = movimiento,
                      categoria = categoria,
                      title = movimiento.descripcion?.takeUnless { it.isBlank() } ?: categoryLabel,
                      categoryLabel = categoryLabel,
                  )
                },
            )
          }
  return HistoryProjection.Content(summary, groups)
}
