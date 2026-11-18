package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.OccurrenceResult
import com.kurobello.tallybook.core.model.OccurrenceUndo
import com.kurobello.tallybook.core.model.OccurrenceUndoResult
import com.kurobello.tallybook.core.model.Recurrencia
import com.kurobello.tallybook.core.model.RecurrenciaDraft
import com.kurobello.tallybook.core.model.RecurrenciaRules
import com.kurobello.tallybook.core.model.RecurrenciaWithPending
import com.kurobello.tallybook.core.model.RecurrenciaWrite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/**
 * In-memory recurrencias under the repository's occurrence rules. Each write bumps a revision that
 * stands in for the change stamp, so Undo conflicts as the real one does. [failure] makes every
 * call fail with that error; [confirmFailure] fails only confirms.
 */
internal class FakeRecurrenciaRepository(initial: List<Recurrencia> = emptyList()) :
    RecurrenciaRepository {

  val recurrencias = MutableStateFlow(initial.associateBy { it.id })
  val created = mutableListOf<Pair<Movimiento, Frecuencia>>()
  val confirmed = mutableListOf<Movimiento>()
  val updates = mutableListOf<RecurrenciaDraft>()
  var failure: DataError? = null
  var confirmFailure: DataError? = null
  private val revisions = mutableMapOf<String, Int>()
  private val tombstoned = mutableSetOf<String>()
  private var nextId = 0

  override fun observeLive(today: LocalDate): Flow<DataOutcome<List<RecurrenciaWithPending>>> =
      recurrencias.map { byId ->
        outcome {
          byId.values
              .sortedBy { it.proximaFecha }
              .map { RecurrenciaWithPending(it, it.pendingThrough(today)) }
        }
      }

  override fun observePendingCount(today: LocalDate): Flow<DataOutcome<Int>> =
      recurrencias.map { byId ->
        outcome { byId.values.sumOf { it.pendingThrough(today).size } }
      }

  override suspend fun create(
      movimiento: Movimiento,
      frecuencia: Frecuencia,
      today: LocalDate,
  ): DataOutcome<Recurrencia> = outcome { createOne(movimiento, frecuencia, today) }

  override suspend fun createAll(
      movimientos: List<Movimiento>,
      frecuencia: Frecuencia,
      today: LocalDate,
  ): DataOutcome<List<Recurrencia>> = outcome {
    movimientos.map { createOne(it, frecuencia, today) }
  }

  private fun createOne(
      movimiento: Movimiento,
      frecuencia: Frecuencia,
      today: LocalDate,
  ): Recurrencia {
    created += movimiento to frecuencia
    val recurrencia =
        Recurrencia(
            id = "recurrencia-${++nextId}",
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
            proximaFecha = RecurrenciaRules.firstProximaFecha(movimiento.fecha, frecuencia, today),
            createdAt = movimiento.createdAt,
        )
    write(recurrencia)
    return recurrencia
  }

  override suspend fun confirm(
      id: String,
      today: LocalDate,
      monto: Money,
      fecha: LocalDate,
  ): DataOutcome<OccurrenceResult> {
    confirmFailure?.let {
      return DataOutcome.Failure(it)
    }
    return advance(id, today) { current, occurrence ->
      val movimiento =
          Movimiento(
              id = "confirmed-${confirmed.size + 1}",
              tipo = current.tipo,
              monto = monto,
              fecha = fecha,
              categoriaId = current.categoriaId,
              titulo = current.titulo,
              descripcion = current.descripcion,
              createdAt = current.createdAt,
              cuentaId = current.cuentaId,
              medio = current.medio,
              etiquetaIds = current.etiquetaIds,
          )
      confirmed += movimiento
      Advance(current.copy(monto = monto), movimiento.id, occurrence)
    }
  }

  override suspend fun skip(id: String, today: LocalDate): DataOutcome<OccurrenceResult> =
      advance(id, today) { current, occurrence -> Advance(current, null, occurrence) }

  override suspend fun update(
      id: String,
      draft: RecurrenciaDraft,
      today: LocalDate,
  ): DataOutcome<RecurrenciaWrite> =
      withRecurrencia(id) { current ->
        val scheduleChanged =
            draft.frecuencia != current.frecuencia || draft.proximaFecha != current.proximaFecha
        if (scheduleChanged && current.pendingThrough(today).isNotEmpty()) {
          RecurrenciaWrite.ScheduleLocked
        } else {
          updates += draft
          val updated =
              current.copy(
                  tipo = draft.tipo,
                  monto = draft.monto,
                  categoriaId = draft.categoriaId,
                  titulo = draft.titulo,
                  descripcion = draft.descripcion,
                  cuentaId = draft.cuentaId,
                  medio = draft.medio,
                  etiquetaIds = draft.etiquetaIds,
                  frecuencia = draft.frecuencia,
                  fechaAncla = if (scheduleChanged) draft.proximaFecha else current.fechaAncla,
                  proximaFecha = draft.proximaFecha,
              )
          write(updated)
          RecurrenciaWrite.Saved(updated)
        }
      }

  override suspend fun delete(id: String): DataOutcome<Unit> =
      withRecurrencia(id) {
        tombstoned += id
        recurrencias.value -= id
      }

  override suspend fun undo(undo: OccurrenceUndo): DataOutcome<OccurrenceUndoResult> = outcome {
    val current = recurrencias.value[undo.recurrenciaId]
    val movimientoLive = undo.movimientoId == null || confirmed.any { it.id == undo.movimientoId }
    if (current == null || stampOf(current.id) != undo.stamp || !movimientoLive) {
      OccurrenceUndoResult.Conflict
    } else {
      confirmed.removeAll { it.id == undo.movimientoId }
      write(current.copy(proximaFecha = undo.occurrence, monto = undo.previousMonto))
      OccurrenceUndoResult.Undone
    }
  }

  /** Stands in for an edit made elsewhere after a confirm or skip. */
  fun touch(id: String) {
    write(requireNotNull(recurrencias.value[id]))
  }

  private class Advance(val next: Recurrencia, val movimientoId: String?, val occurrence: LocalDate)

  private fun advance(
      id: String,
      today: LocalDate,
      apply: (Recurrencia, LocalDate) -> Advance,
  ): DataOutcome<OccurrenceResult> =
      withRecurrencia(id) { current ->
        val occurrence = current.pendingThrough(today).firstOrNull()
        if (occurrence == null) {
          OccurrenceResult.NothingPending
        } else {
          val step = apply(current, occurrence)
          val next = RecurrenciaRules.firstAfter(current.fechaAncla, current.frecuencia, occurrence)
          write(step.next.copy(proximaFecha = next))
          OccurrenceResult.Done(
              OccurrenceUndo(id, occurrence, step.movimientoId, current.monto, stampOf(id))
          )
        }
      }

  private fun write(recurrencia: Recurrencia) {
    revisions[recurrencia.id] = (revisions[recurrencia.id] ?: 0) + 1
    recurrencias.value += recurrencia.id to recurrencia
  }

  private fun stampOf(id: String): ChangeStamp =
      ChangeStamp.of(epochMillis = revisions[id]?.toLong() ?: 0L, counter = 0, node = NODE)

  private fun <T> withRecurrencia(id: String, block: (Recurrencia) -> T): DataOutcome<T> = outcome {
    val current =
        recurrencias.value[id]
            ?: return DataOutcome.Failure(DataError.InvalidInput("Unknown recurrencia $id"))
    block(current)
  }

  private inline fun <T> outcome(block: () -> T): DataOutcome<T> =
      failure?.let { DataOutcome.Failure(it) } ?: DataOutcome.Success(block())

  private companion object {
    const val NODE = "0000000000000000"
  }
}
