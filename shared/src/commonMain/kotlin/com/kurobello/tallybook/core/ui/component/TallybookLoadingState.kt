package com.kurobello.tallybook.core.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

private const val FULL_ROTATION_DEGREES = 360f
private const val PROGRESS_ARC_START_ANGLE = -90f
private const val PROGRESS_ARC_SWEEP_ANGLE = 270f
private const val HALF_STROKE_WIDTH = 2f

@Composable
internal fun TallybookProgressIndicator(
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
  val rotation =
      rememberInfiniteTransition(label = "loading_rotation")
          .animateFloat(
              initialValue = 0f,
              targetValue = FULL_ROTATION_DEGREES,
              animationSpec =
                  infiniteRepeatable(
                      animation =
                          tween(
                              durationMillis = TallybookTheme.motion.loadingRotationMillis,
                              easing = LinearEasing,
                          )
                  ),
              label = "loading_rotation_degrees",
          )
          .value
  val brandColor = TallybookTheme.colors.brand.primary
  val indicatorSize = TallybookTheme.dimensions.loadingIndicatorSize
  val strokeWidth = TallybookTheme.dimensions.loadingIndicatorStrokeWidth

  Canvas(
      modifier =
          modifier.size(indicatorSize).rotate(rotation).semantics {
            this.contentDescription = contentDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
          }
  ) {
    val strokeWidthPx = strokeWidth.toPx()
    val inset = strokeWidthPx / HALF_STROKE_WIDTH
    drawArc(
        color = brandColor,
        startAngle = PROGRESS_ARC_START_ANGLE,
        sweepAngle = PROGRESS_ARC_SWEEP_ANGLE,
        useCenter = false,
        topLeft = Offset(inset, inset),
        size = Size(size.width - strokeWidthPx, size.height - strokeWidthPx),
        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
    )
  }
}

@Composable
internal fun TallybookLoadingState(
    contentDescription: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
  val supportingTextGap = TallybookTheme.spacing.space12
  Layout(
      modifier = modifier.fillMaxSize(),
      content = {
        TallybookProgressIndicator(contentDescription = contentDescription)
        supportingText?.let {
          TallybookText(
              text = it,
              modifier = Modifier.padding(horizontal = TallybookTheme.spacing.space20),
              style = TallybookTheme.typography.body.copy(textAlign = TextAlign.Center),
          )
        }
      },
  ) { measurables, constraints ->
    val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
    val indicator = measurables.first().measure(childConstraints)
    val supportingCopy = measurables.getOrNull(1)?.measure(childConstraints)
    val supportingTextGapPx = supportingTextGap.roundToPx()
    val desiredHeight =
        supportingCopy?.let { copy -> indicator.height + (supportingTextGapPx + copy.height) * 2 }
            ?: indicator.height
    val width =
        if (constraints.hasBoundedWidth) {
          constraints.maxWidth
        } else {
          maxOf(indicator.width, supportingCopy?.width ?: 0)
              .coerceIn(constraints.minWidth, constraints.maxWidth)
        }
    val height = desiredHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
    val indicatorX = (width - indicator.width) / 2
    val indicatorY = (height - indicator.height) / 2

    layout(width, height) {
      indicator.placeRelative(indicatorX, indicatorY)
      supportingCopy?.let { copy ->
        val copyX = (width - copy.width) / 2
        val desiredCopyY = indicatorY + indicator.height + supportingTextGapPx
        val copyY = desiredCopyY.coerceAtMost(height - copy.height).coerceAtLeast(0)
        copy.placeRelative(copyX, copyY)
      }
    }
  }
}
