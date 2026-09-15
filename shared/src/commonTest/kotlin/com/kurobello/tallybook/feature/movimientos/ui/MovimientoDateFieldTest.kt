package com.kurobello.tallybook.feature.movimientos.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class MovimientoDateFieldTest {

  @Test
  fun selectableDatesRejectsTheDayAfterToday() {
    val today = LocalDate.parse("2026-09-15")
    val maxDateMillis = today.toUtcMillis()

    assertTrue(isDateSelectable(today.toUtcMillis(), maxDateMillis))
    assertFalse(isDateSelectable(LocalDate.parse("2026-09-16").toUtcMillis(), maxDateMillis))
  }

  @Test
  fun selectedUtcMidnightConvertsToTheSameCalendarDay() {
    assertEquals(LocalDate.parse("2026-09-15"), 1_789_430_400_000L.toUtcLocalDate())
  }
}
