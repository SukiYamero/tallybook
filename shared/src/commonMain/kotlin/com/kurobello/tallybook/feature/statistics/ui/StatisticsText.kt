package com.kurobello.tallybook.feature.statistics.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kurobello.tallybook.core.model.Change
import com.kurobello.tallybook.core.model.Direction
import com.kurobello.tallybook.core.ui.account.labelResource
import com.kurobello.tallybook.core.ui.chart.ChangeIndicator
import com.kurobello.tallybook.core.ui.chart.ChangeTone
import com.kurobello.tallybook.core.ui.chart.chartRankColor
import com.kurobello.tallybook.core.ui.chart.chartRestColor
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsColor
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsLabel
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.activity_missing_account
import tallybook.shared.generated.resources.statistics_change_down
import tallybook.shared.generated.resources.statistics_change_flat
import tallybook.shared.generated.resources.statistics_change_up
import tallybook.shared.generated.resources.statistics_general
import tallybook.shared.generated.resources.statistics_otras
import tallybook.shared.generated.resources.statistics_unknown_etiqueta
import tallybook.shared.generated.resources.unknown_category

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
internal fun StatisticsColor.color(): Color =
    when (this) {
      is StatisticsColor.Rank -> chartRankColor(rank)
      StatisticsColor.Otras -> chartRestColor()
    }

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
