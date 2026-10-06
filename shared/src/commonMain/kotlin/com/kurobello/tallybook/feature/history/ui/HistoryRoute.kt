package com.kurobello.tallybook.feature.history.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.kurobello.tallybook.feature.history.viewmodel.HistoryUiState
import com.kurobello.tallybook.feature.history.viewmodel.HistoryViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.unknown_category

@Composable
internal fun HistoryRoute(
    onAdd: () -> Unit,
    bottomBarClearance: Dp,
    modifier: Modifier = Modifier,
) {
  val label = stringResource(Res.string.unknown_category)
  val viewModel: HistoryViewModel = koinViewModel { parametersOf(label) }
  val state = rememberHistoryRouteState(viewModel, label)
  HistoryScreen(state.value, viewModel, onAdd, bottomBarClearance, modifier)
}

@Composable
internal fun rememberHistoryRouteState(
    viewModel: HistoryViewModel,
    label: String,
): State<HistoryUiState> {
  val owner = LocalLifecycleOwner.current
  return key(viewModel, owner, label) {
    produceState(initialValue = viewModel.uiState.value.routeRevalidating()) {
      owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        try {
          viewModel.onPresentationChanged(label)
          viewModel.onResume()
          viewModel.uiState.collect { value = it }
        } finally {
          value = value.routeRevalidating()
        }
      }
    }
  }
}

private fun HistoryUiState.routeRevalidating(): HistoryUiState =
    copy(
        period = period.refreshing(),
        years = years.copy(isLoading = true),
    )
