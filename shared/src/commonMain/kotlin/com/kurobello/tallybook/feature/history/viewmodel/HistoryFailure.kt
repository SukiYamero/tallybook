package com.kurobello.tallybook.feature.history.viewmodel

import com.kurobello.tallybook.core.model.DataError

internal sealed interface HistoryFailure {
  data class Repository(val error: DataError) : HistoryFailure

  data object CalculationOverflow : HistoryFailure
}
