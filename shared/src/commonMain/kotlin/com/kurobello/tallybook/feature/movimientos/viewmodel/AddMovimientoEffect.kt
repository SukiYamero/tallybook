package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.BudgetNotice

internal sealed interface AddMovimientoEffect {
  data class Saved(val notice: BudgetNotice?) : AddMovimientoEffect

  data object RequestDictationPermission : AddMovimientoEffect

  data object OpenDictationSettings : AddMovimientoEffect
}
