package com.kurobello.tallybook.feature.statistics.viewmodel

import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.StatisticsRequest
import com.kurobello.tallybook.core.model.StatisticsScope
import com.kurobello.tallybook.core.model.isCurrentStatisticsPeriod
import com.kurobello.tallybook.core.model.selectableYears
import com.kurobello.tallybook.core.model.statisticsPeriod
import com.kurobello.tallybook.core.model.statisticsRequest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth

/** The selected period is the month or year holding [anchor], which is never after today. */
internal data class StatisticsSelection(val scope: StatisticsScope, val anchor: LocalDate) {
  fun request(moneda: Moneda, today: LocalDate): StatisticsRequest =
      statisticsRequest(scope, anchor, moneda, today)

  /**
   * The chips of the scope, oldest first and ending by [today]: the months of the anchor's year, or
   * one per year of [years] (as [availableYears] lists them).
   */
  fun periods(today: LocalDate, years: List<Int>): List<MovimientoDateRange> {
    require(anchor <= today) { "Statistics anchor must not be in the future" }
    val starts =
        when (scope) {
          StatisticsScope.Month ->
              Month.entries.map { LocalDate(anchor.year, it, 1) }.filter { it <= today }
          StatisticsScope.Year ->
              years.filter { it <= today.year }.distinct().sorted().map { LocalDate(it, 1, 1) }
        }
    return starts.map { statisticsPeriod(scope, it, today) }
  }

  // Retention belongs to the navigation entry so a deleted year's chip survives later selections.
  fun availableYears(
      persistedYears: List<Int>,
      today: LocalDate,
      retainedYears: Set<Int> = emptySet(),
  ): List<Int> = selectableYears(persistedYears, today, retainedYears + anchor.year)

  fun selectScope(scope: StatisticsScope): StatisticsSelection = copy(scope = scope)

  fun selectPeriod(period: MovimientoDateRange, today: LocalDate): StatisticsSelection {
    require(period.start <= today && period == statisticsPeriod(scope, period.start, today)) {
      "Statistics period must be a whole $scope ending by today"
    }
    if (anchor >= period.start && anchor <= period.end) return this
    return when (scope) {
      StatisticsScope.Month -> selectMonth(YearMonth(period.start.year, period.start.month), today)
      StatisticsScope.Year -> selectYear(period.start.year, today)
    }
  }

  fun selectYear(year: Int, today: LocalDate): StatisticsSelection {
    require(year <= today.year) { "Statistics year must not be in the future" }
    return selectMonth(YearMonth(year, anchor.month), today)
  }

  /** A selection in progress keeps following today; a historical one stays where it was. */
  fun refresh(previousToday: LocalDate, today: LocalDate): StatisticsSelection =
      copy(
          anchor =
              if (isCurrentStatisticsPeriod(scope, anchor, previousToday)) today
              else minOf(anchor, today)
      )

  private fun selectMonth(month: YearMonth, today: LocalDate): StatisticsSelection {
    val bounded = minOf(month, YearMonth(today.year, today.month))
    val date = LocalDate(bounded.year, bounded.month, minOf(anchor.day, bounded.numberOfDays))
    return copy(anchor = minOf(date, today))
  }

  companion object {
    fun initial(today: LocalDate): StatisticsSelection =
        StatisticsSelection(StatisticsScope.Month, today)
  }
}
