package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlinx.coroutines.delay

@Immutable
internal data class TallybookSurfaceStyle(
    val color: Color,
    val contentColor: Color,
    val shape: Shape,
    val border: BorderStroke?,
    val contentAlignment: Alignment = Alignment.TopStart,
)

internal object TallybookSurfaceDefaults {
  @Composable
  fun cardStyle(): TallybookSurfaceStyle =
      TallybookSurfaceStyle(
          color = TallybookTheme.colors.surface.card,
          contentColor = TallybookTheme.colors.foreground.primary,
          shape = RoundedCornerShape(TallybookTheme.radii.radius16),
          border = null,
      )
}

internal fun resolveTallybookSurfaceContainerColor(
    containerColor: Color,
    pressedOverlay: Color,
    isPressed: Boolean,
    hasTapFeedback: Boolean = false,
): Color =
    if (isPressed || hasTapFeedback) pressedOverlay.compositeOver(containerColor)
    else containerColor

@Stable
internal class TallybookTapFeedback {
  var isActive by mutableStateOf(false)
    private set

  private var tapCount by mutableIntStateOf(0)

  fun trigger() {
    isActive = true
    tapCount += 1
  }

  internal fun clear() {
    isActive = false
  }

  internal val currentTapCount: Int
    get() = tapCount
}

@Composable
internal fun rememberTallybookTapFeedback(): TallybookTapFeedback {
  val tapFeedback = remember { TallybookTapFeedback() }
  LaunchedEffect(tapFeedback.currentTapCount) {
    if (tapFeedback.currentTapCount > 0) {
      delay(TallybookTheme.motion.tapFeedbackMillis.toLong())
      tapFeedback.clear()
    }
  }
  return tapFeedback
}

@Composable
internal fun TallybookSurface(
    modifier: Modifier = Modifier,
    style: TallybookSurfaceStyle = TallybookSurfaceDefaults.cardStyle(),
    isPressed: Boolean = false,
    hasTapFeedback: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
  CompositionLocalProvider(LocalTallybookContentColor provides style.contentColor) {
    Box(
        modifier =
            modifier
                .clip(style.shape)
                .background(
                    resolveTallybookSurfaceContainerColor(
                        containerColor = style.color,
                        pressedOverlay = TallybookTheme.colors.interaction.pressedOverlay,
                        isPressed = isPressed,
                        hasTapFeedback = hasTapFeedback,
                    )
                )
                .then(
                    if (style.border == null) Modifier
                    else Modifier.border(style.border, style.shape)
                ),
        contentAlignment = style.contentAlignment,
        content = content,
    )
  }
}
