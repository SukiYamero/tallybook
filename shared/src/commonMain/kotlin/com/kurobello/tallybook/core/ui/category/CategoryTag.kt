package com.kurobello.tallybook.core.ui.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.X
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_tag_add
import tallybook.shared.generated.resources.category_tag_add_description
import tallybook.shared.generated.resources.category_tag_remove

/**
 * The only category tag size: a 28 dp avatar and a name. It shows [categoria] as part of the
 * current selection (tinted fill) or, with [pending], as the neutral dashed Otros fallback. Passing
 * [onRemove] adds a trailing "×"; the tag itself is never tappable.
 */
@Composable
internal fun CategoryTag(
    categoria: Categoria,
    modifier: Modifier = Modifier,
    pending: Boolean = false,
    enabled: Boolean = true,
    onRemove: (() -> Unit)? = null,
) {
  val appearance = selectedTagAppearance(categoria, pending, removable = onRemove != null)
  val glyphColor = if (enabled) appearance.accent else TallybookTheme.colors.foreground.disabled
  Box(
      modifier =
          if (onRemove == null) modifier
          else modifier.defaultMinSize(minHeight = TallybookTheme.dimensions.minimumTouchTarget),
      contentAlignment = Alignment.Center,
  ) {
    CategoryTagSurface(
        appearance = appearance,
        modifier = Modifier.semantics(mergeDescendants = true) { selected = true },
    ) {
      CategoryAvatar(categoria, Modifier.size(TallybookTheme.spacing.space28), pending)
      CategoryTagLabel(categoria.nombre)
      if (onRemove != null) CategoryTagIcon(Lucide.X, glyphColor)
    }
    if (onRemove != null) {
      CategoryTagRemoveTarget(
          description = stringResource(Res.string.category_tag_remove, categoria.nombre),
          enabled = enabled,
          onRemove = onRemove,
          modifier = Modifier.matchParentSize(),
      )
    }
  }
}

/** Outlined tag with a leading plus that offers [categoria] as a choice to add. */
@Composable
internal fun CategorySuggestionTag(
    categoria: Categoria,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  val appearance = suggestedTagAppearance(categoria, enabled)
  CategoryTagButton(
      onClick = onClick,
      enabled = enabled,
      appearance = appearance,
      modifier = modifier,
  ) {
    CategoryTagIcon(Lucide.Plus, appearance.accent)
    CategoryAvatar(categoria, Modifier.size(TallybookTheme.spacing.space28))
    CategoryTagLabel(categoria.nombre)
  }
}

/** Neutral "+ Add" tag that opens the category picker. */
@Composable
internal fun CategoryAddTag(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  val appearance = addTagAppearance(enabled)
  val description = stringResource(Res.string.category_tag_add_description)
  CategoryTagButton(
      onClick = onClick,
      enabled = enabled,
      appearance = appearance,
      modifier = modifier.semantics { contentDescription = description },
  ) {
    CategoryTagIcon(Lucide.Plus, appearance.accent)
    CategoryTagLabel(stringResource(Res.string.category_tag_add))
  }
}

@Composable
private fun selectedTagAppearance(
    categoria: Categoria,
    pending: Boolean,
    removable: Boolean,
): CategoryTagAppearance {
  val foreground = TallybookTheme.colors.foreground
  val padding = avatarTagPadding(trailing = removable)
  if (pending) {
    return CategoryTagAppearance(
        container = Color.Transparent,
        border = foreground.faint,
        text = foreground.tertiary,
        accent = foreground.tertiary,
        padding = padding,
        dashedBorder = true,
    )
  }
  val tint = categoria.color.tintColors()
  return CategoryTagAppearance(
      container = tint.container,
      border = tint.border,
      text = tint.content,
      accent = tint.content,
      padding = padding,
  )
}

@Composable
private fun suggestedTagAppearance(categoria: Categoria, enabled: Boolean): CategoryTagAppearance {
  val colors = TallybookTheme.colors
  val padding = avatarTagPadding(leading = true)
  if (!enabled) {
    return CategoryTagAppearance(
        container = colors.surface.card,
        border = colors.border.subtle,
        text = colors.foreground.disabled,
        accent = colors.foreground.disabled,
        padding = padding,
    )
  }
  val tint = categoria.color.tintColors()
  return CategoryTagAppearance(
      container = colors.surface.card,
      border = tint.border,
      text = colors.foreground.primary,
      accent = tint.content,
      padding = padding,
  )
}

@Composable
private fun addTagAppearance(enabled: Boolean): CategoryTagAppearance {
  val colors = TallybookTheme.colors
  val content = if (enabled) colors.foreground.secondary else colors.foreground.disabled
  return CategoryTagAppearance(
      container = colors.surface.card,
      border = if (enabled) colors.border.strong else colors.border.subtle,
      text = content,
      accent = content,
      padding =
          PaddingValues(
              start = TallybookTheme.spacing.space12,
              end = TallybookTheme.spacing.space14,
              top = TallybookTheme.spacing.space4,
              bottom = TallybookTheme.spacing.space4,
          ),
  )
}

private val previewCategoria =
    Categoria(
        id = "cat_cine",
        nombre = "Cine",
        padreId = null,
        icono = CategoryIcon.FILM,
        color = CategoryTint.PURPLE,
    )

private val previewPending =
    Categoria(
        id = CATEGORIA_OTROS_ID,
        nombre = "Otros",
        padreId = null,
        icono = CategoryIcon.CIRCLE_DASHED,
        color = CategoryTint.NEUTRAL,
    )

@Composable
private fun CategoryTagPreviewMatrix() {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    Row(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
      CategoryTag(previewCategoria)
      CategoryTag(previewCategoria, onRemove = {})
      CategoryTag(previewPending, pending = true)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
      CategorySuggestionTag(previewCategoria, onClick = {})
      CategorySuggestionTag(previewCategoria, onClick = {}, enabled = false)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
      CategoryAddTag(onClick = {})
      CategoryAddTag(onClick = {}, enabled = false)
      CategoryTag(previewCategoria, enabled = false, onRemove = {})
    }
  }
}

@Preview
@Composable
internal fun CategoryTagLightPreview() {
  TallybookTheme(darkTheme = false) { CategoryTagPreviewMatrix() }
}

@Preview
@Composable
internal fun CategoryTagDarkPreview() {
  TallybookTheme(darkTheme = true) { CategoryTagPreviewMatrix() }
}
