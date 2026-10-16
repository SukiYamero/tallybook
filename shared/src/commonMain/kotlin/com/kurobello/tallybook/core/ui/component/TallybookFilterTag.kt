package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

internal object TallybookFilterTagDefaults {
  @Composable
  fun colors(): TallybookButtonColors =
      TallybookButtonColors(
          TallybookTheme.colors.surface.deep,
          TallybookTheme.colors.foreground.secondary,
          TallybookTheme.colors.surface.deep,
          TallybookTheme.colors.foreground.disabled,
      )
}

@Composable
@Suppress("LongParameterList")
internal fun TallybookFilterTag(
    label: String,
    removeContentDescription: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: TallybookButtonColors = TallybookFilterTagDefaults.colors(),
    leadingIcon: ImageVector? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val contentColor = if (enabled) colors.content else colors.disabledContent
  Box(
      modifier =
          modifier
              .defaultMinSize(
                  minWidth = TallybookTheme.dimensions.minimumTouchTarget,
                  minHeight = TallybookTheme.dimensions.minimumTouchTarget,
              )
              .clickable(
                  interactionSource = interactionSource,
                  indication = null,
                  enabled = enabled,
                  role = Role.Button,
                  onClick = onRemove,
              )
              .semantics(mergeDescendants = true) { contentDescription = removeContentDescription },
      contentAlignment = Alignment.Center,
  ) {
    TallybookSurface(
        modifier =
            Modifier.defaultMinSize(minHeight = TallybookTheme.dimensions.filterTagVisualHeight),
        style =
            TallybookSurfaceStyle(
                color = if (enabled) colors.container else colors.disabledContainer,
                contentColor = contentColor,
                shape = RoundedCornerShape(TallybookTheme.radii.radius18),
                border = null,
                contentAlignment = Alignment.Center,
            ),
    ) {
      FilterTagContent(label, leadingIcon, contentColor)
    }
  }
}

@Composable
private fun FilterTagContent(label: String, leadingIcon: ImageVector?, contentColor: Color) {
  Row(
      modifier =
          Modifier.padding(
              horizontal = TallybookTheme.spacing.space8,
              vertical = TallybookTheme.spacing.space4,
          ),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    if (leadingIcon != null) FilterTagIcon(leadingIcon, contentColor)
    TallybookText(label, style = TallybookTheme.typography.caption)
    FilterTagIcon(Lucide.X, contentColor)
  }
}

@Composable
private fun FilterTagIcon(icon: ImageVector, tint: Color) {
  Image(
      icon,
      null,
      Modifier.size(TallybookTheme.dimensions.iconSmall),
      colorFilter = ColorFilter.tint(tint),
  )
}
