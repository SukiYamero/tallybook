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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kurobello.tallybook.core.ui.component.DismissGuardState
import com.kurobello.tallybook.core.ui.component.LastWhileVisible
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetLayout
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialog
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialogCopy
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.core.ui.component.TallybookModalDialog
import com.kurobello.tallybook.core.ui.component.rememberDismissGuard
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.categories.viewmodel.QuickCategoryViewModel
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
@Suppress("LongParameterList")
private fun AddMovimientoSheet(
    visible: Boolean,
    uiState: AddMovimientoUiState,
    actions: AddMovimientoActions,
    callbacks: ModalBottomSheetCallbacks,
    modifier: Modifier = Modifier,
    categoryCreation: CategoryPickerCreation? = null,
) {
  val dismissGuard =
      rememberDismissGuard(
          hasUnsavedChanges = uiState.hasUnsavedChanges,
          isDismissEnabled = !uiState.isSubmitting,
          onDiscardChanges = actions::onDiscardChanges,
          onDismiss = callbacks.onDismiss,
      )
  val pickerStates =
      EntryPickerStates(
          category = rememberMovimientoPicker(interactionEnabled = !uiState.isSubmitting),
          etiqueta = rememberMovimientoPicker(interactionEnabled = !uiState.isSubmitting),
          cuenta = rememberMovimientoPicker(interactionEnabled = !uiState.isSubmitting),
      )
  val pickers = pickerStates.callbacks
  val isSentence = uiState.step == AddMovimientoStep.PHRASE
  // Either surface also hides when the flow moves between them; only a closed session is final.
  val currentVisible by rememberUpdatedState(visible)
  val onHidden: () -> Unit = { if (!currentVisible) callbacks.onHidden() }
  MovimientoSentenceDialog(
      visible = visible && isSentence,
      uiState = uiState,
      actions = actions,
      pickers = pickers,
      onDismissRequest = dismissGuard::requestDismiss,
      onHidden = onHidden,
  )
  MovimientoReviewSheet(
      visible = visible && !isSentence,
      uiState = uiState,
      actions = actions,
      pickers = pickers,
      categoryCreation = categoryCreation,
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
  EntryPickerSheets(uiState, actions, pickerStates, categoryCreation)
  DiscardEntryConfirmation(dismissGuard)
}

@Composable
private fun DiscardEntryConfirmation(dismissGuard: DismissGuardState) {
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
    pickers: EntryPickers,
    onDismissRequest: () -> Boolean,
    onHidden: () -> Unit,
) {
  val focusManager = LocalFocusManager.current
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
          onDeselectCategoria = actions::onCategoriaCleared,
          onChooseCategoria = pickers.onChooseCategoria,
          onSuggestion = { actions.onSuggestionSelected(it.id) },
          etiquetas = actions.etiquetasCallbacks(onAdd = pickers.onAddEtiqueta),
          cuenta = actions.cuentaCallbacks(onOpenPicker = pickers.onChooseCuenta),
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
    pickers: EntryPickers,
    categoryCreation: CategoryPickerCreation?,
    callbacks: ModalBottomSheetCallbacks,
    modifier: Modifier = Modifier,
) {
  val fadeMillis = TallybookTheme.motion.fadeMillis
  // Back and Discard reset the draft while the sheet is already leaving; it keeps the content the
  // user was looking at for the exit animation.
  val shown = remember { LastWhileVisible(uiState) }.resolve(visible, uiState)
  val step = rememberSheetStep(shown.step)
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
      if (current == AddMovimientoStep.FORM) {
        MovimientoForm(
            uiState = shown,
            actions = actions,
            categoryCreation = categoryCreation,
            onDeselectCategoria = actions::onCategoriaCleared,
        )
      } else SummaryStepContent(shown, actions, pickers)
    }
  }
}

// The sheet composes before the review is ever shown, while the step still reads PHRASE, which has
// no detent; the review step is the stand-in until a real one arrives.
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
    pickers: EntryPickers,
) {
  MovimientoSummaryStep(
      summary = uiState.summary,
      callbacks =
          MovimientoSummaryCallbacks(
              onDeselectCategoria = actions::onCategoriaCleared,
              onChooseCategoria = pickers.onChooseCategoria,
              onSuggestion = { actions.onSuggestionSelected(it.id) },
              etiquetas = actions.etiquetasCallbacks(onAdd = pickers.onAddEtiqueta),
              cuenta = actions.cuentaCallbacks(onOpenPicker = pickers.onChooseCuenta),
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
@Suppress("LongParameterList")
internal fun AddMovimientoSheetRoute(
    visible: Boolean,
    onDismiss: () -> Unit,
    onHidden: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AddMovimientoViewModel = koinViewModel(),
    quickCategoryViewModel: QuickCategoryViewModel =
        rememberQuickCategoryViewModel(QUICK_CATEGORY_ADD_KEY),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val quickCategoryState by quickCategoryViewModel.uiState.collectAsStateWithLifecycle()
  AddMovimientoSavedEffect(effects = viewModel.effects, onSaved = onDismiss)
  QuickCategorySelectionEffect(
      effects = quickCategoryViewModel.effects,
      selection =
          CategoryPickerSelection(
              selectedId = uiState.categoriaId.realCategoriaId(),
              onSelect = viewModel::onCategoriaSelected,
          ),
      catalog = uiState.categorias,
  )
  AddMovimientoSheet(
      visible = visible,
      uiState = uiState,
      actions = viewModel,
      callbacks = ModalBottomSheetCallbacks(onDismiss = onDismiss, onHidden = onHidden),
      modifier = modifier,
      categoryCreation = CategoryPickerCreation(quickCategoryState, quickCategoryViewModel),
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
