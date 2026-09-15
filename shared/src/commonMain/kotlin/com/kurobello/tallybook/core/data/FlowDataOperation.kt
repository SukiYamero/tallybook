package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal fun <T> Flow<T>.asLocalDataOutcome(): Flow<DataOutcome<T>> =
    map<T, DataOutcome<T>> { DataOutcome.Success(it) }
        .catch { error ->
          if (error is CancellationException) {
            throw error
          }
          emit(DataOutcome.Failure(DataError.Local(error)))
        }
