package com.kurobello.tallybook.core.ui.component

import androidx.compose.runtime.Immutable
import com.kurobello.tallybook.core.model.MovimientoPeriodRanges
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

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
  override val start: LocalDate = MovimientoPeriodRanges.calendarMonth(month).start
  override val end: LocalDate = MovimientoPeriodRanges.calendarMonth(month).end
}

internal fun selectableDayPeriods(month: YearMonth, today: LocalDate): List<TallybookDayPeriod> =
    MovimientoPeriodRanges.days(month, today).map { range -> TallybookDayPeriod(range.start) }

internal fun selectableWeekPeriods(month: YearMonth, today: LocalDate): List<TallybookWeekPeriod> =
    MovimientoPeriodRanges.weeks(month, today).map { range ->
      TallybookWeekPeriod(range.start, range.end)
    }

internal fun selectableMonthPeriods(year: Int, today: LocalDate): List<TallybookMonthPeriod> =
    MovimientoPeriodRanges.months(year, today).map { range ->
      TallybookMonthPeriod(YearMonth(range.start.year, range.start.month))
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
