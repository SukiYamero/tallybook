package com.kurobello.tallybook.feature.history.viewmodel

import com.kurobello.tallybook.core.model.MovimientoDateRange

internal interface HistoryActions {
  fun onScopeChanged(scope: HistoryScope)

  fun onYearSelected(year: Int)

  fun onPeriodSelected(period: MovimientoDateRange)

  fun onRetry()

  fun onResume()

  fun onPresentationChanged(unknownCategoryLabel: String)
}
