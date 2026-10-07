package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

  @Composable
  fun destructiveColors(): TallybookButtonColors =
      TallybookButtonColors(
          container = TallybookTheme.colors.status.danger.strong,
          content = TallybookTheme.colors.status.danger.foreground,
          disabledContainer = TallybookTheme.colors.surface.high,
          disabledContent = TallybookTheme.colors.foreground.disabled,
      )

  @Composable
  fun textColors(): TallybookButtonColors =
      TallybookButtonColors(
          container = Color.Transparent,
          content = TallybookTheme.colors.brand.secondary,
          disabledContainer = Color.Transparent,
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
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  val containerColor = if (enabled) colors.container else colors.disabledContainer
  val contentColor = if (enabled) colors.content else colors.disabledContent
  TallybookSurface(
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
                  onClick = {
                    tapFeedback.trigger()
                    onClick()
                  },
              ),
      style =
          TallybookSurfaceStyle(
              color = containerColor,
              contentColor = contentColor,
              shape = RoundedCornerShape(TallybookTheme.radii.radius18),
              border = null,
          ),
      isPressed = isPressed,
      hasTapFeedback = tapFeedback.isActive,
  ) {
    Row(
        modifier = Modifier.align(Alignment.Center).padding(TallybookButtonDefaults.contentPadding),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
  }
}

@Composable
internal fun TallybookTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
  TallybookButton(
      onClick = onClick,
      modifier = modifier,
      enabled = enabled,
      colors = TallybookButtonDefaults.textColors(),
      content = content,
  )
}
