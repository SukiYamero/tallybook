package com.kurobello.tallybook.core.ui.chart

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.patrykandpatrick.vico.compose.cartesian.CartesianChart
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore

private enum class PaceSeries {
  Average,
  Projection,
  Cumulative,
}

/**
 * The month's cumulative spending ([cumulative], one value per elapsed day, the last one being
 * today) against a straight line to [averageMonth], plus a dotted projection from today to
 * [projection] on the last of [daysInMonth] days. Today carries a persistent marker labelled
 * [todayLabel].
 */
@Suppress("LongParameterList")
@Composable
internal fun TallybookPaceChart(
    cumulative: List<Double>,
    averageMonth: Double,
    projection: Double,
    daysInMonth: Int,
    todayLabel: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
  val lastDayX = (maxOf(daysInMonth, cumulative.size) - 1).coerceAtLeast(0)
  val todayX = cumulative.lastIndex
  val producer = rememberPaceModelProducer(cumulative, averageMonth, projection, lastDayX)
  val lineProvider = rememberPaceLineProvider(todayX)
  val rangeProvider =
      remember(lastDayX) {
        CartesianLayerRangeProvider.fixed(minX = 0.0, maxX = lastDayX.toDouble(), minY = 0.0)
      }
  val todayMarker = rememberTodayMarker(todayLabel)
  val persistentMarkers = remember(todayMarker, todayX) { todayMarkerAt(todayMarker, todayX) }
  val chart =
      rememberCartesianChart(
          rememberLineCartesianLayer(
              lineProvider = lineProvider,
              pointSpacing = TallybookTheme.spacing.space0,
              rangeProvider = rangeProvider,
          ),
          persistentMarkers = persistentMarkers,
          getXStep = DailyXStep,
      )
  ProvideTallybookChartTheme {
    CartesianChartHost(
        chart = chart,
        modelProducer = producer,
        modifier =
            modifier.fillMaxWidth().clearAndSetSemantics {
              this.contentDescription = contentDescription
            },
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        animationSpec = TallybookChartTheme.animationSpec,
        chartAreaHeight = TallybookTheme.dimensions.chartPaceAreaHeight,
    )
  }
}

private val DailyXStep: (CartesianChartModel, Double, Double) -> Double = { _, _, _ -> 1.0 }

@Composable
private fun rememberPaceModelProducer(
    cumulative: List<Double>,
    averageMonth: Double,
    projection: Double,
    lastDayX: Int,
): CartesianChartModelProducer {
  val producer = remember { CartesianChartModelProducer() }
  LaunchedEffect(producer, cumulative, averageMonth, projection, lastDayX) {
    producer.runTransaction {
      if (cumulative.isEmpty()) return@runTransaction
      val todayX = cumulative.lastIndex
      lineModel {
        series(
            x = listOf(0, lastDayX),
            y = listOf(averageMonth / (lastDayX + 1), averageMonth),
            key = PaceSeries.Average,
        )
        if (todayX < lastDayX) {
          series(
              x = listOf(todayX, lastDayX),
              y = listOf(cumulative.last(), projection),
              key = PaceSeries.Projection,
          )
        }
        series(x = cumulative.indices.toList(), y = cumulative, key = PaceSeries.Cumulative)
      }
    }
  }
  return producer
}

@Composable
private fun rememberPaceLineProvider(todayX: Int): LineCartesianLayer.LineProvider {
  val colors = TallybookChartTheme.colors
  val dimensions = TallybookTheme.dimensions
  val todayPoint =
      remember(colors, dimensions) {
        LineCartesianLayer.Point(
            component =
                ShapeComponent(
                    fill = Fill(colors.pace),
                    shape = CircleShape,
                    strokeFill = Fill(colors.pointFill),
                    strokeThickness = dimensions.chartPointStrokeWidth,
                ),
            size = dimensions.chartTodayIndicatorSize,
        )
      }
  val cumulativeLine =
      LineCartesianLayer.rememberLine(
          fill = remember(colors) { LineCartesianLayer.LineFill.single(Fill(colors.pace)) },
          stroke =
              LineCartesianLayer.LineStroke.Continuous(
                  thickness = dimensions.chartLineThickness,
                  cap = StrokeCap.Round,
              ),
          areaFill = remember(colors) { LineCartesianLayer.AreaFill.single(Fill(colors.paceArea)) },
          pointProvider = remember(todayX, todayPoint) { TodayPointProvider(todayX, todayPoint) },
      )
  val averageLine =
      rememberDashedLine(colors.average, dimensions.chartDashLength, dimensions.chartDashGap)
  val projectionLine =
      rememberDashedLine(colors.projection, dimensions.chartDotLength, dimensions.chartDotGap)
  return remember(cumulativeLine, averageLine, projectionLine) {
    val lines =
        mapOf<Any, LineCartesianLayer.Line>(
            PaceSeries.Average to averageLine,
            PaceSeries.Projection to projectionLine,
            PaceSeries.Cumulative to cumulativeLine,
        )
    LineCartesianLayer.LineProvider { seriesKey, _, _ -> lines.getValue(seriesKey) }
  }
}

@Composable
private fun rememberDashedLine(
    color: Color,
    dashLength: Dp,
    gapLength: Dp,
): LineCartesianLayer.Line =
    LineCartesianLayer.rememberLine(
        fill = remember(color) { LineCartesianLayer.LineFill.single(Fill(color)) },
        stroke =
            LineCartesianLayer.LineStroke.Dashed(
                thickness = TallybookTheme.dimensions.chartLineThickness,
                cap = StrokeCap.Round,
                dashLength = dashLength,
                gapLength = gapLength,
            ),
    )

@Composable
private fun rememberTodayMarker(todayLabel: String): DefaultCartesianMarker {
  val colors = TallybookChartTheme.colors
  val spacing = TallybookTheme.spacing
  val radius = TallybookTheme.radii.radius8
  val label =
      rememberTextComponent(
          style = TallybookTheme.typography.caption.copy(color = colors.markerLabelContent),
          margins = Insets(bottom = spacing.space8),
          padding = Insets(horizontal = spacing.space8, vertical = spacing.space2),
          background =
              remember(colors, radius) {
                ShapeComponent(
                    fill = Fill(colors.markerLabelBackground),
                    shape = RoundedCornerShape(radius),
                )
              },
      )
  val guidelineThickness = TallybookTheme.dimensions.chartBaselineThickness
  val guideline =
      remember(colors, guidelineThickness) {
        LineComponent(fill = Fill(colors.baseline), thickness = guidelineThickness)
      }
  return rememberDefaultCartesianMarker(
      label = label,
      valueFormatter =
          remember(todayLabel) { DefaultCartesianMarker.ValueFormatter { _, _ -> todayLabel } },
      labelPosition = DefaultCartesianMarker.LabelPosition.AbovePoint,
      guideline = guideline,
  )
}

private fun todayMarkerAt(
    marker: DefaultCartesianMarker,
    todayX: Int,
): (CartesianChart.PersistentMarkerScope.(ExtraStore) -> Unit)? =
    if (todayX < 0) null else { _ -> marker at todayX }

/** Only today's entry gets a point, so the marker sits on the cumulative line alone. */
private data class TodayPointProvider(
    private val todayX: Int,
    private val point: LineCartesianLayer.Point,
) : LineCartesianLayer.PointProvider {
  override fun getPoint(
      entry: LineCartesianLayerModel.Entry,
      extraStore: ExtraStore,
  ): LineCartesianLayer.Point? = point.takeIf { entry.x.toInt() == todayX }

  override fun getLargestPoint(extraStore: ExtraStore): LineCartesianLayer.Point = point
}
