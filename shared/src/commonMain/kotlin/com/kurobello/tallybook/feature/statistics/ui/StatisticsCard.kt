package com.kurobello.tallybook.feature.statistics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Composable
internal fun StatisticsCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
  TallybookSurface(modifier = modifier.fillMaxWidth()) {
    Column(
        modifier = Modifier.padding(TallybookTheme.spacing.space20),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
    ) {
      TallybookText(
          text = title,
          modifier = Modifier.semantics { heading() },
          color = TallybookTheme.colors.foreground.secondary,
          style = TallybookTheme.typography.bodyEmphasis,
      )
      content()
    }
  }
}

@Immutable internal data class LegendEntry(val label: String, val color: Color)

@Composable
internal fun StatisticsLegend(
    entries: List<LegendEntry>,
    modifier: Modifier = Modifier,
    note: String? = null,
) {
  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
  ) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
    ) {
      entries.forEach { entry ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
              Modifier.size(TallybookTheme.dimensions.chartLegendDot)
                  .background(entry.color, CircleShape)
          )
          TallybookText(
              entry.label,
              color = TallybookTheme.colors.foreground.secondary,
              style = TallybookTheme.typography.caption,
          )
        }
      }
    }
    if (note != null) {
      TallybookText(
          note,
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.caption,
      )
    }
  }
}
