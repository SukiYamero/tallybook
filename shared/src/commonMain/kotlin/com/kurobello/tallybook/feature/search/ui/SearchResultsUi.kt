package com.kurobello.tallybook.feature.search.ui

import com.kurobello.tallybook.core.model.Actividad
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import kotlinx.datetime.LocalDate

internal data class SearchRequest(val criteria: MovimientoReadCriteria, val normalizedQuery: String)

internal data class SearchCurrencyYears(val moneda: Moneda, val persistedYears: List<Int>) {
  val hasCurrencyData: Boolean
    get() = persistedYears.isNotEmpty()
}

internal data class SearchResultsUi(
    val request: SearchRequest,
    val rows: List<SearchActividadRowUi>,
    val years: SearchCurrencyYears,
) {
  val count: Int
    get() = rows.size

  // Rows arrive newest day first, so grouping keeps that order and only days with results exist.
  val groups: List<SearchDayGroupUi> =
      rows.groupBy { it.fecha }.map { (fecha, rows) -> SearchDayGroupUi(fecha, rows) }
}

internal sealed interface SearchActividadRowUi {
  val id: String
  val fecha: LocalDate
  val key: String
}

internal data class SearchResultRowUi(
    val movimiento: Movimiento,
    val categoria: Categoria?,
    val title: String,
    val categoryLabel: String,
) : SearchActividadRowUi {
  override val id: String
    get() = movimiento.id

  override val fecha: LocalDate
    get() = movimiento.fecha

  override val key: String
    get() = "search-row:$id"
}

/** A null [origen] or [destino] is an account the catalog no longer lists. */
internal data class SearchTransferenciaRowUi(
    val actividad: Actividad.DeTransferencia,
    val origen: Cuenta?,
    val destino: Cuenta?,
) : SearchActividadRowUi {
  override val id: String
    get() = actividad.id

  override val fecha: LocalDate
    get() = actividad.fecha

  override val key: String
    get() = "search-transfer:$id"
}

internal fun searchCategoryLabel(category: Categoria?, unknownCategoryLabel: String): String =
    category?.nombre?.takeUnless { it.isBlank() } ?: unknownCategoryLabel
