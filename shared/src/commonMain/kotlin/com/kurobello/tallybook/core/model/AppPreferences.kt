package com.kurobello.tallybook.core.model

data class AppPreferences(
    val monedaPrincipal: Moneda,
)

sealed interface AppPreferencesState {
  data object Uninitialized : AppPreferencesState

  data class Ready(
      val value: AppPreferences,
  ) : AppPreferencesState
}
