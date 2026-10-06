package com.kurobello.tallybook.feature.search.ui

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoReadCriteria

internal data class SearchRequest(val criteria: MovimientoReadCriteria, val normalizedQuery: String)

internal data class SearchCurrencyYears(val moneda: Moneda, val persistedYears: List<Int>) {
  val hasCurrencyData: Boolean
    get() = persistedYears.isNotEmpty()
}

internal data class SearchResultsUi(
    val request: SearchRequest,
    val rows: List<SearchResultRowUi>,
    val years: SearchCurrencyYears,
) {
  val count: Int
    get() = rows.size
}

internal data class SearchResultRowUi(
    val movimiento: Movimiento,
    val categoria: Categoria?,
    val title: String,
    val categoryLabel: String,
) {
  val id: String
    get() = movimiento.id
}

internal fun searchCategoryLabel(category: Categoria?, unknownCategoryLabel: String): String =
    category?.nombre?.takeUnless { it.isBlank() } ?: unknownCategoryLabel
