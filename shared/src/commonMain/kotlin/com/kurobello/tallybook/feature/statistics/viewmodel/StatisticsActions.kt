package com.kurobello.tallybook.feature.statistics.viewmodel

import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.StatisticsKey
import com.kurobello.tallybook.core.model.StatisticsScope

internal interface StatisticsActions {
  fun onScopeChanged(scope: StatisticsScope)

  fun onPeriodSelected(period: MovimientoDateRange)

  fun onYearSelected(year: Int)

  fun onMonedaSelected(moneda: Moneda)

  fun onDimensionSelected(dimension: StatisticsDimension)

  fun onTrendMonthSelected(index: Int)

  /** A categoria opens its sheet, a tag or an account opens Search, and a medio does nothing. */
  fun onShareSelected(key: StatisticsKey)

  fun onDetailDismissed()

  fun onDetailHidden()

  fun onViewMovimientos()

  fun onAddMovimiento()

  fun onRetry()

  fun onResume()
}
