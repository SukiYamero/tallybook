package com.kurobello.tallybook.feature.movimientos.ui

import com.kurobello.tallybook.core.ui.component.monthGridDates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

class MovimientoDateFieldTest {

  @Test
  fun calendarGridUsesTheSmallestNumberOfCompleteWeeks() {
    val dates = monthGridDates(YearMonth(2026, 9), DayOfWeek.MONDAY)

    assertEquals(LocalDate(2026, 8, 31), dates.first())
    assertEquals(LocalDate(2026, 10, 4), dates.last())
    assertEquals(35, dates.size)
  }

  @Test
  fun calendarGridKeepsSixWeeksWhenAMonthNeedsThem() {
    val dates = monthGridDates(YearMonth(2020, 8), DayOfWeek.MONDAY)

    assertEquals(42, dates.size)
  }
}
