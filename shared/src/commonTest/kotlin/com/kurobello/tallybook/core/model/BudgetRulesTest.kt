package com.kurobello.tallybook.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

class BudgetRulesTest {
  @Test
  fun overWhenSpentAboveMonto() {
    val status = status(spent = pesos(520_000))

    assertEquals(BudgetState.OVER, status.state)
    assertEquals(pesos(-20_000), status.remaining)
    assertNull(status.perDay)
    assertNull(status.paceDay)
  }

  @Test
  fun nearAtEightyPercentAndAtExactMonto() {
    val eighty = status(spent = pesos(400_000))
    val exact = status(spent = pesos(500_000))

    assertEquals(BudgetState.NEAR, eighty.state)
    assertEquals(BudgetState.NEAR, exact.state)
    assertEquals(pesos(0), exact.remaining)
    assertNull(exact.perDay)
  }

  @Test
  fun noPaceBeforeDayEight() {
    assertEquals(
        BudgetState.FINE,
        status(spent = pesos(300_000), today = LocalDate(2026, 11, 7)).state,
    )
  }

  @Test
  fun paceFromDayEightWhenProjectionExceeds() {
    val status = status(spent = pesos(200_000), today = LocalDate(2026, 11, 10))

    assertEquals(BudgetState.PACE, status.state)
    assertEquals(26, status.paceDay)
  }

  @Test
  fun fineReportsPerDayIncludingToday() {
    val status = status(spent = pesos(140_000), today = LocalDate(2026, 11, 19))

    assertEquals(BudgetState.FINE, status.state)
    assertEquals(pesos(30_000), status.perDay)
    assertNull(status.paceDay)
  }

  @Test
  fun perDayOnLastDayIsRemaining() {
    val status = status(spent = pesos(100_000), today = LocalDate(2026, 11, 30))

    assertEquals(BudgetState.FINE, status.state)
    assertEquals(pesos(400_000), status.perDay)
  }

  @Test
  fun paceDayFallsInsideFebruary() {
    val status =
        status(monto = pesos(100_000), spent = pesos(30_000), today = LocalDate(2027, 2, 8))

    assertEquals(BudgetState.PACE, status.state)
    assertEquals(27, status.paceDay)
  }

  @Test
  fun paceNeverOverflowsNearTheCap() {
    val cap = Money(Money.MAX_MINOR_UNITS, Moneda.COP)
    val nearCap = status(monto = cap, spent = Money(cap.minorUnits - 1, Moneda.COP))
    val huge =
        status(
            monto = Money(Long.MAX_VALUE / 2, Moneda.COP),
            spent = Money(Long.MAX_VALUE / 3, Moneda.COP),
            today = LocalDate(2026, 11, 8),
        )

    assertEquals(BudgetState.NEAR, nearCap.state)
    assertEquals(BudgetState.PACE, huge.state)
    assertNotNull(huge.paceDay)
  }

  @Test
  fun orderBySeverityThenShareThenName() {
    val fine = status(spent = pesos(10_000), name = "A")
    val nearLow = status(spent = pesos(400_000), name = "B")
    val nearHigh = status(spent = pesos(450_000), name = "C")
    val nearHighTie = status(spent = pesos(450_000), name = "A")
    val over = status(spent = pesos(600_000), name = "Z")

    val sorted =
        listOf(fine, nearLow, nearHigh, nearHighTie, over).sortedWith(budgetOrder).map {
          it.categoriaNombre to it.state
        }

    assertEquals(
        listOf(
            "Z" to BudgetState.OVER,
            "A" to BudgetState.NEAR,
            "C" to BudgetState.NEAR,
            "B" to BudgetState.NEAR,
            "A" to BudgetState.FINE,
        ),
        sorted,
    )
  }

  @Test
  fun noticeIgnoresOtherCurrency() {
    val over = status(spent = pesos(600_000))

    assertNull(budgetNotice(listOf(over), listOf(touch("comida", moneda = Moneda.USD)), today))
  }

  @Test
  fun noticeIgnoresGastoOutsideCurrentMonth() {
    val over = status(spent = pesos(600_000))

    assertNull(
        budgetNotice(listOf(over), listOf(touch("comida", fecha = LocalDate(2026, 10, 31))), today)
    )
  }

  @Test
  fun noticeIgnoresFineBudgets() {
    val fine = status(spent = pesos(10_000))

    assertNull(budgetNotice(listOf(fine), listOf(touch("comida")), today))
  }

  @Test
  fun noticeMostSevereWins() {
    val parent = status(categoriaId = "comida", name = "Comida", spent = pesos(600_000))
    val child =
        status(
            categoriaId = "restaurantes",
            name = "Restaurantes",
            padreId = "comida",
            spent = pesos(450_000),
        )

    val notice =
        assertNotNull(
            budgetNotice(listOf(child, parent), listOf(touch("restaurantes", "comida")), today)
        )

    assertEquals(BudgetNotice("Comida", BudgetState.OVER, pesos(100_000), null), notice)
  }

