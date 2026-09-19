package com.kurobello.tallybook.core.ui.component

import androidx.compose.runtime.Immutable
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import kotlinx.datetime.plus

internal enum class TallybookPeriodScope {
  Day,
  Week,
  Month,
}

@Immutable
internal sealed interface TallybookPeriod {
  val start: LocalDate
  val end: LocalDate
}

@Immutable
internal data class TallybookDayPeriod(val date: LocalDate) : TallybookPeriod {
  override val start: LocalDate = date
  override val end: LocalDate = date
}

@Immutable
internal data class TallybookWeekPeriod(
    override val start: LocalDate,
    override val end: LocalDate,
) : TallybookPeriod

@Immutable
internal data class TallybookMonthPeriod(val month: YearMonth) : TallybookPeriod {
  override val start: LocalDate = LocalDate(month.year, month.month, 1)
  override val end: LocalDate = LocalDate(month.year, month.month, month.numberOfDays)
}

internal fun selectableDayPeriods(month: YearMonth, today: LocalDate): List<TallybookDayPeriod> {
  val lastDay = selectableMonthEnd(month, today)?.day ?: return emptyList()
  return (1..lastDay).map { day -> TallybookDayPeriod(LocalDate(month.year, month.month, day)) }
}

internal fun selectableWeekPeriods(month: YearMonth, today: LocalDate): List<TallybookWeekPeriod> {
  val monthEnd = selectableMonthEnd(month, today) ?: return emptyList()
  val periods = mutableListOf<TallybookWeekPeriod>()
  var start = LocalDate(month.year, month.month, 1)
  while (start <= monthEnd) {
    periods +=
        TallybookWeekPeriod(start = start, end = minOf(start.plus(DatePeriod(days = 6)), monthEnd))
    start = start.plus(DatePeriod(days = DAYS_PER_WEEK))
  }
  return periods
}

internal fun selectableMonthPeriods(year: Int, today: LocalDate): List<TallybookMonthPeriod> {
  if (year > today.year) return emptyList()
  val lastMonth = if (year == today.year) today.month.ordinal + 1 else MONTHS_PER_YEAR
  return Month.entries.take(lastMonth).map { month -> TallybookMonthPeriod(YearMonth(year, month)) }
}

internal fun focusedMonthForYear(
    focusedMonth: YearMonth,
    year: Int,
    today: LocalDate,
): YearMonth =
    YearMonth(
        year,
        if (year == today.year && focusedMonth.month > today.month) today.month
        else focusedMonth.month,
    )

private fun selectableMonthEnd(month: YearMonth, today: LocalDate): LocalDate? {
  val firstDay = LocalDate(month.year, month.month, 1)
  if (firstDay > today) return null
  return if (month.year == today.year && month.month == today.month) today
  else LocalDate(month.year, month.month, month.numberOfDays)
}

private const val DAYS_PER_WEEK = 7
private const val MONTHS_PER_YEAR = 12
