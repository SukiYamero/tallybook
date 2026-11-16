package com.kurobello.tallybook.core.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.OccurrenceResult
import com.kurobello.tallybook.core.model.OccurrenceUndo
import com.kurobello.tallybook.core.model.OccurrenceUndoResult
import com.kurobello.tallybook.core.model.Recurrencia
import com.kurobello.tallybook.core.model.RecurrenciaDraft
import com.kurobello.tallybook.core.model.RecurrenciaRules
import com.kurobello.tallybook.core.model.RecurrenciaWithPending
import com.kurobello.tallybook.core.model.RecurrenciaWrite
import com.kurobello.tallybook.core.model.map
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

internal class RecurrenciaRepositoryImpl(
    private val database: TallybookDatabase,
    private val dispatcher: CoroutineDispatcher,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val changeClock: ChangeClock,
) : RecurrenciaRepository {

  private val queries
    get() = database.recurrenciaQueries

  private val writes = MovimientoWrites(database)

  override fun observeLive(today: LocalDate): Flow<DataOutcome<List<RecurrenciaWithPending>>> =
      queries
          .selectLiveRecurrencias()
          .asFlow()
          .mapToList(dispatcher)
          .map { rows ->
            rows.map { row ->
              val recurrencia = row.toDomain()
              RecurrenciaWithPending(recurrencia, recurrencia.pendingThrough(today))
            }
          }
          .flowOn(dispatcher)
          .asLocalDataOutcome()

  override fun observePendingCount(today: LocalDate): Flow<DataOutcome<Int>> =
      observeLive(today).map { outcome ->
        outcome.map { recurrencias -> recurrencias.sumOf { it.pending.size } }
      }

  override suspend fun create(
      movimiento: Movimiento,
      frecuencia: Frecuencia,
      today: LocalDate,
  ): DataOutcome<Recurrencia> {
    validateMovimiento(movimiento)?.let {
      return DataOutcome.Failure(it)
    }
    return runLocalDataOperation(dispatcher) {
          database.transactionWithResult {
            writes.rejection(movimiento.changes())?.let {
              return@transactionWithResult DataOutcome.Failure(it)
            }
            val recurrencia =
                Recurrencia(
                    id = idGenerator.next(),
                    tipo = movimiento.tipo,
                    monto = movimiento.monto,
                    categoriaId = movimiento.categoriaId,
                    titulo = movimiento.titulo,
                    descripcion = movimiento.descripcion,
                    cuentaId = movimiento.cuentaId,
                    medio = movimiento.medio,
                    etiquetaIds = movimiento.etiquetaIds,
                    frecuencia = frecuencia,
                    fechaAncla = movimiento.fecha,
                    proximaFecha =
                        RecurrenciaRules.firstProximaFecha(movimiento.fecha, frecuencia, today),
                    createdAt = movimiento.createdAt,
                )
            val stamp = changeClock.next()
            writes.insertRow(movimiento, stamp, recurrenciaId = recurrencia.id)
            database.insertRecurrenciaRow(recurrencia, stamp)
            DataOutcome.Success(recurrencia)
          }
        }
        .flatten()
  }

  override suspend fun confirm(
      id: String,
      today: LocalDate,
      monto: Money,
      fecha: LocalDate,
  ): DataOutcome<OccurrenceResult> =
      actOnOldestPending(id, today) { current ->
        val movimiento =
            current.movimiento(
                id = idGenerator.next(),
                monto = monto,
                fecha = fecha,
                createdAt = clock.now(),
            )
        val invalid = validateMovimiento(movimiento) ?: writes.rejection(movimiento.changes())
        if (invalid != null) {
          DataOutcome.Failure(invalid)
        } else {
          val stamp = changeClock.next()
          writes.insertRow(movimiento, stamp, recurrenciaId = id)
          database.advanceRecurrencia(current, monto, stamp)
          DataOutcome.Success(OccurrenceResult.Done(current.undoOf(movimiento.id, stamp)))
        }
      }

  override suspend fun skip(id: String, today: LocalDate): DataOutcome<OccurrenceResult> =
      actOnOldestPending(id, today) { current ->
        val stamp = changeClock.next()
        database.advanceRecurrencia(current, current.monto, stamp)
        DataOutcome.Success(OccurrenceResult.Done(current.undoOf(movimientoId = null, stamp)))
      }

  private suspend fun actOnOldestPending(
      id: String,
      today: LocalDate,
      act: (Recurrencia) -> DataOutcome<OccurrenceResult>,
  ): DataOutcome<OccurrenceResult> =
      runLocalDataOperation(dispatcher) {
            database.transactionWithResult {
              val current =
                  selectLive(id)
                      ?: return@transactionWithResult DataOutcome.Failure(unknownRecurrencia(id))
              if (current.proximaFecha > today) {
                DataOutcome.Success(OccurrenceResult.NothingPending)
              } else act(current)
            }
          }
          .flatten()

  override suspend fun update(
      id: String,
      draft: RecurrenciaDraft,
      today: LocalDate,
  ): DataOutcome<RecurrenciaWrite> {
    validateMovimientoChanges(draft.movimientoChanges())?.let {
      return DataOutcome.Failure(it)
    }
    return runLocalDataOperation(dispatcher) {
          database.transactionWithResult {
            val current =
                selectLive(id)
                    ?: return@transactionWithResult DataOutcome.Failure(unknownRecurrencia(id))
            val rescheduled =
                draft.frecuencia != current.frecuencia || draft.proximaFecha != current.proximaFecha
            if (rescheduled && current.proximaFecha <= today) {
              return@transactionWithResult DataOutcome.Success(RecurrenciaWrite.ScheduleLocked)
            }
            writes
                .rejection(
                    draft.movimientoChanges(),
                    keptCategoriaId = current.categoriaId,
                    keptCuentaId = current.cuentaId,
                )
                ?.let {
                  return@transactionWithResult DataOutcome.Failure(it)
                }
            if (draft == current.draft) {
              return@transactionWithResult DataOutcome.Success(RecurrenciaWrite.Saved(current))
            }
            val updated =
                current.withDraft(
                    draft,
                    fechaAncla = if (rescheduled) draft.proximaFecha else current.fechaAncla,
                )
            database.updateRecurrenciaRow(updated, changeClock.next())
            DataOutcome.Success(RecurrenciaWrite.Saved(updated))
          }
        }
        .flatten()
  }

  override suspend fun delete(id: String): DataOutcome<Unit> =
      runLocalDataOperation(dispatcher) {
            database.transactionWithResult {
              if (selectLive(id) == null) {
                DataOutcome.Failure(unknownRecurrencia(id))
              } else {
                check(
                    queries.tombstoneRecurrencia(stamp = changeClock.next().value, id = id).value ==
                        1L
                )
                queries.deleteRecurrenciaEtiquetas(id)
                DataOutcome.Success(Unit)
              }
            }
          }
          .flatten()

  // Both rows carry the stamp the confirm or skip wrote until anything else touches them.
  override suspend fun undo(undo: OccurrenceUndo): DataOutcome<OccurrenceUndoResult> =
      runLocalDataOperation(dispatcher) {
        database.transactionWithResult {
          val recurrenciaStamp =
              queries.selectRecurrenciaById(undo.recurrenciaId).executeAsOneOrNull()?.updated_at
          val movimientoStamp =
              undo.movimientoId?.let {
                database.movimientoQueries.selectMovimientoById(it).executeAsOneOrNull()?.updated_at
              }
          val unchanged =
              recurrenciaStamp == undo.stamp.value &&
                  (undo.movimientoId == null || movimientoStamp == undo.stamp.value)
          if (unchanged) {
            val stamp = changeClock.next()
            database.writeOccurrence(
                undo.recurrenciaId,
                undo.occurrence,
                undo.previousMonto,
                stamp,
            )
            undo.movimientoId?.let { writes.tombstone(it, stamp) }
            OccurrenceUndoResult.Undone
          } else OccurrenceUndoResult.Conflict
        }
      }

  private fun selectLive(id: String): Recurrencia? =
      queries.selectRecurrenciaById(id).executeAsOneOrNull()?.toDomain()
}

