package com.kurobello.tallybook.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.datetime.LocalDate

class CreditCycleTest {
  private data class Case(
      val today: LocalDate,
      val diaCorte: Int?,
      val expected: MovimientoDateRange,
  )

  @Test
  fun cycleRunsFromTheDayAfterThePreviousStatementThroughTheNextOne() {
    val cases =
        listOf(
            Case(date(2026, 4, 10), 31, range(date(2026, 4, 1), date(2026, 4, 30))),
            Case(date(2026, 4, 30), 31, range(date(2026, 4, 1), date(2026, 4, 30))),
            Case(date(2026, 5, 1), 31, range(date(2026, 5, 1), date(2026, 5, 31))),
            Case(date(2024, 2, 10), 29, range(date(2024, 1, 30), date(2024, 2, 29))),
            Case(date(2024, 2, 10), 30, range(date(2024, 1, 31), date(2024, 2, 29))),
            Case(date(2024, 2, 10), 31, range(date(2024, 2, 1), date(2024, 2, 29))),
            Case(date(2024, 3, 1), 30, range(date(2024, 3, 1), date(2024, 3, 30))),
            Case(date(2025, 2, 10), 29, range(date(2025, 1, 30), date(2025, 2, 28))),
            Case(date(2025, 2, 10), 30, range(date(2025, 1, 31), date(2025, 2, 28))),
            Case(date(2025, 2, 10), 31, range(date(2025, 2, 1), date(2025, 2, 28))),
            Case(date(2025, 2, 28), 30, range(date(2025, 1, 31), date(2025, 2, 28))),
            Case(date(2025, 3, 1), 30, range(date(2025, 3, 1), date(2025, 3, 30))),
            Case(date(2026, 6, 15), 15, range(date(2026, 5, 16), date(2026, 6, 15))),
            Case(date(2026, 6, 16), 15, range(date(2026, 6, 16), date(2026, 7, 15))),
            Case(date(2026, 6, 1), 1, range(date(2026, 5, 2), date(2026, 6, 1))),
            Case(date(2026, 1, 10), 15, range(date(2025, 12, 16), date(2026, 1, 15))),
            Case(date(2025, 12, 20), 15, range(date(2025, 12, 16), date(2026, 1, 15))),
            Case(date(2025, 12, 31), 31, range(date(2025, 12, 1), date(2025, 12, 31))),
            Case(date(2026, 1, 1), 31, range(date(2026, 1, 1), date(2026, 1, 31))),
            Case(date(2026, 2, 14), null, range(date(2026, 2, 1), date(2026, 2, 28))),
            Case(date(2024, 2, 29), null, range(date(2024, 2, 1), date(2024, 2, 29))),
            Case(date(2025, 12, 31), null, range(date(2025, 12, 1), date(2025, 12, 31))),
        )

    for ((today, diaCorte, expected) in cases) {
      assertEquals(expected, creditCycle(today, diaCorte), "$today d=$diaCorte")
    }
  }

  @Test
  fun statementDayOutsideTheMonthIsACallerBug() {
    for (diaCorte in listOf(0, 32)) {
      assertFailsWith<IllegalArgumentException> { creditCycle(date(2026, 6, 1), diaCorte) }
    }
  }

  @Test
  fun deudaIsTheNegatedNegativeBalanceAndNeverBelowZero() {
    val cases =
        listOf(
            Money(-50_000_00, Moneda.COP) to Money(50_000_00, Moneda.COP),
            Money(0, Moneda.COP) to Money(0, Moneda.COP),
            Money(1_000_00, Moneda.COP) to Money(0, Moneda.COP),
            Money(-1, Moneda.CLP) to Money(1, Moneda.CLP),
        )

    for ((saldo, deuda) in cases) {
      assertEquals(deuda, CreditEstimate.deudaOf(saldo), "$saldo")
    }
  }

  private fun date(year: Int, month: Int, day: Int) = LocalDate(year, month, day)

  private fun range(start: LocalDate, end: LocalDate) = MovimientoDateRange(start, end)
}
