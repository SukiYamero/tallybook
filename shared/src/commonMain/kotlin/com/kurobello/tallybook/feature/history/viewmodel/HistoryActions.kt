package com.kurobello.tallybook.feature.history.viewmodel

import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange

internal interface HistoryActions {
  fun onScopeChanged(scope: HistoryScope)

  fun onYearSelected(year: Int)

  fun onPeriodSelected(period: MovimientoDateRange)

  /** Shows [moneda]'s activity; the scope and period stay selected. */
  fun onMonedaSelected(moneda: Moneda)

  fun onRetry()

  fun onResume()

  fun onPresentationChanged(unknownCategoryLabel: String)
}
