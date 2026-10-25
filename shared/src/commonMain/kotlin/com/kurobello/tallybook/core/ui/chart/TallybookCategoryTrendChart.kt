package com.kurobello.tallybook.core.ui.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.DashedShape
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.LineComponent

/** Monthly totals of one category as columns, with [average] as a dashed reference line. */
@Suppress("LongParameterList")
@Composable
internal fun TallybookCategoryTrendChart(
    values: List<Double>,
    labels: List<String>,
    average: Double,
    partialLast: Boolean,
    color: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
  val partialIndices =
      if (partialLast && values.isNotEmpty()) setOf(values.lastIndex) else emptySet()
  Column(
      modifier = modifier.clearAndSetSemantics { this.contentDescription = contentDescription },
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
  ) {
    CategoryTrendCanvas(
        values = values,
        average = average,
        partialIndices = partialIndices,
        color = color,
    )
    ChartSlotLabels(labels = labels, partialIndices = partialIndices)
  }
}

@Composable
private fun CategoryTrendCanvas(
    values: List<Double>,
    average: Double,
    partialIndices: Set<Int>,
    color: Color,
) {
  val dimensions = TallybookTheme.dimensions
  val slotSpacing = TallybookTheme.spacing.space16
  val producer = remember { CartesianChartModelProducer() }
  LaunchedEffect(producer, values) {
    producer.runTransaction { if (values.isNotEmpty()) columnModel { series(values) } }
  }
  val columnProvider =
      remember(color, partialIndices, dimensions) {
        EmphasisColumnProvider(
            seriesColors = listOf(color),
            thickness = dimensions.chartWideColumnWidth,
            shape = columnTopShape(dimensions.chartWideColumnCornerRadius),
            partialStrokeThickness = dimensions.chartPartialStrokeWidth,
            emphasizedX = null,
            partialX = partialIndices,
        )
      }
  val maxY = maxOf(values.maxOrNull() ?: 0.0, average)
  val rangeProvider =
      remember(maxY) {
        CartesianLayerRangeProvider.fixed(minY = 0.0, maxY = maxY.takeIf { it > 0.0 })
      }
  val decorations = rememberAverageDecorations(average)
  val chart =
      rememberCartesianChart(
          rememberColumnCartesianLayer(
              columnProvider = columnProvider,
              columnCollectionSpacing = slotSpacing,
              rangeProvider = rangeProvider,
          ),
          layerPadding = rememberSlotPadding(slotSpacing),
          decorations = decorations,
      )
  ProvideTallybookChartTheme {
    CartesianChartHost(
        chart = chart,
        modelProducer = producer,
        modifier = Modifier.fillMaxWidth(),
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        animationSpec = TallybookChartTheme.animationSpec,
        chartAreaHeight = dimensions.chartCategoryTrendAreaHeight,
    )
  }
}

@Composable
private fun rememberAverageDecorations(average: Double): List<Decoration> {
  val colors = TallybookChartTheme.colors
  val dimensions = TallybookTheme.dimensions
  val baseline = rememberBaseline(colors.baseline)
  return remember(baseline, average, colors.average, dimensions) {
    baseline +
        HorizontalLine(
            y = { average },
            line =
                LineComponent(
                    fill = Fill(colors.average),
                    thickness = dimensions.chartLineThickness,
                    shape =
                        DashedShape(
                            dashLength = dimensions.chartDashLength,
                            gapLength = dimensions.chartDashGap,
                        ),
                ),
        )
  }
}
