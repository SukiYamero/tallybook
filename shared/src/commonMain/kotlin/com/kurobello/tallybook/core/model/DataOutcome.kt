package com.kurobello.tallybook.core.model

sealed class DataOutcome<out T> {

  data class Success<T>(val data: T) : DataOutcome<T>()

  data class Failure(val error: DataError) : DataOutcome<Nothing>()
}

inline fun <T, R> DataOutcome<T>.fold(onSuccess: (T) -> R, onFailure: (DataError) -> R): R =
    when (this) {
      is DataOutcome.Success -> onSuccess(data)
      is DataOutcome.Failure -> onFailure(error)
    }

inline fun <T, R> DataOutcome<T>.map(transform: (T) -> R): DataOutcome<R> =
    when (this) {
      is DataOutcome.Success -> DataOutcome.Success(transform(data))
      is DataOutcome.Failure -> this
    }
