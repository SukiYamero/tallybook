package com.kurobello.tallybook.core.ui.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.TransferenciaRepository
import kotlinx.coroutines.CoroutineScope

/**
 * Hosts the shell's [ManagementFeedbackQueue] in the same child store as the management view models
 * that enqueue into it, so the queue and every reference to it share one lifetime.
 */
internal class ManagementFeedbackViewModel(
    movimientoRepository: MovimientoRepository,
    transferenciaRepository: TransferenciaRepository,
    scope: CoroutineScope? = null,
) : ViewModel() {
  val queue =
      ManagementFeedbackQueue(scope ?: viewModelScope) { snapshot ->
        when (snapshot) {
          is DeletedSnapshot.OfMovimiento ->
              movimientoRepository.restoreDeleted(snapshot.movimiento)
          is DeletedSnapshot.OfTransferencia ->
              transferenciaRepository.restoreDeleted(snapshot.transferencia)
        }
      }

  override fun onCleared() = queue.clear()
}
