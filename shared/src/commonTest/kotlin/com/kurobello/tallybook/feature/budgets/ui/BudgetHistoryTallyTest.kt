package com.kurobello.tallybook.feature.budgets.ui

import com.kurobello.tallybook.core.model.BudgetMonth
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.YearMonth

class BudgetHistoryTallyTest {

  @Test
  fun countsOnlyClosedMonthsWithinTheCurrentMonto() {
    val history =
        listOf(
            month(6, spent = 400_000),
            month(7, spent = 500_000),
            month(8, spent = 520_000),
            month(9, spent = 0),
            month(10, spent = 700_000),
            month(11, spent = 100_000),
        )

    assertEquals(BudgetHistoryTally(within = 3, closed = 5), history.tally(cop(500_000)))
  }

  @Test
  fun anInProgressMonthOverTheMontoDoesNotCount() {
    val history = listOf(month(10, spent = 100_000), month(11, spent = 900_000))

    assertEquals(BudgetHistoryTally(within = 1, closed = 1), history.tally(cop(500_000)))
  }

  @Test
  fun monthsTheUserNeverRecordedDoNotCount() {
    val history =
        listOf(
            month(8, spent = 0, recorded = false),
            month(9, spent = 0, recorded = false),
            month(10, spent = 100_000),
            month(11, spent = 100_000),
        )

    assertEquals(BudgetHistoryTally(within = 1, closed = 1), history.tally(cop(500_000)))
  }

  @Test
  fun noRecordedClosedMonthLeavesNothingToTally() {
    val history = listOf(month(10, spent = 0, recorded = false), month(11, spent = 100_000))

    assertEquals(BudgetHistoryTally(within = 0, closed = 0), history.tally(cop(500_000)))
  }

  private fun month(month: Int, spent: Long, recorded: Boolean = true) =
      BudgetMonth(YearMonth(2026, month), cop(spent), recorded)

  private fun cop(units: Long) = Money(units * 100, Moneda.COP)
}
