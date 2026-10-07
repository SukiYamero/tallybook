package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import com.kurobello.tallybook.core.ui.theme.TallybookSpacing
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

internal object TallybookSnackbarDefaults {
  val verticalPadding = TallybookSpacing.space10
}

@Composable
internal fun TallybookSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
    actionContent: (@Composable () -> Unit)? = null,
    dismissContent: (@Composable () -> Unit)? = null,
) {
  val shape = RoundedCornerShape(TallybookTheme.radii.radius16)
  TallybookSurface(
      modifier = modifier,
      style =
          TallybookSurfaceStyle(
              color = TallybookTheme.colors.surface.raised,
              contentColor = TallybookTheme.colors.foreground.primary,
              shape = shape,
              border =
                  BorderStroke(
                      TallybookTheme.dimensions.borderThin,
                      TallybookTheme.colors.border.base,
                  ),
          ),
  ) {
    SnackbarContent(
        message = message,
        leadingContent = leadingContent,
        actionContent = actionContent,
        dismissContent = dismissContent,
    )
  }
}

@Composable
@Suppress("LongMethod")
private fun SnackbarContent(
    message: String,
    leadingContent: (@Composable () -> Unit)?,
    actionContent: (@Composable () -> Unit)?,
    dismissContent: (@Composable () -> Unit)?,
) {
  val spacing = TallybookTheme.spacing
  Layout(
      modifier =
          Modifier.fillMaxWidth()
              .padding(
                  horizontal = spacing.space12,
                  vertical = TallybookSnackbarDefaults.verticalPadding,
              ),
      content = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.space12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          leadingContent?.invoke()
          TallybookText(
              text = message,
              modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
          )
        }
        actionContent?.let { Box { it() } }
        dismissContent?.let { Box { it() } }
      },
  ) { measurables, constraints ->
    val gap = spacing.space8.roundToPx()
    val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
    val controls = measurables.drop(1).map { it.measure(childConstraints) }
    val controlsWidth = controls.sumOf { it.width } + gap * (controls.size - 1).coerceAtLeast(0)
    val naturalMessageWidth = measurables.first().maxIntrinsicWidth(constraints.maxHeight)
    val inline =
        controls.isEmpty() ||
            naturalMessageWidth.toLong() + controlsWidth + gap <= constraints.maxWidth
    val messageWidth =
        if (inline && controls.isNotEmpty())
            (constraints.maxWidth - controlsWidth - gap).coerceAtLeast(0)
        else constraints.maxWidth
    val text = measurables.first().measure(childConstraints.copy(maxWidth = messageWidth))
    val width = snackbarContentWidth(constraints, text.width, controlsWidth, controls.size, gap)
    val controlsInline = controlsWidth <= width
    val controlsHeight =
        if (controlsInline) controls.maxOfOrNull { it.height } ?: 0
        else controls.sumOf { it.height } + gap * (controls.size - 1).coerceAtLeast(0)
    val desiredHeight =
        if (inline) maxOf(text.height, controlsHeight) else text.height + gap + controlsHeight
    val height = desiredHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
    val (controlsY, controlRowHeight) =
        if (inline) 0 to height else (text.height + gap) to controlsHeight
    layout(width, height) {
      text.placeRelative(0, if (inline) (height - text.height) / 2 else 0)
      if (controlsInline) {
        var x = width - controlsWidth
        controls.forEach {
          val y = controlsY + (controlRowHeight - it.height) / 2
          it.placeRelative(x, y)
          x += it.width + gap
        }
      } else {
        var y = text.height + gap
        controls.forEach {
          it.placeRelative(width - it.width, y)
          y += it.height + gap
        }
      }
    }
  }
}

private fun snackbarContentWidth(
    constraints: Constraints,
    textWidth: Int,
    controlsWidth: Int,
    controlCount: Int,
    gap: Int,
): Int =
    if (constraints.hasBoundedWidth) constraints.maxWidth
    else {
      val controlsGap = if (controlCount == 0) 0 else gap
      (textWidth + controlsWidth + controlsGap).coerceAtLeast(constraints.minWidth)
    }
