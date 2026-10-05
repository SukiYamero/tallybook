package com.kurobello.tallybook.feature.history.viewmodel

import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoSummary
import kotlinx.datetime.LocalDate

internal data class HistoryUiState(
    val selection: HistorySelection,
    val today: LocalDate,
    val moneda: Moneda? = null,
    val period: HistoryLoadState<HistoryProjection.Content> = HistoryLoadState.Loading(),
    val overall: HistoryLoadState<MovimientoSummary> = HistoryLoadState.Loading(),
    val years: HistoryYearState = HistoryYearState(),
    val preferenceFailure: DataError? = null,
) {
  val range
    get() = selection.range(today)

  val periods: List<MovimientoDateRange>
    get() = selection.periods(today)

  val selectedPeriod: MovimientoDateRange
    get() = selection.selectedPeriod(today)

  val availableYears
    get() = selection.availableYears(years.persisted, today, years.retained)

  val isEmpty: Boolean
    get() = period is HistoryLoadState.Ready && period.value.movementCount == 0
}

internal sealed interface HistoryLoadState<out T> {
  data class Loading<T>(val previous: T? = null) : HistoryLoadState<T>

  data class Ready<T>(val value: T) : HistoryLoadState<T>

  data class Failed<T>(val failure: HistoryFailure, val previous: T? = null) : HistoryLoadState<T>
}

internal val <T> HistoryLoadState<T>.cachedValue: T?
  get() =
      when (this) {
        is HistoryLoadState.Ready -> value
        is HistoryLoadState.Loading -> previous
        is HistoryLoadState.Failed -> previous
      }

internal data class HistoryYearState(
    val persisted: List<Int> = emptyList(),
    val retained: Set<Int> = emptySet(),
    val isLoading: Boolean = true,
    val failure: DataError? = null,
)
