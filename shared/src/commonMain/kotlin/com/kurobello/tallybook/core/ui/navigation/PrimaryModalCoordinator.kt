package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kurobello.tallybook.core.ui.movimiento.AddMovimientoSheetSession

internal interface SearchFilterController {
  val hasActiveSession: Boolean

  fun open(): Boolean
}

internal class PrimaryModalCoordinator(
    private val creation: AddMovimientoSheetSession,
    private val isSearchSelected: () -> Boolean,
) {
  private var filters: SearchFilterController? by mutableStateOf(null)

  val canNavigate: Boolean
    get() = !creation.hasBeenOpened && !(filters?.hasActiveSession ?: isSearchSelected())

  fun attach(controller: SearchFilterController) {
    filters = controller
  }

  fun detach(controller: SearchFilterController) {
    if (filters === controller) filters = null
  }

  fun tryOpenCreation(): Boolean {
    if (!canNavigate) return false
    creation.open()
    return true
  }

  fun tryOpenFilters(): Boolean {
    if (creation.hasBeenOpened || !isSearchSelected()) return false
    return filters?.open() ?: false
  }
}
