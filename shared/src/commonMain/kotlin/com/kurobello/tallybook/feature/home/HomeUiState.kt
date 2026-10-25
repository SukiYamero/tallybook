package com.kurobello.tallybook.feature.home

import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.ui.state.RetainedLoadState

internal data class HomeUiState(
    val moneda: Moneda,
    val content: RetainedLoadState<HomeContent, DataError> = RetainedLoadState.Loading(),
) {
  val isLoading: Boolean
    get() = content.isLoading

  val hasSnapshot: Boolean
    get() = content.hasSnapshot

  val summary: MovimientoSummary
    get() = content.cachedValue?.summary ?: emptySummary(moneda)

  val totalBalance: Money
    get() = content.cachedValue?.totalBalance ?: Money(0, moneda)

  val recent
    get() = content.cachedValue?.recent.orEmpty()

  val categoriasById
    get() = content.cachedValue?.categoriasById.orEmpty()

  val statistics: HomeStatistics?
    get() = content.cachedValue?.statistics

  val error: DataError?
    get() = content.failure

  val isEmpty: Boolean
    get() = error == null && content.cachedValue?.recent?.isEmpty() == true

  companion object {
    fun loading(moneda: Moneda = Moneda.COP) = HomeUiState(moneda)

    fun ready(snapshot: HomeContent) =
        HomeUiState(snapshot.summary.balance.moneda, RetainedLoadState.Ready(snapshot))

    fun error(error: DataError, moneda: Moneda = Moneda.COP) =
        HomeUiState(moneda, RetainedLoadState.Failed(error))

    fun emptySummary(moneda: Moneda) = MovimientoSummary(Money(0, moneda), Money(0, moneda))
  }
}
