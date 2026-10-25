package com.kurobello.tallybook.feature.statistics.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kurobello.tallybook.core.model.Change
import com.kurobello.tallybook.core.model.Direction
import com.kurobello.tallybook.core.ui.account.labelResource
import com.kurobello.tallybook.core.ui.chart.ChangeIndicator
import com.kurobello.tallybook.core.ui.chart.ChangeTone
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsColor
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsLabel
import com.kurobello.tallybook.feature.statistics.viewmodel.chartPaletteIndex
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.activity_missing_account
import tallybook.shared.generated.resources.statistics_change_down
import tallybook.shared.generated.resources.statistics_change_flat
import tallybook.shared.generated.resources.statistics_change_up
import tallybook.shared.generated.resources.statistics_general
import tallybook.shared.generated.resources.statistics_otras
import tallybook.shared.generated.resources.statistics_share
import tallybook.shared.generated.resources.statistics_share_small
import tallybook.shared.generated.resources.statistics_unknown_etiqueta
import tallybook.shared.generated.resources.unknown_category

private const val BASIS_POINTS_PER_PERCENT = 100
private const val HALF_PERCENT = 50

@Composable
internal fun StatisticsLabel.text(): String =
    when (this) {
      is StatisticsLabel.Named -> nombre
      is StatisticsLabel.Medio -> stringResource(medio.labelResource)
      StatisticsLabel.UnknownCategoria -> stringResource(Res.string.unknown_category)
      StatisticsLabel.UnknownEtiqueta -> stringResource(Res.string.statistics_unknown_etiqueta)
      StatisticsLabel.UnknownCuenta -> stringResource(Res.string.activity_missing_account)
      StatisticsLabel.Otras -> stringResource(Res.string.statistics_otras)
      StatisticsLabel.General -> stringResource(Res.string.statistics_general)
    }

@Composable
internal fun StatisticsColor.color(): Color {
  val palette = TallybookTheme.colors.chart
  val faint = TallybookTheme.colors.foreground.faint
  return when (this) {
    is StatisticsColor.Rank -> chartPaletteIndex(rank, palette.size)?.let(palette::get) ?: faint
    StatisticsColor.Otras -> faint
  }
}

/** Rounded half up; a share too small to round to 1% still reads as present. */
@Composable
internal fun shareText(basisPoints: Int): String =
    if (basisPoints in 1 until HALF_PERCENT) stringResource(Res.string.statistics_share_small)
    else
        stringResource(
            Res.string.statistics_share,
            (basisPoints + HALF_PERCENT) / BASIS_POINTS_PER_PERCENT,
        )

internal fun shareFraction(basisPoints: Int): Float =
    basisPoints.toFloat() / (BASIS_POINTS_PER_PERCENT * BASIS_POINTS_PER_PERCENT)

/** A gasto that grows is unfavorable. */
@Composable
internal fun Change.indicator(): ChangeIndicator =
    when (direction) {
      Direction.Up ->
          ChangeIndicator(
              stringResource(Res.string.statistics_change_up, percent),
              ChangeTone.Unfavorable,
          )
      Direction.Down ->
          ChangeIndicator(
              stringResource(Res.string.statistics_change_down, percent),
              ChangeTone.Favorable,
          )
      Direction.Flat ->
          ChangeIndicator(stringResource(Res.string.statistics_change_flat), ChangeTone.Neutral)
    }

@Composable
internal fun ChangeTone.color(): Color =
    when (this) {
      ChangeTone.Unfavorable -> TallybookTheme.colors.status.danger.base
      ChangeTone.Favorable -> TallybookTheme.colors.status.success.base
      ChangeTone.Neutral -> TallybookTheme.colors.foreground.tertiary
    }
