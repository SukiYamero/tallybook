package com.kurobello.tallybook.core.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.number
import kotlinx.datetime.onDay
import kotlinx.datetime.plus
import kotlinx.datetime.yearMonth

/**
 * Occurrences are counted from the anchor rather than from each other, so a monthly or annual day
 * past the month's end falls on its last day without drifting (31 → 30 April → 31 May).
 */
object RecurrenciaRules {

  fun occurrence(fechaAncla: LocalDate, frecuencia: Frecuencia, n: Int): LocalDate {
    require(n >= 0) { "Occurrence index must not be negative: $n" }
    return when (frecuencia) {
      Frecuencia.SEMANAL -> fechaAncla.plus(n * DAYS_PER_WEEK, DateTimeUnit.DAY)
      Frecuencia.MENSUAL -> fechaAncla.monthsLater(n)
      Frecuencia.ANUAL -> fechaAncla.monthsLater(n * MONTHS_PER_YEAR)
    }
  }

  /** The first occurrence strictly after [date]; [fechaAncla] itself when [date] precedes it. */
  fun firstAfter(fechaAncla: LocalDate, frecuencia: Frecuencia, date: LocalDate): LocalDate {
    var n = maxOf(indexNotAfter(fechaAncla, frecuencia, date), 0)
    while (occurrence(fechaAncla, frecuencia, n) <= date) n++
    return occurrence(fechaAncla, frecuencia, n)
  }

  /** The proximaFecha of a recurrence created together with a movimiento dated [fecha]. */
  fun firstProximaFecha(fecha: LocalDate, frecuencia: Frecuencia, today: LocalDate): LocalDate =
      firstAfter(fecha, frecuencia, maxOf(fecha, today))

  /** Every occurrence from [proximaFecha] through [today], oldest first. */
  fun pending(
      fechaAncla: LocalDate,
      frecuencia: Frecuencia,
      proximaFecha: LocalDate,
      today: LocalDate,
  ): List<LocalDate> =
      generateSequence(proximaFecha) { firstAfter(fechaAncla, frecuencia, it) }
          .takeWhile { it <= today }
          .toList()
}

// Never past the answer of firstAfter, so its search only walks forward a step or two.
private fun indexNotAfter(fechaAncla: LocalDate, frecuencia: Frecuencia, date: LocalDate): Int =
    when (frecuencia) {
      Frecuencia.SEMANAL -> fechaAncla.daysUntil(date) / DAYS_PER_WEEK
      Frecuencia.MENSUAL ->
          (date.year - fechaAncla.year) * MONTHS_PER_YEAR +
              (date.month.number - fechaAncla.month.number)
      Frecuencia.ANUAL -> date.year - fechaAncla.year
    }

private fun LocalDate.monthsLater(months: Int): LocalDate {
  val month = yearMonth.plus(months, DateTimeUnit.MONTH)
  return month.onDay(minOf(day, month.numberOfDays))
}

private const val DAYS_PER_WEEK = 7
private const val MONTHS_PER_YEAR = 12
