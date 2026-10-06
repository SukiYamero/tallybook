package com.kurobello.tallybook.core.ui.component

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.FIRST_SELECTABLE_YEAR
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import kotlinx.datetime.plus

internal fun monthGridDates(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate> {
  val firstDate = month.firstDay
  val leadingDays =
      (firstDate.dayOfWeek.ordinal - firstDayOfWeek.ordinal + DAYS_IN_WEEK) % DAYS_IN_WEEK
  val gridDays = leadingDays + month.numberOfDays
  val completeWeeks = (gridDays + DAYS_IN_WEEK - 1) / DAYS_IN_WEEK
  return List(completeWeeks * DAYS_IN_WEEK) { firstDate.plus(DatePeriod(days = it - leadingDays)) }
}

internal fun selectableYears(
    selectedDate: LocalDate,
    maxDate: LocalDate?,
    minYear: Int = FIRST_SELECTABLE_YEAR,
    minDate: LocalDate? = null,
): IntRange {
  if (minDate != null && maxDate != null && minDate > maxDate) return IntRange.EMPTY
  val maximum =
      maxDate?.year
          ?: maxOf(
              selectedDate.year + YEARS_AHEAD_WITHOUT_LIMIT,
              minDate?.year ?: selectedDate.year,
          )
  val floor = minOf(minYear, selectedDate.year, maximum)
  val minimum = if (minDate != null) maxOf(floor, minDate.year) else floor
  return minimum..maximum
}

internal fun initialCalendarMonth(
    value: LocalDate,
    maxDate: LocalDate?,
    minDate: LocalDate? = null,
): YearMonth {
  val visible =
      when {
        maxDate != null && (value > maxDate || (minDate != null && minDate > maxDate)) -> maxDate
        minDate != null && value < minDate -> minDate
        else -> value
      }
  return YearMonth(visible.year, visible.month)
}

internal fun isCalendarDateSelectable(
    date: LocalDate,
    minimumYear: Int,
    maxDate: LocalDate? = null,
    minDate: LocalDate? = null,
): Boolean =
    date.year >= minimumYear &&
        (maxDate == null || date <= maxDate) &&
        (minDate == null || date >= minDate)

internal fun minimumCalendarMonth(years: IntRange, minDate: LocalDate?): YearMonth =
    if (minDate != null && minDate.year == years.first) YearMonth(minDate.year, minDate.month)
    else YearMonth(years.first, Month.JANUARY)

internal fun calendarPopupMaximumHeight(
    windowHeight: Dp,
    verticalSafeInsets: Dp,
    edgeMargin: Dp,
): Dp =
    if (windowHeight > 0.dp)
        (windowHeight - verticalSafeInsets - edgeMargin * 2).coerceAtLeast(0.dp)
    else Dp.Unspecified

internal fun yearSelectorRowCount(years: IntRange): Int =
    if (years.isEmpty()) 0
    else ((years.last.toLong() - years.first + YEARS_PER_ROW) / YEARS_PER_ROW).toInt()

internal fun yearSelectorRowFirst(years: IntRange, rowIndex: Int): Int {
  require(rowIndex in 0 until yearSelectorRowCount(years))
  return (years.last.toLong() - rowIndex.toLong() * YEARS_PER_ROW).toInt()
}

internal fun yearSelectorRow(years: IntRange, rowIndex: Int): IntProgression {
  val first = yearSelectorRowFirst(years, rowIndex)
  val last = maxOf(years.first.toLong(), first.toLong() - YEARS_PER_ROW + 1).toInt()
  return first downTo last
}

internal fun yearSelectorRowIndex(years: IntRange, selectedYear: Int): Int =
    ((years.last.toLong() - selectedYear).coerceAtLeast(0) / YEARS_PER_ROW)
        .toInt()
        .coerceAtMost((yearSelectorRowCount(years) - 1).coerceAtLeast(0))

private const val DAYS_IN_WEEK = 7
private const val YEARS_AHEAD_WITHOUT_LIMIT = 1

private const val YEARS_PER_ROW = 4
