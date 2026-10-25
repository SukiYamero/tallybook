package com.kurobello.tallybook.feature.statistics.viewmodel

import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.StatisticsKey

internal sealed interface StatisticsEffect {
  /** Search filtered by [key], [moneda], [range] and gastos only. */
  data class OpenSearch(
      val key: StatisticsKey,
      val moneda: Moneda,
      val range: MovimientoDateRange,
  ) : StatisticsEffect

  data object OpenCreation : StatisticsEffect
}
