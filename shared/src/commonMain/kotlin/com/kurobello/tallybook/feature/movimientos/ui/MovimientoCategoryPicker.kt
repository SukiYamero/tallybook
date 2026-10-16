package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID

// Otros is the fallback for "no category", so choosing it explicitly would defeat the pending flag.
internal val PICKER_EXCLUDED_CATEGORY_IDS: Set<String> = setOf(CATEGORIA_OTROS_ID)

internal data class MovimientoCategoryPickerState(
    val isVisible: Boolean,
    val onOpen: () -> Unit,
    val onDismiss: () -> Unit,
)

@Composable
internal fun rememberMovimientoCategoryPicker(
    interactionEnabled: Boolean
): MovimientoCategoryPickerState {
  var isVisible by rememberSaveable { mutableStateOf(false) }
  val focusManager = LocalFocusManager.current
  return MovimientoCategoryPickerState(
      isVisible = isVisible,
      onOpen = {
        if (interactionEnabled) {
          focusManager.clearFocus(force = true)
          isVisible = true
        }
      },
      onDismiss = { isVisible = false },
  )
}

/** The picker offers real categories only; the Otros fallback is not a selection. */
internal fun List<String>.realCategoriaIds(): List<String> = filterNot {
  it in PICKER_EXCLUDED_CATEGORY_IDS
}
