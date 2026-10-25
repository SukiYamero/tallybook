package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.CategoryStatisticsRequest
import com.kurobello.tallybook.core.model.CategoryStatisticsSnapshot
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.StatisticsRequest
import com.kurobello.tallybook.core.model.StatisticsSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Statistics aggregates over live movimientos. Each emission reads every aggregate in one
 * transaction and any write to the tables behind them re-emits, so totals never mix two states.
 */
internal interface StatisticsRepository {

  fun observe(request: StatisticsRequest): Flow<DataOutcome<StatisticsSnapshot>>

  fun observeCategory(
      request: CategoryStatisticsRequest
  ): Flow<DataOutcome<CategoryStatisticsSnapshot>>
}
