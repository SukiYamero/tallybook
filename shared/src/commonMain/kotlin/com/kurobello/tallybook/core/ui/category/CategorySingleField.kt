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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_choose
import tallybook.shared.generated.resources.category_picker_single_title
import tallybook.shared.generated.resources.category_suggestion_replaces
import tallybook.shared.generated.resources.movimiento_category_section
import tallybook.shared.generated.resources.movimiento_pending_label

/**
 * The one category of a movimiento under a "Category" heading, with [suggestions] that act as a
 * switch: while a real [categoria] is selected they render dimmed but stay tappable, and choosing
 * one replaces it through [onSuggestion]. "Choose" opens the picker through [onChoose]. [pending]
 * shows the Otros fallback as a dashed tag labeled "to complete". [deselectable] adds a remove
 * action on a real category that reports [onDeselect]; the editor keeps it off so a movimiento
 * always holds one category.
 */
@Composable
@Suppress("LongParameterList")
internal fun CategorySingleField(
    categoria: Categoria?,
    deselectable: Boolean,
    onChoose: () -> Unit,
    onSuggestion: (Categoria) -> Unit,
    modifier: Modifier = Modifier,
    onDeselect: () -> Unit = {},
    pending: Boolean = false,
    suggestions: List<Categoria> = emptyList(),
    enabled: Boolean = true,
) {
  val selectedReal = categoria?.takeUnless { pending }
  val offered = suggestions.filter { it.id != categoria?.id }
  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    CategorySingleFieldHeader(pending)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
      if (categoria != null) {
        CategoryTag(
            categoria = categoria,
            pending = pending,
            enabled = enabled,
            onRemove = if (deselectable && selectedReal != null) onDeselect else null,
        )
      }
      offered.forEach { suggestion ->
        key(suggestion.id) {
          CategorySwitchSuggestion(
              suggestion = suggestion,
              replaces = selectedReal,
              onClick = { onSuggestion(suggestion) },
              enabled = enabled,
          )
        }
      }
      CategoryChooseTag(onClick = onChoose, enabled = enabled)
    }
  }
}

@Composable
private fun CategorySingleFieldHeader(pending: Boolean) {
  Row(
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    TallybookSectionHeading(stringResource(Res.string.movimiento_category_section))
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
private fun CategorySwitchSuggestion(
    suggestion: Categoria,
    replaces: Categoria?,
    onClick: () -> Unit,
    enabled: Boolean,
) {
  val replacement = replaces?.let {
    stringResource(Res.string.category_suggestion_replaces, it.nombre)
  }
  CategorySuggestionTag(
      categoria = suggestion,
      onClick = onClick,
      modifier =
          if (replacement == null) Modifier
          else Modifier.semantics { stateDescription = replacement },
      enabled = enabled,
      dimmed = replaces != null,
  )
}

@Composable
private fun CategoryChooseTag(onClick: () -> Unit, enabled: Boolean) {
  val appearance = addTagAppearance(enabled)
  val description = stringResource(Res.string.category_picker_single_title)
  CategoryTagButton(
      onClick = onClick,
      enabled = enabled,
      appearance = appearance,
      modifier = Modifier.semantics { contentDescription = description },
  ) {
    CategoryTagLabel(stringResource(Res.string.category_choose))
    CategoryTagIcon(Lucide.ChevronDown, appearance.accent)
  }
}

private val previewSingleCategories =
    listOf(
        Categoria("cat_cine", "Cine", null, CategoryIcon.FILM, CategoryTint.PURPLE),
        Categoria("cat_comida", "Comida", null, CategoryIcon.UTENSILS, CategoryTint.AMBER),
        Categoria("cat_transporte", "Transporte", null, CategoryIcon.CAR, CategoryTint.BLUE),
    )

private val previewSingleOtros =
    Categoria(CATEGORIA_OTROS_ID, "Otros", null, CategoryIcon.CIRCLE_DASHED, CategoryTint.NEUTRAL)

@Composable
private fun CategorySingleFieldPreviewMatrix() {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space24)) {
    CategorySingleField(
        categoria = previewSingleCategories.first(),
        deselectable = true,
        onChoose = {},
        onSuggestion = {},
        suggestions = previewSingleCategories,
    )
    CategorySingleField(
        categoria = previewSingleOtros,
        deselectable = true,
        onChoose = {},
        onSuggestion = {},
        pending = true,
        suggestions = previewSingleCategories.drop(1),
    )
    CategorySingleField(
        categoria = previewSingleCategories.first(),
        deselectable = false,
        onChoose = {},
        onSuggestion = {},
    )
  }
}

@Preview
@Composable
internal fun CategorySingleFieldLightPreview() {
  TallybookTheme(darkTheme = false) { CategorySingleFieldPreviewMatrix() }
}

@Preview
@Composable
internal fun CategorySingleFieldDarkPreview() {
  TallybookTheme(darkTheme = true) { CategorySingleFieldPreviewMatrix() }
}
