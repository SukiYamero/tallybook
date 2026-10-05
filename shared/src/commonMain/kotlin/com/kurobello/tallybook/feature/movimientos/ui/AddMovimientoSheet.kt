package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetLayout
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialog
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialogCopy
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.core.ui.component.rememberDismissGuard
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoEffect
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoUiState
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoViewModel
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.add_movimiento_pane_title
import tallybook.shared.generated.resources.continue_editing
import tallybook.shared.generated.resources.discard_changes_confirm
import tallybook.shared.generated.resources.discard_changes_message
import tallybook.shared.generated.resources.discard_changes_title

private const val MOVIMIENTO_COMPACT_DETENT = "add-movimiento-compact"
private const val MOVIMIENTO_DETAILS_DETENT = "add-movimiento-details"

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
  var detailsExpanded by rememberSaveable { mutableStateOf(false) }
  var isCategoryLayerReady by rememberSaveable { mutableStateOf(false) }
  val showForm =
      isCategoryLayerReady || uiState.selectedCategoriaId != null || uiState.error != null
  val dismissGuard =
      rememberDismissGuard(
          hasUnsavedChanges = uiState.hasUnsavedChanges,
          isDismissEnabled = !uiState.isSubmitting,
          onDiscardChanges = actions::onDiscardChanges,
          onDismiss = callbacks.onDismiss,
      )
  TallybookModalBottomSheet(
      visible = visible,
      paneTitle = stringResource(Res.string.add_movimiento_pane_title),
      callbacks =
          callbacks.copy(
              onDismissRequest = dismissGuard::requestDismiss,
          ),
      layout =
          ModalBottomSheetLayout(
              modifier = modifier.preparedFormVisibility(showForm),
              contentDetentIdentifiers =
                  listOf(MOVIMIENTO_COMPACT_DETENT, MOVIMIENTO_DETAILS_DETENT),
              activeContentDetentIdentifier = addMovimientoDetentIdentifier(detailsExpanded),
          ),
  ) {
    MovimientoForm(
        uiState = uiState,
        actions = actions,
        startWithCategory = visible,
        onCategoryLayerReady = { isCategoryLayerReady = true },
        detailsExpanded = detailsExpanded,
        onDetailsExpandedChange = { detailsExpanded = it },
        modifier = Modifier,
    )
  }
  TallybookConfirmationDialog(
      visible = dismissGuard.isConfirmationVisible,
      copy =
          TallybookConfirmationDialogCopy(
              title = stringResource(Res.string.discard_changes_title),
              message = stringResource(Res.string.discard_changes_message),
              confirmLabel = stringResource(Res.string.discard_changes_confirm),
              dismissLabel = stringResource(Res.string.continue_editing),
          ),
      onConfirm = dismissGuard::confirmDiscard,
      onDismiss = dismissGuard::continueEditing,
  )
}

private fun Modifier.preparedFormVisibility(isVisible: Boolean): Modifier =
    alpha(if (isVisible) 1f else 0f)
        .focusProperties { canFocus = isVisible }
        .semantics { if (!isVisible) hideFromAccessibility() }
        .then(
            if (isVisible) Modifier
            else
                Modifier.pointerInput(Unit) {
                  awaitPointerEventScope {
                    while (true) {
                      awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                  }
                }
        )

private fun addMovimientoDetentIdentifier(detailsExpanded: Boolean): String =
    if (detailsExpanded) MOVIMIENTO_DETAILS_DETENT else MOVIMIENTO_COMPACT_DETENT

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
