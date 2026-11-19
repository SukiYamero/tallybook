package com.kurobello.tallybook.feature.budgets.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.ui.navigation.DestinationModalController
import com.kurobello.tallybook.core.ui.navigation.PrimaryModalCoordinator
import com.kurobello.tallybook.feature.budgets.viewmodel.BudgetsActions
import com.kurobello.tallybook.feature.budgets.viewmodel.BudgetsEffect
import com.kurobello.tallybook.feature.budgets.viewmodel.BudgetsUiState
import com.kurobello.tallybook.feature.budgets.viewmodel.BudgetsViewModel
import kotlinx.coroutines.flow.first
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun BudgetsRoute(
    modals: PrimaryModalCoordinator,
    active: Boolean,
    onBack: () -> Unit,
    onViewMovements: (categoriaId: String, moneda: Moneda, range: MovimientoDateRange) -> Unit,
    modifier: Modifier = Modifier,
) {
  val viewModel: BudgetsViewModel = koinViewModel()
  val state = viewModel.uiState.collectAsStateWithLifecycle()
  val uiState by state
  val controller = remember(viewModel, state) { BudgetSheetController(viewModel, state) }
  DisposableEffect(modals, controller, active) {
    if (active) modals.attach(controller)
    onDispose { modals.detach(controller) }
  }
  LifecycleResumeEffect(viewModel) {
    viewModel.onResume()
    onPauseOrDispose {}
  }
  val actions =
      remember(viewModel, modals, controller, state) {
        object : BudgetsActions by viewModel {
          override fun onCreate() {
            controller.requested = BudgetSheetRequest.Create
            modals.tryOpenDestinationModal(controller)
          }

          override fun onOpen(id: String) {
            // Reloading the open budget's history happens under its own sheet, which the shell
            // already counts as a modal and would refuse to admit again.
            if (state.value.detail?.status?.presupuesto?.id == id) {
              viewModel.onOpen(id)
            } else {
              controller.requested = BudgetSheetRequest.Open(id)
              modals.tryOpenDestinationModal(controller)
            }
          }
        }
      }
  val latestViewMovements = rememberUpdatedState(onViewMovements)
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  LaunchedEffect(viewModel, lifecycle) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      viewModel.effects.collect { effect ->
        when (effect) {
          is BudgetsEffect.OpenSearch -> {
            // The shell refuses to navigate while a modal is open, and the collected State can
            // trail the ViewModel's by a frame.
            snapshotFlow { !state.value.hasSheet }.first { it }
            latestViewMovements.value(effect.categoriaId, effect.moneda, effect.range)
          }
        }
      }
    }
  }
  BudgetsScreen(uiState, actions, onBack, modifier)
}

private val BudgetsUiState.hasSheet: Boolean
  get() = editor != null || detail != null

private sealed interface BudgetSheetRequest {
  data object Create : BudgetSheetRequest

  data class Open(val id: String) : BudgetSheetRequest
}

private class BudgetSheetController(
    private val viewModel: BudgetsViewModel,
    private val state: State<BudgetsUiState>,
) : DestinationModalController {
  var requested: BudgetSheetRequest? = null

  // Reads the snapshot State so shell navigation recomposes when a sheet opens or closes.
  override val hasActiveSession: Boolean
    get() = state.value.hasSheet

  override fun open(): Boolean {
    when (val request = requested) {
      null -> return false
      BudgetSheetRequest.Create -> viewModel.onCreate()
      is BudgetSheetRequest.Open -> viewModel.onOpen(request.id)
    }
    return viewModel.uiState.value.hasSheet
  }
}
