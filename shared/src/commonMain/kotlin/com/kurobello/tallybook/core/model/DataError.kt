package com.kurobello.tallybook.core.model

/**
 * Errores del dominio. El repositorio es el único que hace `catch` de excepciones de plataforma y
 * las traduce acá; ViewModel y UI consumen `DataOutcome`, nunca una excepción cruda.
 */
sealed class DataError(message: String, cause: Throwable? = null) : Exception(message, cause) {

  class Local(cause: Throwable) : DataError("Error de almacenamiento local", cause)

  class Unknown(cause: Throwable) : DataError("Error inesperado", cause)
}
