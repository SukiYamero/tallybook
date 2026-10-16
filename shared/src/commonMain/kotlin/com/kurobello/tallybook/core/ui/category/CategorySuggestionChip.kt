package com.kurobello.tallybook.core.ui.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceStyle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

/**
 * Compact pill that offers [categoria] as a choice. The unselected form is outlined with a leading
 * plus and the selected form is filled with a leading check, so the state never relies on color.
 */
@Composable
internal fun CategorySuggestionChip(
    categoria: Categoria,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  val appearance = suggestionChipAppearance(categoria.color.tintColors(), selected, enabled)
  Box(
      modifier =
          modifier
              .defaultMinSize(
                  minWidth = TallybookTheme.dimensions.minimumTouchTarget,
                  minHeight = TallybookTheme.dimensions.minimumTouchTarget,
              )
              .selectable(
                  selected = selected,
                  enabled = enabled,
                  interactionSource = interactionSource,
                  indication = null,
                  role = Role.Button,
                  onClick = {
                    tapFeedback.trigger()
                    onClick()
                  },
              ),
      contentAlignment = Alignment.Center,
  ) {
    TallybookSurface(
        modifier =
            Modifier.defaultMinSize(minHeight = TallybookTheme.dimensions.choicePillVisualHeight),
        style =
            TallybookSurfaceStyle(
                color = appearance.container,
                contentColor = appearance.text,
                shape = RoundedCornerShape(TallybookTheme.radii.radius20),
                border = BorderStroke(TallybookTheme.dimensions.borderThin, appearance.border),
                contentAlignment = Alignment.Center,
            ),
        isPressed = isPressed,
        hasTapFeedback = tapFeedback.isActive,
    ) {
      SuggestionChipContent(categoria, selected, appearance.accent)
    }
  }
}

@Composable
private fun SuggestionChipContent(categoria: Categoria, selected: Boolean, accent: Color) {
  Row(
      modifier =
          Modifier.padding(
              start = TallybookTheme.spacing.space10,
              end = TallybookTheme.spacing.space12,
              top = TallybookTheme.spacing.space4,
              bottom = TallybookTheme.spacing.space4,
          ),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    SuggestionChipIcon(if (selected) Lucide.Check else Lucide.Plus, accent)
    SuggestionChipIcon(categoria.icono.toImageVector(), accent)
    TallybookText(
        text = categoria.nombre,
        style = TallybookTheme.typography.controlValue,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun SuggestionChipIcon(imageVector: ImageVector, tint: Color) {
  Image(
      imageVector = imageVector,
      contentDescription = null,
      modifier = Modifier.size(TallybookTheme.dimensions.iconSmall),
      colorFilter = ColorFilter.tint(tint),
  )
}

private data class SuggestionChipAppearance(
    val container: Color,
    val border: Color,
    val text: Color,
    val accent: Color,
)

@Composable
private fun suggestionChipAppearance(
    tint: CategoryTintColors,
    selected: Boolean,
    enabled: Boolean,
): SuggestionChipAppearance {
  val colors = TallybookTheme.colors
  return when {
    !enabled ->
        SuggestionChipAppearance(
            container = colors.surface.card,
            border = colors.border.subtle,
            text = colors.foreground.disabled,
            accent = colors.foreground.disabled,
        )
    selected ->
        SuggestionChipAppearance(
            container = tint.container,
            border = tint.content,
            text = tint.content,
            accent = tint.content,
        )
    else ->
        SuggestionChipAppearance(
            container = colors.surface.card,
            border = tint.border,
            text = colors.foreground.primary,
            accent = tint.content,
        )
  }
}

private val suggestionPreviewCategoria =
    Categoria(
        id = "cat_cine",
        nombre = "Cine",
        padreId = null,
        icono = CategoryIcon.FILM,
        color = CategoryTint.PURPLE,
    )

@Composable
private fun SuggestionChipPreviewRow() {
  Row(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    CategorySuggestionChip(suggestionPreviewCategoria, selected = false, onClick = {})
    CategorySuggestionChip(suggestionPreviewCategoria, selected = true, onClick = {})
  }
}

@Preview
@Composable
internal fun CategorySuggestionChipLightPreview() {
  TallybookTheme(darkTheme = false) { SuggestionChipPreviewRow() }
}

@Preview
@Composable
internal fun CategorySuggestionChipDarkPreview() {
  TallybookTheme(darkTheme = true) { SuggestionChipPreviewRow() }
}
