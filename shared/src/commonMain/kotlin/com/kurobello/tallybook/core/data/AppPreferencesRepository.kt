package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
  val preferences: Flow<DataOutcome<AppPreferencesState>>

  suspend fun initialize(monedaPrincipal: Moneda): DataOutcome<Unit>

  suspend fun setMonedaPrincipal(moneda: Moneda): DataOutcome<Unit>

  suspend fun setThemeMode(themeMode: ThemeMode): DataOutcome<Unit>

  /** `null` follows the device language. */
  suspend fun setAppLocale(appLocale: AppLocale?): DataOutcome<Unit>
}
