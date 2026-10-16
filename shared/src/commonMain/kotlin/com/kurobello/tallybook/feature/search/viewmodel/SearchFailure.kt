package com.kurobello.tallybook.feature.search.viewmodel

import com.kurobello.tallybook.core.model.DataError

internal sealed interface SearchFailure {
  data class Repository(val source: SearchSource, val error: DataError) : SearchFailure

  data class InvalidDates(val failure: SearchDateFailure) : SearchFailure
}

internal enum class SearchSource {
  Preferences,
  Categories,
  Etiquetas,
  Movimientos,
}
