package com.kurobello.tallybook.core.model

import kotlinx.datetime.LocalDate

data class MovimientoDateRange(val start: LocalDate, val end: LocalDate) {
  init {
    require(start <= end) { "Movimiento date range start must be on or before its end" }
  }
}
