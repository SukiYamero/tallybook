package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetLayout
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialog
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialogCopy
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.core.ui.component.TallybookModalDialog
import com.kurobello.tallybook.core.ui.component.rememberDismissGuard
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoEffect
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoStep
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

// One detent per sheet step lets the sheet animate between their heights; growth inside a step
// (keyboard, error message) is tracked by the sheet itself.
private val SHEET_STEP_DETENTS: Map<AddMovimientoStep, String> =
    mapOf(
        AddMovimientoStep.SUMMARY to "add-movimiento-summary",
        AddMovimientoStep.FORM to "add-movimiento-form",
    )

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
  val dismissGuard =
      rememberDismissGuard(
          hasUnsavedChanges = uiState.hasUnsavedChanges,
          isDismissEnabled = !uiState.isSubmitting,
          onDiscardChanges = actions::onDiscardChanges,
          onDismiss = callbacks.onDismiss,
      )
  val categoryPicker = rememberMovimientoCategoryPicker(interactionEnabled = !uiState.isSubmitting)
  val isSentence = uiState.step == AddMovimientoStep.PHRASE
  // Either surface also hides when the flow moves between them; only a closed session is final.
  val currentVisible by rememberUpdatedState(visible)
  val onHidden: () -> Unit = { if (!currentVisible) callbacks.onHidden() }
  MovimientoSentenceDialog(
      visible = visible && isSentence,
      uiState = uiState,
      actions = actions,
      onAddCategoria = categoryPicker.onOpen,
      onDismissRequest = dismissGuard::requestDismiss,
      onHidden = onHidden,
  )
  MovimientoReviewSheet(
      visible = visible && !isSentence,
      uiState = uiState,
      actions = actions,
      onAddCategoria = categoryPicker.onOpen,
      callbacks =
          callbacks.copy(
              onDismissRequest = dismissGuard::requestDismiss,
              onHidden = onHidden,
              onBack = {
                if (uiState.stepBackTarget != null) actions.onStepBack()
                else dismissGuard.requestDismiss()
              },
          ),
      modifier = modifier,
  )
  // The form hosts its own picker; this one serves the dialog and the review step.
  CategoryPickerSheet(
      visible = categoryPicker.isVisible && uiState.step != AddMovimientoStep.FORM,
      categorias = uiState.categorias,
      selectedIds = uiState.categoriaIds.realCategoriaIds(),
      onSelectionChange = actions::onCategoriasChanged,
      onDismiss = categoryPicker.onDismiss,
      onBack = categoryPicker.onDismiss,
      excludedIds = PICKER_EXCLUDED_CATEGORY_IDS,
  )
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

@Composable
@Suppress("LongParameterList")
private fun MovimientoSentenceDialog(
    visible: Boolean,
    uiState: AddMovimientoUiState,
    actions: AddMovimientoActions,
    onAddCategoria: () -> Unit,
    onDismissRequest: () -> Boolean,
    onHidden: () -> Unit,
) {
  val focusManager = LocalFocusManager.current
  val focusRequester = remember { FocusRequester() }
  val callbacks =
      MovimientoSentenceCallbacks(
          onTextChange = actions::onPhraseChanged,
          onContinue = {
            focusManager.clearFocus(force = true)
            actions.onPhraseContinue()
          },
          onFillManually = {
            focusManager.clearFocus(force = true)
            actions.onFillManually()
          },
          onRemoveCategoria = { actions.onCategoriaRemoved(it.id) },
          onAddCategoria = onAddCategoria,
          onSuggestion = { actions.onSuggestionSelected(it.id) },
      )
  TallybookModalDialog(
      visible = visible,
      paneTitle = stringResource(Res.string.add_movimiento_pane_title),
      onDismissRequest = { onDismissRequest() },
      onHidden = onHidden,
  ) {
    MovimientoSentenceStep(
        sentence = uiState.sentence,
        callbacks = callbacks,
        focusRequester = focusRequester,
        modifier = Modifier.weight(1f, fill = false),
    )
    MovimientoFormErrorMessage(
        error = uiState.loadError,
        onRetry = actions::onRetryLoad,
        interactionEnabled = !uiState.isSubmitting,
    )
  }
}

@Composable
@Suppress("LongParameterList")
private fun MovimientoReviewSheet(
    visible: Boolean,
    uiState: AddMovimientoUiState,
    actions: AddMovimientoActions,
    onAddCategoria: () -> Unit,
    callbacks: ModalBottomSheetCallbacks,
    modifier: Modifier = Modifier,
) {
  val fadeMillis = TallybookTheme.motion.fadeMillis
  val step = rememberSheetStep(uiState.step)
  TallybookModalBottomSheet(
      visible = visible,
      paneTitle = stringResource(Res.string.add_movimiento_pane_title),
      callbacks = callbacks,
      layout =
          ModalBottomSheetLayout(
              modifier = modifier,
              contentDetentIdentifiers = SHEET_STEP_DETENTS.values.toList(),
              activeContentDetentIdentifier = SHEET_STEP_DETENTS.getValue(step),
          ),
  ) {
    AnimatedContent(
        targetState = step,
        transitionSpec = { stepTransition(animated = visible, fadeMillis = fadeMillis) },
        label = "add movimiento step",
    ) { current ->
      if (current == AddMovimientoStep.FORM) MovimientoForm(uiState = uiState, actions = actions)
      else SummaryStepContent(uiState, actions, onAddCategoria)
    }
  }
}

// Back from the review hides the sheet while the step already reads PHRASE; keeping the last sheet
// step lets the exit animation play over the content the user was looking at.
@Composable
private fun rememberSheetStep(step: AddMovimientoStep): AddMovimientoStep {
  val last = remember { LastSheetStep() }
  return last.resolve(step)
}

private class LastSheetStep {
  private var value = AddMovimientoStep.SUMMARY

  fun resolve(step: AddMovimientoStep): AddMovimientoStep {
    if (step != AddMovimientoStep.PHRASE) value = step
    return value
  }
}

// The size snaps so the sheet's own detent animation is the only height motion: an animated size
// would still be growing when the detent settles and its capture would cap the content early. A
// discard resets the step while the sheet leaves, and animating that reset would flash content.
private fun stepTransition(animated: Boolean, fadeMillis: Int): ContentTransform =
    ContentTransform(
        targetContentEnter = if (animated) fadeIn(tween(fadeMillis)) else EnterTransition.None,
        initialContentExit = if (animated) fadeOut(tween(fadeMillis)) else ExitTransition.None,
        sizeTransform = SizeTransform(clip = true) { _, _ -> snap() },
    )

@Composable
private fun SummaryStepContent(
    uiState: AddMovimientoUiState,
    actions: AddMovimientoActions,
    onAddCategoria: () -> Unit,
) {
  val summary = uiState.summary ?: return
  MovimientoSummaryStep(
      summary = summary,
      callbacks =
          MovimientoSummaryCallbacks(
              onRemoveCategoria = { actions.onCategoriaRemoved(it.id) },
              onAddCategoria = onAddCategoria,
              onSuggestion = { actions.onSuggestionSelected(it.id) },
              onEdit = actions::onEditMovimiento,
              onSave = actions::onSubmit,
          ),
      today = uiState.today,
      modifier =
          Modifier.heightIn(min = rememberReviewMinimumHeight())
              .padding(horizontal = TallybookTheme.dimensions.screenHorizontal)
              .padding(bottom = TallybookTheme.spacing.space8),
      messageContent = {
        MovimientoFormErrorMessage(
            error = uiState.error,
            onRetry = actions::onRetryLoad,
            interactionEnabled = !uiState.isSubmitting,
        )
      },
  )
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
