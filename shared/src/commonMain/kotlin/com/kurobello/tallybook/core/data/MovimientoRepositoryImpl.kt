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
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

internal class MovimientoRepositoryImpl(
    private val database: TallybookDatabase,
    private val dispatcher: CoroutineDispatcher,
) : MovimientoRepository {

  override fun observeFiltered(
      criteria: MovimientoReadCriteria
  ): Flow<DataOutcome<MovimientoSearchSnapshot>> {
    val categoryIds = criteria.categoryIds.sorted()
    val invalidInput =
        when {
          categoryIds.any { it.isBlank() } ->
              DataError.InvalidInput("Categoria ids must not be blank")
          else -> criteria.range?.let(::validateRange)
        }
    if (invalidInput != null) return flowOf(DataOutcome.Failure(invalidInput))
    return flow {
          val queries = database.movimientoQueries
          val query =
              if (categoryIds.isEmpty()) {
                queries.selectFilteredMovimientos(
                    criteria.moneda.name,
                    criteria.range?.start?.toString(),
                    criteria.range?.end?.toString(),
                    criteria.tipo?.name,
                )
              } else {
                queries.selectFilteredMovimientosByCategory(
                    criteria.moneda.name,
                    criteria.range?.start?.toString(),
                    criteria.range?.end?.toString(),
                    criteria.tipo?.name,
                    categoryIds,
                )
              }
          emitAll(
              query.asFlow().map { invalidatedQuery ->
                withContext(dispatcher) {
                  val (rows, years) =
                      database.transactionWithResult {
                        invalidatedQuery.executeAsList() to
                            queries.selectMovimientoYears(criteria.moneda.name).executeAsList()
                      }
                  val context = currentCoroutineContext()
                  context.ensureActive()
                  val movimientos = rows.map { row ->
                    context.ensureActive()
                    row.toDomain()
                  }
                  val persistedYears = years.map { year ->
                    context.ensureActive()
                    parseStoredYear(year)
                  }
                  context.ensureActive()
                  MovimientoSearchSnapshot(movimientos, persistedYears)
                }
              }
          )
        }
        .flowOn(dispatcher)
        .asLocalDataOutcome()
  }

  override fun observeRange(
      moneda: Moneda,
      range: MovimientoDateRange,
  ): Flow<DataOutcome<List<Movimiento>>> {
    validateRange(range)?.let {
      return flowOf(DataOutcome.Failure(it))
    }
    return flow {
          emitAll(
              database.movimientoQueries
                  .selectMovimientosInRange(
                      moneda.name,
                      range.start.toString(),
                      range.end.toString(),
                  )
                  .asFlow()
                  .mapToList(dispatcher)
                  .map { rows ->
                    rows.map { row ->
                      currentCoroutineContext().ensureActive()
                      row.toDomain()
                    }
                  }
          )
        }
        .flowOn(dispatcher)
        .asLocalDataOutcome()
  }

  override fun observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>> =
      flow {
            emitAll(
                database.movimientoQueries
                    .selectMovimientoYears(moneda.name)
                    .asFlow()
                    .mapToList(dispatcher)
                    .map { rows ->
                      rows.map { row ->
                        currentCoroutineContext().ensureActive()
                        parseStoredYear(row)
                      }
                    }
            )
          }
          .flowOn(dispatcher)
          .asLocalDataOutcome()

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

  override fun observeRecent(moneda: Moneda, limit: Int): Flow<DataOutcome<List<Movimiento>>> =
      if (limit <= 0) {
        flowOf(
            DataOutcome.Failure(DataError.InvalidInput("Recent movimiento limit must be positive"))
        )
      } else {
        flow {
              emitAll(
                  database.movimientoQueries
                      .selectRecentMovimientosByMoneda(moneda.name, limit.toLong())
                      .asFlow()
                      .mapToList(dispatcher)
                      .map { rows -> rows.map { it.toDomain() } }
              )
            }
            .flowOn(dispatcher)
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

private fun parseStoredYear(value: String): Int {
  val canonicalDate = "$value-01-01"
  val date = LocalDate.parse(canonicalDate)
  require(date.toString() == canonicalDate) {
    "Persisted movimiento year must use canonical ISO representation"
  }
  return date.year
}

private fun validateRange(range: MovimientoDateRange): DataError.InvalidInput? =
    if (
        range.start.year != range.end.year &&
            (range.start.year < 0 || range.end.year > MAX_FOUR_DIGIT_YEAR)
    ) {
      DataError.InvalidInput("Cross-year movimiento ranges require years from 0000 through 9999")
    } else null

private const val MAX_FOUR_DIGIT_YEAR = 9999
