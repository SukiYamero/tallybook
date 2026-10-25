package com.kurobello.tallybook.feature.statistics.viewmodel

import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.statisticsPeriod
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import kotlinx.datetime.LocalDate

internal enum class StatisticsDimension {
  Categorias,
  Etiquetas,
  Cuentas,
  Medios,
}

internal sealed interface StatisticsFailure {
  data class Repository(val error: DataError) : StatisticsFailure

  data object CalculationOverflow : StatisticsFailure
}

/**
 * [years] are the selectable years as [StatisticsSelection.availableYears] lists them;
 * [trendSelection] is the selected Evolución month, null for the last one.
 */
internal data class StatisticsUiState(
    val selection: StatisticsSelection,
    val today: LocalDate,
    val years: List<Int>,
    val moneda: Moneda? = null,
    val monedas: List<Moneda> = emptyList(),
    val dimension: StatisticsDimension = StatisticsDimension.Categorias,
    val trendSelection: Int? = null,
    val content: RetainedLoadState<StatisticsContent, StatisticsFailure> =
        RetainedLoadState.Loading(),
    val detail: CategoryDetailState? = null,
) {
  val showsMonedaSelector: Boolean
    get() = moneda != null && monedas.size > 1

  val periods: List<MovimientoDateRange>
    get() = selection.periods(today, years)

  val selectedPeriod: MovimientoDateRange
    get() = statisticsPeriod(selection.scope, selection.anchor, today)
}
