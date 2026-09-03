package com.kurobello.tallybook.core.model

/**
 * Domain errors. The repository is the only layer that catches platform exceptions and translates
 * them here; ViewModel and UI consume `DataOutcome`, never a raw exception.
 */
sealed class DataError(message: String, cause: Throwable? = null) : Exception(message, cause) {

  class Local(cause: Throwable) : DataError("Error de almacenamiento local", cause)

  class Unknown(cause: Throwable) : DataError("Error inesperado", cause)
}
