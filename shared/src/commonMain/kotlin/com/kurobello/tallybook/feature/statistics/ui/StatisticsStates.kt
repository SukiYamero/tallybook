package com.kurobello.tallybook.feature.statistics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookSkeletonCard
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsFailure
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.home_load_error_body
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.navigation_add_movement
import tallybook.shared.generated.resources.retry
import tallybook.shared.generated.resources.statistics_empty_body
import tallybook.shared.generated.resources.statistics_empty_title
import tallybook.shared.generated.resources.statistics_error
import tallybook.shared.generated.resources.statistics_overflow_error
import tallybook.shared.generated.resources.statistics_stale

@Composable
internal fun StatisticsError(failure: StatisticsFailure?, onRetry: () -> Unit) {
  TallybookSurface(modifier = Modifier.fillMaxWidth()) {
    Column(
        modifier = Modifier.padding(TallybookTheme.spacing.space20),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
    ) {
      TallybookText(
          stringResource(
              if (failure == StatisticsFailure.CalculationOverflow)
                  Res.string.statistics_overflow_error
              else Res.string.statistics_error
          ),
          modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
          style = TallybookTheme.typography.bodyEmphasis,
      )
      TallybookText(
          stringResource(Res.string.home_load_error_body),
          color = TallybookTheme.colors.foreground.secondary,
          style = TallybookTheme.typography.bodySmall,
      )
      TallybookButton(onClick = onRetry) { TallybookText(stringResource(Res.string.retry)) }
    }
  }
}

@Composable
internal fun StatisticsSkeleton(showLoading: Boolean) {
  val loading = stringResource(Res.string.loading)
  TallybookSkeletonCard(
      modifier =
          Modifier.fillMaxWidth()
              .height(TallybookTheme.dimensions.chartTrendAreaHeight)
              .alpha(if (showLoading) 1f else 0f)
              .then(
                  if (showLoading)
                      Modifier.semantics {
                        contentDescription = loading
                        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                      }
                  else Modifier
              )
  ) {}
}

@Composable
internal fun StatisticsEmpty(onAdd: () -> Unit) {
  TallybookSurface(modifier = Modifier.fillMaxWidth()) {
    Column(
        modifier = Modifier.padding(TallybookTheme.spacing.space20),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
    ) {
      TallybookText(
          stringResource(Res.string.statistics_empty_title),
          modifier = Modifier.semantics { heading() },
          style = TallybookTheme.typography.title,
      )
      TallybookText(
          stringResource(Res.string.statistics_empty_body),
          color = TallybookTheme.colors.foreground.secondary,
      )
      TallybookButton(onClick = onAdd) {
        TallybookText(stringResource(Res.string.navigation_add_movement))
      }
    }
  }
}

@Composable
internal fun StatisticsStaleNotice() {
  TallybookText(
      stringResource(Res.string.statistics_stale),
      modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
      color = TallybookTheme.colors.foreground.secondary,
      style = TallybookTheme.typography.bodySmall,
  )
}
