package com.kurobello.tallybook.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

class MovimientoDateRangeTest {
  @Test
  fun reversedEndpointsAreRejected() {
    assertFailsWith<IllegalArgumentException> {
      MovimientoDateRange(LocalDate(2026, 6, 24), LocalDate(2026, 6, 23))
    }
  }

  @Test
  fun oneDayIsAValidInclusiveRange() {
    val date = LocalDate(2026, 6, 23)
    val range = MovimientoDateRange(date, date)

    assertEquals(date, range.start)
    assertEquals(date, range.end)
  }

  @Test
  fun historicalWeeksUseExactMonthBlocksAcrossCalendarLengths() {
    val cases =
        listOf(
            YearMonth(2024, 2) to listOf(1 to 7, 8 to 14, 15 to 21, 22 to 28, 29 to 29),
            YearMonth(2025, 2) to listOf(1 to 7, 8 to 14, 15 to 21, 22 to 28),
            YearMonth(2025, 9) to listOf(1 to 7, 8 to 14, 15 to 21, 22 to 28, 29 to 30),
            YearMonth(2025, 10) to listOf(1 to 7, 8 to 14, 15 to 21, 22 to 28, 29 to 31),
        )

    for ((month, expected) in cases) {
      val periods = MovimientoPeriodRanges.weeks(month, LocalDate(2026, 6, 23))
      assertEquals(expected, periods.map { it.start.day to it.end.day }, "$month")
      assertEquals(month.year, periods.first().start.year)
      assertEquals(month.month, periods.last().end.month)
    }
  }

  @Test
  fun currentWeeksEndAtTodayOnFirstDayInsideBlockAndMonthEnd() {
    val cases =
        listOf(
            1 to listOf(1 to 1),
            23 to listOf(1 to 7, 8 to 14, 15 to 21, 22 to 23),
            30 to listOf(1 to 7, 8 to 14, 15 to 21, 22 to 28, 29 to 30),
        )

    for ((day, expected) in cases) {
      val periods = MovimientoPeriodRanges.weeks(YearMonth(2026, 6), LocalDate(2026, 6, day))
      assertEquals(expected, periods.map { it.start.day to it.end.day })
    }
  }

  @Test
  fun dayOptionsIncludeTheCompletePastMonthAndOnlyAvailableCurrentDays() {
    val today = LocalDate(2026, 6, 1)
    assertEquals(
        listOf(MovimientoDateRange(today, today)),
        MovimientoPeriodRanges.days(YearMonth(2026, 6), today),
    )
    val past = MovimientoPeriodRanges.days(YearMonth(2024, 2), today)
    assertEquals(29, past.size)
    assertEquals(MovimientoDateRange(LocalDate(2024, 2, 29), LocalDate(2024, 2, 29)), past.last())
    assertEquals(emptyList(), MovimientoPeriodRanges.days(YearMonth(2026, 7), today))
    assertEquals(emptyList(), MovimientoPeriodRanges.weeks(YearMonth(2026, 7), today))
  }

  @Test
  fun monthOptionsRepresentFullCalendarMonthsAndExcludeFutureMonths() {
    val today = LocalDate(2026, 6, 23)
    assertEquals(
        MovimientoDateRange(LocalDate(2024, 2, 1), LocalDate(2024, 2, 29)),
        MovimientoPeriodRanges.calendarMonth(YearMonth(2024, 2)),
    )
    val current = MovimientoPeriodRanges.months(2026, today)
    assertEquals(6, current.size)
    assertEquals(MovimientoDateRange(LocalDate(2026, 6, 1), LocalDate(2026, 6, 30)), current.last())
    assertEquals(12, MovimientoPeriodRanges.months(2025, today).size)
    assertEquals(emptyList(), MovimientoPeriodRanges.months(2027, today))
  }
}
