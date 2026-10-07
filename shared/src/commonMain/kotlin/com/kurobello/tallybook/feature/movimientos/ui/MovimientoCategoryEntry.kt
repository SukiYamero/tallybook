package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormState

private enum class CategoryEntryPhase {
  NOT_STARTED,
  CHOOSING,
  CLOSING_AFTER_SELECTION,
  CLOSING_AFTER_DISMISSAL,
  FOCUSING_AMOUNT,
  EDITING,
}

internal data class MovimientoCategoryEntry(
    val isPickerVisible: Boolean,
    val blocksValidationFocus: Boolean,
    val onOpen: () -> Unit,
    val onSelect: (String) -> Unit,
    val onDismiss: () -> Unit,
    val onHidden: () -> Unit,
)

@Composable
internal fun rememberMovimientoCategoryEntry(
    uiState: MovimientoFormState,
    actions: MovimientoFormActions,
    amountFocusRequester: FocusRequester,
    policy: MovimientoCategoryEntryPolicy = MovimientoCategoryEntryPolicy(),
): MovimientoCategoryEntry {
  var phase by rememberSaveable { mutableStateOf(CategoryEntryPhase.NOT_STARTED) }
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current

  LaunchedEffect(policy.startWithCategory, uiState.isLoading, uiState.error) {
    if (phase != CategoryEntryPhase.NOT_STARTED) return@LaunchedEffect
    if (policy.startWithCategory && !uiState.isLoading && uiState.error == null) {
      phase =
          if (uiState.selectedCategoriaId == null) CategoryEntryPhase.CHOOSING
          else CategoryEntryPhase.EDITING
    }
  }
  LaunchedEffect(phase, uiState.selectedCategoriaId, uiState.isSubmitting) {
    if (
        phase == CategoryEntryPhase.FOCUSING_AMOUNT &&
            uiState.selectedCategoriaId != null &&
            !uiState.isSubmitting
    ) {
      withFrameNanos {}
      amountFocusRequester.requestFocus()
      keyboardController?.show()
      phase = CategoryEntryPhase.EDITING
    }
  }

  return MovimientoCategoryEntry(
      isPickerVisible = phase == CategoryEntryPhase.CHOOSING,
      blocksValidationFocus =
          phase != CategoryEntryPhase.EDITING && phase != CategoryEntryPhase.NOT_STARTED,
      onOpen = {
        if (policy.interactionEnabled) {
          focusManager.clearFocus(force = true)
          phase = CategoryEntryPhase.CHOOSING
        }
      },
      onSelect = {
        actions.onCategoriaSelected(it)
        phase = CategoryEntryPhase.CLOSING_AFTER_SELECTION
      },
      onDismiss = {
        if (phase == CategoryEntryPhase.CHOOSING)
            phase =
                if (policy.focusAmountAfterSelection) CategoryEntryPhase.EDITING
                else CategoryEntryPhase.CLOSING_AFTER_DISMISSAL
      },
      onHidden = { phase = phase.afterHidden(policy.focusAmountAfterSelection) },
  )
}

private fun CategoryEntryPhase.afterHidden(focusAmount: Boolean): CategoryEntryPhase =
    when (this) {
      CategoryEntryPhase.CLOSING_AFTER_DISMISSAL -> CategoryEntryPhase.EDITING
      CategoryEntryPhase.CLOSING_AFTER_SELECTION ->
          if (focusAmount) CategoryEntryPhase.FOCUSING_AMOUNT else CategoryEntryPhase.EDITING
      else -> this
    }
