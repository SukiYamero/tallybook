package com.kurobello.tallybook.core.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal class MovimientoRepositoryImpl(
    private val database: TallybookDatabase,
    private val dispatcher: CoroutineDispatcher,
) : MovimientoRepository {

  override fun observeRecent(limit: Int): Flow<DataOutcome<List<Movimiento>>> =
      if (limit <= 0) {
        flowOf(
            DataOutcome.Failure(DataError.InvalidInput("Recent movimiento limit must be positive"))
        )
      } else {
        database.movimientoQueries
            .selectRecentMovimientos(limit.toLong())
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }
            .asLocalDataOutcome()
      }

  override fun observeSummary(moneda: Moneda): Flow<DataOutcome<MovimientoSummary>> =
      database.movimientoQueries
          .sumMovimientosByTipo(moneda.name)
          .asFlow()
          .mapToList(dispatcher)
          .map { rows ->
            MovimientoSummary(
                ingresos =
                    Money(
                        rows.firstOrNull { it.tipo == TipoMovimiento.INGRESO.name }?.total_minor
                            ?: 0,
                        moneda,
                    ),
                gastos =
                    Money(
                        rows.firstOrNull { it.tipo == TipoMovimiento.GASTO.name }?.total_minor ?: 0,
                        moneda,
                    ),
            )
          }
          .asLocalDataOutcome()

  override suspend fun insert(movimiento: Movimiento): DataOutcome<Unit> {
    validate(movimiento)?.let {
      return DataOutcome.Failure(it)
    }
    return runLocalDataOperation(dispatcher) {
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
                  created_at = movimiento.createdAt.toString(),
              )
              .value == 1L
      )
    }
  }

  override suspend fun getAll(): DataOutcome<List<Movimiento>> =
      runLocalDataOperation(dispatcher) {
        database.movimientoQueries.selectAllMovimientos().executeAsList().map { it.toDomain() }
      }

  override suspend fun delete(id: String): DataOutcome<Unit> =
      runLocalDataOperation(dispatcher) {
        check(database.movimientoQueries.deleteMovimiento(id).value in 0L..1L)
      }

  private fun validate(movimiento: Movimiento): DataError.InvalidInput? =
      when {
        movimiento.monto.minorUnits <= 0 ->
            DataError.InvalidInput("Movimiento amount must be positive")
        movimiento.id.isEmpty() -> DataError.InvalidInput("Movimiento id must not be empty")
        movimiento.categoriaId.isEmpty() -> DataError.InvalidInput("Categoria id must not be empty")
        movimiento.descripcion?.length ?: 0 > MOVIMIENTO_DESCRIPTION_MAX_LENGTH ->
            DataError.InvalidInput("Movimiento description is too long")
        else -> null
      }
}
