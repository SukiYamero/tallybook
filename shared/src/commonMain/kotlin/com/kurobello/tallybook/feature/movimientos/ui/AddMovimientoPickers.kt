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
    val onCreateCuenta: (() -> Unit)? = null,
)

internal data class EntryPickerStates(
    val category: MovimientoPickerState,
    val etiqueta: MovimientoPickerState,
    val cuenta: MovimientoPickerState,
) {
  fun callbacks(onCreateCuenta: (() -> Unit)?): EntryPickers =
      EntryPickers(category.onOpen, etiqueta.onOpen, cuenta.onOpen, onCreateCuenta)
}

/**
 * The picker sheets the dialog and the review open, hidden while the form shows its own. In the
 * review the category picker serves the part [categoryTarget] names, through that part's sheet.
 */
@Composable
internal fun EntryPickerSheets(
    uiState: AddMovimientoUiState,
    actions: AddMovimientoActions,
    pickers: EntryPickerStates,
    categoryCreation: CategoryPickerCreation?,
    categoryTarget: ReviewCategoryTarget,
) {
  val outsideForm = uiState.step != AddMovimientoStep.FORM
  val partIndex = categoryTarget.index?.takeIf { uiState.step == AddMovimientoStep.REVIEW }
  ReviewPartCategorySheet(
      visible = partIndex != null && !pickers.category.isVisible,
      part = partIndex?.let { uiState.review.parts.getOrNull(it) },
      enabled = !uiState.isSubmitting,
      callbacks =
          ReviewPartCategoryCallbacks(
              onSuggestion = { categoria ->
                partIndex?.let {
                  categoryTarget.close()
                  actions.onPartCategoriaSelected(it, categoria.id)
                }
              },
              onDeselect = { partIndex?.let(actions::onPartCategoriaCleared) },
              onChoose = pickers.category.onOpen,
              onDismiss = categoryTarget::close,
          ),
  )
  MovimientoCategoryPickerSheet(
      visible = pickers.category.isVisible && outsideForm,
      categorias = uiState.categorias,
      selection = uiState.categoryPickerSelection(actions, categoryTarget),
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
