package com.kurobello.tallybook.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

class StatisticsRulesTest {
  @Test
  fun changeRoundsHalfAwayFromZeroWithItsDirection() {
    assertEquals(Change(13, Direction.Up), change(money(1125), money(1000)))
    assertEquals(Change(13, Direction.Down), change(money(875), money(1000)))
    assertEquals(Change(12, Direction.Up), change(money(1124), money(1000)))
    assertEquals(Change(0, Direction.Flat), change(money(1000), money(1000)))
    assertEquals(Change(100, Direction.Down), change(money(0), money(1000)))
  }

  @Test
  fun changeIsAbsentWithoutAPositivePrevious() {
    assertNull(change(money(500), null))
    assertNull(change(money(500), money(0)))
  }

  @Test
  fun exclusiveSharesGiveTheRoundingRemainderToTheLargestGroup() {
    val shares = exclusiveShares(listOf(total("a", 1), total("b", 1), total("c", 1)))

    assertEquals(listOf(3334, 3333, 3333), shares.map { it.basisPoints })
    assertEquals(BASIS_POINTS, shares.sumOf { it.basisPoints })
  }

  @Test
  fun exclusiveSharesTakeBackTheExcessFromTheLargestGroup() {
    val shares = exclusiveShares(listOf(total("a", 3), total("b", 1), total("c", 1), total("d", 1)))

    assertEquals(listOf(4999, 1667, 1667, 1667), shares.map { it.basisPoints })
  }

  @Test
  fun sharesOfTotalsAtTheMontoCapStayExact() {
    val cap = Money.MAX_MINOR_UNITS
    val exclusive = exclusiveShares(listOf(total("a", cap), total("b", cap / 3)))
    val overlapping =
        overlappingShares(
            listOf(DimensionTotal(StatisticsKey.Etiqueta("t"), money(cap / 8), null)),
            money(cap),
        )

    assertEquals(listOf(7500, 2500), exclusive.map { it.basisPoints })
    assertEquals(listOf(1250), overlapping.map { it.basisPoints })
    assertEquals(Change(200, Direction.Up), change(money(cap), money(cap / 3)))
  }

  @Test
  fun sevenExclusiveGroupsShowFiveAndGroupTheRestInOtras() {
    val totals =
        listOf(
            total("a", 300, previous = 200),
            total("b", 200),
            total("c", 150),
            total("d", 120),
            total("e", 100),
            total("f", 80),
            total("g", 50),
        )

    val shares = exclusiveShares(totals)

    assertEquals(
        listOf(
            categoria("a"),
            categoria("b"),
            categoria("c"),
            categoria("d"),
            categoria("e"),
            null,
        ),
        shares.map { it.key },
    )
    assertEquals(money(130), shares.last().amount)
    assertEquals(listOf(3000, 2000, 1500, 1200, 1000, 1300), shares.map { it.basisPoints })
    assertEquals(Change(50, Direction.Up), shares.first().change)
    assertNull(shares.last().change)
  }

  @Test
  fun sixExclusiveGroupsAreAllShownWithoutOtras() {
    val totals = ('a'..'f').map { total(it.toString(), 10) }

    assertEquals(totals.map { it.key }, exclusiveShares(totals).map { it.key })
  }

  @Test
  fun exclusiveSharesBreakTiesByIdAndSkipGroupsWithoutCurrentGasto() {
    val shares =
        exclusiveShares(listOf(total("b", 10), total("z", 0, previous = 40), total("a", 10)))

    assertEquals(listOf(categoria("a"), categoria("b")), shares.map { it.key })
    assertEquals(emptyList(), exclusiveShares(listOf(total("z", 0, previous = 40))))
  }

