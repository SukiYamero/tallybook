package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import com.kurobello.tallybook.core.ui.theme.TallybookDimensions
import com.kurobello.tallybook.core.ui.theme.TallybookSpacing
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoUiState
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.add_movimiento_pane_title
import tallybook.shared.generated.resources.movimiento_part_done

@Composable
internal fun SummaryStepContent(
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
              cuenta = actions.cuentaCallbacks(pickers.onChooseCuenta, pickers.onCreateCuenta),
              onEdit = actions::onEditMovimiento,
              onSave = actions::onSubmit,
          ),
      today = uiState.today,
      modifier = Modifier.entryStepContent(rememberReviewMinimumHeight()),
      supportingContent = { EntryRepetirField(uiState, actions) },
      messageContent = { EntryStepError(uiState, actions) },
  )
}

@Composable
internal fun ReviewStepContent(
    uiState: AddMovimientoUiState,
    actions: AddMovimientoActions,
    pickers: EntryPickers,
    onPartCategoria: (Int) -> Unit,
) {
  MovimientoReviewStep(
      review = uiState.review,
      callbacks =
          MovimientoReviewCallbacks(
              etiquetas = actions.etiquetasCallbacks(onAdd = pickers.onAddEtiqueta),
              cuenta = actions.cuentaCallbacks(pickers.onChooseCuenta, pickers.onCreateCuenta),
              onPartEdit = actions::onPartEdit,
              onPartCategoria = onPartCategoria,
              onPartRemoved = actions::onPartRemoved,
              onJoin = actions::onJoinParts,
              onSave = actions::onSubmit,
          ),
      today = uiState.today,
      modifier = Modifier.testTag(REVIEW_STEP_TAG).entryStepContent(rememberReviewMinimumHeight()),
      supportingContent = { EntryRepetirField(uiState, actions) },
      messageContent = { EntryStepError(uiState, actions) },
  )
}

/**
 * The full form. For a review part its primary action is Done, which returns to the review without
 * saving, and Repetir stays with the review, which applies it to every part.
 */
@Composable
internal fun EntryFormContent(
    uiState: AddMovimientoUiState,
    actions: AddMovimientoActions,
    pickers: EntryPickers,
    categoryCreation: CategoryPickerCreation?,
) {
  val editsPart = uiState.editingPart != null
  MovimientoForm(
      uiState = uiState,
      actions = actions,
      supportingContent =
          if (editsPart) null
          else {
            { EntryRepetirField(uiState, actions) }
          },
      footerContent =
          if (editsPart) {
            { PartDoneButton(uiState, actions) }
          } else null,
      categoryCreation = categoryCreation,
      onDeselectCategoria = actions::onCategoriaCleared,
      onCreateCuenta = pickers.onCreateCuenta,
  )
}

@Composable
private fun PartDoneButton(uiState: AddMovimientoUiState, actions: AddMovimientoActions) {
  MovimientoFormSubmit(
      uiState = uiState,
      onSubmit = actions::onPartDone,
      presentation =
          MovimientoFormPresentation(
              title = stringResource(Res.string.add_movimiento_pane_title),
              submitLabel = stringResource(Res.string.movimiento_part_done),
          ),
      interactionEnabled = true,
      modifier = Modifier.padding(bottom = TallybookTheme.spacing.space8),
  )
}

@Composable
private fun EntryStepError(uiState: AddMovimientoUiState, actions: AddMovimientoActions) {
  MovimientoFormErrorMessage(
      error = uiState.error,
      onRetry = actions::onRetryLoad,
      interactionEnabled = !uiState.isSubmitting,
  )
}

private fun Modifier.entryStepContent(minHeight: Dp): Modifier =
    heightIn(min = minHeight)
        .padding(horizontal = TallybookDimensions.screenHorizontal)
        .padding(bottom = TallybookSpacing.space8)
