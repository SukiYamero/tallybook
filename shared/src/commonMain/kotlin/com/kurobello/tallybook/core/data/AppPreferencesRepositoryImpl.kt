package com.kurobello.tallybook.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.IOException

class AppPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : AppPreferencesRepository {

  override val preferences: Flow<DataOutcome<AppPreferencesState>> =
      dataStore.data
          .map<Preferences, DataOutcome<AppPreferencesState>> { storedPreferences ->
            DataOutcome.Success(storedPreferences.toAppPreferencesState())
          }
          .catch { error ->
            if (error is IOException) {
              emit(DataOutcome.Failure(DataError.Local(error)))
            } else {
              throw error
            }
          }

  override suspend fun initialize(monedaPrincipal: Moneda): DataOutcome<Unit> =
      edit { preferences ->
        if (preferences[MONEDA_PRINCIPAL_KEY] == null) {
          preferences[MONEDA_PRINCIPAL_KEY] = monedaPrincipal.name
        }
      }

  override suspend fun setMonedaPrincipal(moneda: Moneda): DataOutcome<Unit> = edit {
    it[MONEDA_PRINCIPAL_KEY] = moneda.name
  }

  override suspend fun setThemeMode(themeMode: ThemeMode): DataOutcome<Unit> = edit {
    it[THEME_MODE_KEY] = themeMode.name
  }

  override suspend fun setAppLocale(appLocale: AppLocale?): DataOutcome<Unit> = edit {
    if (appLocale == null) it.remove(APP_LOCALE_KEY) else it[APP_LOCALE_KEY] = appLocale.languageTag
  }

  private suspend fun edit(transform: (MutablePreferences) -> Unit): DataOutcome<Unit> =
      try {
        dataStore.edit(transform)
        DataOutcome.Success(Unit)
      } catch (error: IOException) {
        DataOutcome.Failure(DataError.Local(error))
      }

  private fun Preferences.toAppPreferencesState(): AppPreferencesState {
    val monedaName = this[MONEDA_PRINCIPAL_KEY] ?: return AppPreferencesState.Uninitialized
    return AppPreferencesState.Ready(
        AppPreferences(
            monedaPrincipal = decode(monedaName, Moneda.entries) { it.name },
            // Display values fall back so an unknown one never hides the money preference.
            themeMode =
                ThemeMode.entries.firstOrNull { it.name == this[THEME_MODE_KEY] }
                    ?: ThemeMode.SYSTEM,
            appLocale = AppLocale.entries.firstOrNull { it.languageTag == this[APP_LOCALE_KEY] },
        )
    )
  }

  private companion object {
    val MONEDA_PRINCIPAL_KEY = stringPreferencesKey("moneda_principal")
    val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    val APP_LOCALE_KEY = stringPreferencesKey("app_locale")
  }
}

private fun <T> decode(stored: String, entries: List<T>, key: (T) -> String): T =
    entries.firstOrNull { key(it) == stored }
        ?: throw IOException("Unknown persisted preference value: $stored")
