package com.kurobello.tallybook.feature.history.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.movimiento.BalanceCard
import com.kurobello.tallybook.core.ui.movimiento.BalanceCardSkeleton
import com.kurobello.tallybook.core.ui.movimiento.signedMovimientoAmountText
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.history.viewmodel.HistoryFailure
import com.kurobello.tallybook.feature.history.viewmodel.HistoryProjection
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.history_overflow_error
import tallybook.shared.generated.resources.history_period_balance
import tallybook.shared.generated.resources.history_period_error
import tallybook.shared.generated.resources.history_stale
import tallybook.shared.generated.resources.home_load_error_body
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.retry

@Composable
internal fun HistoryPeriodSummary(
    period: RetainedLoadState<HistoryProjection.Content, HistoryFailure>,
    onRetry: () -> Unit,
    showLoading: Boolean = true,
) {
  val label = stringResource(Res.string.history_period_balance)
  if (period.isInitialLoading) {
    HistoryPeriodSummarySkeleton(showLoading, label)
    return
  }
  val summary = period.cachedValue?.summary
  if (summary != null) {
    BalanceCard(
        summary = summary,
        modifier = Modifier.fillMaxWidth(),
        label = label,
        secondaryAmountFormatter = ::signedMovimientoAmountText,
        footerContent =
            if (
                period is RetainedLoadState.Ready ||
                    period is RetainedLoadState.Loading && !showLoading
            )
                null
            else {
              { HistoryPeriodSummaryFeedback(period, onRetry, showLoading) }
            },
    )
  } else {
    TallybookSurface(modifier = Modifier.fillMaxWidth()) {
      Column(
          modifier = Modifier.padding(TallybookTheme.spacing.space16),
          verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      ) {
        TallybookText(label, style = TallybookTheme.typography.balanceLabel)
        HistoryPeriodSummaryFeedback(period, onRetry, showLoading)
      }
    }
  }
}

@Composable
private fun HistoryPeriodSummaryFeedback(
    period: RetainedLoadState<HistoryProjection.Content, HistoryFailure>,
    onRetry: () -> Unit,
    showLoading: Boolean,
) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12)) {
    when (period) {
      is RetainedLoadState.Loading ->
          if (showLoading)
              TallybookText(
                  stringResource(Res.string.loading),
                  modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
              )
      is RetainedLoadState.Failed -> {
        TallybookText(
            stringResource(
                if (period.failure == HistoryFailure.CalculationOverflow)
                    Res.string.history_overflow_error
                else Res.string.history_period_error
            ),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        TallybookText(
            stringResource(Res.string.home_load_error_body),
            style = TallybookTheme.typography.bodySmall,
        )
        TallybookButton(onClick = onRetry) { TallybookText(stringResource(Res.string.retry)) }
      }
      is RetainedLoadState.Ready -> Unit
    }
    val showStaleNotice =
        when (period) {
          is RetainedLoadState.Ready -> false
          is RetainedLoadState.Loading -> showLoading && period.previous != null
          is RetainedLoadState.Failed -> period.previous != null
        }
    if (showStaleNotice) HistoryStaleNotice()
  }
}

@Composable
private fun HistoryPeriodSummarySkeleton(showLoading: Boolean, label: String) {
  val loadingDescription = stringResource(Res.string.loading)
  val feedback =
      if (showLoading)
          Modifier.semantics {
            contentDescription = loadingDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            liveRegion = LiveRegionMode.Polite
          }
      else Modifier
  Box(modifier = Modifier.fillMaxWidth().then(feedback)) {
    BalanceCardSkeleton(
        modifier = Modifier.fillMaxWidth().alpha(if (showLoading) 1f else 0f),
        label = label,
    )
  }
}

@Composable
internal fun HistoryStaleNotice() {
  TallybookText(
      stringResource(Res.string.history_stale),
      modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
      color = TallybookTheme.colors.foreground.secondary,
      style = TallybookTheme.typography.bodySmall,
  )
}
