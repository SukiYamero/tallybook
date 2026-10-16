package com.kurobello.tallybook.core.model

data class AppPreferences(
    val monedaPrincipal: Moneda,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLocale: AppLocale? = null,
)

sealed interface AppPreferencesState {
  data object Uninitialized : AppPreferencesState

  data class Ready(
      val value: AppPreferences,
  ) : AppPreferencesState
}
