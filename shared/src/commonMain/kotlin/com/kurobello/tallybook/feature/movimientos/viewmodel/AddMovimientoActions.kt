package com.kurobello.tallybook.feature.movimientos.viewmodel

import androidx.compose.runtime.Stable
import com.kurobello.tallybook.core.model.Frecuencia

@Stable
internal interface AddMovimientoActions : MovimientoFormActions {
  fun onDiscardChanges()

  fun onPhraseChanged(value: String)

  fun onPhraseContinue()

  fun onFillManually()

  fun onEditMovimiento()

  fun onSuggestionSelected(id: String)

  fun onCategoriaCleared()

  fun onStepBack()

  /** Turning Repetir on starts with a monthly frecuencia. */
  fun onRepetirChanged(enabled: Boolean)

  fun onFrecuenciaChanged(frecuencia: Frecuencia)
}
