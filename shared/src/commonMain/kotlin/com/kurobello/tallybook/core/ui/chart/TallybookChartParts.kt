package com.kurobello.tallybook.core.ui.chart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookSpacing
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerPadding
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore

/**
 * Columns keyed by their x index: when [emphasizedX] is set every other x is dimmed, and an x in
 * [partialX] keeps a full-color outline over a faint fill so a month in progress never reads as
 * merely unselected.
 */
internal data class EmphasisColumnProvider(
    private val seriesColors: List<Color>,
    private val thickness: Dp,
    private val shape: Shape,
    private val partialStrokeThickness: Dp,
    private val emphasizedX: Int?,
    private val partialX: Set<Int>,
) : ColumnCartesianLayer.ColumnProvider {
  private val variants: List<ColumnVariants> = seriesColors.map(::columnVariants)

  override fun getColumn(
      entry: ColumnCartesianLayerModel.Entry,
      extraStore: ExtraStore,
  ): LineComponent {
    val x = entry.x.toInt()
    val variant = variants[entry.seriesIndex % variants.size]
    val emphasized = emphasizedX == null || emphasizedX == x
    return when {
      x in partialX && emphasized -> variant.partial
      x in partialX -> variant.dimmedPartial
      emphasized -> variant.complete
      else -> variant.dimmed
    }
  }

  override fun getWidestSeriesColumn(
      seriesKey: Any,
      seriesIndex: Int,
      extraStore: ExtraStore,
  ): LineComponent = variants[seriesIndex % variants.size].complete

  private fun columnVariants(color: Color): ColumnVariants {
    val dimmed = color.copy(alpha = color.alpha * TallybookChartTheme.UNSELECTED_ALPHA)
    return ColumnVariants(
        complete = column(fill = color),
        dimmed = column(fill = dimmed),
        partial = column(fill = color.partialFill(), stroke = color),
        dimmedPartial = column(fill = dimmed.partialFill(), stroke = dimmed),
    )
  }

  private fun column(fill: Color, stroke: Color = Color.Transparent) =
      LineComponent(
          fill = Fill(fill),
          thickness = thickness,
          shape = shape,
          strokeFill = Fill(stroke),
          strokeThickness =
              if (stroke == Color.Transparent) TallybookSpacing.space0 else partialStrokeThickness,
      )

  private fun Color.partialFill() = copy(alpha = alpha * TallybookChartTheme.PARTIAL_FILL_ALPHA)

  private class ColumnVariants(
      val complete: LineComponent,
      val dimmed: LineComponent,
      val partial: LineComponent,
      val dimmedPartial: LineComponent,
  )
}

internal fun columnTopShape(radius: Dp): Shape =
    RoundedCornerShape(topStart = radius, topEnd = radius)

/**
 * One label per x slot, in the same equal-width slots Vico lays the columns out in once the chart
 * zooms its content to the full width; [edgeInset] mirrors the layer's unscalable padding.
 */
@Composable
internal fun ChartSlotLabels(
    labels: List<String>,
    modifier: Modifier = Modifier,
    emphasizedIndex: Int? = null,
    partialIndices: Set<Int> = emptySet(),
    edgeInset: Dp = TallybookSpacing.space0,
) {
  val color = TallybookChartTheme.colors.label
  val dashLength = TallybookTheme.dimensions.chartDashLength
  val dashGap = TallybookTheme.dimensions.chartDashGap
  val underlineThickness = TallybookTheme.dimensions.chartBaselineThickness
  val captionStyle = TallybookTheme.typography.caption
  Row(modifier = modifier.fillMaxWidth().padding(horizontal = edgeInset)) {
    labels.forEachIndexed { index, label ->
      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
        val emphasized = emphasizedIndex == index
        TallybookText(
            text = label,
            modifier =
                if (index in partialIndices) {
                  Modifier.drawBehind {
                    val y = size.height - underlineThickness.toPx() / 2
                    drawLine(
                        color = color,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = underlineThickness.toPx(),
                        pathEffect =
                            PathEffect.dashPathEffect(
                                floatArrayOf(dashLength.toPx(), dashGap.toPx())
                            ),
                    )
                  }
                } else {
                  Modifier
                },
            color = color,
            style =
                captionStyle.copy(
                    fontWeight = if (emphasized) FontWeight.ExtraBold else captionStyle.fontWeight,
                    textAlign = TextAlign.Center,
                ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

/**
 * Half the slot spacing on each edge splits the zoomed-to-fit content into equal x slots, the
 * geometry [ChartSlotLabels] and the trend chart's tap targets rely on.
 */
@Composable
internal fun rememberSlotPadding(slotSpacing: Dp): (ExtraStore) -> CartesianLayerPadding =
    remember(slotSpacing) {
      val padding =
          CartesianLayerPadding(scalableStart = slotSpacing / 2, scalableEnd = slotSpacing / 2)
      ({ _ -> padding })
    }

@Composable
internal fun rememberBaseline(color: Color): List<HorizontalLine> {
  val thickness = TallybookTheme.dimensions.chartBaselineThickness
  return remember(color, thickness) {
    listOf(HorizontalLine(y = { 0.0 }, line = LineComponent(Fill(color), thickness)))
  }
}
