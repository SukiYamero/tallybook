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

/** The primary moneda; preferences still uninitialized after app startup count as a failure. */
internal fun DataOutcome<AppPreferencesState>.primaryMoneda(): DataOutcome<Moneda> =
    when (this) {
      is DataOutcome.Failure -> this
      is DataOutcome.Success ->
          when (val state = data) {
            is AppPreferencesState.Ready -> DataOutcome.Success(state.value.monedaPrincipal)
            AppPreferencesState.Uninitialized ->
                DataOutcome.Failure(
                    DataError.InvalidInput("Preferences are uninitialized after app startup")
                )
          }
    }
