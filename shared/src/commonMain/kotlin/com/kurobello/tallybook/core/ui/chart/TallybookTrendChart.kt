package com.kurobello.tallybook.core.ui.chart

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore

@Immutable
internal data class TrendPoint(
    val label: String,
    val gastos: Double,
    val ingresos: Double,
    val partial: Boolean,
)

/**
 * Monthly gastos and ingresos as grouped columns with the balance as a line. The chart is one
 * accessibility node: [contentDescription] carries the summary, the selected month is its state,
 * and every month is a custom accessibility action, so selection never depends on the drawing.
 */
@Composable
internal fun TallybookTrendChart(
    points: List<TrendPoint>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
  val selectedLabel = points.getOrNull(selectedIndex)?.label
  Box(
      modifier =
          modifier.clearAndSetSemantics {
            this.contentDescription = contentDescription
            if (selectedLabel != null) stateDescription = selectedLabel
            customActions = points.mapIndexed { index, point ->
              CustomAccessibilityAction(point.label) {
                onSelect(index)
                true
              }
            }
          }
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6)) {
      TrendChartCanvas(points = points, selectedIndex = selectedIndex)
      ChartSlotLabels(
          labels = points.map(TrendPoint::label),
          emphasizedIndex = selectedIndex.takeIf { it in points.indices },
          partialIndices = points.partialIndices(),
          edgeInset = TallybookTheme.dimensions.chartPointSize / 2,
      )
    }
    Row(modifier = Modifier.matchParentSize()) {
      points.indices.forEach { index ->
        Box(
            Modifier.weight(1f).fillMaxHeight().clickable(
                interactionSource = null,
                indication = null,
            ) {
              onSelect(index)
            }
        )
      }
    }
  }
}

@Composable
private fun TrendChartCanvas(points: List<TrendPoint>, selectedIndex: Int) {
  val slotSpacing = TallybookTheme.spacing.space16
  val producer = remember { CartesianChartModelProducer() }
  LaunchedEffect(producer, points) {
    producer.runTransaction {
      if (points.isEmpty()) return@runTransaction
      columnModel {
        series(points.map(TrendPoint::gastos))
        series(points.map(TrendPoint::ingresos))
      }
      lineModel { series(points.map { it.ingresos - it.gastos }) }
    }
  }
  val chart =
      rememberCartesianChart(
          rememberTrendColumnLayer(
              emphasizedX = selectedIndex.takeIf { it in points.indices },
              partialX = points.partialIndices(),
              slotSpacing = slotSpacing,
          ),
          rememberBalanceLineLayer(),
          layerPadding = rememberSlotPadding(slotSpacing),
          decorations = rememberBaseline(TallybookChartTheme.colors.baseline),
      )
  ProvideTallybookChartTheme {
    CartesianChartHost(
        chart = chart,
        modelProducer = producer,
        modifier = Modifier.fillMaxWidth(),
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        animationSpec = TallybookChartTheme.animationSpec,
        chartAreaHeight = TallybookTheme.dimensions.chartTrendAreaHeight,
    )
  }
}

@Composable
private fun rememberTrendColumnLayer(
    emphasizedX: Int?,
    partialX: Set<Int>,
    slotSpacing: Dp,
): ColumnCartesianLayer {
  val colors = TallybookChartTheme.colors
  val dimensions = TallybookTheme.dimensions
  val columnProvider =
      remember(colors, emphasizedX, partialX, dimensions) {
        EmphasisColumnProvider(
            seriesColors = listOf(colors.gastos, colors.ingresos),
            thickness = dimensions.chartColumnWidth,
            shape = columnTopShape(dimensions.chartColumnCornerRadius),
            partialStrokeThickness = dimensions.chartPartialStrokeWidth,
            emphasizedX = emphasizedX,
            partialX = partialX,
        )
      }
  val groupSpacing = dimensions.chartColumnGroupSpacing
  val mergeMode =
      remember(groupSpacing) {
        { _: ExtraStore -> ColumnCartesianLayer.MergeMode.Grouped(columnSpacing = groupSpacing) }
      }
  return rememberColumnCartesianLayer(
      columnProvider = columnProvider,
      columnCollectionSpacing = slotSpacing,
      mergeMode = mergeMode,
  )
}

@Composable
private fun rememberBalanceLineLayer(): LineCartesianLayer {
  val colors = TallybookChartTheme.colors
  val dimensions = TallybookTheme.dimensions
  val balancePoint =
      remember(colors, dimensions) {
        LineCartesianLayer.Point(
            component =
                ShapeComponent(
                    fill = Fill(colors.pointFill),
                    shape = CircleShape,
                    strokeFill = Fill(colors.balance),
                    strokeThickness = dimensions.chartPointStrokeWidth,
                ),
            size = dimensions.chartPointSize,
        )
      }
  val balanceLine =
      LineCartesianLayer.rememberLine(
          fill = remember(colors) { LineCartesianLayer.LineFill.single(Fill(colors.balance)) },
          stroke =
              LineCartesianLayer.LineStroke.Continuous(
                  thickness = dimensions.chartLineThickness,
                  cap = StrokeCap.Round,
              ),
          pointProvider =
              remember(balancePoint) { LineCartesianLayer.PointProvider.single(balancePoint) },
      )
  return rememberLineCartesianLayer(
      lineProvider = remember(balanceLine) { LineCartesianLayer.LineProvider.series(balanceLine) },
      pointSpacing = TallybookTheme.spacing.space0,
  )
}

private fun List<TrendPoint>.partialIndices(): Set<Int> =
    indices.filterTo(mutableSetOf()) { this[it].partial }
