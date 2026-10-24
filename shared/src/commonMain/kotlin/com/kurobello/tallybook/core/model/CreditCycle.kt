package com.kurobello.tallybook.core.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.onDay
import kotlinx.datetime.plus
import kotlinx.datetime.plusMonth
import kotlinx.datetime.yearMonth

/**
 * Inclusive civil range of the credit-card cycle containing [today]: from the day after the
 * previous statement date through the next one, today included when it is the statement date. A
 * [diaCorte] past the month's end falls on its last day (31 → 30 April). Without a [diaCorte] the
 * cycle is the calendar month.
 */
fun creditCycle(today: LocalDate, diaCorte: Int?): MovimientoDateRange {
  if (diaCorte == null) return MovimientoPeriodRanges.calendarMonth(today.yearMonth)
  require(diaCorte in CuentaRules.DIA_CORTE_RANGE) { "Statement day out of range: $diaCorte" }
  val month = today.yearMonth
  val closingMonth = if (today <= month.statementDate(diaCorte)) month else month.plusMonth()
  return MovimientoDateRange(
      start = closingMonth.minusMonth().statementDate(diaCorte).plus(DatePeriod(days = 1)),
      end = closingMonth.statementDate(diaCorte),
  )
}

private fun YearMonth.statementDate(diaCorte: Int): LocalDate = onDay(minOf(diaCorte, numberOfDays))
