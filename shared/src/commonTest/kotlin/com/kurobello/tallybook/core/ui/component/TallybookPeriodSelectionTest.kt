package com.kurobello.tallybook.core.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth

class TallybookPeriodSelectionTest {

  @Test
  fun selectableDaysForTheCurrentMonthEndOnToday() {
    val periods = selectableDayPeriods(YearMonth(2026, Month.JUNE), today)

    assertEquals(23, periods.size)
    assertEquals(TallybookDayPeriod(LocalDate(2026, 6, 23)), periods.last())
  }

  @Test
  fun selectableDaysForAHistoricalMonthIncludeItsEntireCalendar() {
    val periods = selectableDayPeriods(YearMonth(2025, Month.JUNE), today)

    assertEquals(30, periods.size)
    assertEquals(TallybookDayPeriod(LocalDate(2025, 6, 30)), periods.last())
  }

  @Test
  fun selectableWeeksForTheCurrentMonthStopOnToday() {
    val periods = selectableWeekPeriods(YearMonth(2026, Month.JUNE), today)

    assertEquals(LocalDate(2026, 6, 1), periods.first().start)
    assertEquals(LocalDate(2026, 6, 23), periods.last().end)
    assertEquals(
        listOf(1 to 7, 8 to 14, 15 to 21, 22 to 23),
        periods.map { it.start.day to it.end.day },
    )
  }

  @Test
  fun selectableWeeksStayWithinTheSelectedMonth() {
    val periods = selectableWeekPeriods(YearMonth(2026, Month.SEPTEMBER), LocalDate(2026, 9, 30))

    assertEquals(LocalDate(2026, 9, 1), periods.first().start)
    assertEquals(LocalDate(2026, 9, 30), periods.last().end)
    assertEquals(
        listOf(1 to 7, 8 to 14, 15 to 21, 22 to 28, 29 to 30),
        periods.map { it.start.day to it.end.day },
    )
  }

  @Test
  fun selectableMonthsForTheCurrentYearExcludeFutureMonths() {
    val periods = selectableMonthPeriods(2026, today)

    assertEquals(6, periods.size)
    assertEquals(TallybookMonthPeriod(YearMonth(2026, 6)), periods.last())
    assertEquals(LocalDate(2026, 6, 30), periods.last().end)
  }

  @Test
  fun changingToAPastYearKeepsTheFocusedMonth() {
    val focusedMonth = focusedMonthForYear(YearMonth(2026, Month.JUNE), 2024, today)

    assertEquals(YearMonth(2024, Month.JUNE), focusedMonth)
  }

  @Test
  fun changingToTheCurrentYearClampsAFutureFocusedMonth() {
    val focusedMonth = focusedMonthForYear(YearMonth(2024, Month.NOVEMBER), 2026, today)

    assertEquals(YearMonth(2026, Month.JUNE), focusedMonth)
  }

  private companion object {
    val today = LocalDate(2026, 6, 23)
  }
}
