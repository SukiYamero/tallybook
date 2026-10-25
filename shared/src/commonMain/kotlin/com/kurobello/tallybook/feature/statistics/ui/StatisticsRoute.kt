package com.kurobello.tallybook.feature.statistics.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.StatisticsKey
import com.kurobello.tallybook.core.ui.navigation.DestinationModalController
import com.kurobello.tallybook.core.ui.navigation.PrimaryModalCoordinator
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsActions
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsEffect
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsUiState
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsViewModel
import kotlinx.coroutines.flow.first
import org.koin.compose.viewmodel.koinViewModel

/**
 * [onOpenSearch] receives a gasto filter for Search; [onOpenCreation] is the same entry the shell's
 * "+" opens. While [active], the category sheet opens through [modals] like every shell modal.
 */
@Suppress("LongParameterList")
@Composable
internal fun StatisticsRoute(
    onOpenSearch: (StatisticsKey, Moneda, MovimientoDateRange) -> Unit,
    onOpenCreation: () -> Unit,
    clearance: Dp,
    modals: PrimaryModalCoordinator,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
  val viewModel: StatisticsViewModel = koinViewModel()
  val state = viewModel.uiState.collectAsStateWithLifecycle()
  val uiState by state
  val controller = remember(viewModel, state) { CategoryDetailController(viewModel, state) }
  DisposableEffect(modals, controller, active) {
    if (active) modals.attach(controller)
    onDispose { modals.detach(controller) }
  }
  val actions =
      remember(viewModel, modals, controller) {
        object : StatisticsActions by viewModel {
          override fun onShareSelected(key: StatisticsKey) {
            if (key is StatisticsKey.Categoria) {
              controller.requested = key
              modals.tryOpenDestinationModal(controller)
            } else {
              viewModel.onShareSelected(key)
            }
          }
        }
      }
  val latestOpenSearch by rememberUpdatedState(onOpenSearch)
  val latestOpenCreation by rememberUpdatedState(onOpenCreation)
  LifecycleResumeEffect(viewModel) {
    viewModel.onResume()
    onPauseOrDispose {}
  }
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  LaunchedEffect(viewModel, lifecycle) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      viewModel.effects.collect { effect ->
        when (effect) {
          is StatisticsEffect.OpenSearch -> {
            // The shell refuses to navigate while a modal is open, and the collected State can
            // trail the ViewModel's by a frame.
            snapshotFlow { state.value.detail == null }.first { it }
            latestOpenSearch(effect.key, effect.moneda, effect.range)
          }
          StatisticsEffect.OpenCreation -> latestOpenCreation()
        }
      }
    }
  }
  StatisticsScreen(uiState, actions, clearance, modifier)
}

private class CategoryDetailController(
    private val viewModel: StatisticsViewModel,
    private val state: State<StatisticsUiState>,
) : DestinationModalController {
  var requested: StatisticsKey.Categoria? = null

  // Reads the snapshot State so shell navigation recomposes when the sheet opens or hides.
  override val hasActiveSession: Boolean
    get() = state.value.detail != null

  override fun open(): Boolean {
    viewModel.onShareSelected(requested ?: return false)
    return viewModel.uiState.value.detail != null
  }
}
