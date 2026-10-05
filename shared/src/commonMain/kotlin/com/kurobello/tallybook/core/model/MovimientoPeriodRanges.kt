package com.kurobello.tallybook.core.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth

internal object MovimientoPeriodRanges {
  fun calendarMonth(month: YearMonth): MovimientoDateRange {
    return MovimientoDateRange(
        LocalDate(month.year, month.month, 1),
        LocalDate(month.year, month.month, month.numberOfDays),
    )
  }

  fun days(month: YearMonth, today: LocalDate): List<MovimientoDateRange> {
    val lastDay = selectableMonthEnd(month, today)?.day ?: return emptyList()
    return (1..lastDay).map { day ->
      val date = LocalDate(month.year, month.month, day)
      MovimientoDateRange(date, date)
    }
  }

  fun weeks(month: YearMonth, today: LocalDate): List<MovimientoDateRange> {
    val lastDay = selectableMonthEnd(month, today)?.day ?: return emptyList()
    return (1..lastDay step DAYS_PER_WEEK).map { day ->
      MovimientoDateRange(
          LocalDate(month.year, month.month, day),
          LocalDate(month.year, month.month, minOf(day + DAYS_PER_WEEK - 1, lastDay)),
      )
    }
  }

  fun months(year: Int, today: LocalDate): List<MovimientoDateRange> {
    if (year > today.year) return emptyList()
    val months =
        if (year == today.year) Month.entries.take(today.month.ordinal + 1) else Month.entries
    return months.map { month -> calendarMonth(YearMonth(year, month)) }
  }

  private fun selectableMonthEnd(month: YearMonth, today: LocalDate): LocalDate? {
    val range = calendarMonth(month)
    if (range.start > today) return null
    return minOf(range.end, today)
  }

  private const val DAYS_PER_WEEK = 7
}