  @Test
  fun noticeTieGoesToSubcategory() {
    val parent = status(categoriaId = "comida", name = "Comida", spent = pesos(450_000))
    val child =
        status(
            categoriaId = "restaurantes",
            name = "Restaurantes",
            padreId = "comida",
            spent = pesos(420_000),
        )

    val notice =
        assertNotNull(
            budgetNotice(listOf(parent, child), listOf(touch("restaurantes", "comida")), today)
        )

    assertEquals(BudgetNotice("Restaurantes", BudgetState.NEAR, pesos(80_000), null), notice)
  }

  @Test
  fun noticeTouchesParentThroughChildCategory() {
    val parent = status(categoriaId = "comida", name = "Comida", spent = pesos(450_000))

    val notice =
        assertNotNull(budgetNotice(listOf(parent), listOf(touch("restaurantes", "comida")), today))

    assertEquals("Comida", notice.categoriaNombre)
  }

  @Test
  fun noticeReportsPaceDay() {
    val day = LocalDate(2026, 11, 10)
    val pace = status(spent = pesos(200_000), today = day)

    val notice =
        assertNotNull(budgetNotice(listOf(pace), listOf(touch("comida", fecha = day)), day))

    assertEquals(BudgetNotice("Comida", BudgetState.PACE, pesos(300_000), 26), notice)
  }

  @Test
  fun suggestionAveragesWithZeroMonths() {
    val months = setOf(YearMonth(2026, 8), YearMonth(2026, 9), YearMonth(2026, 10))
    val gasto = mapOf(YearMonth(2026, 9) to pesos(300_000))

    assertEquals(pesos(100_000), suggestedBudget(gasto, months, Moneda.COP, today))
  }

  @Test
  fun suggestionSkipsMonthsBeforeRecording() {
    val months = setOf(YearMonth(2026, 9), YearMonth(2026, 10), YearMonth(2026, 11))
    val gasto =
        mapOf(
            YearMonth(2026, 9) to pesos(100_000),
            YearMonth(2026, 10) to pesos(300_000),
            YearMonth(2026, 11) to pesos(900_000),
            YearMonth(2026, 3) to pesos(900_000),
        )

    assertEquals(pesos(200_000), suggestedBudget(gasto, months, Moneda.COP, today))
  }

  @Test
  fun suggestionUsesOnlyTheThreeLatestOfThePreviousFive() {
    val months = (1..10).map { YearMonth(2026, it) }.toSet()
    val gasto = months.associateWith { pesos(if (it.month.ordinal < 7) 900_000 else 300_000) }

    assertEquals(pesos(300_000), suggestedBudget(gasto, months, Moneda.COP, today))
  }

  @Test
  fun suggestionNullWithoutWholeMonths() {
    assertNull(
        suggestedBudget(
            mapOf(YearMonth(2026, 11) to pesos(300_000)),
            setOf(YearMonth(2026, 11)),
            Moneda.COP,
            today,
        )
    )
  }

  @Test
  fun suggestionNullWhenAverageZero() {
    assertNull(suggestedBudget(emptyMap(), setOf(YearMonth(2026, 10)), Moneda.COP, today))
  }

  @Test
  fun roundsUpToTwoSignificantDigits() {
    assertEquals(43_000_000, roundUpTwoSignificant(42_033_700))
    assertEquals(8_800, roundUpTwoSignificant(8_750))
    assertEquals(99, roundUpTwoSignificant(99))
    assertEquals(0, roundUpTwoSignificant(0))
    assertEquals(42_000_000, roundUpTwoSignificant(42_000_000))
  }

  private fun status(
      monto: Money = pesos(500_000),
      spent: Money,
      today: LocalDate = BudgetRulesTest.today,
      name: String = "Comida",
      categoriaId: String = "comida",
      padreId: String? = null,
  ) =
      budgetStatus(
          Presupuesto("p-$categoriaId", categoriaId, monto, Instant.fromEpochSeconds(0)),
          name,
          padreId,
          spent,
          today,
      )

  private fun touch(
      categoriaId: String,
      padreId: String? = null,
      moneda: Moneda = Moneda.COP,
      fecha: LocalDate = today,
  ) = BudgetTouch(categoriaId, padreId, moneda, fecha)

  private fun pesos(amount: Long) = Money(amount * COP_MINOR_UNITS_PER_PESO, Moneda.COP)

  private companion object {
    const val COP_MINOR_UNITS_PER_PESO = 100L
    val today = LocalDate(2026, 11, 15)
  }
}
