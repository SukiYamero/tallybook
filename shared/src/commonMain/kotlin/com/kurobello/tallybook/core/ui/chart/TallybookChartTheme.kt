@file:Suppress("MagicNumber") // This file is the sole definition point for chart alpha tokens.

package com.kurobello.tallybook.core.ui.chart

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.VicoTheme

@Immutable
internal data class TallybookChartColors(
    val gastos: Color,
    val ingresos: Color,
    val balance: Color,
    val average: Color,
    val pace: Color,
    val paceArea: Color,
    val projection: Color,
    val track: Color,
    val baseline: Color,
    val label: Color,
    val pointFill: Color,
    val markerLabelBackground: Color,
    val markerLabelContent: Color,
)

internal object TallybookChartTheme {
  const val UNSELECTED_ALPHA = 0.5f
  const val PARTIAL_FILL_ALPHA = 0.4f
  const val PACE_AREA_ALPHA = 0.35f
  const val PROJECTION_ALPHA = 0.7f

  val colors: TallybookChartColors
    @Composable
    @ReadOnlyComposable
    get() {
      val colors = TallybookTheme.colors
      return TallybookChartColors(
          gastos = colors.status.danger.base,
          ingresos = colors.brand.primary,
          balance = colors.foreground.primary,
          average = colors.foreground.faint,
          pace = colors.brand.secondary,
          paceArea = colors.brand.primary.copy(alpha = PACE_AREA_ALPHA),
          projection = colors.brand.secondary.copy(alpha = PROJECTION_ALPHA),
          track = colors.surface.high,
          baseline = colors.border.strong,
          label = colors.foreground.secondary,
          pointFill = colors.surface.card,
          markerLabelBackground = colors.foreground.primary,
          markerLabelContent = colors.surface.card,
      )
    }

  val animationSpec: AnimationSpec<Float> =
      tween(
          durationMillis = TallybookTheme.motion.pushInMillis,
          easing = TallybookTheme.motion.iosEasing,
      )
}

/** Paints every Vico default that a chart does not set explicitly with Tallybook tokens. */
@Composable
internal fun ProvideTallybookChartTheme(content: @Composable () -> Unit) {
  val colors = TallybookTheme.colors
  val theme =
      remember(colors) {
        VicoTheme(
            candlestickCartesianLayerColors =
                VicoTheme.CandlestickCartesianLayerColors(
                    bullish = colors.status.success.base,
                    neutral = colors.foreground.faint,
                    bearish = colors.status.danger.base,
                ),
            columnCartesianLayerColors = colors.chart,
            lineColor = colors.border.base,
            textColor = colors.foreground.tertiary,
        )
      }
  ProvideVicoTheme(theme, content)
}