  @Test
  fun overlappingSharesMeasureEachTagAgainstTheTotalWithoutGrouping() {
    val tags =
        ('a'..'h').map { DimensionTotal(StatisticsKey.Etiqueta(it.toString()), money(1), null) }

    val shares = overlappingShares(tags, money(3))

    assertEquals(8, shares.size)
    assertEquals(List(8) { 3333 }, shares.map { it.basisPoints })
    assertEquals(emptyList(), overlappingShares(tags, money(0)))
  }

  @Test
  fun paceIsAbsentWithoutACompletePreviousMonthWithGasto() {
    val months = listOf(month(2026, 8, 0), month(2026, 9, 0), month(2026, 10, 500))

    assertNull(pace(octoberDaily(), months, today))
  }

  @Test
  fun paceAveragesASingleCompletePreviousMonth() {
    val pace = pace(octoberDaily(), listOf(month(2026, 9, 3100), month(2026, 10, 500)), today)

    assertEquals(money(3100), pace?.averageMonth)
  }

  @Test
  fun paceAveragesTheThreeLatestCompleteMonthsWithGasto() {
    val months =
        listOf(
            month(2026, 5, 9000),
            month(2026, 6, 1000),
            month(2026, 7, 0),
            month(2026, 8, 1000),
            month(2026, 9, 1001),
            month(2026, 10, 2400),
        )
    val daily = octoberDaily(gastoOn = mapOf(1 to 100, 3 to 200, 24 to 100))

    val pace = pace(daily, months, today)

    assertEquals(money(1000), pace?.averageMonth)
    assertEquals(24, pace?.cumulative?.size)
    assertEquals(money(100), pace?.cumulative?.first())
    assertEquals(money(300), pace?.cumulative?.get(2))
    assertEquals(money(400), pace?.cumulative?.last())
    assertEquals(money(517), pace?.projection)
    assertEquals(Change(48, Direction.Down), pace?.change)
  }

  @Test
  fun paceIsAbsentUnlessDailyIsTheCurrentMonth() {
    val months = listOf(month(2026, 8, 1000), month(2026, 9, 1000))
    val september = (1..30).map { DayGasto(LocalDate(2026, 9, it), money(10)) }

    assertNull(pace(september, months, LocalDate(2026, 10, 24)))
    assertNull(pace(octoberDaily().dropLast(1), months, today))
  }

  @Test
  fun insightThresholdsAreInclusive() {
    val atThreshold =
        snapshot(
            gastos = 10_000,
            categorias = listOf(total("a", 5_500, previous = 5_000)),
        )
    val belowPercent =
        snapshot(gastos = 4_000, categorias = listOf(total("a", 5_499, previous = 5_000)))
    val belowShare =
        snapshot(gastos = 10_020, categorias = listOf(total("a", 5_500, previous = 5_000)))

    assertEquals(
        listOf(Insight.CategoryRose(categoria("a"), Change(10, Direction.Up), money(500))),
        insights(atThreshold),
    )
    assertEquals(emptyList(), insights(belowPercent))
    assertEquals(emptyList(), insights(belowShare))
  }

  @Test
  fun categoriesWithoutAPositivePreviousAreNeverReported() {
    val snapshot =
        snapshot(
            gastos = 1_000,
            categorias = listOf(total("a", 900, previous = 0), total("b", 100, previous = null)),
        )

    assertEquals(emptyList(), insights(snapshot))
  }

  @Test
  fun aCategoryFallingToZeroIsTheOneThatFellTheMost() {
    val snapshot =
        snapshot(
            gastos = 1_000,
            categorias =
                listOf(
                    total("a", 1_000, previous = 1_000),
                    total("b", 0, previous = 400),
                    total("c", 0, previous = 300),
                ),
        )

    assertEquals(
        listOf(Insight.CategoryFell(categoria("b"), Change(100, Direction.Down), money(400))),
        insights(snapshot),
    )
  }

  @Test
  fun biggestDayNeedsGastoOnAtLeastTwoDays() {
    val oneDay = snapshot(gastos = 50, daily = listOf(day(2025, 3, 1, 0), day(2025, 3, 2, 50)))

    assertEquals(emptyList(), insights(oneDay))
  }

