package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Composable
internal fun TallybookSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(TallybookTheme.radii.radius8),
) {
  Box(
      modifier =
          modifier.clearAndSetSemantics {}.background(TallybookTheme.colors.surface.high, shape)
  )
}

@Composable
internal fun TallybookSkeletonLine(
    sampleText: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
  Box(modifier = modifier) {
    // Measuring a hidden line preserves font metrics and system text scaling without fixed heights.
    TallybookText(
        text = sampleText,
        modifier = Modifier.clearAndSetSemantics {},
        color = Color.Transparent,
        style = style,
        maxLines = 1,
    )
    TallybookSkeleton(
        modifier = Modifier.matchParentSize().padding(vertical = TallybookTheme.spacing.space4)
    )
  }
}

@Composable
internal fun TallybookSkeletonCard(
    modifier: Modifier = Modifier,
    style: TallybookSurfaceStyle = TallybookSurfaceDefaults.cardStyle(),
    content: @Composable BoxScope.() -> Unit,
) {
  TallybookSurface(
      modifier = modifier.clearAndSetSemantics {},
      style = style,
      content = content,
  )
}
