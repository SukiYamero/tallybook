package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.Composable
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoStep
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoUiState

/** The pickers the sentence dialog and the review share; the form hosts its own. */
internal data class EntryPickers(
    val onChooseCategoria: () -> Unit,
    val onAddEtiqueta: () -> Unit,
    val onChooseCuenta: () -> Unit,
)

internal data class EntryPickerStates(
    val category: MovimientoPickerState,
    val etiqueta: MovimientoPickerState,
    val cuenta: MovimientoPickerState,
) {
  val callbacks: EntryPickers
    get() = EntryPickers(category.onOpen, etiqueta.onOpen, cuenta.onOpen)
}

/** The picker sheets the dialog and the review open, hidden while the form shows its own. */
@Composable
internal fun EntryPickerSheets(
    uiState: AddMovimientoUiState,
    actions: AddMovimientoActions,
    pickers: EntryPickerStates,
    categoryCreation: CategoryPickerCreation?,
) {
  val outsideForm = uiState.step != AddMovimientoStep.FORM
  MovimientoCategoryPickerSheet(
      visible = pickers.category.isVisible && outsideForm,
      uiState = uiState,
      onSelect = actions::onCategoriaSelected,
      picker = pickers.category,
      creation = categoryCreation,
  )
  MovimientoEtiquetaPickerSheet(
      visible = pickers.etiqueta.isVisible && outsideForm,
      uiState = uiState,
      actions = actions,
      picker = pickers.etiqueta,
  )
  MovimientoCuentaPickerSheet(
      visible = pickers.cuenta.isVisible && outsideForm,
      field = uiState.cuentaField,
      onSelect = actions::onCuentaSelected,
      picker = pickers.cuenta,
  )
}
