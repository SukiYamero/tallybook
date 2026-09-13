package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

// SQLDelight exposes different storage exception types on each platform, so commonMain cannot catch
// a narrower portable type at this repository boundary.
@Suppress("TooGenericExceptionCaught")
internal suspend fun <T> runLocalDataOperation(
    dispatcher: CoroutineDispatcher,
    operation: () -> T,
): DataOutcome<T> =
    withContext(dispatcher) {
      try {
        DataOutcome.Success(operation())
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        DataOutcome.Failure(DataError.Local(error))
      }
    }
