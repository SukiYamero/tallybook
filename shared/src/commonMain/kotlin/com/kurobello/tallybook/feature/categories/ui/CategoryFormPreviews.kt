package com.kurobello.tallybook.feature.categories.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.categories.viewmodel.CategoryDraftState

private val previewDraft =
    CategoryDraftState(
        nombre = "Gimnasio",
        icono = CategoryIcon.DUMBBELL,
        color = CategoryTint.ROSE,
        descripcion = "Mensualidad y clases",
    )

private object PreviewCategoryFormActions : CategoryFormActions {
  override fun onNombreChanged(value: String) = Unit

  override fun onIconoSelected(icono: CategoryIcon) = Unit

  override fun onColorSelected(color: CategoryTint) = Unit

  override fun onDescripcionChanged(value: String) = Unit

  override fun onMoreOptions() = Unit
}

@Preview
@Composable
internal fun CategoryFormCompactPreview() {
  TallybookTheme(darkTheme = false) {
    CategoryForm(previewDraft, PreviewCategoryFormActions, CategoryFormMode.COMPACT)
  }
}

@Preview
@Composable
internal fun CategoryFormFullDarkPreview() {
  TallybookTheme(darkTheme = true) {
    CategoryForm(previewDraft, PreviewCategoryFormActions, CategoryFormMode.FULL)
  }
}
