package com.kurobello.tallybook.core.ui.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_tag_field_label
import tallybook.shared.generated.resources.category_tag_field_suggestions
import tallybook.shared.generated.resources.movimiento_pending_label

/**
 * The category surface of the entry flow: a labeled list of [categorias] as tags. Each tag can be
 * removed except when it is the only one. [pending] marks the Otros fallback, shown as a neutral
 * dashed tag without removal and labeled "to complete" beside the field label. [suggestions]
 * already selected are skipped.
 */
@Composable
@Suppress("LongParameterList")
internal fun CategoryTagField(
    categorias: List<Categoria>,
    onRemove: (Categoria) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    pending: Boolean = false,
    suggestions: List<Categoria> = emptyList(),
    onSuggestion: (Categoria) -> Unit = {},
    enabled: Boolean = true,
) {
  val selectedIds = categorias.mapTo(mutableSetOf(), Categoria::id)
  val offered = suggestions.filter { it.id !in selectedIds }
  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    CategoryTagFieldHeader(pending)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
      categorias.forEach { categoria ->
        key(categoria.id) {
          CategoryTag(
              categoria = categoria,
              pending = pending,
              enabled = enabled,
              onRemove = if (categorias.size > 1) ({ onRemove(categoria) }) else null,
          )
        }
      }
      CategoryAddTag(onClick = onAdd, enabled = enabled)
    }
    if (offered.isNotEmpty()) {
      CategoryTagFieldSuggestions(offered, onSuggestion, enabled)
    }
  }
}

@Composable
private fun CategoryTagFieldHeader(pending: Boolean) {
  Row(
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    TallybookSectionHeading(stringResource(Res.string.category_tag_field_label))
    if (pending) {
      TallybookText(
          text = stringResource(Res.string.movimiento_pending_label),
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.caption,
      )
    }
  }
}

@Composable
private fun CategoryTagFieldSuggestions(
    suggestions: List<Categoria>,
    onSuggestion: (Categoria) -> Unit,
    enabled: Boolean,
) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
    TallybookText(
        text = stringResource(Res.string.category_tag_field_suggestions),
        color = TallybookTheme.colors.foreground.tertiary,
        style = TallybookTheme.typography.label,
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
      suggestions.forEach { suggestion ->
        key(suggestion.id) {
          CategorySuggestionTag(
              categoria = suggestion,
              onClick = { onSuggestion(suggestion) },
              enabled = enabled,
          )
        }
      }
    }
  }
}

private val previewCategories =
    listOf(
        Categoria("cat_cine", "Cine", null, CategoryIcon.FILM, CategoryTint.PURPLE),
        Categoria("cat_comida", "Comida", null, CategoryIcon.UTENSILS, CategoryTint.AMBER),
        Categoria("cat_transporte", "Transporte", null, CategoryIcon.CAR, CategoryTint.BLUE),
        Categoria("cat_salud", "Salud", null, CategoryIcon.HEART_PULSE, CategoryTint.ROSE),
        Categoria("cat_casa", "Casa", null, CategoryIcon.HOUSE, CategoryTint.EMERALD),
        Categoria("cat_regalos", "Regalos", null, CategoryIcon.GIFT, CategoryTint.PURPLE),
    )

private val previewOtros =
    Categoria(
        CATEGORIA_OTROS_ID,
        "Otros",
        null,
        CategoryIcon.CIRCLE_DASHED,
        CategoryTint.NEUTRAL,
    )

@Composable
private fun CategoryTagFieldPreviewMatrix() {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space24)) {
    CategoryTagField(
        categorias = previewCategories.take(2),
        onRemove = {},
        onAdd = {},
        suggestions = previewCategories.drop(2).take(2),
    )
    CategoryTagField(
        categorias = listOf(previewOtros),
        onRemove = {},
        onAdd = {},
        pending = true,
        suggestions = previewCategories.take(2),
    )
    CategoryTagField(
        categorias = previewCategories,
        onRemove = {},
        onAdd = {},
    )
  }
}

@Preview
@Composable
internal fun CategoryTagFieldLightPreview() {
  TallybookTheme(darkTheme = false) { CategoryTagFieldPreviewMatrix() }
}

@Preview
@Composable
internal fun CategoryTagFieldDarkPreview() {
  TallybookTheme(darkTheme = true) { CategoryTagFieldPreviewMatrix() }
}
