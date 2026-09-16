package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Composable
internal fun TallybookIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  val containerColor =
      if (enabled) TallybookTheme.colors.navigation.surface else TallybookTheme.colors.surface.high
  val contentColor =
      if (enabled) TallybookTheme.colors.navigation.content
      else TallybookTheme.colors.foreground.disabled
  TallybookSurface(
      modifier =
          modifier
              .size(TallybookTheme.dimensions.iconButtonContainerSize)
              .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
              .semantics { this.contentDescription = contentDescription },
      style =
          TallybookSurfaceStyle(
              color = containerColor,
              contentColor = contentColor,
              shape = RoundedCornerShape(TallybookTheme.radii.radius12),
              border = null,
          ),
  ) {
    Image(
        imageVector = icon,
        contentDescription = null,
        modifier =
            Modifier.align(Alignment.Center).size(TallybookTheme.dimensions.iconButtonIconSize),
        colorFilter = ColorFilter.tint(contentColor),
    )
  }
}
