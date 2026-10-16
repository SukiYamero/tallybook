package com.kurobello.tallybook.feature.movimientos.ui

import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.ui.movimiento.pendingSubtitle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

class MovimientoEntryPresentationTest {
  private val today = LocalDate(2026, 9, 15)

  @Test
  fun relativeDayNamesRecentDatesAndFallsBackToTheCalendarDay() {
    assertEquals(RelativeDay.Today, relativeDayOf(today, today))
    assertEquals(RelativeDay.Yesterday, relativeDayOf(LocalDate(2026, 9, 14), today))
    assertEquals(
        RelativeDay.Weekday(DayOfWeek.WEDNESDAY),
        relativeDayOf(LocalDate(2026, 9, 9), today),
    )
    assertEquals(
        RelativeDay.Calendar(LocalDate(2026, 9, 8), showsYear = false),
        relativeDayOf(LocalDate(2026, 9, 8), today),
    )
  }

  @Test
  fun relativeDayShowsTheYearOnlyForOtherYearsAndTreatsFutureDatesAsCalendarDays() {
    assertEquals(
        RelativeDay.Calendar(LocalDate(2025, 12, 31), showsYear = true),
        relativeDayOf(LocalDate(2025, 12, 31), today),
    )
    assertEquals(
        RelativeDay.Calendar(LocalDate(2026, 9, 16), showsYear = false),
        relativeDayOf(LocalDate(2026, 9, 16), today),
    )
  }

  @Test
  fun everyMonedaHasAPlaceholderSampleFormattedByTheMoneyFormatter() {
    assertEquals(Moneda.entries.toSet(), PHRASE_SAMPLE_MINOR_UNITS.keys)
    assertEquals("$ 20.000,00", phraseSampleAmount(Moneda.COP))
    assertEquals("$5.000", phraseSampleAmount(Moneda.CLP))
    assertEquals("$12.50", phraseSampleAmount(Moneda.USD))
  }

  @Test
  fun pendingSubtitleLeadsWithTheLabelAndKeepsTheDescription() {
    assertEquals("To complete", pendingSubtitle("To complete", null))
    assertEquals("To complete · lunch", pendingSubtitle("To complete", "lunch"))
    assertEquals("lunch", pendingSubtitle(null, "lunch"))
    assertNull(pendingSubtitle(null, null))
  }
}
