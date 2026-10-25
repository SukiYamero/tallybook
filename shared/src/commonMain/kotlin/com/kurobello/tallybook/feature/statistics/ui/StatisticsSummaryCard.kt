package com.kurobello.tallybook.feature.statistics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kurobello.tallybook.core.model.Change
import com.kurobello.tallybook.core.model.Direction
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsSummary
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.statistics_balance
import tallybook.shared.generated.resources.statistics_change_down_description
import tallybook.shared.generated.resources.statistics_change_flat_description
import tallybook.shared.generated.resources.statistics_change_up_description
import tallybook.shared.generated.resources.statistics_income
import tallybook.shared.generated.resources.statistics_spent
import tallybook.shared.generated.resources.statistics_vs_previous

@Composable
internal fun StatisticsSummaryCard(
    summary: StatisticsSummary,
    modifier: Modifier = Modifier,
    stale: Boolean = false,
) {
  TallybookSurface(modifier = modifier.fillMaxWidth()) {
    Column(
        modifier = Modifier.padding(TallybookTheme.spacing.space20),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
        TallybookText(
            stringResource(Res.string.statistics_spent),
            color = TallybookTheme.colors.foreground.secondary,
            style = TallybookTheme.typography.balanceLabel,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space10),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
          TallybookText(summary.gastos, style = TallybookTheme.typography.headline)
          summary.change?.let { SummaryChange(it) }
        }
        val previous = summary.previousGastos
        val comparison = summary.comparison
        if (previous != null && comparison != null) {
          TallybookText(
              stringResource(Res.string.statistics_vs_previous, previous, comparison.phrase()),
              color = TallybookTheme.colors.foreground.tertiary,
              style = TallybookTheme.typography.bodySmall,
          )
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12)) {
        SummaryFigure(
            stringResource(Res.string.statistics_income),
            summary.ingresos,
            Modifier.weight(1f),
        )
        SummaryFigure(
            stringResource(Res.string.statistics_balance),
            summary.balance,
            Modifier.weight(1f),
        )
      }
      if (stale) StatisticsStaleNotice()
    }
  }
}

@Composable
private fun SummaryChange(change: Change) {
  val indicator = change.indicator()
  val description =
      when (change.direction) {
        Direction.Up -> stringResource(Res.string.statistics_change_up_description, change.percent)
        Direction.Down ->
            stringResource(Res.string.statistics_change_down_description, change.percent)
        Direction.Flat -> stringResource(Res.string.statistics_change_flat_description)
      }
  TallybookText(
      indicator.label,
      modifier = Modifier.clearAndSetSemantics { contentDescription = description },
      color = indicator.tone.color(),
      style = TallybookTheme.typography.bodyEmphasis,
  )
}

@Composable
private fun SummaryFigure(label: String, amount: String, modifier: Modifier) {
  Column(
      modifier = modifier.semantics(mergeDescendants = true) {},
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space2),
  ) {
    TallybookText(
        label,
        color = TallybookTheme.colors.foreground.secondary,
        style = TallybookTheme.typography.caption,
    )
    TallybookText(amount, style = TallybookTheme.typography.balanceSecondaryAmount)
  }
}
