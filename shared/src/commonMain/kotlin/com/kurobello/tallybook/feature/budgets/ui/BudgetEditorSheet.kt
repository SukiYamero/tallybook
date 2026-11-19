package com.kurobello.tallybook.feature.budgets.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.ui.account.MonedaSelector
import com.kurobello.tallybook.core.ui.category.CategorySingleField
import com.kurobello.tallybook.core.ui.category.CategoryTag
import com.kurobello.tallybook.core.ui.component.TallybookAmountInput
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessage
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.component.TallybookSheetForm
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.component.message
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.budgets.viewmodel.BudgetEditorState
import com.kurobello.tallybook.feature.budgets.viewmodel.BudgetsActions
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.budget_amount_label
import tallybook.shared.generated.resources.budget_category_label
import tallybook.shared.generated.resources.budget_duplicate
import tallybook.shared.generated.resources.budget_save
import tallybook.shared.generated.resources.budget_save_error
import tallybook.shared.generated.resources.budget_suggestion
import tallybook.shared.generated.resources.cancel

/**
 * Creates a budget, or changes the monto of one: an existing budget keeps its categoria and moneda,
 * so neither is offered again.
 */
@Composable
internal fun BudgetEditorContent(
    editor: BudgetEditorState,
    categorias: List<Categoria>,
    title: String,
    actions: BudgetsActions,
    onChooseCategory: () -> Unit,
) {
  val enabled = !editor.saving
  val categoria = categorias.find { it.id == editor.categoriaId }
  TallybookSheetForm(
      header = { TallybookSheetTitle(title) },
      footer = { BudgetEditorFooter(editor, actions) },
  ) {
    if (editor.editingId == null) {
      BudgetTarget(editor, categoria, actions, onChooseCategory)
    } else if (categoria != null) {
      LockedCategory(categoria)
    }
    TallybookAmountInput(
        value = editor.amountInput,
        onValueChange = actions::onAmountChanged,
        label = stringResource(Res.string.budget_amount_label),
        currencySymbol = editor.moneda.symbol,
        modifier = Modifier.revealWhenFocused(),
        enabled = enabled,
        message = editor.amountError?.let { TallybookFieldMessage.Error(it.message()) },
    )
    editor.suggestion?.let { suggestion ->
      TallybookTextButton(
          onClick = actions::onUseSuggestion,
          modifier = Modifier.align(Alignment.CenterHorizontally),
          enabled = enabled,
      ) {
        TallybookText(stringResource(Res.string.budget_suggestion, suggestion.format()))
      }
    }
    editor.duplicateOf?.let { existing ->
      val nombre = categorias.find { it.id == existing.categoriaId }?.nombre.orEmpty()
      TallybookText(
          stringResource(Res.string.budget_duplicate, nombre, existing.monto.moneda.name),
          modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
          color = TallybookTheme.colors.foreground.secondary,
          style = TallybookTheme.typography.bodySmall,
      )
    }
    if (editor.saveFailed) {
      TallybookText(
          stringResource(Res.string.budget_save_error),
          modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
          color = TallybookTheme.colors.status.danger.base,
      )
    }
  }
}

/** The categoria and, with accounts in more than one, the moneda a new budget tracks. */
@Composable
private fun BudgetTarget(
    editor: BudgetEditorState,
    categoria: Categoria?,
    actions: BudgetsActions,
    onChooseCategory: () -> Unit,
) {
  CategorySingleField(
      categoria = categoria,
      deselectable = false,
      onChoose = onChooseCategory,
      onSuggestion = {},
      enabled = !editor.saving,
  )
  if (editor.monedas.size > 1) {
    MonedaSelector(
        monedas = editor.monedas,
        selected = editor.moneda,
        onSelect = actions::onMonedaSelected,
        enabled = !editor.saving,
    )
  }
}

@Composable
private fun LockedCategory(categoria: Categoria) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    TallybookSectionHeading(stringResource(Res.string.budget_category_label))
    CategoryTag(categoria)
  }
}

@Composable
private fun BudgetEditorFooter(editor: BudgetEditorState, actions: BudgetsActions) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    TallybookTextButton(
        onClick = actions::onDismissEditor,
        modifier = Modifier.weight(1f),
        enabled = !editor.saving,
    ) {
      TallybookText(stringResource(Res.string.cancel))
    }
    TallybookButton(
        onClick = actions::onSave,
        modifier = Modifier.weight(1f),
        enabled =
            !editor.saving &&
                (editor.editingId != null || editor.categoriaId != null) &&
                editor.amountInput.isNotBlank(),
    ) {
      TallybookText(stringResource(Res.string.budget_save))
    }
  }
}
