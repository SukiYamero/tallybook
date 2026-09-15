package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoSummary
import kotlinx.coroutines.flow.Flow

interface MovimientoRepository {

  fun observeRecent(limit: Int): Flow<DataOutcome<List<Movimiento>>>

  fun observeSummary(moneda: Moneda): Flow<DataOutcome<MovimientoSummary>>

  suspend fun insert(movimiento: Movimiento): DataOutcome<Unit>

  suspend fun getAll(): DataOutcome<List<Movimiento>>

  suspend fun delete(id: String): DataOutcome<Unit>
}
