package com.kurobello.tallybook.core.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.database.Transferencia as StoredTransferencia
import com.kurobello.tallybook.core.database.Transferencia_con_monedas as DatabaseTransferencia
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.RestoreResult
import com.kurobello.tallybook.core.model.TRANSFERENCIA_DESCRIPCION_MAX
import com.kurobello.tallybook.core.model.Transferencia
import com.kurobello.tallybook.core.model.TransferenciaRules
import com.kurobello.tallybook.core.model.TransferenciaUpdateResult
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.todayIn

@Suppress("TooManyFunctions")
internal class TransferenciaRepositoryImpl(
    private val database: TallybookDatabase,
    private val dispatcher: CoroutineDispatcher,
    private val clock: Clock,
    private val timeZoneProvider: TimeZoneProvider,
    private val changeClock: ChangeClock,
) : TransferenciaRepository {

  private val queries
    get() = database.transferenciaQueries

  override suspend fun getById(id: String): DataOutcome<Transferencia?> =
      runLocalDataOperation(dispatcher) {
        queries.selectTransferenciaById(id).executeAsOneOrNull()?.toDomain()
      }

  override suspend fun insert(transferencia: Transferencia): DataOutcome<Unit> {
    validateShape(transferencia)?.let {
      return DataOutcome.Failure(it)
    }
    return runLocalDataOperation(dispatcher) {
          database.transactionWithResult {
            val invalid = invalidAgainstCuentas(transferencia)
            if (invalid != null) {
              DataOutcome.Failure(invalid)
            } else {
              database.insertTransferenciaRow(transferencia, changeClock.next())
              DataOutcome.Success(Unit)
            }
          }
        }
        .flatten()
  }

  override suspend fun update(
      id: String,
      changes: Transferencia,
  ): DataOutcome<TransferenciaUpdateResult> {
    (validateShape(changes)
            ?: DataError.InvalidInput("Transferencia changes must carry id $id").takeIf {
              changes.id != id
            })
        ?.let {
          return DataOutcome.Failure(it)
        }
    return runLocalDataOperation(dispatcher) {
          database.transactionWithResult {
            val current =
                queries.selectTransferenciaById(id).executeAsOneOrNull()?.toDomain()
                    ?: return@transactionWithResult DataOutcome.Success(
                        TransferenciaUpdateResult.NotFound
                    )
            val updated = changes.copy(createdAt = current.createdAt)
            invalidAgainstCuentas(updated, stored = current)?.let {
              return@transactionWithResult DataOutcome.Failure(it)
            }
            if (updated != current) {
              check(
                  queries
                      .updateTransferencia(
                          cuenta_origen_id = updated.cuentaOrigenId,
                          cuenta_destino_id = updated.cuentaDestinoId,
                          monto_origen_minor = updated.montoOrigen.minorUnits,
                          monto_destino_minor = updated.montoDestino.minorUnits,
                          fecha = updated.fecha.toString(),
                          descripcion = updated.descripcion,
                          updated_at = changeClock.next().value,
                          id = id,
                      )
                      .value == 1L
              )
            }
            DataOutcome.Success(TransferenciaUpdateResult.Updated)
          }
        }
        .flatten()
  }

  override suspend fun deleteAndCapture(id: String): DataOutcome<Transferencia?> =
      runLocalDataOperation(dispatcher) {
        database.transactionWithResult {
          queries.selectTransferenciaById(id).executeAsOneOrNull()?.toDomain()?.also {
            check(
                queries.tombstoneTransferencia(stamp = changeClock.next().value, id = id).value ==
                    1L
            )
          }
        }
      }

  override suspend fun restoreDeleted(transferencia: Transferencia): DataOutcome<RestoreResult> {
    validateShape(transferencia)?.let {
      return DataOutcome.Failure(it)
    }
    return runLocalDataOperation(dispatcher) {
          database.transactionWithResult {
            val live = queries.selectTransferenciaById(transferencia.id).executeAsOneOrNull()
            if (live != null) {
              return@transactionWithResult DataOutcome.Success(
                  if (live.isIdenticalTo(transferencia)) RestoreResult.AlreadyPresent
                  else RestoreResult.Conflict
              )
            }
            // Archived cuentas still take the transfer back: Undo returns money where it was.
            val origen = cuenta(transferencia.cuentaOrigenId)
            val destino = cuenta(transferencia.cuentaDestinoId)
            val stored =
                queries.selectStoredTransferenciaById(transferencia.id).executeAsOneOrNull()
            when {
              origen == null || destino == null -> DataOutcome.Success(RestoreResult.MissingCuenta)
              origen.moneda != transferencia.montoOrigen.moneda ||
                  destino.moneda != transferencia.montoDestino.moneda ->
                  DataOutcome.Success(RestoreResult.Conflict)
              else ->
                  // Time only moves forward, so a date that was valid on capture stays valid; only
                  // the rules that depend on the cuentas are checked again.
                  rulesError(transferencia, origen, destino, today = transferencia.fecha)?.let {
                    DataOutcome.Failure(it)
                  } ?: DataOutcome.Success(restore(transferencia, stored))
            }
          }
        }
        .flatten()
  }

  private fun restore(transferencia: Transferencia, stored: StoredTransferencia?): RestoreResult =
      when {
        stored == null -> {
          database.insertTransferenciaRow(transferencia, changeClock.next())
          RestoreResult.Restored
        }
        stored.holdsContentOf(transferencia) -> {
          val stamp = changeClock.next().value
          check(queries.reviveTransferencia(stamp = stamp, id = transferencia.id).value == 1L)
          RestoreResult.Restored
        }
        else -> RestoreResult.Conflict
      }

  override suspend fun getAll(): DataOutcome<List<Transferencia>> =
      runLocalDataOperation(dispatcher) {
        queries.selectAllTransferencias().executeAsList().map { it.toDomain() }
      }

  override fun observeRecent(limit: Int): Flow<DataOutcome<List<Transferencia>>> =
      if (limit <= 0) {
        flowOf(
            DataOutcome.Failure(
                DataError.InvalidInput("Recent transferencia limit must be positive")
            )
        )
      } else {
        queries
            .selectRecentTransferencias(limit.toLong())
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }
            .asLocalDataOutcome()
      }

  override suspend fun defaultOrigin(primary: Moneda): DataOutcome<String?> =
      runLocalDataOperation(dispatcher) {
        queries
            .selectDefaultOrigin(cash_id = Cuenta.defaultCashId(primary), primary = primary.name)
            .executeAsOneOrNull()
      }

  private fun cuenta(id: String): Cuenta? =
      database.cuentaQueries.selectCuentaById(id).executeAsOneOrNull()?.toDomain()

  // Archiving removes a cuenta from pickers, so only a side the edit leaves untouched may be one.
  private fun invalidAgainstCuentas(
      transferencia: Transferencia,
      stored: Transferencia? = null,
  ): DataError.InvalidInput? {
    val origen = cuenta(transferencia.cuentaOrigenId)
    val destino = cuenta(transferencia.cuentaDestinoId)
    val newlyArchived =
        (origen?.archivada == true && origen.id != stored?.cuentaOrigenId) ||
            (destino?.archivada == true && destino.id != stored?.cuentaDestinoId)
    return if (newlyArchived) DataError.InvalidInput("Transferencia picks an archived cuenta")
    else
        rulesError(
            transferencia,
            origen,
            destino,
            today = clock.todayIn(timeZoneProvider.current()),
        )
  }
}

