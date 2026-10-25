package com.kurobello.tallybook.core.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

/** A slice of the ring; [basisPoints] are hundredths of a percent, so 10 000 fills the ring. */
@Immutable internal data class DonutSlice(val basisPoints: Int, val color: Color)

internal const val DONUT_FULL_BASIS_POINTS = 10_000
private const val FULL_SWEEP_DEGREES = 360f
private const val START_ANGLE_DEGREES = -90f

@Composable
internal fun TallybookDonut(
    slices: List<DonutSlice>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    center: @Composable () -> Unit = {},
) {
  val trackColor = TallybookChartTheme.colors.track
  val thickness = TallybookTheme.dimensions.chartDonutThickness
  Box(
      modifier =
          modifier.size(TallybookTheme.dimensions.chartDonutSize).clearAndSetSemantics {
            this.contentDescription = contentDescription
          },
      contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawDonut(slices = slices, trackColor = trackColor, strokeWidth = thickness.toPx())
    }
    center()
  }
}

private fun DrawScope.drawDonut(slices: List<DonutSlice>, trackColor: Color, strokeWidth: Float) {
  val inset = strokeWidth / 2
  val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
  val topLeft = Offset(inset, inset)
  val stroke = Stroke(width = strokeWidth)
  drawArc(trackColor, 0f, FULL_SWEEP_DEGREES, false, topLeft, arcSize, style = stroke)
  var startAngle = START_ANGLE_DEGREES
  var remaining = DONUT_FULL_BASIS_POINTS
  slices.forEach { slice ->
    // Shares are rounded upstream, so a total above 100 % is clipped instead of overlapping.
    val basisPoints = slice.basisPoints.coerceIn(0, remaining)
    if (basisPoints == 0) return@forEach
    val sweep = FULL_SWEEP_DEGREES * basisPoints / DONUT_FULL_BASIS_POINTS
    drawArc(slice.color, startAngle, sweep, false, topLeft, arcSize, style = stroke)
    startAngle += sweep
    remaining -= basisPoints
  }
}
