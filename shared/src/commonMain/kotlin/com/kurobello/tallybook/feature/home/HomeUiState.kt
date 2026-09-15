package com.kurobello.tallybook.feature.home

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoSummary

internal data class HomeUiState(
    val isLoading: Boolean,
    val summary: MovimientoSummary,
    val recent: List<Movimiento>,
    val categoriasById: Map<String, Categoria>,
    val error: DataError?,
) {
  val isEmpty: Boolean
    get() = !isLoading && error == null && recent.isEmpty()

  companion object {
    fun loading() =
        HomeUiState(
            isLoading = true,
            summary = emptySummary(Moneda.COP),
            recent = emptyList(),
            categoriasById = emptyMap(),
            error = null,
        )

    fun error(error: DataError) =
        HomeUiState(
            isLoading = false,
            summary = emptySummary(Moneda.COP),
            recent = emptyList(),
            categoriasById = emptyMap(),
            error = error,
        )

    fun emptySummary(moneda: Moneda) =
        MovimientoSummary(ingresos = Money(0, moneda), gastos = Money(0, moneda))
  }
}
