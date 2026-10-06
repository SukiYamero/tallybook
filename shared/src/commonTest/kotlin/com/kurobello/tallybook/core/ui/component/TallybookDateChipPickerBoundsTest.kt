package com.kurobello.tallybook.core.ui.component

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

class TallybookDateChipPickerBoundsTest {
  @Test
  fun firstNavigableMonthHonorsMinimumDateOnlyWithinTheSelectableFloorYear() {
    assertEquals(YearMonth(2026, 9), minimumCalendarMonth(2026..2026, LocalDate(2026, 9, 20)))
    assertEquals(YearMonth(2019, 1), minimumCalendarMonth(2019..2027, null))
    assertEquals(YearMonth(2019, 1), minimumCalendarMonth(2019..2027, LocalDate(1900, 9, 20)))
  }

  @Test
  fun daySelectionHonorsInclusiveMinimumAndMaximumWithoutChangingDefaults() {
    val minimum = LocalDate(2026, 9, 20)
    val maximum = LocalDate(2026, 10, 6)
    assertFalse(isCalendarDateSelectable(LocalDate(2026, 9, 19), 2019, maximum, minimum))
    assertTrue(isCalendarDateSelectable(minimum, 2019, maximum, minimum))
    assertTrue(isCalendarDateSelectable(maximum, 2019, maximum, minimum))
    assertFalse(isCalendarDateSelectable(LocalDate(2026, 10, 7), 2019, maximum, minimum))
    assertTrue(isCalendarDateSelectable(LocalDate(2026, 9, 19), 2019, maximum))
    assertTrue(isCalendarDateSelectable(LocalDate(2029, 1, 1), 2019))
    assertFalse(isCalendarDateSelectable(LocalDate(1900, 1, 1), 2019))
  }

  @Test
  fun correctionMonthAndYearRangeRespectTheMinimumWithoutMutatingTheValue() {
    val value = LocalDate(1900, 1, 1)
    val minimum = LocalDate(2025, 9, 20)
    val maximum = LocalDate(2026, 10, 6)
    assertEquals(YearMonth(2025, 9), initialCalendarMonth(value, maximum, minimum))
    assertEquals(2025..2026, selectableYears(value, maximum, 1900, minimum))
    assertEquals(LocalDate(1900, 1, 1), value)
    assertEquals(
        2030..2030,
        selectableYears(LocalDate(2026, 1, 1), null, 2019, LocalDate(2030, 4, 1)),
    )
  }

  @Test
  fun contradictoryBoundsHaveNoSelectableDayAndRevealTheMaximumCorrectionMonth() {
    val maximum = LocalDate(2026, 10, 6)
    listOf(LocalDate(2027, 1, 1), LocalDate(2026, 10, 10)).forEach { minimum ->
      assertFalse(isCalendarDateSelectable(maximum, 2019, maximum, minimum))
      assertFalse(isCalendarDateSelectable(minimum, 2019, maximum, minimum))
      assertEquals(
          YearMonth(2026, 10),
          initialCalendarMonth(LocalDate(1900, 1, 1), maximum, minimum),
      )
      assertTrue(selectableYears(minimum, maximum, 1900, minimum).isEmpty())
    }
  }

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
