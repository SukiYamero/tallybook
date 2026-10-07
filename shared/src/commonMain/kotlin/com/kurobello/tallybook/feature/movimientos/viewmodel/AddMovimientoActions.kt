package com.kurobello.tallybook.feature.movimientos.viewmodel

import androidx.compose.runtime.Stable

@Stable
internal interface AddMovimientoActions : MovimientoFormActions {
  fun onDiscardChanges()
}
