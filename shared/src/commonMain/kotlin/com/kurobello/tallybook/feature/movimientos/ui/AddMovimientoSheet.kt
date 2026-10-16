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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.clearFocusOnUnconsumedTap
import com.kurobello.tallybook.core.ui.component.needsManualInputReveal
import com.kurobello.tallybook.core.ui.component.rememberDismissGuard
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoEffect
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoStep
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoUiState
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoViewModel
import com.kurobello.tallybook.feature.movimientos.viewmodel.PhrasePreviewUi
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.add_movimiento_pane_title
import tallybook.shared.generated.resources.continue_editing
import tallybook.shared.generated.resources.discard_changes_confirm
import tallybook.shared.generated.resources.discard_changes_message
import tallybook.shared.generated.resources.discard_changes_title

// One detent per step lets the sheet animate between step heights; growth inside a step (keyboard,
// error message) is tracked by the sheet itself.
private val STEP_DETENTS: Map<AddMovimientoStep, String> =
    mapOf(
        AddMovimientoStep.PHRASE to "add-movimiento-phrase",
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
  val fadeMillis = TallybookTheme.motion.fadeMillis
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
              onBack = {
                if (uiState.stepBackTarget != null) actions.onStepBack()
                else dismissGuard.requestDismiss()
              },
          ),
      layout =
          ModalBottomSheetLayout(
              modifier = modifier,
              contentDetentIdentifiers = STEP_DETENTS.values.toList(),
              activeContentDetentIdentifier = STEP_DETENTS.getValue(uiState.step),
          ),
  ) {
    AnimatedContent(
        targetState = uiState.step,
        transitionSpec = { stepTransition(animated = visible, fadeMillis = fadeMillis) },
        label = "add movimiento step",
    ) { step ->
      when (step) {
        AddMovimientoStep.PHRASE -> PhraseStepContent(uiState, actions)
        AddMovimientoStep.SUMMARY -> SummaryStepContent(uiState, actions)
        AddMovimientoStep.FORM -> MovimientoForm(uiState = uiState, actions = actions)
      }
    }
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
private fun PhraseStepContent(uiState: AddMovimientoUiState, actions: AddMovimientoActions) {
  val focusManager = LocalFocusManager.current
  val focusRequester = remember { FocusRequester() }
  StepColumn(modifier = if (needsManualInputReveal) Modifier.imePadding() else Modifier) {
    MovimientoPhraseStep(
        text = uiState.phrase,
        preview = uiState.preview ?: PhrasePreviewUi.Empty,
        moneda = uiState.moneda,
        onTextChange = actions::onPhraseChanged,
        onContinue = {
          focusManager.clearFocus(force = true)
          actions.onPhraseContinue()
        },
        onFillManually = {
          focusManager.clearFocus(force = true)
          actions.onFillManually()
        },
        focusRequester = focusRequester,
        today = uiState.today,
        enabled = !uiState.isSubmitting,
        actionsEnabled = !uiState.isLoading && uiState.loadError == null,
    )
    MovimientoFormErrorMessage(
        error = uiState.loadError,
        onRetry = actions::onRetryLoad,
        interactionEnabled = !uiState.isSubmitting,
    )
  }
}

@Composable
private fun SummaryStepContent(uiState: AddMovimientoUiState, actions: AddMovimientoActions) {
  val summary = uiState.summary ?: return
  StepColumn {
    MovimientoSummaryStep(
        summary = summary,
        onSelectSuggestion = { actions.onSuggestionSelected(it.id) },
        onEdit = actions::onEditMovimiento,
        onSave = actions::onSubmit,
        today = uiState.today,
    )
    MovimientoFormErrorMessage(
        error = uiState.error,
        onRetry = actions::onRetryLoad,
        interactionEnabled = !uiState.isSubmitting,
    )
  }
}

@Composable
private fun StepColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(horizontal = TallybookTheme.dimensions.screenHorizontal)
              .padding(bottom = TallybookTheme.spacing.space8)
              .clearFocusOnUnconsumedTap(),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
  ) {
    TallybookSheetTitle(stringResource(Res.string.add_movimiento_pane_title))
    content()
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
