package com.kurobello.tallybook.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

class RecurrenciaRulesTest {

  @Test
  fun monthlyAnchorOnThe31stFallsOnEachMonthsLastDayWithoutDrifting() {
    val anchor = date(2026, 1, 31)

    assertEquals(
        listOf(
            date(2026, 1, 31),
            date(2026, 2, 28),
            date(2026, 3, 31),
            date(2026, 4, 30),
            date(2026, 5, 31),
        ),
        (0..4).map { RecurrenciaRules.occurrence(anchor, Frecuencia.MENSUAL, it) },
    )
    assertEquals(date(2028, 2, 29), RecurrenciaRules.occurrence(anchor, Frecuencia.MENSUAL, 25))
  }

  @Test
  fun annualAnchorOnLeapDayFallsOnThe28thInCommonYears() {
    val anchor = date(2024, 2, 29)

    assertEquals(
        listOf(date(2024, 2, 29), date(2025, 2, 28), date(2026, 2, 28), date(2028, 2, 29)),
        listOf(0, 1, 2, 4).map { RecurrenciaRules.occurrence(anchor, Frecuencia.ANUAL, it) },
    )
  }

  @Test
  fun weeklyOccurrencesAreSevenDaysApartAcrossMonthsAndYears() {
    val anchor = date(2025, 12, 24)

    assertEquals(
        listOf(date(2025, 12, 24), date(2025, 12, 31), date(2026, 1, 7), date(2026, 3, 4)),
        listOf(0, 1, 2, 10).map { RecurrenciaRules.occurrence(anchor, Frecuencia.SEMANAL, it) },
    )
  }

  @Test
  fun firstAfterIsStrictlyLaterThanTheDate() {
    val anchor = date(2026, 1, 31)

    assertEquals(
        date(2026, 1, 31),
        RecurrenciaRules.firstAfter(anchor, Frecuencia.MENSUAL, date(2026, 1, 30)),
    )
    assertEquals(
        date(2026, 2, 28),
        RecurrenciaRules.firstAfter(anchor, Frecuencia.MENSUAL, date(2026, 1, 31)),
    )
    assertEquals(
        date(2026, 3, 31),
        RecurrenciaRules.firstAfter(anchor, Frecuencia.MENSUAL, date(2026, 2, 28)),
    )
    assertEquals(
        date(2026, 11, 30),
        RecurrenciaRules.firstAfter(anchor, Frecuencia.MENSUAL, date(2026, 11, 15)),
    )
    assertEquals(
        date(2026, 1, 14),
        RecurrenciaRules.firstAfter(date(2025, 12, 31), Frecuencia.SEMANAL, date(2026, 1, 7)),
    )
    assertEquals(
        date(2027, 2, 28),
        RecurrenciaRules.firstAfter(date(2024, 2, 29), Frecuencia.ANUAL, date(2026, 2, 28)),
    )
  }

  @Test
  fun firstProximaFechaFollowsTheLaterOfFechaAndToday() {
    val today = date(2026, 11, 15)

    assertEquals(
        date(2026, 11, 20),
        RecurrenciaRules.firstProximaFecha(date(2026, 3, 20), Frecuencia.MENSUAL, today),
    )
    assertEquals(
        date(2026, 12, 15),
        RecurrenciaRules.firstProximaFecha(date(2026, 11, 15), Frecuencia.MENSUAL, today),
    )
    assertEquals(
        date(2027, 2, 28),
        RecurrenciaRules.firstProximaFecha(date(2027, 1, 31), Frecuencia.MENSUAL, today),
    )
    assertEquals(
        date(2026, 11, 22),
        RecurrenciaRules.firstProximaFecha(date(2026, 11, 1), Frecuencia.SEMANAL, today),
    )
  }

  @Test
  fun pendingListsEveryOccurrenceFromProximaFechaThroughToday() {
    val anchor = date(2026, 1, 31)

    assertEquals(
        listOf(date(2026, 2, 28), date(2026, 3, 31), date(2026, 4, 30)),
        RecurrenciaRules.pending(anchor, Frecuencia.MENSUAL, date(2026, 2, 28), date(2026, 4, 30)),
    )
    assertEquals(
        listOf(date(2026, 2, 28), date(2026, 3, 31)),
        RecurrenciaRules.pending(anchor, Frecuencia.MENSUAL, date(2026, 2, 28), date(2026, 4, 29)),
    )
    assertEquals(
        emptyList(),
        RecurrenciaRules.pending(anchor, Frecuencia.MENSUAL, date(2026, 2, 28), date(2026, 2, 27)),
    )
    assertEquals(
        listOf(date(2025, 2, 28), date(2026, 2, 28)),
        RecurrenciaRules.pending(
            date(2024, 2, 29),
            Frecuencia.ANUAL,
            date(2025, 2, 28),
            date(2027, 2, 27),
        ),
    )
  }

  private fun date(year: Int, month: Int, day: Int) = LocalDate(year, month, day)
}
