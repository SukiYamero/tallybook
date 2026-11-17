package com.kurobello.tallybook.core.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.kurobello.tallybook.core.database.Movimiento_con_etiquetas as DatabaseMovimiento
import com.kurobello.tallybook.core.database.SelectSelectionCandidates
import com.kurobello.tallybook.core.database.SelectStoredMovimientoById as StoredMovimiento
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaSelection
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.MovimientoUpdateResult
import com.kurobello.tallybook.core.model.RestoreResult
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.TipoMovimiento
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
import kotlinx.datetime.LocalDate

// Reads and identity-safe mutations share one storage/error boundary and legacy validation policy.
@Suppress("TooManyFunctions")
internal class MovimientoRepositoryImpl(
    private val database: TallybookDatabase,
    private val dispatcher: CoroutineDispatcher,
    private val changeClock: ChangeClock,
) : MovimientoRepository {

  private val writes = MovimientoWrites(database)

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

  override fun observeSummary(
      moneda: Moneda,
      range: MovimientoDateRange,
  ): Flow<DataOutcome<MovimientoSummary>> {
    validateRange(range)?.let {
      return flowOf(DataOutcome.Failure(it))
    }
    return database.movimientoQueries
        .sumMovimientosByTipoInRange(moneda.name, range.start.toString(), range.end.toString())
        .asFlow()
        .mapToList(dispatcher)
        .map { rows -> summaryOf(moneda, rows.associate { it.tipo to it.total_minor }) }
        .asLocalDataOutcome()
  }

  override suspend fun insert(movimiento: Movimiento): DataOutcome<Unit> =
      insertAll(listOf(movimiento))

  override suspend fun insertAll(movimientos: List<Movimiento>): DataOutcome<Unit> {
    movimientos.firstNotNullOfOrNull(::validateMovimiento)?.let {
      return DataOutcome.Failure(it)
    }
    return runLocalDataOperation(dispatcher) {
          database.transactionWithResult {
            val missing = movimientos.firstNotNullOfOrNull { writes.rejection(it.changes()) }
            if (missing != null) {
              DataOutcome.Failure(missing)
            } else {
              movimientos.forEach { writes.insertRow(it, changeClock.next()) }
              DataOutcome.Success(Unit)
            }
          }
        }
        .flatten()
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
    (validateId(id) ?: validateMovimientoChanges(changes))?.let {
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
            writes
                .rejection(
                    changes,
                    keptCategoriaId = current.categoriaId,
                    keptCuentaId = current.cuentaId,
                )
                ?.let {
                  return@transactionWithResult DataOutcome.Failure(it)
                }
            if (current.changes() == changes) {
              return@transactionWithResult DataOutcome.Success(MovimientoUpdateResult.Updated)
            }
            check(
                queries
                    .updateMovimiento(
                        tipo = changes.tipo.name,
                        monto_minor = changes.monto.minorUnits,
                        fecha = changes.fecha.toString(),
                        titulo = changes.titulo,
                        descripcion = changes.descripcion,
                        categoria_id = changes.categoriaId,
                        cuenta_id = changes.cuentaId,
                        medio_pago = changes.medio.name,
                        updated_at = changeClock.next().value,
                        id = id,
                    )
                    .value == 1L
            )
            queries.deleteMovimientoEtiquetas(id)
            writes.insertEtiquetas(id, changes.etiquetaIds)
            DataOutcome.Success(MovimientoUpdateResult.Updated)
          }
        }
        .flatten()
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
                writes.tombstone(id, changeClock.next())
                DataOutcome.Success(captured)
              }
            }
          }
        }
        .flatten()
  }

  override suspend fun restoreDeleted(movimiento: Movimiento): DataOutcome<RestoreResult> {
    validateRestorable(movimiento)?.let {
      return DataOutcome.Failure(it)
    }
    return runLocalDataOperation(dispatcher) {
      database.transactionWithResult {
        val queries = database.movimientoQueries
        val restorable = withSurvivingReferences(movimiento)
        val live = queries.selectMovimientoById(movimiento.id).executeAsOneOrNull()
        if (live != null) {
          return@transactionWithResult if (live.isIdenticalTo(restorable)) {
            RestoreResult.AlreadyPresent
          } else RestoreResult.Conflict
        }
        // An archived cuenta still takes its movimiento back: Undo returns money where it was.
        val cuenta =
            database.cuentaQueries
                .selectCuentaById(movimiento.cuentaId)
                .executeAsOneOrNull()
                ?.toDomain()
        val tombstone = queries.selectStoredMovimientoById(movimiento.id).executeAsOneOrNull()
        when {
          cuenta == null -> RestoreResult.MissingCuenta
          !cuenta.accepts(movimiento.medio, movimiento.monto.moneda, movimiento.cuentaId) ->
              RestoreResult.Conflict
          tombstone == null -> {
            queries.deleteMovimientoEtiquetas(movimiento.id)
            writes.insertRow(restorable, changeClock.next())
            RestoreResult.Restored
          }
          tombstone.holdsContentOf(movimiento) -> {
            revive(restorable, changeClock.next())
            RestoreResult.Restored
          }
          else -> RestoreResult.Conflict
        }
      }
    }
  }

  private fun revive(movimiento: Movimiento, stamp: ChangeStamp) {
    val queries = database.movimientoQueries
    check(
        queries
            .reviveMovimiento(
                categoria_id = movimiento.categoriaId,
                stamp = stamp.value,
                id = movimiento.id,
            )
            .value == 1L
    )
    queries.deleteMovimientoEtiquetas(movimiento.id)
    writes.insertEtiquetas(movimiento.id, movimiento.etiquetaIds)
  }

  // Deleting a custom categoria is refused while a stored movimiento uses it, but a captured one no
  // longer counts; restoring it falls back to Otros, the same pending state creation uses.
  private fun withSurvivingReferences(movimiento: Movimiento): Movimiento {
    val existing = writes.existingEtiquetaIds(movimiento.etiquetaIds)
    return movimiento.copy(
        categoriaId = movimiento.categoriaId.takeIf(writes::categoriaExists) ?: CATEGORIA_OTROS_ID,
        etiquetaIds = movimiento.etiquetaIds.filter { it in existing },
    )
  }

  private fun validateRestorable(movimiento: Movimiento): DataError.InvalidInput? =
      validateId(movimiento.id) ?: validateMovimientoChanges(movimiento.changes())

  private fun validateId(id: String): DataError.InvalidInput? =
      if (id.isBlank()) DataError.InvalidInput("Movimiento id must not be blank") else null

  override suspend fun defaultSelection(
      primary: Moneda,
      tipo: TipoMovimiento,
      medio: MedioPago?,
  ): DataOutcome<CuentaSelection?> {
    if (medio != null && !medio.allows(tipo)) {
      return DataOutcome.Failure(DataError.InvalidInput("Medio $medio does not allow $tipo"))
    }
    return runLocalDataOperation(dispatcher) {
      database.movimientoQueries
          .selectSelectionCandidates(
              cash_id = Cuenta.defaultCashId(primary),
              primary = primary.name,
              medio = medio?.name,
          )
          .executeAsList()
          .firstNotNullOfOrNull { it.toSelection(tipo, medio) }
    }
  }
}

