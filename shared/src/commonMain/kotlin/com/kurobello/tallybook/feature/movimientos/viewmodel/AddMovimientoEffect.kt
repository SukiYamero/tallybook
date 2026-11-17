package com.kurobello.tallybook.feature.movimientos.viewmodel

internal sealed interface AddMovimientoEffect {
  data object Saved : AddMovimientoEffect

  data object RequestDictationPermission : AddMovimientoEffect

  data object OpenDictationSettings : AddMovimientoEffect
}
