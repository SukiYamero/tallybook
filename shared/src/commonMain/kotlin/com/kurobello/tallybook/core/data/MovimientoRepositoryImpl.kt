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
import com.kurobello.tallybook.core.model.CuentaRejection
import com.kurobello.tallybook.core.model.CuentaSelection
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MAX_ETIQUETAS_POR_MOVIMIENTO
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.MOVIMIENTO_TITLE_MAX_LENGTH
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
          .map { rows -> summaryOf(moneda, rows.associate { it.tipo to it.total_minor }) }
          .asLocalDataOutcome()

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

  override suspend fun insert(movimiento: Movimiento): DataOutcome<Unit> {
    validate(movimiento)?.let {
      return DataOutcome.Failure(it)
    }
    return runLocalDataOperation(dispatcher) {
          database.transactionWithResult {
            val missing =
                missingReference(movimiento.categoriaId, movimiento.etiquetaIds)
                    ?: invalidCuenta(movimiento.cuentaId, movimiento.medio, movimiento.monto)
            if (missing != null) {
              DataOutcome.Failure(missing)
            } else {
              insertRow(movimiento, changeClock.next())
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
            // A stored categoria the catalog no longer has stays valid while the user keeps it.
            missingReference(
                    changes.categoriaId,
                    changes.etiquetaIds,
                    keptCategoriaId = current.categoriaId,
                )
                ?.let {
                  return@transactionWithResult DataOutcome.Failure(it)
                }
            invalidCuenta(changes.cuentaId, changes.medio, changes.monto, stored = current)?.let {
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
            insertEtiquetas(id, changes.etiquetaIds)
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
                check(
                    queries.tombstoneMovimiento(stamp = changeClock.next().value, id = id).value ==
                        1L
                )
                queries.deleteMovimientoEtiquetas(id)
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
            insertRow(restorable, changeClock.next())
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
    insertEtiquetas(movimiento.id, movimiento.etiquetaIds)
  }

  private fun insertRow(movimiento: Movimiento, stamp: ChangeStamp) {
    check(
        database.movimientoQueries
            .insertMovimiento(
                id = movimiento.id,
                tipo = movimiento.tipo.name,
                monto_minor = movimiento.monto.minorUnits,
                fecha = movimiento.fecha.toString(),
                titulo = movimiento.titulo,
                descripcion = movimiento.descripcion,
                created_at = movimiento.createdAt.toString(),
                categoria_id = movimiento.categoriaId,
                cuenta_id = movimiento.cuentaId,
                medio_pago = movimiento.medio.name,
                updated_at = stamp.value,
            )
            .value == 1L
    )
    insertEtiquetas(movimiento.id, movimiento.etiquetaIds)
  }

  private fun insertEtiquetas(movimientoId: String, etiquetaIds: List<String>) {
    etiquetaIds.forEachIndexed { posicion, etiquetaId ->
      check(
          database.movimientoQueries
              .insertMovimientoEtiqueta(movimientoId, etiquetaId, posicion.toLong())
              .value == 1L
      )
    }
  }

  private fun missingReference(
      categoriaId: String,
      etiquetaIds: List<String>,
      keptCategoriaId: String? = null,
  ): DataError.InvalidInput? =
      when {
        categoriaId != keptCategoriaId && !categoriaExists(categoriaId) ->
            DataError.InvalidInput("Unknown categoria $categoriaId")
        existingEtiquetaIds(etiquetaIds).size != etiquetaIds.size ->
            DataError.InvalidInput("Movimiento references an unknown etiqueta")
        else -> null
      }

  // Deleting a custom categoria is refused while a stored movimiento uses it, but a captured one no
  // longer counts; restoring it falls back to Otros, the same pending state creation uses.
  private fun withSurvivingReferences(movimiento: Movimiento): Movimiento {
    val existing = existingEtiquetaIds(movimiento.etiquetaIds)
    return movimiento.copy(
        categoriaId = movimiento.categoriaId.takeIf(::categoriaExists) ?: CATEGORIA_OTROS_ID,
        etiquetaIds = movimiento.etiquetaIds.filter { it in existing },
    )
  }

  // Checked inside the write transaction so an archive or delete cannot slip between check and
  // write. A medio never outlives its cuenta's type: the type is locked while a live movimiento
  // uses it.
  private fun invalidCuenta(
      cuentaId: String,
      medio: MedioPago,
      monto: Money,
      stored: Movimiento? = null,
  ): DataError.InvalidInput? {
    val cuenta =
        database.cuentaQueries.selectCuentaById(cuentaId).executeAsOneOrNull()?.toDomain()
            ?: return DataError.InvalidInput("Unknown cuenta $cuentaId")
    return when (cuenta.rejectionFor(medio, monto.moneda, stored?.cuentaId)) {
      null -> null
      CuentaRejection.ARCHIVED -> DataError.InvalidInput("Cuenta $cuentaId is archived")
      CuentaRejection.MEDIO_MISMATCH ->
          DataError.InvalidInput("Medio $medio does not fit a ${cuenta.tipo} cuenta")
      CuentaRejection.MONEDA_MISMATCH ->
          DataError.InvalidInput("Movimiento currency must be its cuenta's ${cuenta.moneda}")
    }
  }

  private fun categoriaExists(id: String): Boolean =
      database.categoriaQueries.selectCategoriaById(id).executeAsOneOrNull() != null

  private fun existingEtiquetaIds(ids: List<String>): Set<String> =
      if (ids.isEmpty()) emptySet()
      else database.etiquetaQueries.selectExistingEtiquetaIds(ids).executeAsList().toSet()

  private fun validateRestorable(movimiento: Movimiento): DataError.InvalidInput? =
      validateId(movimiento.id) ?: validateChanges(movimiento.changes())

  private fun validateId(id: String): DataError.InvalidInput? =
      if (id.isBlank()) DataError.InvalidInput("Movimiento id must not be blank") else null

  private fun validateChanges(changes: MovimientoChanges): DataError.InvalidInput? =
      when {
        changes.monto.minorUnits <= 0 ->
            DataError.InvalidInput("Movimiento amount must be positive")
        !changes.medio.allows(changes.tipo) ->
            DataError.InvalidInput("Medio ${changes.medio} does not allow ${changes.tipo}")
        else ->
            validateReferences(changes.categoriaId, changes.etiquetaIds)
                ?: validateTitle(changes.titulo)
                ?: validateDescription(changes.descripcion)
      }

  override suspend fun delete(id: String): DataOutcome<Unit> =
      runLocalDataOperation(dispatcher) {
        database.transaction {
          val queries = database.movimientoQueries
          if (queries.selectMovimientoById(id).executeAsOneOrNull() != null) {
            check(
                queries.tombstoneMovimiento(stamp = changeClock.next().value, id = id).value == 1L
            )
          }
          queries.deleteMovimientoEtiquetas(id)
        }
      }

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

  private fun validate(movimiento: Movimiento): DataError.InvalidInput? =
      when {
        movimiento.monto.minorUnits <= 0 ->
            DataError.InvalidInput("Movimiento amount must be positive")
        movimiento.id.isEmpty() -> DataError.InvalidInput("Movimiento id must not be empty")
        !movimiento.medio.allows(movimiento.tipo) ->
            DataError.InvalidInput("Medio ${movimiento.medio} does not allow ${movimiento.tipo}")
        else ->
            validateReferences(movimiento.categoriaId, movimiento.etiquetaIds)
                ?: validateTitle(movimiento.titulo)
                ?: validateDescription(movimiento.descripcion)
      }
}

private fun Movimiento.changes(): MovimientoChanges =
    MovimientoChanges(
        tipo,
        monto,
        fecha,
        categoriaId,
        etiquetaIds,
        titulo,
        descripcion,
        cuentaId,
        medio,
    )

// A last used medio follows the tipo the way a draft does; any other candidate gets the medio its
// cuenta type implies. Either way the pair must still be compatible, e.g. after a cuenta changed
// type.
private fun SelectSelectionCandidates.toSelection(
    tipo: TipoMovimiento,
    requested: MedioPago?,
): CuentaSelection? {
  val tipoCuenta = TipoCuenta.valueOf(this.tipo)
  val medio =
      requested
          ?: medio_pago?.let { MedioPago.valueOf(it).forTipo(tipo) }
          ?: tipoCuenta.defaultMedio(tipo)
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

private fun validateTitle(titulo: String?): DataError.InvalidInput? =
    if ((titulo?.length ?: 0) > MOVIMIENTO_TITLE_MAX_LENGTH) {
      DataError.InvalidInput("Movimiento title is too long")
    } else null

private fun validateDescription(descripcion: String?): DataError.InvalidInput? =
    if ((descripcion?.length ?: 0) > MOVIMIENTO_DESCRIPTION_MAX_LENGTH) {
      DataError.InvalidInput("Movimiento description is too long")
    } else null

private fun validateReferences(
    categoriaId: String,
    etiquetaIds: List<String>,
): DataError.InvalidInput? =
    when {
      categoriaId.isBlank() -> DataError.InvalidInput("Categoria id must not be blank")
      etiquetaIds.size > MAX_ETIQUETAS_POR_MOVIMIENTO ->
          DataError.InvalidInput(
              "Movimiento allows $MAX_ETIQUETAS_POR_MOVIMIENTO etiquetas, got ${etiquetaIds.size}"
          )
      etiquetaIds.any { it.isBlank() } -> DataError.InvalidInput("Etiqueta id must not be blank")
      etiquetaIds.any { id -> id.any { it in VIEW_SEPARATORS } } ->
          DataError.InvalidInput("Etiqueta id must not contain a control separator")
      etiquetaIds.toSet().size != etiquetaIds.size ->
          DataError.InvalidInput("Movimiento etiquetas must be distinct")
      else -> null
    }

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
