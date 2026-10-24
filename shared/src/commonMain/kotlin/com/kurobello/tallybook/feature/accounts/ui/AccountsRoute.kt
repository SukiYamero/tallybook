package com.kurobello.tallybook.feature.accounts.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.ui.component.TallybookScreenSnackbar
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarDuration
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarMessage
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarStatus
import com.kurobello.tallybook.core.ui.navigation.DestinationModalController
import com.kurobello.tallybook.core.ui.navigation.PrimaryModalCoordinator
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountSheetRequest
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountsActions
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountsEffect
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountsFeedback
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountsUiState
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountsViewModel
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.getString
import org.koin.compose.viewmodel.koinViewModel
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.account_archived_feedback
import tallybook.shared.generated.resources.account_created
import tallybook.shared.generated.resources.account_deleted
import tallybook.shared.generated.resources.account_unarchived_feedback
import tallybook.shared.generated.resources.account_updated

/**
 * [onTransfer] opens the transfer between the user's accounts once the account sheet hid; while it
 * is null the sheet offers no transfer actions.
 */
@Composable
internal fun AccountsRoute(
    modals: PrimaryModalCoordinator,
    active: Boolean,
    onBack: () -> Unit,
    onTransfer: ((origenId: String?, destinoId: String?, prefill: Money?) -> Unit)?,
    modifier: Modifier = Modifier,
) {
  val viewModel: AccountsViewModel = koinViewModel()
  val state = viewModel.uiState.collectAsStateWithLifecycle()
  val uiState by state
  val controller = remember(viewModel, state) { AccountSheetController(viewModel, state) }
  DisposableEffect(modals, controller, active) {
    if (active) modals.attach(controller)
    onDispose { modals.detach(controller) }
  }
  LifecycleResumeEffect(viewModel) {
    viewModel.onResume()
    onPauseOrDispose {}
  }
  val actions =
      remember(viewModel, modals, controller) {
        object : AccountsActions by viewModel {
          override fun onCreate() = open(AccountSheetRequest.New(viewModel.uiState.value.query))

          override fun onOpenCuenta(id: String) = open(AccountSheetRequest.Open(id))

          private fun open(request: AccountSheetRequest) {
            controller.requested = request
            modals.tryOpenDestinationModal(controller)
          }
        }
      }
  var snackbar by remember { mutableStateOf<TallybookSnackbarMessage?>(null) }
  var nextSnackbarToken by remember { mutableLongStateOf(0L) }
  val latestTransfer = rememberUpdatedState(onTransfer)
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  LaunchedEffect(viewModel, lifecycle) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      viewModel.effects.collect { effect ->
        when (effect) {
          is AccountsEffect.Transfer -> {
            // The shell refuses new modals while one is open, and the collected State can trail
            // the ViewModel's by a frame.
            snapshotFlow { state.value.sheet == null }.first { it }
            latestTransfer.value?.invoke(effect.origenId, effect.destinoId, effect.prefill)
          }
          is AccountsEffect.Feedback ->
              snackbar = effect.feedback.toSnackbarMessage(++nextSnackbarToken)
        }
      }
    }
  }
  Box(modifier = modifier.fillMaxSize()) {
    AccountsScreen(uiState, actions, onBack, canTransfer = onTransfer != null)
    TallybookScreenSnackbar(
        message = snackbar,
        isActive = uiState.sheet == null,
        onDismiss = { token -> if (snackbar?.token == token) snackbar = null },
    )
  }
}

private suspend fun AccountsFeedback.toSnackbarMessage(token: Long): TallybookSnackbarMessage {
  val text =
      when (this) {
        AccountsFeedback.Created -> getString(Res.string.account_created)
        AccountsFeedback.Updated -> getString(Res.string.account_updated)
        AccountsFeedback.Deleted -> getString(Res.string.account_deleted)
        is AccountsFeedback.Archived -> getString(Res.string.account_archived_feedback, nombre)
        is AccountsFeedback.Unarchived -> getString(Res.string.account_unarchived_feedback, nombre)
      }
  return TallybookSnackbarMessage(
      token = token,
      revision = 0,
      message = text,
      duration = TallybookSnackbarDuration.Short,
      status = TallybookSnackbarStatus.Success,
  )
}

private class AccountSheetController(
    private val viewModel: AccountsViewModel,
    private val state: State<AccountsUiState>,
) : DestinationModalController {
  var requested: AccountSheetRequest = AccountSheetRequest.New("")

  // Reads the snapshot State so shell navigation recomposes when the sheet opens or hides.
  override val hasActiveSession: Boolean
    get() = state.value.sheet != null

  override fun open(): Boolean = viewModel.openSheet(requested)
}
