package com.kurobello.tallybook.feature.budgets.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetLayout
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.feature.budgets.viewmodel.BudgetDetailState
import com.kurobello.tallybook.feature.budgets.viewmodel.BudgetEditorState
import com.kurobello.tallybook.feature.budgets.viewmodel.BudgetsActions
import com.kurobello.tallybook.feature.budgets.viewmodel.BudgetsUiState
import com.kurobello.tallybook.feature.movimientos.ui.CategoryPickerSheet
import com.kurobello.tallybook.feature.movimientos.ui.pickerExcludedCategoryIds
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.budget_edit_title
import tallybook.shared.generated.resources.budget_new_title

private const val EDITOR_DETENT = "editor"
private const val DETAIL_DETENT = "detail"
private val SHEET_DETENTS = listOf(EDITOR_DETENT, DETAIL_DETENT)

private sealed interface BudgetSheet {
  data class Editor(val state: BudgetEditorState) : BudgetSheet

  data class Detail(val state: BudgetDetailState) : BudgetSheet
}

/**
 * One sheet for the editor and the detail, so changing the monto from a budget swaps the content in
 * place instead of stacking a second modal. Its content stays while it slides away and clears in
 * [BudgetsActions.onSheetHidden].
 */
@Composable
internal fun BudgetSheetHost(uiState: BudgetsUiState, actions: BudgetsActions) {
  val shown = uiState.editor?.let(BudgetSheet::Editor) ?: uiState.detail?.let(BudgetSheet::Detail)
  var pickerVisible by rememberSaveable { mutableStateOf(false) }
  val focusManager = LocalFocusManager.current
  if (shown != null) {
    BudgetSheet(shown, visible = !uiState.sheetClosing, uiState, actions) {
      focusManager.clearFocus(force = true)
      pickerVisible = true
    }
  }
  val creating = uiState.editor?.takeIf { it.editingId == null }
  LaunchedEffect(creating == null) { if (creating == null) pickerVisible = false }
  CategoryPickerSheet(
      visible = pickerVisible && creating != null && !uiState.sheetClosing,
      categorias = uiState.categorias,
      selectedId = creating?.categoriaId,
      onSelect = actions::onCategorySelected,
      onDismiss = { pickerVisible = false },
      excludedIds = pickerExcludedCategoryIds(uiState.categorias, creating?.categoriaId),
      onBack = { pickerVisible = false },
  )
  BudgetDeleteDialog(
      visible =
          !uiState.sheetClosing &&
              uiState.editor == null &&
              uiState.detail?.let { it.confirmingDelete && !it.deleting } == true,
      actions = actions,
  )
}

@Composable
private fun BudgetSheet(
    shown: BudgetSheet,
    visible: Boolean,
    uiState: BudgetsUiState,
    actions: BudgetsActions,
    onChooseCategory: () -> Unit,
) {
  val dismiss = {
    if (uiState.editor != null) actions.onDismissEditor() else actions.onDismissDetail()
  }
  val title = sheetTitle(shown, uiState)
  TallybookModalBottomSheet(
      visible = visible,
      paneTitle = title,
      callbacks =
          ModalBottomSheetCallbacks(
              onDismiss = dismiss,
              onDismissRequest = {
                dismiss()
                false
              },
              onHidden = actions::onSheetHidden,
          ),
      layout =
          ModalBottomSheetLayout(
              contentDetentIdentifiers = SHEET_DETENTS,
              activeContentDetentIdentifier =
                  if (shown is BudgetSheet.Editor) EDITOR_DETENT else DETAIL_DETENT,
          ),
  ) {
    when (shown) {
      is BudgetSheet.Editor ->
          key(shown.state.editingId) {
            BudgetEditorContent(
                editor = shown.state,
                categorias = uiState.categorias,
                title = title,
                actions = actions,
                onChooseCategory = onChooseCategory,
            )
          }
      is BudgetSheet.Detail ->
          BudgetDetailContent(
              detail = shown.state,
              categoria =
                  uiState.categorias.find { it.id == shown.state.status.presupuesto.categoriaId },
              primaryMoneda = uiState.primaryMoneda,
              actions = actions,
          )
    }
  }
}

@Composable
private fun sheetTitle(shown: BudgetSheet, uiState: BudgetsUiState): String =
    when (shown) {
      is BudgetSheet.Editor ->
          stringResource(
              if (shown.state.editingId == null) Res.string.budget_new_title
              else Res.string.budget_edit_title
          )
      is BudgetSheet.Detail -> budgetName(shown.state.status, uiState.primaryMoneda)
    }