  @Test
  fun biggestDayOfAHistoricalMonthTakesTheEarliestOnATie() {
    val daily = (1..31).map { day(2025, 3, it, if (it in setOf(5, 9, 20)) 300 else 10) }
    val march = snapshot(gastos = daily.sumOf { it.gasto.minorUnits }, daily = daily)

    assertEquals(listOf(Insight.BiggestDay(LocalDate(2025, 3, 5), money(300))), insights(march))
  }

  @Test
  fun biggestDayOfAYearSpansTheWholePeriod() {
    val start = LocalDate(2025, 1, 1)
    val daily =
        (start..LocalDate(2025, 12, 31)).map {
          DayGasto(it, money(if (it == LocalDate(2025, 11, 30)) 9_000 else 1))
        }
    val year = snapshot(gastos = daily.sumOf { it.gasto.minorUnits }, daily = daily)

    assertEquals(listOf(Insight.BiggestDay(LocalDate(2025, 11, 30), money(9_000))), insights(year))
  }

  @Test
  fun insightsComeRoseFellThenBiggestDayAndAreAtMostThree() {
    val snapshot =
        snapshot(
            gastos = 10_000,
            categorias =
                listOf(
                    total("a", 6_000, previous = 4_000),
                    total("b", 3_000, previous = 2_000),
                    total("c", 1_000, previous = 2_000),
                    total("d", 0, previous = 1_000),
                ),
            daily = listOf(day(2025, 3, 1, 4_000), day(2025, 3, 2, 6_000)),
        )

    assertEquals(
        listOf(
            Insight.CategoryRose(categoria("a"), Change(50, Direction.Up), money(2_000)),
            Insight.CategoryFell(categoria("c"), Change(50, Direction.Down), money(1_000)),
            Insight.BiggestDay(LocalDate(2025, 3, 2), money(6_000)),
        ),
        insights(snapshot),
    )
  }

  @Test
  fun equalDifferencesBreakTiesByCategoriaId() {
    val snapshot =
        snapshot(
            gastos = 4_000,
            categorias =
                listOf(total("b", 2_000, previous = 1_000), total("a", 2_000, previous = 1_000)),
        )

    assertEquals(
        listOf(Insight.CategoryRose(categoria("a"), Change(100, Direction.Up), money(1_000))),
        insights(snapshot),
    )
  }

  private fun octoberDaily(gastoOn: Map<Int, Long> = emptyMap()): List<DayGasto> =
      (1..today.day).map { DayGasto(LocalDate(2026, 10, it), money(gastoOn[it] ?: 0)) }

  private fun month(year: Int, month: Int, gastos: Long) =
      MonthTotals(YearMonth(year, month), MovimientoSummary(money(0), money(gastos)))

  private fun day(year: Int, month: Int, day: Int, gasto: Long) =
      DayGasto(LocalDate(year, month, day), money(gasto))

  private fun snapshot(
      gastos: Long,
      categorias: List<DimensionTotal> = emptyList(),
      daily: List<DayGasto> = emptyList(),
  ) =
      StatisticsSnapshot(
          current = MovimientoSummary(money(0), money(gastos)),
          previous = null,
          categorias = categorias,
          etiquetas = emptyList(),
          cuentas = emptyList(),
          medios = emptyList(),
          months = emptyList(),
          daily = daily,
          hasAnyMovimiento = true,
      )

  private fun total(id: String, current: Long, previous: Long? = null) =
      DimensionTotal(categoria(id), money(current), previous?.let(::money))

  private fun categoria(id: String) = StatisticsKey.Categoria(id)

  private fun money(minorUnits: Long) = Money(minorUnits, Moneda.COP)

  private companion object {
    const val BASIS_POINTS = 10_000
    val today = LocalDate(2026, 10, 24)
  }
}
