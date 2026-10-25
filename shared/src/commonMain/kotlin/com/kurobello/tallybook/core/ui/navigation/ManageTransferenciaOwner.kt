package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kurobello.tallybook.core.ui.feedback.ManagementFeedbackQueue
import com.kurobello.tallybook.feature.transfers.viewmodel.ManageTransferenciaViewModel
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferEntry
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferUiState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Lives in the shell's management owner, beside the queue it enqueues into, sharing its lifetime.
 */
@Composable
internal fun rememberManageTransferenciaViewModel(
    modals: PrimaryModalCoordinator,
    owner: ViewModelStoreOwner,
    feedback: ManagementFeedbackQueue,
): ManageTransferenciaViewModel {
  val viewModel: ManageTransferenciaViewModel =
      koinViewModel(viewModelStoreOwner = owner) { parametersOf(feedback) }
  val state = viewModel.uiState.collectAsStateWithLifecycle()
  val controller = remember(viewModel, state) { ManageTransferenciaController(viewModel, state) }
  DisposableEffect(modals, controller) {
    modals.attachTransfer(controller)
    onDispose { modals.detachTransfer(controller) }
  }
  return viewModel
}

internal class ManageTransferenciaController(
    private val viewModel: ManageTransferenciaViewModel,
    private val state: State<TransferUiState>,
) : TransferManagementController {
  // Same contract as ManageMovimientoController: subscribe through the collected State, answer
  // guards from the view model.
  override val hasActiveSession
    get() = state.value.session != null || viewModel.uiState.value.session != null

  override fun open(entry: TransferEntry) = viewModel.open(entry)
}

@Composable
internal fun rememberOpenTransfer(modals: PrimaryModalCoordinator): (TransferEntry) -> Unit {
  val focusManager = LocalFocusManager.current
  val keyboard = LocalSoftwareKeyboardController.current
  return remember(modals, focusManager, keyboard) {
    { entry ->
      if (modals.tryOpenTransfer(entry)) {
        keyboard?.hide()
        focusManager.clearFocus(force = true)
      }
    }
  }
}
