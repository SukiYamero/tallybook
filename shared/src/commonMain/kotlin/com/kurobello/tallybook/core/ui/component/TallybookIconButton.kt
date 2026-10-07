package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Immutable
internal enum class TallybookIconButtonStyle {
  Neutral,
  Brand,
}

@Immutable
internal data class TallybookIconButtonColors(
    val container: Color,
    val content: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
)

@Immutable
internal data class TallybookIconButtonColorTokens(
    val neutralContainer: Color,
    val neutralContent: Color,
    val brandContainer: Color,
    val brandContent: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
)

internal fun resolveTallybookIconButtonColors(
    style: TallybookIconButtonStyle,
    tokens: TallybookIconButtonColorTokens,
): TallybookIconButtonColors =
    TallybookIconButtonColors(
        container =
            when (style) {
              TallybookIconButtonStyle.Neutral -> tokens.neutralContainer
              TallybookIconButtonStyle.Brand -> tokens.brandContainer
            },
        content =
            when (style) {
              TallybookIconButtonStyle.Neutral -> tokens.neutralContent
              TallybookIconButtonStyle.Brand -> tokens.brandContent
            },
        disabledContainer = tokens.disabledContainer,
        disabledContent = tokens.disabledContent,
    )

internal object TallybookIconButtonDefaults {
  @Composable
  fun destructiveColors(): TallybookIconButtonColors =
      colors(TallybookIconButtonStyle.Neutral)
          .copy(
              container = TallybookTheme.colors.status.danger.base.copy(alpha = 0.12f),
              content = TallybookTheme.colors.status.danger.base,
          )

  @Composable
  fun colors(style: TallybookIconButtonStyle): TallybookIconButtonColors =
      resolveTallybookIconButtonColors(
          style = style,
          tokens =
              TallybookIconButtonColorTokens(
                  neutralContainer = TallybookTheme.colors.navigation.surface,
                  neutralContent = TallybookTheme.colors.navigation.content,
                  brandContainer = TallybookTheme.colors.brand.primary,
                  brandContent = TallybookTheme.colors.brand.onPrimary,
                  disabledContainer = Color.Transparent,
                  disabledContent = TallybookTheme.colors.foreground.disabled,
              ),
      )
}

@Composable
@Suppress("LongParameterList")
internal fun TallybookIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: TallybookIconButtonStyle = TallybookIconButtonStyle.Neutral,
    colors: TallybookIconButtonColors = TallybookIconButtonDefaults.colors(style),
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  val containerColor = if (enabled) colors.container else colors.disabledContainer
  val contentColor = if (enabled) colors.content else colors.disabledContent
  TallybookSurface(
      modifier =
          modifier
              .size(TallybookTheme.dimensions.iconButtonContainerSize)
              .clickable(
                  interactionSource = interactionSource,
                  indication = null,
                  enabled = enabled,
                  role = Role.Button,
                  onClick = {
                    tapFeedback.trigger()
                    onClick()
                  },
              )
              .semantics { this.contentDescription = contentDescription },
      style =
          TallybookSurfaceStyle(
              color = containerColor,
              contentColor = contentColor,
              shape = RoundedCornerShape(TallybookTheme.radii.radius12),
              border = null,
          ),
      isPressed = isPressed,
      hasTapFeedback = tapFeedback.isActive,
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
