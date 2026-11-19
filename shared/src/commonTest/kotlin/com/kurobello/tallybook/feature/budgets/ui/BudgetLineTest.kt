package com.kurobello.tallybook.feature.budgets.ui

import com.kurobello.tallybook.core.model.BudgetStatus
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Presupuesto
import com.kurobello.tallybook.core.model.budgetStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class BudgetLineTest {

  @Test
  fun overShowsHowFarAboveTheBudget() {
    assertEquals(BudgetLine.Over(cop(20_000)), status(500_000, spent = 520_000).line())
  }

  @Test
  fun nearShowsWhatIsLeft() {
    assertEquals(BudgetLine.Left(cop(30_000)), status(500_000, spent = 470_000).line())
  }

  @Test
  fun nearWithNothingLeftSaysTheBudgetIsUsed() {
    assertEquals(BudgetLine.Reached, status(500_000, spent = 500_000).line())
  }

  @Test
  fun paceShowsTheDayTheLimitWouldBeReached() {
    val pace = status(200_000, spent = 110_000, day = LocalDate(2026, 11, 15))

    assertEquals(BudgetLine.Pace(assertNotNullPaceDay(pace)), pace.line())
  }

  @Test
  fun fineShowsWhatIsLeftAndThePerDayAmount() {
    val fine = status(200_000, spent = 90_000, day = LocalDate(2026, 11, 20))

    assertEquals(BudgetLine.PerDay(cop(110_000), cop(10_000)), fine.line())
  }

  private fun assertNotNullPaceDay(status: BudgetStatus): Int =
      requireNotNull(status.paceDay) { "expected a PACE status" }

  private fun status(
      monto: Long,
      spent: Long,
      day: LocalDate = LocalDate(2026, 11, 20),
  ): BudgetStatus =
      budgetStatus(
          Presupuesto("p", "cat", cop(monto), Instant.parse("2026-01-01T00:00:00Z")),
          "Comida",
          null,
          cop(spent),
          day,
      )

  private fun cop(units: Long) = Money(units * 100, Moneda.COP)
}
