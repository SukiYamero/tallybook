package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Immutable
internal data class TallybookButtonColors(
    val container: Color,
    val content: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
)

internal object TallybookButtonDefaults {
  val contentPadding =
      PaddingValues(
          horizontal = TallybookTheme.spacing.space24,
          vertical = TallybookTheme.spacing.space12,
      )

  @Composable
  fun primaryColors(): TallybookButtonColors =
      TallybookButtonColors(
          container = TallybookTheme.colors.brand.primary,
          content = TallybookTheme.colors.brand.onPrimary,
          disabledContainer = TallybookTheme.colors.surface.high,
          disabledContent = TallybookTheme.colors.foreground.disabled,
      )
}

@Composable
internal fun TallybookButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: TallybookButtonColors = TallybookButtonDefaults.primaryColors(),
    content: @Composable RowScope.() -> Unit,
) {
  val containerColor = if (enabled) colors.container else colors.disabledContainer
  val contentColor = if (enabled) colors.content else colors.disabledContent
  TallybookSurface(
      modifier =
          modifier
              .defaultMinSize(
                  minWidth = TallybookTheme.dimensions.minimumTouchTarget,
                  minHeight = TallybookTheme.dimensions.minimumTouchTarget,
              )
              .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
      style =
          TallybookSurfaceStyle(
              color = containerColor,
              contentColor = contentColor,
              shape = RoundedCornerShape(TallybookTheme.radii.radius18),
              border = null,
          ),
  ) {
    Row(
        modifier = Modifier.align(Alignment.Center).padding(TallybookButtonDefaults.contentPadding),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
  }
}
