package com.kurobello.tallybook.feature.history.viewmodel

import com.kurobello.tallybook.core.model.FIRST_SELECTABLE_YEAR
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoPeriodRanges
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.serialization.Serializable

internal enum class HistoryScope {
  Day,
  Week,
  Month,
}

internal data class HistorySelection(val scope: HistoryScope, val anchor: LocalDate) {
  val contextMonth: YearMonth
    get() = YearMonth(anchor.year, anchor.month)

  fun range(today: LocalDate): MovimientoDateRange {
    val period = selectedPeriod(today)
    return period.copy(end = minOf(period.end, today))
  }

  fun selectedPeriod(today: LocalDate): MovimientoDateRange =
      periods(today).first { anchor >= it.start && anchor <= it.end }

  fun periods(today: LocalDate): List<MovimientoDateRange> {
    require(anchor <= today) { "History anchor must not be in the future" }
    return when (scope) {
      HistoryScope.Day -> MovimientoPeriodRanges.days(contextMonth, today)
      HistoryScope.Week -> MovimientoPeriodRanges.weeks(contextMonth, today)
      HistoryScope.Month -> MovimientoPeriodRanges.months(anchor.year, today)
    }
  }

  fun selectScope(scope: HistoryScope): HistorySelection = copy(scope = scope)

  fun selectYear(year: Int, today: LocalDate): HistorySelection =
      selectMonth(YearMonth(year, anchor.month), today)

  fun selectMonth(month: YearMonth, today: LocalDate): HistorySelection {
    require(month.year <= today.year) { "History year must not be in the future" }
    val boundedMonth = minOf(month, YearMonth(today.year, today.month))
    val date =
        LocalDate(
            boundedMonth.year,
            boundedMonth.month,
            minOf(anchor.day, boundedMonth.numberOfDays),
        )
    return copy(anchor = minOf(date, today))
  }

  fun selectPeriod(period: MovimientoDateRange, today: LocalDate): HistorySelection {
    require(period in periods(today)) { "History period must be an available option for its scope" }
    if (anchor >= period.start && anchor <= period.end) return this
    return when (scope) {
      HistoryScope.Day,
      HistoryScope.Week -> copy(anchor = period.start)
      HistoryScope.Month -> selectMonth(YearMonth(period.start.year, period.start.month), today)
    }
  }

  fun refresh(previousToday: LocalDate, today: LocalDate): HistorySelection =
      copy(anchor = if (anchor == previousToday) today else minOf(anchor, today))

  // Retention belongs to the navigation entry so a deleted year's chip survives later selections.
  fun availableYears(
      persistedYears: List<Int>,
      today: LocalDate,
      retainedYears: Set<Int> = emptySet(),
  ): List<Int> =
      ((FIRST_SELECTABLE_YEAR..today.year) +
              persistedYears +
              retainedYears +
              today.year +
              anchor.year)
          .filter { it <= today.year }
          .distinct()
          .sortedDescending()

  fun savedInput(): HistorySavedInput =
      HistorySavedInput(HISTORY_SAVED_VERSION, scope.name, anchor.toString())

  companion object {
    fun initial(today: LocalDate): HistorySelection = HistorySelection(HistoryScope.Day, today)

    fun restore(input: HistorySavedInput?, today: LocalDate): HistorySelection {
      if (input == null || input.version != HISTORY_SAVED_VERSION) return initial(today)
      val scope = HistoryScope.entries.firstOrNull { it.name == input.scope }
      val anchor =
          try {
            LocalDate.parse(input.anchor)
          } catch (_: IllegalArgumentException) {
            null
          }
      return if (scope != null && anchor != null && anchor <= today) HistorySelection(scope, anchor)
      else initial(today)
    }
  }
}

/** [moneda] is the currency the user picked; an absent or unknown one restores the primary. */
@Serializable
internal data class HistorySavedInput(
    val version: Int,
    val scope: String,
    val anchor: String,
    val moneda: String? = null,
) {
  fun restoredMoneda(): Moneda? =
      if (version == HISTORY_SAVED_VERSION) Moneda.entries.firstOrNull { it.name == moneda }
      else null
}

private const val HISTORY_SAVED_VERSION = 1
