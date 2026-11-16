package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.OccurrenceResult
import com.kurobello.tallybook.core.model.OccurrenceUndo
import com.kurobello.tallybook.core.model.OccurrenceUndoResult
import com.kurobello.tallybook.core.model.Recurrencia
import com.kurobello.tallybook.core.model.RecurrenciaDraft
import com.kurobello.tallybook.core.model.RecurrenciaWithPending
import com.kurobello.tallybook.core.model.RecurrenciaWrite
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Movimiento references are checked as [MovimientoRepository.insert] checks them, so an unknown,
 * archived or incompatible cuenta, or an unknown categoria or etiqueta, is a
 * [com.kurobello.tallybook.core.model.DataError.InvalidInput], as is an unknown recurrencia id.
 */
interface RecurrenciaRepository {

  /** Live recurrencias by proximaFecha, each with its occurrences pending through [today]. */
  fun observeLive(today: LocalDate): Flow<DataOutcome<List<RecurrenciaWithPending>>>

  /** Occurrences, not recurrencias, pending through [today]. */
  fun observePendingCount(today: LocalDate): Flow<DataOutcome<Int>>

  /**
   * Inserts [movimiento] and a recurrencia anchored on its fecha under one stamp; the first
   * proximaFecha is the first occurrence after both that fecha and [today].
   */
  suspend fun create(
      movimiento: Movimiento,
      frecuencia: Frecuencia,
      today: LocalDate,
  ): DataOutcome<Recurrencia>

  /**
   * Creates the movimiento of the oldest occurrence pending through [today] with [monto], which
   * also becomes the template's amount, dated [fecha], and advances proximaFecha.
   */
  suspend fun confirm(
      id: String,
      today: LocalDate,
      monto: Money,
      fecha: LocalDate,
  ): DataOutcome<OccurrenceResult>

  /** Advances proximaFecha past the oldest occurrence pending through [today]. */
  suspend fun skip(id: String, today: LocalDate): DataOutcome<OccurrenceResult>

  /**
   * The fields apply to pending and future occurrences. A different frecuencia or proximaFecha
   * re-anchors the schedule on that proximaFecha, and is [RecurrenciaWrite.ScheduleLocked] while an
   * occurrence is pending through [today].
   */
  suspend fun update(
      id: String,
      draft: RecurrenciaDraft,
      today: LocalDate,
  ): DataOutcome<RecurrenciaWrite>

  /** The movimientos it created stay, still linked to the tombstone. */
  suspend fun delete(id: String): DataOutcome<Unit>

  /** Restores proximaFecha and the amount and tombstones the movimiento a confirm created. */
  suspend fun undo(undo: OccurrenceUndo): DataOutcome<OccurrenceUndoResult>
}
