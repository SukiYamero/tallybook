package com.kurobello.tallybook.feature.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kurobello.tallybook.core.ui.navigation.Destination
import com.kurobello.tallybook.core.ui.navigation.DestinationModalController
import com.kurobello.tallybook.core.ui.navigation.PrimaryModalCoordinator
import com.kurobello.tallybook.feature.profile.viewmodel.ProfileActions
import com.kurobello.tallybook.feature.profile.viewmodel.ProfileSheetKind
import com.kurobello.tallybook.feature.profile.viewmodel.ProfileUiState
import com.kurobello.tallybook.feature.profile.viewmodel.ProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ProfileRoute(
    bottomBarClearance: Dp,
    modals: PrimaryModalCoordinator,
    active: Boolean,
    onOpenSecondary: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
  val viewModel: ProfileViewModel = koinViewModel()
  val state = viewModel.uiState.collectAsStateWithLifecycle()
  val uiState by state
  val controller = remember(viewModel, state) { ProfileSheetController(viewModel, state) }
  DisposableEffect(modals, controller, active) {
    if (active) modals.attach(controller)
    onDispose { modals.detach(controller) }
  }
  val actions =
      remember(viewModel, modals, controller) {
        object : ProfileActions by viewModel {
          override fun onOpenSheet(kind: ProfileSheetKind): Boolean {
            controller.requested = kind
            return modals.tryOpenDestinationModal(controller)
          }
        }
      }
  ProfileScreen(uiState, actions, onOpenSecondary, bottomBarClearance, modifier)
}

private class ProfileSheetController(
    private val viewModel: ProfileViewModel,
    private val state: State<ProfileUiState>,
) : DestinationModalController {
  var requested: ProfileSheetKind = ProfileSheetKind.CURRENCY

  // Reads the snapshot State so shell feedback recomposes when a sheet opens or hides.
  override val hasActiveSession: Boolean
    get() = state.value.sheet != null

  override fun open(): Boolean = viewModel.onOpenSheet(requested)
}
