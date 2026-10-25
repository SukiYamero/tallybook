package com.kurobello.tallybook.feature.statistics.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsEffect
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsViewModel
import kotlinx.coroutines.flow.first
import org.koin.compose.viewmodel.koinViewModel

/**
 * [onOpenSearch] receives a gasto filter for Search; [onOpenCreation] is the same entry the shell's
 * "+" opens.
 */
@Composable
internal fun StatisticsRoute(
    onOpenSearch: (StatisticsKey, Moneda, MovimientoDateRange) -> Unit,
    onOpenCreation: () -> Unit,
    clearance: Dp,
    modifier: Modifier = Modifier,
) {
  val viewModel: StatisticsViewModel = koinViewModel()
  val state = viewModel.uiState.collectAsStateWithLifecycle()
  val uiState by state
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
  StatisticsScreen(uiState, viewModel, clearance, modifier)
}
