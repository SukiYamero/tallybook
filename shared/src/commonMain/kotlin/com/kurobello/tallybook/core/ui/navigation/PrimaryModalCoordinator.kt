package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kurobello.tallybook.core.ui.movimiento.AddMovimientoSheetSession

internal interface DestinationModalController {
  val hasActiveSession: Boolean

  fun open(): Boolean
}

internal interface MovimientoManagementController {
  val hasActiveSession: Boolean
  val isRestoring: Boolean

  fun open(id: String): Boolean
}

internal class PrimaryModalCoordinator(
    private val creation: AddMovimientoSheetSession,
    private val isSearchSelected: () -> Boolean,
) {
  private var destinationModal: DestinationModalController? by mutableStateOf(null)

  private var management: MovimientoManagementController? by mutableStateOf(null)

  fun attachManagement(controller: MovimientoManagementController) {
    management = controller
  }

  fun detachManagement(controller: MovimientoManagementController) {
    if (management === controller) management = null
  }

  fun tryOpenManagement(id: String): Boolean {
    if (!canStartModal) return false
    return management?.open(id) ?: false
  }

  fun dispatchFeedback(isForeground: Boolean, dispatch: () -> Unit): Boolean {
    if (!isForeground || !canStartModal) return false
    dispatch()
    return true
  }

  private val canStartModal: Boolean
    get() = canNavigate && management?.isRestoring != true

  val canNavigate: Boolean
    get() =
        !creation.hasBeenOpened &&
            !(destinationModal?.hasActiveSession ?: isSearchSelected()) &&
            management?.hasActiveSession != true

  fun attach(controller: DestinationModalController) {
    destinationModal = controller
  }

  fun detach(controller: DestinationModalController) {
    if (destinationModal === controller) destinationModal = null
  }

  fun tryOpenCreation(): Boolean {
    if (!canStartModal) return false
    creation.open()
    return true
  }

  fun tryOpenDestinationModal(controller: DestinationModalController): Boolean {
    if (!canStartModal || destinationModal !== controller) return false
    return controller.open()
  }
}
