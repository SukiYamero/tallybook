package com.kurobello.tallybook.core.ui.component

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import com.kurobello.tallybook.core.model.FIRST_SELECTABLE_YEAR
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
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
): IntRange {
  val maximum = maxDate?.year ?: selectedDate.year + YEARS_AHEAD_WITHOUT_LIMIT
  return minOf(minYear, selectedDate.year, maximum)..maximum
}

internal fun initialCalendarMonth(value: LocalDate, maxDate: LocalDate?): YearMonth {
  val visible = if (maxDate != null && value > maxDate) maxDate else value
  return YearMonth(visible.year, visible.month)
}

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

internal class TallybookAnchoredPopupPositionProvider(
    private val verticalOffset: Int,
    private val edgeMargin: Int,
) : PopupPositionProvider {
  override fun calculatePosition(
      anchorBounds: IntRect,
      windowSize: IntSize,
      layoutDirection: LayoutDirection,
      popupContentSize: IntSize,
  ): IntOffset {
    val centeredX = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
    val maxX = maxOf(edgeMargin, windowSize.width - popupContentSize.width - edgeMargin)
    val availableBelow = windowSize.height - anchorBounds.bottom - verticalOffset
    val availableAbove = anchorBounds.top - verticalOffset
    val maxY = maxOf(edgeMargin, windowSize.height - popupContentSize.height - edgeMargin)
    val y =
        when {
          availableBelow >= popupContentSize.height -> anchorBounds.bottom + verticalOffset
          availableAbove >= popupContentSize.height ->
              anchorBounds.top - verticalOffset - popupContentSize.height
          availableAbove > availableBelow ->
              (anchorBounds.top - verticalOffset - popupContentSize.height).coerceIn(
                  edgeMargin,
                  maxY,
              )
          else -> (anchorBounds.bottom + verticalOffset).coerceIn(edgeMargin, maxY)
        }
    return IntOffset(centeredX.coerceIn(edgeMargin, maxX), y)
  }
}

private const val DAYS_IN_WEEK = 7
private const val YEARS_AHEAD_WITHOUT_LIMIT = 1

private const val YEARS_PER_ROW = 4
