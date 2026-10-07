package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.defaultViewModelCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import com.kurobello.tallybook.feature.movimientos.viewmodel.ManageMovimientoViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun rememberManageMovimientoOwner(): ViewModelStoreOwner {
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
internal fun rememberManageMovimientoViewModel(
    modals: PrimaryModalCoordinator
): ManageMovimientoViewModel {
  val viewModel: ManageMovimientoViewModel =
      koinViewModel(viewModelStoreOwner = rememberManageMovimientoOwner())
  val controller =
      remember(viewModel) {
        object : MovimientoManagementController {
          override val hasActiveSession
            get() = viewModel.uiState.value.session != null

          override val isRestoring
            get() = viewModel.uiState.value.isRestoring

          override fun open(id: String) = viewModel.open(id)
        }
      }
  DisposableEffect(modals, controller) {
    modals.attachManagement(controller)
    onDispose { modals.detachManagement(controller) }
  }
  return viewModel
}
