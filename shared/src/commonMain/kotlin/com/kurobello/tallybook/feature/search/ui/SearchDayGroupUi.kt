package com.kurobello.tallybook.feature.search.ui

import kotlinx.datetime.LocalDate

internal data class SearchDayGroupUi(
    val fecha: LocalDate,
    val rows: List<SearchActividadRowUi>,
) {
  val key: String
    get() = "search-day:$fecha"
}
