package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
  val preferences: Flow<DataOutcome<AppPreferencesState>>

  suspend fun initialize(monedaPrincipal: Moneda): DataOutcome<Unit>

  suspend fun setMonedaPrincipal(moneda: Moneda): DataOutcome<Unit>
}
