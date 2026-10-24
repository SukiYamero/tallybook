package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.defaultViewModelCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import com.kurobello.tallybook.core.ui.feedback.ManagementFeedbackQueue
import com.kurobello.tallybook.core.ui.feedback.ManagementFeedbackViewModel
import com.kurobello.tallybook.feature.movimientos.viewmodel.ManageMovimientoUiState
import com.kurobello.tallybook.feature.movimientos.viewmodel.ManageMovimientoViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun rememberManagementOwner(): ViewModelStoreOwner {
  val parent = requireNotNull(LocalViewModelStoreOwner.current)
  // Intent extras may contain newly copied Bundles on every read of the same Activity.
  val extras = remember(parent) { parent.defaultViewModelCreationExtras }
  return rememberViewModelStoreOwner(
      parent = parent,
      savedStateRegistryOwner = null,
      defaultCreationExtras = extras,
  )
}

@Composable
internal fun managementFeedbackQueue(owner: ViewModelStoreOwner): ManagementFeedbackQueue =
    koinViewModel<ManagementFeedbackViewModel>(viewModelStoreOwner = owner).queue

// The queue must come from the same owner: parameters apply only on first creation, so the
// retained view model keeps a queue that lives exactly as long as it does.
@Composable
internal fun manageMovimientoViewModel(
    owner: ViewModelStoreOwner,
    feedback: ManagementFeedbackQueue,
): ManageMovimientoViewModel = koinViewModel(viewModelStoreOwner = owner) { parametersOf(feedback) }

@Composable
internal fun rememberManageMovimientoViewModel(
    modals: PrimaryModalCoordinator,
    owner: ViewModelStoreOwner,
    feedback: ManagementFeedbackQueue,
): ManageMovimientoViewModel {
  val viewModel = manageMovimientoViewModel(owner, feedback)
  val state = viewModel.uiState.collectAsStateWithLifecycle()
  val controller =
      remember(viewModel, state, feedback) {
        ManageMovimientoController(viewModel, state, feedback)
      }
  DisposableEffect(modals, controller) {
    modals.attachManagement(controller)
    onDispose { modals.detachManagement(controller) }
  }
  return viewModel
}

internal class ManageMovimientoController(
    private val viewModel: ManageMovimientoViewModel,
    private val state: State<ManageMovimientoUiState>,
    private val feedback: ManagementFeedbackQueue,
) : MovimientoManagementController {
  // Same contract as SearchFilterController: subscribe through the collected State, answer guards
  // from the view model.
  override val hasActiveSession
    get() = state.value.session != null || viewModel.uiState.value.session != null

  override val isRestoring
    get() = feedback.state.value.isRestoring

  override fun open(id: String) = viewModel.open(id)
}
