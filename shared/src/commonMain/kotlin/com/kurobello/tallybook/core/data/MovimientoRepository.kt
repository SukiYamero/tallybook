package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Movimiento

interface MovimientoRepository {

  suspend fun insertar(movimiento: Movimiento): DataOutcome<Unit>

  suspend fun obtenerTodos(): DataOutcome<List<Movimiento>>

  suspend fun eliminar(id: String): DataOutcome<Unit>
}
