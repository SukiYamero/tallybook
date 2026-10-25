package com.kurobello.tallybook.feature.statistics.viewmodel

import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.StatisticsRequest
import com.kurobello.tallybook.core.model.StatisticsScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class StatisticsSelectionTest {
  @Test
  fun initialSelectionIsTheCurrentMonthComparedWithTheSameStretchOfTheLastOne() {
    assertEquals(
        StatisticsRequest(
            moneda = Moneda.COP,
            period = range(2026, 10, 1, 2026, 10, 24),
            comparison = range(2026, 9, 1, 2026, 9, 24),
            trend = range(2026, 5, 1, 2026, 10, 24),
            daily = range(2026, 10, 1, 2026, 10, 24),
        ),
        StatisticsSelection.initial(today).request(Moneda.COP, today),
    )
  }

  @Test
  fun thirtyFirstOfMarchComparesWithTheWholeFebruaryOfLeapAndCommonYears() {
    val leap = LocalDate(2024, 3, 31)
    val common = LocalDate(2025, 3, 31)

    assertEquals(
        range(2024, 2, 1, 2024, 2, 29),
        StatisticsSelection.initial(leap).request(Moneda.COP, leap).comparison,
    )
    assertEquals(
        range(2025, 2, 1, 2025, 2, 28),
        StatisticsSelection.initial(common).request(Moneda.COP, common).comparison,
    )
  }

  @Test
  fun aDayMissingFromThePreviousMonthMovesToItsLastDay() {
    val may31 = LocalDate(2026, 5, 31)

    assertEquals(
        range(2026, 4, 1, 2026, 4, 30),
        StatisticsSelection.initial(may31).request(Moneda.COP, may31).comparison,
    )
  }

  @Test
  fun leapDayInYearComparesWithTheTwentyEighthOfTheYearBefore() {
    val leapDay = LocalDate(2024, 2, 29)
    val request = StatisticsSelection(StatisticsScope.Year, leapDay).request(Moneda.COP, leapDay)

    assertEquals(range(2024, 1, 1, 2024, 2, 29), request.period)
    assertEquals(range(2023, 1, 1, 2023, 2, 28), request.comparison)
    assertEquals(range(2024, 1, 1, 2024, 2, 29), request.trend)
    assertEquals(request.period, request.daily)
  }

  @Test
  fun januaryComparesWithDecemberOfThePreviousYear() {
    val january15 = LocalDate(2026, 1, 15)

    val current = StatisticsSelection.initial(january15).request(Moneda.COP, january15)
    val historical =
        StatisticsSelection(StatisticsScope.Month, january15).request(Moneda.COP, today)

    assertEquals(range(2025, 12, 1, 2025, 12, 15), current.comparison)
    assertEquals(range(2025, 8, 1, 2026, 1, 15), current.trend)
    assertEquals(range(2025, 12, 1, 2025, 12, 31), historical.comparison)
  }

  @Test
  fun aHistoricalMonthIsComparedWholeAgainstTheWholePreviousMonth() {
    val request =
        StatisticsSelection(StatisticsScope.Month, LocalDate(2025, 4, 10))
            .request(Moneda.COP, today)

    assertEquals(
        StatisticsRequest(
            moneda = Moneda.COP,
            period = range(2025, 4, 1, 2025, 4, 30),
            comparison = range(2025, 3, 1, 2025, 3, 31),
            trend = range(2024, 11, 1, 2025, 4, 30),
            daily = range(2025, 4, 1, 2025, 4, 30),
        ),
        request,
    )
  }

  @Test
  fun aHistoricalYearIsComparedWholeAndTrendsAcrossItsTwelveMonths() {
    val request =
        StatisticsSelection(StatisticsScope.Year, LocalDate(2024, 6, 1)).request(Moneda.COP, today)

    assertEquals(range(2024, 1, 1, 2024, 12, 31), request.period)
    assertEquals(range(2023, 1, 1, 2023, 12, 31), request.comparison)
    assertEquals(range(2024, 1, 1, 2024, 12, 31), request.trend)
  }

  @Test
  fun theCurrentYearTrendsUpToTheCurrentMonth() {
    val request = StatisticsSelection(StatisticsScope.Year, today).request(Moneda.COP, today)

    assertEquals(range(2026, 1, 1, 2026, 10, 24), request.period)
    assertEquals(range(2025, 1, 1, 2025, 10, 24), request.comparison)
    assertEquals(range(2026, 1, 1, 2026, 10, 24), request.trend)
  }

  @Test
  fun noPeriodOrRequestedRangeEndsAfterToday() {
    val years = listOf(2026, 2025, 2019)
    for (scope in StatisticsScope.entries) {
      val selection = StatisticsSelection(scope, LocalDate(2026, 3, 9))
      val request = selection.request(Moneda.COP, today)
      val ranges =
          selection.periods(today, years) +
              listOfNotNull(request.period, request.comparison, request.trend, request.daily)

      assertTrue(ranges.all { it.end <= today }, "$scope ranges must end by today")
    }
  }

  @Test
  fun monthChipsCoverTheAnchorYearAndTheCurrentOneEndsToday() {
    val periods = StatisticsSelection(StatisticsScope.Month, today).periods(today, listOf(2026))

    assertEquals(10, periods.size)
    assertEquals(range(2026, 1, 1, 2026, 1, 31), periods.first())
    assertEquals(range(2026, 10, 1, 2026, 10, 24), periods.last())
  }

  @Test
  fun yearChipsListTheGivenYearsInOrderAndTheCurrentOneEndsToday() {
    val periods =
        StatisticsSelection(StatisticsScope.Year, today).periods(today, listOf(2026, 2024, 2025))

    assertEquals(
        listOf(
            range(2024, 1, 1, 2024, 12, 31),
            range(2025, 1, 1, 2025, 12, 31),
            range(2026, 1, 1, 2026, 10, 24),
        ),
        periods,
    )
  }

  @Test
  fun availableYearsKeepTheSelectedLegacyYear() {
    val selection = StatisticsSelection(StatisticsScope.Year, LocalDate(2016, 5, 1))

    assertEquals(
        listOf(2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019, 2016, 2014),
        selection.availableYears(listOf(2014), today),
    )
  }

  @Test
  fun selectingAMonthPreservesTheDayClampedToTheMonthAndToday() {
    val selection = StatisticsSelection(StatisticsScope.Month, LocalDate(2026, 1, 31))

    assertEquals(
        StatisticsSelection(StatisticsScope.Month, LocalDate(2026, 2, 28)),
        selection.selectPeriod(range(2026, 2, 1, 2026, 2, 28), today),
    )
    assertEquals(
        StatisticsSelection(StatisticsScope.Month, today),
        selection.selectPeriod(range(2026, 10, 1, 2026, 10, 24), today),
    )
  }

  @Test
  fun selectingTheSelectedPeriodKeepsTheAnchor() {
    val selection = StatisticsSelection(StatisticsScope.Month, LocalDate(2026, 3, 9))

    assertEquals(selection, selection.selectPeriod(range(2026, 3, 1, 2026, 3, 31), today))
  }

  @Test
  fun selectingAYearChipMovesTheAnchorToThatYear() {
    val selection = StatisticsSelection(StatisticsScope.Year, LocalDate(2024, 2, 29))

    assertEquals(
        StatisticsSelection(StatisticsScope.Year, LocalDate(2025, 2, 28)),
        selection.selectPeriod(range(2025, 1, 1, 2025, 12, 31), today),
    )
  }

  @Test
  fun periodsThatAreNotAChipOfTheScopeAreRejected() {
    val month = StatisticsSelection(StatisticsScope.Month, today)
    val year = StatisticsSelection(StatisticsScope.Year, today)

    assertFailsWith<IllegalArgumentException> {
      month.selectPeriod(range(2026, 10, 1, 2026, 10, 31), today)
    }
    assertFailsWith<IllegalArgumentException> {
      month.selectPeriod(range(2026, 9, 2, 2026, 9, 30), today)
    }
    assertFailsWith<IllegalArgumentException> {
      year.selectPeriod(range(2025, 1, 1, 2025, 1, 31), today)
    }
    assertFailsWith<IllegalArgumentException> { month.selectYear(2027, today) }
  }

  @Test
  fun selectingAYearKeepsTheMonthAndClampsToToday() {
    val selection = StatisticsSelection(StatisticsScope.Month, LocalDate(2024, 12, 31))

    assertEquals(LocalDate(2025, 12, 31), selection.selectYear(2025, today).anchor)
    assertEquals(today, selection.selectYear(2026, today).anchor)
  }

  @Test
  fun changingScopeKeepsTheAnchor() {
    val month = StatisticsSelection(StatisticsScope.Month, LocalDate(2025, 4, 10))
    val year = month.selectScope(StatisticsScope.Year)

    assertEquals(StatisticsSelection(StatisticsScope.Year, LocalDate(2025, 4, 10)), year)
    assertEquals(month, year.selectScope(StatisticsScope.Month))
  }

  @Test
  fun refreshFollowsTheNewDayInsideTheCurrentPeriod() {
    val selection = StatisticsSelection(StatisticsScope.Month, LocalDate(2026, 10, 10))
    val next = LocalDate(2026, 10, 25)

    val refreshed = selection.refresh(today, next)

    assertEquals(StatisticsSelection(StatisticsScope.Month, next), refreshed)
    assertEquals(range(2026, 10, 1, 2026, 10, 25), refreshed.request(Moneda.COP, next).period)
  }

  @Test
  fun refreshMovesACurrentMonthIntoTheNewMonth() {
    val october31 = LocalDate(2026, 10, 31)
    val november1 = LocalDate(2026, 11, 1)

    val refreshed = StatisticsSelection.initial(october31).refresh(october31, november1)

    assertEquals(StatisticsSelection(StatisticsScope.Month, november1), refreshed)
    assertEquals(
        range(2026, 11, 1, 2026, 11, 1),
        refreshed.request(Moneda.COP, november1).period,
    )
  }

  @Test
  fun refreshKeepsAHistoricalPeriodAndClampsAFutureAnchor() {
    val historical = StatisticsSelection(StatisticsScope.Month, LocalDate(2025, 4, 10))
    val earlier = LocalDate(2026, 10, 20)
    val year = StatisticsSelection(StatisticsScope.Year, LocalDate(2026, 10, 22))

    assertEquals(historical, historical.refresh(today, LocalDate(2026, 10, 25)))
    assertEquals(
        StatisticsSelection(StatisticsScope.Year, earlier),
        year.refresh(today, earlier),
    )
  }

  private fun range(
      startYear: Int,
      startMonth: Int,
      startDay: Int,
      endYear: Int,
      endMonth: Int,
      endDay: Int,
  ) =
      MovimientoDateRange(
          LocalDate(startYear, startMonth, startDay),
          LocalDate(endYear, endMonth, endDay),
      )

  private companion object {
    val today = LocalDate(2026, 10, 24)
  }
}
