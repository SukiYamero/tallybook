package com.kurobello.tallybook.core.ui.component

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

class TallybookDateChipPickerBoundsTest {
  @Test
  fun compactPopupHeightExcludesSafeInsetsAndBothEdgeMargins() {
    assertEquals(278.dp, calendarPopupMaximumHeight(350.dp, 40.dp, 16.dp))
    assertEquals(0.dp, calendarPopupMaximumHeight(40.dp, 40.dp, 16.dp))
  }

  @Test
  fun arithmeticYearRowsKeepHistoricalEndpointsAndSelectedRowWithoutMaterializingTheRange() {
    val years = -1_000_000..2026
    assertEquals(250_507, yearSelectorRowCount(years))
    assertEquals(listOf(2026, 2025, 2024, 2023), yearSelectorRow(years, 0).toList())
    assertEquals(listOf(-999_998, -999_999, -1_000_000), yearSelectorRow(years, 250_506).toList())
    assertEquals(31, yearSelectorRowIndex(1900..2026, 1900))
    assertEquals(0, yearSelectorRowIndex(2019..2026, 2026))
  }

  @Test
  fun defaultYearFloorAndUnboundedMaximumPreserveExistingCallers() {
    assertEquals(2019..2027, selectableYears(LocalDate(2026, 10, 6), null))
    assertEquals(1900..2026, selectableYears(LocalDate(1900, 1, 1), LocalDate(2026, 10, 6)))
  }

  @Test
  fun callerFloorIncludesAllHistoricalYearsWhileMaximumStillCapsToday() {
    assertEquals(1900..2026, selectableYears(LocalDate(2026, 10, 6), LocalDate(2026, 10, 6), 1900))
    assertEquals(2019..2026, selectableYears(LocalDate(2027, 1, 1), LocalDate(2026, 10, 6)))
  }

  @Test
  fun futureValueOpensAtTodayWithoutChangingTheSelectedValue() {
    val value = LocalDate(2027, 2, 1)
    assertEquals(YearMonth(2026, 10), initialCalendarMonth(value, LocalDate(2026, 10, 6)))
    assertEquals(LocalDate(2027, 2, 1), value)
    assertEquals(YearMonth(2027, 2), initialCalendarMonth(value, null))
  }
}
