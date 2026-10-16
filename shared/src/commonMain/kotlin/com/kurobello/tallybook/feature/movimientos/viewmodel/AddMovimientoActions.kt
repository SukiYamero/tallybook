package com.kurobello.tallybook.feature.movimientos.viewmodel

import androidx.compose.runtime.Stable

@Stable
internal interface AddMovimientoActions : MovimientoFormActions {
  fun onDiscardChanges()

  fun onPhraseChanged(value: String)

  fun onPhraseContinue()

  fun onFillManually()

  fun onEditMovimiento()

  fun onSuggestionSelected(id: String)

  fun onStepBack()
}
