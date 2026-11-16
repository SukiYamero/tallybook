package com.kurobello.tallybook.core.model

import kotlinx.datetime.LocalDate

sealed interface RecurrenciaWrite {

  data class Saved(val recurrencia: Recurrencia) : RecurrenciaWrite

  /** Nothing was written: the frecuencia or proximaFecha changed while an occurrence is pending. */
  data object ScheduleLocked : RecurrenciaWrite
}

/** A confirm or skip of a recurrencia's oldest pending occurrence. */
sealed interface OccurrenceResult {

  data class Done(val undo: OccurrenceUndo) : OccurrenceResult

  /** Nothing was written: no occurrence was due. */
  data object NothingPending : OccurrenceResult
}

/**
 * Everything Undo of a confirm or skip restores. The stamp it carries is how Undo tells that
 * neither the recurrencia nor the created movimiento changed since, so only the repository reads
 * it.
 */
class OccurrenceUndo
internal constructor(
    val recurrenciaId: String,
    /** The occurrence confirmed or skipped, which becomes the proximaFecha again. */
    val occurrence: LocalDate,
    /** The movimiento a confirm created; null for a skip. */
    val movimientoId: String?,
    internal val previousMonto: Money,
    internal val stamp: ChangeStamp,
)

enum class OccurrenceUndoResult {
  Undone,

  /** Nothing was written: the recurrencia or the created movimiento changed since. */
  Conflict,
}