// The SQL ranks every cuenta regardless of medio, so a requested medio rules out the incompatible
// ones here, e.g. DEBITO skips a cash account.
private fun SelectSelectionCandidates.toSelection(
    tipo: TipoMovimiento,
    requested: MedioPago?,
): CuentaSelection? {
  val tipoCuenta = TipoCuenta.valueOf(this.tipo)
  val medio = requested ?: tipoCuenta.defaultMedio(tipo)
  return if (tipoCuenta in medio.compatibleTipos) {
    CuentaSelection(cuentaId = id, medio = medio, moneda = Moneda.valueOf(moneda))
  } else null
}

// An unreadable occupied ID must block restoration rather than become a retryable failure.
private fun DatabaseMovimiento.isIdenticalTo(movimiento: Movimiento): Boolean =
    try {
      toDomain() == movimiento
    } catch (_: IllegalArgumentException) {
      false
    }

// Links are left out because a tombstone owns none; text is parsed so a legacy spelling of the same
// date or instant still matches the snapshot read from it.
private fun StoredMovimiento.holdsContentOf(snapshot: Movimiento): Boolean =
    try {
      tipo == snapshot.tipo.name &&
          monto_minor == snapshot.monto.minorUnits &&
          monto_moneda == snapshot.monto.moneda.name &&
          categoria_id == snapshot.categoriaId &&
          cuenta_id == snapshot.cuentaId &&
          medio_pago == snapshot.medio.name &&
          titulo == snapshot.titulo &&
          descripcion == snapshot.descripcion &&
          LocalDate.parse(fecha) == snapshot.fecha &&
          Instant.parse(created_at) == snapshot.createdAt
    } catch (_: IllegalArgumentException) {
      false
    }

private fun summaryOf(moneda: Moneda, totals: Map<String, Long?>): MovimientoSummary =
    MovimientoSummary(
        ingresos = Money(totals[TipoMovimiento.INGRESO.name] ?: 0, moneda),
        gastos = Money(totals[TipoMovimiento.GASTO.name] ?: 0, moneda),
    )

internal fun parseStoredYear(value: String): Int {
  val canonicalDate = "$value-01-01"
  val date = LocalDate.parse(canonicalDate)
  require(date.toString() == canonicalDate) {
    "Persisted movimiento year must use canonical ISO representation"
  }
  return date.year
}

internal fun validateRange(range: MovimientoDateRange): DataError.InvalidInput? =
    if (
        range.start.year != range.end.year &&
            (range.start.year < 0 || range.end.year > MAX_FOUR_DIGIT_YEAR)
    ) {
      DataError.InvalidInput("Cross-year movimiento ranges require years from 0000 through 9999")
    } else null

private const val MAX_FOUR_DIGIT_YEAR = 9999
