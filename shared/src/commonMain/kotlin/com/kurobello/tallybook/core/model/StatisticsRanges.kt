package com.kurobello.tallybook.core.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.yearMonth

internal enum class StatisticsScope {
  Month,
  Year,
}

/** The month or year holding [anchor]; the period in progress ends at [today]. */
internal fun statisticsPeriod(
    scope: StatisticsScope,
    anchor: LocalDate,
    today: LocalDate,
): MovimientoDateRange {
  require(anchor <= today) { "Statistics anchor must not be in the future" }
  val calendar = calendarPeriod(scope, anchor)
  return calendar.copy(end = minOf(calendar.end, today))
}

/** Whether the period holding [anchor] is still in progress on [today]. */
internal fun isCurrentStatisticsPeriod(
    scope: StatisticsScope,
    anchor: LocalDate,
    today: LocalDate,
): Boolean = anchor <= today && today <= calendarPeriod(scope, anchor).end

/**
 * The whole previous period for a historical one. A period in progress is compared with the same
 * stretch of the previous one, and a day that the previous period lacks moves to its last day.
 */
internal fun statisticsComparison(
    scope: StatisticsScope,
    anchor: LocalDate,
    today: LocalDate,
): MovimientoDateRange {
  require(anchor <= today) { "Statistics anchor must not be in the future" }
  val previous =
      when (scope) {
        StatisticsScope.Month -> anchor.yearMonth.minus(1, DateTimeUnit.MONTH)
        StatisticsScope.Year -> YearMonth(anchor.year - 1, Month.DECEMBER)
      }
  val whole = calendarPeriod(scope, previous.firstDay)
  if (!isCurrentStatisticsPeriod(scope, anchor, today)) return whole
  val stretchMonth =
      when (scope) {
        StatisticsScope.Month -> previous
        StatisticsScope.Year -> YearMonth(previous.year, today.month)
      }
  val stretchEnd =
      LocalDate(stretchMonth.year, stretchMonth.month, minOf(today.day, stretchMonth.numberOfDays))
  return whole.copy(end = stretchEnd)
}

/**
 * Whole months up to the period's end: the [TREND_MONTHS] ending in the selected one for Month, and
 * the selected year's months for Year.
 */
internal fun statisticsTrend(
    scope: StatisticsScope,
    anchor: LocalDate,
    today: LocalDate,
): MovimientoDateRange {
  val period = statisticsPeriod(scope, anchor, today)
  val start =
      when (scope) {
        StatisticsScope.Month ->
            anchor.yearMonth.minus(TREND_MONTHS - 1, DateTimeUnit.MONTH).firstDay
        StatisticsScope.Year -> period.start
      }
  return MovimientoDateRange(start, period.end)
}

internal fun statisticsRequest(
    scope: StatisticsScope,
    anchor: LocalDate,
    moneda: Moneda,
    today: LocalDate,
): StatisticsRequest {
  val period = statisticsPeriod(scope, anchor, today)
  return StatisticsRequest(
      moneda = moneda,
      period = period,
      comparison = statisticsComparison(scope, anchor, today),
      trend = statisticsTrend(scope, anchor, today),
      daily = period,
  )
}

private fun calendarPeriod(scope: StatisticsScope, anchor: LocalDate): MovimientoDateRange =
    when (scope) {
      StatisticsScope.Month -> MovimientoPeriodRanges.calendarMonth(anchor.yearMonth)
      StatisticsScope.Year ->
          MovimientoDateRange(
              LocalDate(anchor.year, Month.JANUARY, 1),
              LocalDate(anchor.year, Month.DECEMBER, DECEMBER_DAYS),
          )
    }

private const val TREND_MONTHS = 6
private const val DECEMBER_DAYS = 31
