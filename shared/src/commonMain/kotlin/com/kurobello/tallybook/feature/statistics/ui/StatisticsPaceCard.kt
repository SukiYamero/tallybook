package com.kurobello.tallybook.feature.statistics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.model.Direction
import com.kurobello.tallybook.core.ui.chart.TallybookChartTheme
import com.kurobello.tallybook.core.ui.chart.TallybookPaceChart
import com.kurobello.tallybook.core.ui.chart.changePercentText
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsPace
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.statistics_pace_above
import tallybook.shared.generated.resources.statistics_pace_below
import tallybook.shared.generated.resources.statistics_pace_description
import tallybook.shared.generated.resources.statistics_pace_even
import tallybook.shared.generated.resources.statistics_pace_legend_average
import tallybook.shared.generated.resources.statistics_pace_legend_projection
import tallybook.shared.generated.resources.statistics_pace_projection
import tallybook.shared.generated.resources.statistics_pace_title
import tallybook.shared.generated.resources.statistics_pace_today

@Composable
internal fun StatisticsPaceCard(pace: StatisticsPace, modifier: Modifier = Modifier) {
  val month = monthName(pace.today.month)
  val headline =
      when (pace.change.direction) {
        Direction.Up ->
            stringResource(Res.string.statistics_pace_above, changePercentText(pace.change.percent))
        Direction.Down ->
            stringResource(Res.string.statistics_pace_below, changePercentText(pace.change.percent))
        Direction.Flat -> stringResource(Res.string.statistics_pace_even)
      }
  StatisticsCard(stringResource(Res.string.statistics_pace_title), modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6)) {
      TallybookText(headline, style = TallybookTheme.typography.title)
      TallybookText(
          stringResource(
              Res.string.statistics_pace_projection,
              month,
              pace.projectionText,
              pace.averageText,
          ),
          color = TallybookTheme.colors.foreground.secondary,
          style = TallybookTheme.typography.bodySmall,
      )
    }
    TallybookPaceChart(
        cumulative = pace.cumulative,
        averageMonth = pace.averageMonth,
        projection = pace.projection,
        daysInMonth = pace.daysInMonth,
        todayLabel = stringResource(Res.string.statistics_pace_today, pace.spentText),
        contentDescription =
            stringResource(
                Res.string.statistics_pace_description,
                month,
                pace.spentText,
                pace.projectionText,
                pace.averageText,
            ),
        modifier = Modifier.fillMaxWidth(),
    )
    val chart = TallybookChartTheme.colors
    StatisticsLegend(
        listOf(
            LegendEntry(month.replaceFirstChar(Char::titlecase), chart.pace),
            LegendEntry(stringResource(Res.string.statistics_pace_legend_average), chart.average),
            LegendEntry(
                stringResource(Res.string.statistics_pace_legend_projection),
                chart.projection,
            ),
        )
    )
  }
}
