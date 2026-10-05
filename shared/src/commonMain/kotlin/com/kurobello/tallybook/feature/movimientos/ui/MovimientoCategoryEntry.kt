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
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoUiState

private enum class CategoryEntryPhase {
  NOT_STARTED,
  CHOOSING,
  CLOSING_AFTER_SELECTION,
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
    uiState: AddMovimientoUiState,
    actions: AddMovimientoActions,
    startWithCategory: Boolean,
    amountFocusRequester: FocusRequester,
): MovimientoCategoryEntry {
  var phase by rememberSaveable { mutableStateOf(CategoryEntryPhase.NOT_STARTED) }
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current

  LaunchedEffect(startWithCategory, uiState.isLoading, uiState.error) {
    if (phase != CategoryEntryPhase.NOT_STARTED) return@LaunchedEffect
    if (startWithCategory && !uiState.isLoading && uiState.error == null) {
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
        focusManager.clearFocus(force = true)
        phase = CategoryEntryPhase.CHOOSING
      },
      onSelect = {
        actions.onCategoriaSelected(it)
        phase = CategoryEntryPhase.CLOSING_AFTER_SELECTION
      },
      onDismiss = { if (phase == CategoryEntryPhase.CHOOSING) phase = CategoryEntryPhase.EDITING },
      onHidden = {
        if (phase == CategoryEntryPhase.CLOSING_AFTER_SELECTION) {
          phase = CategoryEntryPhase.FOCUSING_AMOUNT
        }
      },
  )
}
