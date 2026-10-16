package com.kurobello.tallybook.core.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.kurobello.tallybook.core.data.CategorySeedCatalog
import com.kurobello.tallybook.core.data.entry.CategoryAppearanceSuggester
import com.kurobello.tallybook.core.data.entry.CategoryKeywordDictionary
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.categories.ui.CategoryDuplicateAction
import com.kurobello.tallybook.feature.categories.ui.CategoryForm
import com.kurobello.tallybook.feature.categories.ui.CategoryFormActions
import com.kurobello.tallybook.feature.categories.ui.CategoryFormMode
import com.kurobello.tallybook.feature.categories.viewmodel.CategoryDraftState
import com.kurobello.tallybook.feature.categories.viewmodel.onColor
import com.kurobello.tallybook.feature.categories.viewmodel.onDescripcion
import com.kurobello.tallybook.feature.categories.viewmodel.onIcono
import com.kurobello.tallybook.feature.categories.viewmodel.onMoreOptions
import com.kurobello.tallybook.feature.categories.viewmodel.onNombre
import com.kurobello.tallybook.feature.movimientos.ui.rememberMovimientoEntrySamples
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_duplicate_use
import tallybook.shared.generated.resources.category_gimnasio
import tallybook.shared.generated.resources.ui_catalog_category_form
import tallybook.shared.generated.resources.ui_catalog_state_compact
import tallybook.shared.generated.resources.ui_catalog_state_duplicate
import tallybook.shared.generated.resources.ui_catalog_state_full
import tallybook.shared.generated.resources.ui_catalog_text_field_value

internal fun LazyListScope.catalogCategoryForm() {
  catalogControlSection(
      "category-form",
      { stringResource(Res.string.ui_catalog_category_form) },
  ) {
    CatalogCategoryFormMatrix()
  }
}

@Composable
private fun CatalogCategoryFormMatrix() {
  val suggester = remember {
    CategoryAppearanceSuggester(CategoryKeywordDictionary.builtIn, CategorySeedCatalog.categories)
  }
  val samples = rememberMovimientoEntrySamples()
  val gymName = stringResource(Res.string.category_gimnasio)
  val description = stringResource(Res.string.ui_catalog_text_field_value)
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space24)) {
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_compact)) {
      CatalogCategoryForm(
          initial = CategoryDraftState.forNew(gymName, suggester),
          mode = CategoryFormMode.COMPACT,
          suggester = suggester,
      )
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_full)) {
      CatalogCategoryForm(
          initial = CategoryDraftState.forEdit(samples.cinema).copy(descripcion = description),
          mode = CategoryFormMode.FULL,
          suggester = suggester,
      )
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_duplicate)) {
      CatalogCategoryForm(
          initial =
              CategoryDraftState.forNew(samples.food.nombre, suggester)
                  .copy(duplicate = samples.food),
          mode = CategoryFormMode.COMPACT,
          suggester = suggester,
          duplicateLabel = stringResource(Res.string.category_duplicate_use, samples.food.nombre),
      )
    }
  }
}

@Composable
private fun CatalogCategoryForm(
    initial: CategoryDraftState,
    mode: CategoryFormMode,
    suggester: CategoryAppearanceSuggester,
    duplicateLabel: String? = null,
) {
  val draft = remember(initial) { mutableStateOf(initial) }
  val actions = remember(draft, suggester) { CatalogCategoryFormActions(draft, suggester) }
  CategoryForm(
      state = draft.value,
      actions = actions,
      mode = mode,
      duplicateAction = duplicateLabel?.let { CategoryDuplicateAction(it) {} },
  )
}

private class CatalogCategoryFormActions(
    private val draft: MutableState<CategoryDraftState>,
    private val suggester: CategoryAppearanceSuggester,
) : CategoryFormActions {
  override fun onNombreChanged(value: String) {
    draft.value = draft.value.onNombre(value, suggester)
  }

  override fun onIconoSelected(icono: CategoryIcon) {
    draft.value = draft.value.onIcono(icono)
  }

  override fun onColorSelected(color: CategoryTint) {
    draft.value = draft.value.onColor(color)
  }

  override fun onDescripcionChanged(value: String) {
    draft.value = draft.value.onDescripcion(value)
  }

  override fun onMoreOptions() {
    draft.value = draft.value.onMoreOptions()
  }
}
