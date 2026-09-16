package com.kurobello.tallybook.core.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
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
  Column(
      modifier = modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    TallybookProgressIndicator(contentDescription = contentDescription)
    supportingText?.let {
      TallybookText(
          text = it,
          modifier = Modifier.padding(top = TallybookTheme.spacing.space12),
      )
    }
  }
}