private fun rulesError(
    transferencia: Transferencia,
    origen: Cuenta?,
    destino: Cuenta?,
    today: LocalDate,
): DataError.InvalidInput? {
  val errors =
      TransferenciaRules.validate(
          origen = origen,
          destino = destino,
          montoOrigen = transferencia.montoOrigen,
          montoDestino = transferencia.montoDestino,
          fecha = transferencia.fecha,
          descripcion = transferencia.descripcion,
          today = today,
      )
  return if (errors.isValid) null else DataError.InvalidInput("Invalid transferencia: $errors")
}

// Checks that need no cuenta, so a malformed snapshot or draft fails before opening a transaction.
private fun validateShape(transferencia: Transferencia): DataError.InvalidInput? =
    when {
      transferencia.id.isBlank() -> DataError.InvalidInput("Transferencia id must not be blank")
      transferencia.cuentaOrigenId == transferencia.cuentaDestinoId ->
          DataError.InvalidInput("Transferencia cuentas must differ")
      transferencia.montoOrigen.minorUnits <= 0 || transferencia.montoDestino.minorUnits <= 0 ->
          DataError.InvalidInput("Transferencia amounts must be positive")
      transferencia.montoOrigen.minorUnits > Money.MAX_MINOR_UNITS ||
          transferencia.montoDestino.minorUnits > Money.MAX_MINOR_UNITS ->
          DataError.InvalidInput("Transferencia amounts exceed the maximum")
      (transferencia.descripcion?.length ?: 0) > TRANSFERENCIA_DESCRIPCION_MAX ->
          DataError.InvalidInput("Transferencia descripcion is too long")
      else -> null
    }

// An unreadable occupied ID must block restoration rather than become a retryable failure.
private fun DatabaseTransferencia.isIdenticalTo(transferencia: Transferencia): Boolean =
    try {
      toDomain() == transferencia
    } catch (_: IllegalArgumentException) {
      false
    }

// Currencies are not compared: restore already checked them against the cuentas that own them. Text
// is parsed so a legacy spelling of the same date or instant still matches the snapshot.
private fun StoredTransferencia.holdsContentOf(snapshot: Transferencia): Boolean =
    try {
      deleted_at != null &&
          cuenta_origen_id == snapshot.cuentaOrigenId &&
          cuenta_destino_id == snapshot.cuentaDestinoId &&
          monto_origen_minor == snapshot.montoOrigen.minorUnits &&
          monto_destino_minor == snapshot.montoDestino.minorUnits &&
          descripcion == snapshot.descripcion &&
          LocalDate.parse(fecha) == snapshot.fecha &&
          Instant.parse(created_at) == snapshot.createdAt
    } catch (_: IllegalArgumentException) {
      false
    }
