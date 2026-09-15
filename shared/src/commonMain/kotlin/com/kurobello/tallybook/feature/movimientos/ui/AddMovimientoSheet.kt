package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetLayout
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoEffect
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoUiState
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoViewModel
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.add_movimiento_pane_title

@Composable
internal fun AddMovimientoSheet(
    visible: Boolean,
    uiState: AddMovimientoUiState,
    actions: AddMovimientoActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) =
    AddMovimientoSheet(
        visible = visible,
        uiState = uiState,
        actions = actions,
        callbacks = ModalBottomSheetCallbacks(onDismiss = onDismiss),
        modifier = modifier,
    )

@Composable
private fun AddMovimientoSheet(
    visible: Boolean,
    uiState: AddMovimientoUiState,
    actions: AddMovimientoActions,
    callbacks: ModalBottomSheetCallbacks,
    modifier: Modifier = Modifier,
) {
  TallybookModalBottomSheet(
      visible = visible,
      paneTitle = stringResource(Res.string.add_movimiento_pane_title),
      callbacks = callbacks,
      layout = ModalBottomSheetLayout(modifier = modifier, fillAvailableHeight = true),
  ) {
    MovimientoForm(uiState = uiState, actions = actions, modifier = Modifier)
  }
}

@Composable
internal fun AddMovimientoSheetRoute(
    visible: Boolean,
    onDismiss: () -> Unit,
    onHidden: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AddMovimientoViewModel = koinViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  AddMovimientoSavedEffect(effects = viewModel.effects, onSaved = onDismiss)
  AddMovimientoSheet(
      visible = visible,
      uiState = uiState,
      actions = viewModel,
      callbacks = ModalBottomSheetCallbacks(onDismiss = onDismiss, onHidden = onHidden),
      modifier = modifier,
  )
}

@Composable
internal fun AddMovimientoSavedEffect(effects: Flow<AddMovimientoEffect>, onSaved: () -> Unit) {
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val currentOnSaved by rememberUpdatedState(onSaved)
  LaunchedEffect(effects, lifecycle) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      effects.collect { effect ->
        when (effect) {
          AddMovimientoEffect.Saved -> currentOnSaved()
        }
      }
    }
  }
}
