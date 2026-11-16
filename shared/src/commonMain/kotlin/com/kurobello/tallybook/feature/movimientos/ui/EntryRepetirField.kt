package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.Composable
import com.kurobello.tallybook.core.ui.recurrencia.RepetirField
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoUiState

/** Repetir belongs to creation only: an existing movimiento never becomes recurrent. */
@Composable
internal fun EntryRepetirField(uiState: AddMovimientoUiState, actions: AddMovimientoActions) {
  RepetirField(
      frecuencia = uiState.frecuencia,
      onRepetirChanged = actions::onRepetirChanged,
      onFrecuenciaChanged = actions::onFrecuenciaChanged,
      enabled = !uiState.isSubmitting,
  )
}
