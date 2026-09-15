package com.kurobello.tallybook.feature.movimientos.viewmodel

internal sealed interface AddMovimientoEffect {
  data object Saved : AddMovimientoEffect
}
