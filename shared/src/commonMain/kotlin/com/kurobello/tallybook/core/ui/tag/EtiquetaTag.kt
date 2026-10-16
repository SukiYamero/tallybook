package com.kurobello.tallybook.core.ui.tag

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Hash
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.X
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.ui.category.CategoryTagAppearance
import com.kurobello.tallybook.core.ui.category.CategoryTagButton
import com.kurobello.tallybook.core.ui.category.CategoryTagIcon
import com.kurobello.tallybook.core.ui.category.CategoryTagLabel
import com.kurobello.tallybook.core.ui.category.CategoryTagRemoveTarget
import com.kurobello.tallybook.core.ui.category.CategoryTagSurface
import com.kurobello.tallybook.core.ui.category.addTagAppearance
import com.kurobello.tallybook.core.ui.category.avatarTagPadding
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.tag_add
import tallybook.shared.generated.resources.tag_add_description
import tallybook.shared.generated.resources.tag_remove

/**
 * [etiqueta] as part of the current selection: a neutral chip with a leading "#" and no tint of its
 * own. Passing [onRemove] adds a trailing "×" whose 48 dp target is a separate button; the tag body
 * is never tappable.
 */
@Composable
internal fun EtiquetaTag(
    etiqueta: Etiqueta,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onRemove: (() -> Unit)? = null,
) {
  val appearance = selectedEtiquetaAppearance(removable = onRemove != null)
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
      CategoryTagIcon(Lucide.Hash, appearance.accent)
      CategoryTagLabel(etiqueta.nombre)
      if (onRemove != null) CategoryTagIcon(Lucide.X, glyphColor)
    }
    if (onRemove != null) {
      CategoryTagRemoveTarget(
          description = stringResource(Res.string.tag_remove, etiqueta.nombre),
          enabled = enabled,
          onRemove = onRemove,
          modifier = Modifier.matchParentSize(),
      )
    }
  }
}

/** Outlined tag with a leading plus that offers [etiqueta] as a tag to add. */
@Composable
internal fun EtiquetaSuggestionTag(
    etiqueta: Etiqueta,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  val appearance = suggestedEtiquetaAppearance(enabled)
  CategoryTagButton(
      onClick = onClick,
      enabled = enabled,
      appearance = appearance,
      modifier = modifier,
  ) {
    CategoryTagIcon(Lucide.Plus, appearance.accent)
    CategoryTagLabel(etiqueta.nombre)
  }
}

/** Neutral "+ Add" tag that opens the tag picker. */
@Composable
internal fun EtiquetaAddTag(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  val appearance = addTagAppearance(enabled)
  val description = stringResource(Res.string.tag_add_description)
  CategoryTagButton(
      onClick = onClick,
      enabled = enabled,
      appearance = appearance,
      modifier = modifier.semantics { contentDescription = description },
  ) {
    CategoryTagIcon(Lucide.Plus, appearance.accent)
    CategoryTagLabel(stringResource(Res.string.tag_add))
  }
}

@Composable
private fun selectedEtiquetaAppearance(removable: Boolean): CategoryTagAppearance {
  val colors = TallybookTheme.colors
  return CategoryTagAppearance(
      container = colors.surface.high,
      border = colors.border.base,
      text = colors.foreground.primary,
      accent = colors.foreground.secondary,
      padding = avatarTagPadding(leading = true, trailing = removable),
  )
}

@Composable
private fun suggestedEtiquetaAppearance(enabled: Boolean): CategoryTagAppearance {
  val colors = TallybookTheme.colors
  return CategoryTagAppearance(
      container = colors.surface.card,
      border = if (enabled) colors.border.base else colors.border.subtle,
      text = if (enabled) colors.foreground.primary else colors.foreground.disabled,
      accent = if (enabled) colors.foreground.secondary else colors.foreground.disabled,
      padding = avatarTagPadding(leading = true),
  )
}

internal val previewEtiquetaCreatedAt: Instant = Instant.parse("2026-01-01T00:00:00Z")

private val previewEtiqueta = Etiqueta("tag_empresa", "Empresa", previewEtiquetaCreatedAt)

@Composable
private fun EtiquetaTagPreviewMatrix() {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    Row(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
      EtiquetaTag(previewEtiqueta)
      EtiquetaTag(previewEtiqueta, onRemove = {})
      EtiquetaTag(previewEtiqueta, enabled = false, onRemove = {})
    }
    Row(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
      EtiquetaSuggestionTag(previewEtiqueta, onClick = {})
      EtiquetaSuggestionTag(previewEtiqueta, onClick = {}, enabled = false)
      EtiquetaAddTag(onClick = {})
    }
  }
}

@Preview
@Composable
internal fun EtiquetaTagLightPreview() {
  TallybookTheme(darkTheme = false) { EtiquetaTagPreviewMatrix() }
}

@Preview
@Composable
internal fun EtiquetaTagDarkPreview() {
  TallybookTheme(darkTheme = true) { EtiquetaTagPreviewMatrix() }
}