private fun TallybookDatabase.advanceRecurrencia(
    current: Recurrencia,
    monto: Money,
    stamp: ChangeStamp,
) {
  writeOccurrence(
      id = current.id,
      proximaFecha =
          RecurrenciaRules.firstAfter(current.fechaAncla, current.frecuencia, current.proximaFecha),
      monto = monto,
      stamp = stamp,
  )
}

private fun TallybookDatabase.writeOccurrence(
    id: String,
    proximaFecha: LocalDate,
    monto: Money,
    stamp: ChangeStamp,
) {
  check(
      recurrenciaQueries
          .updateRecurrenciaOccurrence(
              proxima_fecha = proximaFecha.toString(),
              monto_minor = monto.minorUnits,
              updated_at = stamp.value,
              id = id,
          )
          .value == 1L
  )
}

private fun Recurrencia.undoOf(movimientoId: String?, stamp: ChangeStamp) =
    OccurrenceUndo(
        recurrenciaId = id,
        occurrence = proximaFecha,
        movimientoId = movimientoId,
        previousMonto = monto,
        stamp = stamp,
    )

private fun Recurrencia.withDraft(draft: RecurrenciaDraft, fechaAncla: LocalDate) =
    copy(
        tipo = draft.tipo,
        monto = draft.monto,
        categoriaId = draft.categoriaId,
        titulo = draft.titulo,
        descripcion = draft.descripcion,
        cuentaId = draft.cuentaId,
        medio = draft.medio,
        etiquetaIds = draft.etiquetaIds,
        frecuencia = draft.frecuencia,
        fechaAncla = fechaAncla,
        proximaFecha = draft.proximaFecha,
    )

// The template follows the rules of the movimientos it creates; its next one is dated proximaFecha.
private fun RecurrenciaDraft.movimientoChanges() =
    MovimientoChanges(
        tipo = tipo,
        monto = monto,
        fecha = proximaFecha,
        categoriaId = categoriaId,
        etiquetaIds = etiquetaIds,
        titulo = titulo,
        descripcion = descripcion,
        cuentaId = cuentaId,
        medio = medio,
    )

private fun unknownRecurrencia(id: String) = DataError.InvalidInput("Unknown recurrencia $id")
