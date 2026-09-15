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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Immutable
internal data class TallybookSurfaceStyle(
    val color: Color,
    val contentColor: Color,
    val shape: Shape,
    val border: BorderStroke?,
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

@Composable
internal fun TallybookSurface(
    modifier: Modifier = Modifier,
    style: TallybookSurfaceStyle = TallybookSurfaceDefaults.cardStyle(),
    content: @Composable BoxScope.() -> Unit,
) {
  CompositionLocalProvider(LocalTallybookContentColor provides style.contentColor) {
    Box(
        modifier =
            modifier
                .clip(style.shape)
                .background(style.color)
                .then(
                    if (style.border == null) Modifier
                    else Modifier.border(style.border, style.shape)
                ),
        content = content,
    )
  }
}
