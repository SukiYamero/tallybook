package com.kurobello.tallybook.feature.history.viewmodel

import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import kotlinx.datetime.LocalDate

internal data class HistoryUiState(
    val selection: HistorySelection,
    val today: LocalDate,
    val moneda: Moneda? = null,
    val period: RetainedLoadState<HistoryProjection.Content, HistoryFailure> =
        RetainedLoadState.Loading(),
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
    get() = period !is RetainedLoadState.Failed && period.cachedValue?.movementCount == 0
}

internal data class HistoryYearState(
    val persisted: List<Int> = emptyList(),
    val retained: Set<Int> = emptySet(),
    val isLoading: Boolean = true,
    val failure: DataError? = null,
)
