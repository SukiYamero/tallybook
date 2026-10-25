package com.kurobello.tallybook.feature.statistics.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.TrendingDown
import com.composables.icons.lucide.TrendingUp
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsComparison
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsInsight
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.statistics_day_amount
import tallybook.shared.generated.resources.statistics_insight_biggest_day
import tallybook.shared.generated.resources.statistics_insight_fell
import tallybook.shared.generated.resources.statistics_insight_less
import tallybook.shared.generated.resources.statistics_insight_more
import tallybook.shared.generated.resources.statistics_insight_rose
import tallybook.shared.generated.resources.statistics_insights_title

@Composable
internal fun StatisticsInsightsCard(
    insights: List<StatisticsInsight>,
    comparison: StatisticsComparison?,
    modifier: Modifier = Modifier,
) {
  StatisticsCard(stringResource(Res.string.statistics_insights_title), modifier) {
    val than = comparison?.thanPhrase().orEmpty()
    insights.forEach { insight ->
      when (insight) {
        is StatisticsInsight.CategoryRose ->
            InsightRow(
                Lucide.TrendingUp,
                TallybookTheme.colors.status.danger.base,
                stringResource(
                    Res.string.statistics_insight_rose,
                    insight.label.text(),
                    insight.percent,
                ),
                stringResource(Res.string.statistics_insight_more, insight.difference, than),
            )
        is StatisticsInsight.CategoryFell ->
            InsightRow(
                Lucide.TrendingDown,
                TallybookTheme.colors.status.success.base,
                stringResource(
                    Res.string.statistics_insight_fell,
                    insight.label.text(),
                    insight.percent,
                ),
                stringResource(Res.string.statistics_insight_less, insight.difference, than),
            )
        is StatisticsInsight.BiggestDay ->
            InsightRow(
                Lucide.Calendar,
                TallybookTheme.colors.foreground.secondary,
                stringResource(Res.string.statistics_insight_biggest_day),
                stringResource(
                    Res.string.statistics_day_amount,
                    weekdayDate(insight.fecha),
                    insight.gasto,
                ),
            )
      }
    }
  }
}

@Composable
private fun InsightRow(icon: ImageVector, tint: Color, title: String, detail: String) {
  Row(
      modifier = Modifier.semantics(mergeDescendants = true) {},
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Image(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(TallybookTheme.dimensions.iconMedium),
        colorFilter = ColorFilter.tint(tint),
    )
    Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space2)) {
      TallybookText(title, style = TallybookTheme.typography.bodyEmphasis)
      TallybookText(
          detail,
          color = TallybookTheme.colors.foreground.secondary,
          style = TallybookTheme.typography.bodySmall,
      )
    }
  }
}
