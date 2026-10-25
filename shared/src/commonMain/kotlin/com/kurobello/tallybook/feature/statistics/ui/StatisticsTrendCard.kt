package com.kurobello.tallybook.feature.statistics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import com.kurobello.tallybook.core.ui.chart.TallybookChartTheme
import com.kurobello.tallybook.core.ui.chart.TallybookTrendChart
import com.kurobello.tallybook.core.ui.chart.TrendPoint
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsTrendMonth
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.statistics_legend_balance
import tallybook.shared.generated.resources.statistics_legend_gastos
import tallybook.shared.generated.resources.statistics_legend_ingresos
import tallybook.shared.generated.resources.statistics_month_of_year
import tallybook.shared.generated.resources.statistics_trend_description
import tallybook.shared.generated.resources.statistics_trend_month_item
import tallybook.shared.generated.resources.statistics_trend_partial_note
import tallybook.shared.generated.resources.statistics_trend_selected_partial
import tallybook.shared.generated.resources.statistics_trend_title_month
import tallybook.shared.generated.resources.statistics_trend_title_year

private const val MONTH_SEPARATOR = "; "

/**
 * [year] titles a Year scope's months and is null for Month's six; [selected] is the index of the
 * month whose values show above the chart.
 */
@Composable
internal fun StatisticsTrendCard(
    months: List<StatisticsTrendMonth>,
    year: Int?,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
  val title =
      if (year == null) stringResource(Res.string.statistics_trend_title_month)
      else stringResource(Res.string.statistics_trend_title_year, year)
  val labels = months.map { shortMonthName(it.month.month) }
  val points = months.mapIndexed { index, month ->
    TrendPoint(labels[index], month.gastos, month.ingresos, month.partial)
  }
  val items = months.mapIndexed { index, month ->
    stringResource(
        Res.string.statistics_trend_month_item,
        labels[index],
        month.gastosText,
        month.ingresosText,
    )
  }
  StatisticsCard(title, modifier) {
    months.getOrNull(selected)?.let { SelectedMonth(it) }
    TallybookTrendChart(
        points = points,
        selectedIndex = selected,
        onSelect = onSelect,
        contentDescription =
            stringResource(
                Res.string.statistics_trend_description,
                items.joinToString(MONTH_SEPARATOR),
            ),
        modifier = Modifier.fillMaxWidth(),
    )
    val chart = TallybookChartTheme.colors
    val partial = months.lastOrNull()?.takeIf { it.partial }
    StatisticsLegend(
        listOf(
            LegendEntry(stringResource(Res.string.statistics_legend_gastos), chart.gastos),
            LegendEntry(stringResource(Res.string.statistics_legend_ingresos), chart.ingresos),
            LegendEntry(stringResource(Res.string.statistics_legend_balance), chart.balance),
        ),
        note =
            partial?.partialEnd?.let {
              stringResource(Res.string.statistics_trend_partial_note, labels.last(), it.day)
            },
    )
  }
}

@Composable
private fun SelectedMonth(month: StatisticsTrendMonth) {
  val name =
      stringResource(
              Res.string.statistics_month_of_year,
              monthName(month.month.month),
              month.month.year,
          )
          .replaceFirstChar(Char::titlecase)
  val partialEnd = month.partialEnd
  Column(
      modifier = Modifier.semantics(mergeDescendants = true) {},
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
  ) {
    TallybookText(
        if (partialEnd == null) name
        else stringResource(Res.string.statistics_trend_selected_partial, name, partialEnd.day),
        color = TallybookTheme.colors.foreground.secondary,
        style = TallybookTheme.typography.caption,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12)) {
      TrendFigure(
          stringResource(Res.string.statistics_legend_gastos),
          month.gastosText,
          Modifier.weight(1f),
      )
      TrendFigure(
          stringResource(Res.string.statistics_legend_ingresos),
          month.ingresosText,
          Modifier.weight(1f),
      )
      TrendFigure(
          stringResource(Res.string.statistics_legend_balance),
          month.balanceText,
          Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun TrendFigure(label: String, amount: String, modifier: Modifier) {
  Column(modifier = modifier) {
    TallybookText(
        label,
        color = TallybookTheme.colors.foreground.tertiary,
        style = TallybookTheme.typography.caption,
    )
    TallybookText(amount, style = TallybookTheme.typography.bodyEmphasis)
  }
}
