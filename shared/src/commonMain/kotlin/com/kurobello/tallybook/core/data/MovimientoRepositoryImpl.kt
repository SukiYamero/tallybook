package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Movimiento
import kotlinx.coroutines.CoroutineDispatcher

internal class MovimientoRepositoryImpl(
    private val database: TallybookDatabase,
    private val dispatcher: CoroutineDispatcher,
) : MovimientoRepository {

  override suspend fun insertar(movimiento: Movimiento): DataOutcome<Unit> =
      runLocalDataOperation(dispatcher) {
        check(
            database.movimientoQueries
                .insertMovimiento(
                    id = movimiento.id,
                    tipo = movimiento.tipo.name,
                    monto_minor = movimiento.monto.minorUnits,
                    monto_moneda = movimiento.monto.moneda.name,
                    fecha = movimiento.fecha.toString(),
                    categoria_id = movimiento.categoriaId,
                    descripcion = movimiento.descripcion,
                )
                .value == 1L
        )
      }

  override suspend fun obtenerTodos(): DataOutcome<List<Movimiento>> =
      runLocalDataOperation(dispatcher) {
        database.movimientoQueries.selectAllMovimientos().executeAsList().map { it.toDomain() }
      }

  override suspend fun eliminar(id: String): DataOutcome<Unit> =
      runLocalDataOperation(dispatcher) {
        check(database.movimientoQueries.deleteMovimiento(id).value in 0L..1L)
      }
}
