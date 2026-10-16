package com.kurobello.tallybook.core.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MAX_CATEGORIAS_POR_MOVIMIENTO
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.MOVIMIENTO_TITLE_MAX_LENGTH
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoRestoreResult
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.MovimientoUpdateResult
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.fold
import kotlin.time.Instant
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

// Reads and identity-safe mutations share one storage/error boundary and legacy validation policy.
@Suppress("TooManyFunctions")
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

  override fun observeCategoryHistory(limit: Int): Flow<DataOutcome<List<CategoryHistoryRow>>> =
      if (limit <= 0) {
        flowOf(
            DataOutcome.Failure(DataError.InvalidInput("Category history limit must be positive"))
        )
      } else {
        flow {
              emitAll(
                  database.movimientoQueries
                      .selectCategoryHistory(limit.toLong())
                      .asFlow()
                      .mapToList(dispatcher)
                      .map { rows ->
                        rows.map { row ->
                          currentCoroutineContext().ensureActive()
                          CategoryHistoryRow(
                              movimientoId = row.movimiento_id,
                              titulo = row.titulo,
                              categoriaId = row.categoria_id,
                              createdAt = Instant.parse(row.created_at),
                          )
                        }
                      }
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
    return runLocalDataOperation(dispatcher) { database.transaction { insertRow(movimiento) } }
  }

  override suspend fun getAll(): DataOutcome<List<Movimiento>> =
      runLocalDataOperation(dispatcher) {
        database.movimientoQueries.selectAllMovimientos().executeAsList().map { it.toDomain() }
      }

  override suspend fun getById(id: String): DataOutcome<Movimiento?> {
    validateId(id)?.let {
      return DataOutcome.Failure(it)
    }
    return runLocalDataOperation(dispatcher) {
      database.movimientoQueries.selectMovimientoById(id).executeAsOneOrNull()?.toDomain()
    }
  }

  override suspend fun update(
      id: String,
      changes: MovimientoChanges,
  ): DataOutcome<MovimientoUpdateResult> {
    (validateId(id) ?: validateChanges(changes))?.let {
      return DataOutcome.Failure(it)
    }
    return runLocalDataOperation(dispatcher) {
          database.transactionWithResult {
            val queries = database.movimientoQueries
            val current =
                queries.selectMovimientoById(id).executeAsOneOrNull()?.toDomain()
                    ?: return@transactionWithResult DataOutcome.Success(
                        MovimientoUpdateResult.NotFound
                    )
            if (current.monto.moneda != changes.monto.moneda) {
              return@transactionWithResult DataOutcome.Failure(
                  DataError.InvalidInput("Movimiento currency cannot change")
              )
            }
            check(
                queries
                    .updateMovimiento(
                        tipo = changes.tipo.name,
                        monto_minor = changes.monto.minorUnits,
                        fecha = changes.fecha.toString(),
                        titulo = changes.titulo,
                        descripcion = changes.descripcion,
                        id = id,
                    )
                    .value == 1L
            )
            queries.deleteMovimientoCategorias(id)
            insertCategorias(id, changes.categoriaIds)
            DataOutcome.Success(MovimientoUpdateResult.Updated)
          }
        }
        .fold(onSuccess = { it }, onFailure = { DataOutcome.Failure(it) })
  }

  override suspend fun deleteAndCapture(id: String): DataOutcome<Movimiento?> {
    validateId(id)?.let {
      return DataOutcome.Failure(it)
    }
    return runLocalDataOperation(dispatcher) {
          database.transactionWithResult {
            val queries = database.movimientoQueries
            val captured = queries.selectMovimientoById(id).executeAsOneOrNull()?.toDomain()
            val invalid = captured?.let(::validateRestorable)
            when {
              invalid != null -> DataOutcome.Failure(invalid)
              captured == null -> DataOutcome.Success(null)
              else -> {
                check(queries.deleteMovimiento(id).value == 1L)
                queries.deleteMovimientoCategorias(id)
                DataOutcome.Success(captured)
              }
            }
          }
        }
        .fold(onSuccess = { it }, onFailure = { DataOutcome.Failure(it) })
  }

  override suspend fun restoreDeleted(
      movimiento: Movimiento
  ): DataOutcome<MovimientoRestoreResult> {
    validateRestorable(movimiento)?.let {
      return DataOutcome.Failure(it)
    }
    return runLocalDataOperation(dispatcher) {
      database.transactionWithResult {
        val existing =
            database.movimientoQueries.selectMovimientoById(movimiento.id).executeAsOneOrNull()
        if (existing == null) {
          database.movimientoQueries.deleteMovimientoCategorias(movimiento.id)
          insertRow(movimiento)
          MovimientoRestoreResult.Restored
        } else {
          // An unreadable occupied ID must block restoration rather than become a retryable
          // failure.
          val identical =
              try {
                existing.toDomain() == movimiento
              } catch (_: IllegalArgumentException) {
                false
              }
          if (identical) MovimientoRestoreResult.AlreadyPresent
          else MovimientoRestoreResult.Conflict
        }
      }
    }
  }

  private fun insertRow(movimiento: Movimiento) {
    check(
        database.movimientoQueries
            .insertMovimiento(
                id = movimiento.id,
                tipo = movimiento.tipo.name,
                monto_minor = movimiento.monto.minorUnits,
                monto_moneda = movimiento.monto.moneda.name,
                fecha = movimiento.fecha.toString(),
                titulo = movimiento.titulo,
                descripcion = movimiento.descripcion,
                created_at = movimiento.createdAt.toString(),
            )
            .value == 1L
    )
    insertCategorias(movimiento.id, movimiento.categoriaIds)
  }

  private fun insertCategorias(movimientoId: String, categoriaIds: List<String>) {
    categoriaIds.forEachIndexed { posicion, categoriaId ->
      check(
          database.movimientoQueries
              .insertMovimientoCategoria(movimientoId, categoriaId, posicion.toLong())
              .value == 1L
      )
    }
  }

  private fun validateRestorable(movimiento: Movimiento): DataError.InvalidInput? =
      validateId(movimiento.id)
          ?: validateChanges(
              MovimientoChanges(
                  movimiento.tipo,
                  movimiento.monto,
                  movimiento.fecha,
                  movimiento.categoriaIds,
                  movimiento.titulo,
                  movimiento.descripcion,
              )
          )

  private fun validateId(id: String): DataError.InvalidInput? =
      if (id.isBlank()) DataError.InvalidInput("Movimiento id must not be blank") else null

  private fun validateChanges(changes: MovimientoChanges): DataError.InvalidInput? =
      when {
        changes.monto.minorUnits <= 0 ->
            DataError.InvalidInput("Movimiento amount must be positive")
        else ->
            validateCategoriaIds(changes.categoriaIds)
                ?: validateTitle(changes.titulo)
                ?: validateDescription(changes.descripcion)
      }

  override suspend fun delete(id: String): DataOutcome<Unit> =
      runLocalDataOperation(dispatcher) {
        database.transaction {
          check(database.movimientoQueries.deleteMovimiento(id).value in 0L..1L)
          database.movimientoQueries.deleteMovimientoCategorias(id)
        }
      }

  private fun validate(movimiento: Movimiento): DataError.InvalidInput? =
      when {
        movimiento.monto.minorUnits <= 0 ->
            DataError.InvalidInput("Movimiento amount must be positive")
        movimiento.id.isEmpty() -> DataError.InvalidInput("Movimiento id must not be empty")
        else ->
            validateCategoriaIds(movimiento.categoriaIds)
                ?: validateTitle(movimiento.titulo)
                ?: validateDescription(movimiento.descripcion)
      }
}

private fun validateTitle(titulo: String?): DataError.InvalidInput? =
    if ((titulo?.length ?: 0) > MOVIMIENTO_TITLE_MAX_LENGTH) {
      DataError.InvalidInput("Movimiento title is too long")
    } else null

private fun validateDescription(descripcion: String?): DataError.InvalidInput? =
    if ((descripcion?.length ?: 0) > MOVIMIENTO_DESCRIPTION_MAX_LENGTH) {
      DataError.InvalidInput("Movimiento description is too long")
    } else null

private fun validateCategoriaIds(categoriaIds: List<String>): DataError.InvalidInput? =
    when {
      categoriaIds.size !in 1..MAX_CATEGORIAS_POR_MOVIMIENTO ->
          DataError.InvalidInput(
              "Movimiento needs 1 to $MAX_CATEGORIAS_POR_MOVIMIENTO categorias, got ${categoriaIds.size}"
          )
      categoriaIds.any { it.isBlank() } -> DataError.InvalidInput("Categoria id must not be blank")
      categoriaIds.any { id -> id.any { it in VIEW_SEPARATORS } } ->
          DataError.InvalidInput("Categoria id must not contain a control separator")
      categoriaIds.toSet().size != categoriaIds.size ->
          DataError.InvalidInput("Movimiento categorias must be distinct")
      else -> null
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

// movimiento_con_categorias joins ids with these characters, so an id holding one would corrupt
// the parsed set.
private val VIEW_SEPARATORS = charArrayOf('\u001E', '\u001F')
