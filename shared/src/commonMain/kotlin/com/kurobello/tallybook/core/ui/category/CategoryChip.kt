package com.kurobello.tallybook.core.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.component.resolveTallybookSurfaceContainerColor
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_comida

@Composable
@Suppress("LongParameterList")
fun CategoryChip(
    categoria: Categoria,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pending: Boolean = false,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  val colors = if (pending) neutralCategoryTintColors() else categoria.color.tintColors()
  val shape = MaterialTheme.shapes.extraLarge
  val containerColor =
      if (selected) colors.container else MaterialTheme.colorScheme.surfaceContainerLow
  Row(
      modifier =
          modifier
              .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
              .clip(shape)
              .background(
                  resolveTallybookSurfaceContainerColor(
                      containerColor = containerColor,
                      pressedOverlay = TallybookTheme.colors.interaction.pressedOverlay,
                      isPressed = isPressed,
                      hasTapFeedback = tapFeedback.isActive,
                  )
              )
              .border(
                  width = 1.dp,
                  color = if (selected) colors.border else MaterialTheme.colorScheme.outlineVariant,
                  shape = shape,
              )
              .then(
                  if (onClick == null) {
                    Modifier.semantics { this.selected = selected }
                  } else {
                    Modifier.selectable(
                        selected = selected,
                        enabled = enabled,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                          tapFeedback.trigger()
                          onClick()
                        },
                        role = Role.Button,
                    )
                  }
              )
              .semantics(mergeDescendants = true) {}
              .padding(horizontal = 12.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    CategoryAvatar(categoria = categoria, modifier = Modifier.size(28.dp), pending = pending)
    TallybookText(
        text = categoria.nombre,
        color = if (selected) colors.content else MaterialTheme.colorScheme.onSurface,
        style = TallybookTheme.typography.controlValue,
    )
  }
}

@Composable
private fun chipPreviewCategoria() =
    Categoria(
        id = "cat_comida",
        nombre = stringResource(Res.string.category_comida),
        padreId = null,
        icono = CategoryIcon.UTENSILS,
        color = CategoryTint.AMBER,
    )

@Preview
@Composable
internal fun CategoryChipLightPreview() {
  TallybookTheme(darkTheme = false) {
    CategoryChip(categoria = chipPreviewCategoria(), selected = true, onClick = {})
  }
}

@Preview
@Composable
internal fun CategoryChipDarkPreview() {
  TallybookTheme(darkTheme = true) {
    CategoryChip(categoria = chipPreviewCategoria(), selected = false, onClick = {})
  }
}
