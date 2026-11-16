package com.kurobello.tallybook.core.ui.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.RecurrenciaRepository
import com.kurobello.tallybook.core.data.TransferenciaRepository
import com.kurobello.tallybook.core.model.OccurrenceUndoResult
import com.kurobello.tallybook.core.model.RestoreResult
import com.kurobello.tallybook.core.model.map
import kotlinx.coroutines.CoroutineScope

/**
 * Hosts the shell's [ManagementFeedbackQueue] in the same child store as the management view models
 * that enqueue into it, so the queue and every reference to it share one lifetime.
 */
internal class ManagementFeedbackViewModel(
    movimientoRepository: MovimientoRepository,
    transferenciaRepository: TransferenciaRepository,
    recurrenciaRepository: RecurrenciaRepository,
    scope: CoroutineScope? = null,
) : ViewModel() {
  val queue =
      ManagementFeedbackQueue(scope ?: viewModelScope) { snapshot ->
        when (snapshot) {
          is UndoSnapshot.OfMovimiento -> movimientoRepository.restoreDeleted(snapshot.movimiento)
          is UndoSnapshot.OfTransferencia ->
              transferenciaRepository.restoreDeleted(snapshot.transferencia)
          is UndoSnapshot.OfOccurrence ->
              recurrenciaRepository.undo(snapshot.undo).map(occurrenceRestoreResults::getValue)
        }
      }

  override fun onCleared() = queue.clear()
}

internal val occurrenceRestoreResults: Map<OccurrenceUndoResult, RestoreResult> =
    mapOf(
        OccurrenceUndoResult.Undone to RestoreResult.Restored,
        OccurrenceUndoResult.Conflict to RestoreResult.Conflict,
    )
