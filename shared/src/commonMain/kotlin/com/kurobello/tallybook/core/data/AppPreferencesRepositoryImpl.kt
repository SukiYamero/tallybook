package com.kurobello.tallybook.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
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
      try {
        dataStore.edit { preferences ->
          if (preferences[MONEDA_PRINCIPAL_KEY] == null) {
            preferences[MONEDA_PRINCIPAL_KEY] = monedaPrincipal.name
          }
        }
        DataOutcome.Success(Unit)
      } catch (error: IOException) {
        DataOutcome.Failure(DataError.Local(error))
      }

  override suspend fun setMonedaPrincipal(moneda: Moneda): DataOutcome<Unit> =
      try {
        dataStore.edit { preferences -> preferences[MONEDA_PRINCIPAL_KEY] = moneda.name }
        DataOutcome.Success(Unit)
      } catch (error: IOException) {
        DataOutcome.Failure(DataError.Local(error))
      }

  private fun Preferences.toAppPreferencesState(): AppPreferencesState {
    val monedaName = this[MONEDA_PRINCIPAL_KEY] ?: return AppPreferencesState.Uninitialized
    val moneda =
        Moneda.entries.firstOrNull { candidate -> candidate.name == monedaName }
            ?: throw IOException("Unknown persisted moneda: $monedaName")
    return AppPreferencesState.Ready(AppPreferences(monedaPrincipal = moneda))
  }

  private companion object {
    val MONEDA_PRINCIPAL_KEY = stringPreferencesKey("moneda_principal")
  }
}
