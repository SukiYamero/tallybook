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
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookSkeletonCard
import com.kurobello.tallybook.core.ui.component.TallybookSkeletonLine
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookText
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
import tallybook.shared.generated.resources.home_expenses
import tallybook.shared.generated.resources.home_income
import tallybook.shared.generated.resources.home_load_error_body
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.retry

@Composable
internal fun HistoryPeriodSummary(
    period: RetainedLoadState<HistoryProjection.Content, HistoryFailure>,
    onRetry: () -> Unit,
    showLoading: Boolean = true,
) {
  if (period.isInitialLoading) {
    HistoryPeriodSummarySkeleton(showLoading)
    return
  }
  TallybookSurface(modifier = Modifier.fillMaxWidth()) {
    Column(
        modifier = Modifier.padding(TallybookTheme.spacing.space16),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
    ) {
      TallybookText(
          stringResource(Res.string.history_period_balance),
          style = TallybookTheme.typography.label,
      )
      period.cachedValue?.summary?.let { HistoryPeriodTotals(it) }
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
}

@Composable
private fun HistoryPeriodSummarySkeleton(showLoading: Boolean) {
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
    TallybookSkeletonCard(modifier = Modifier.fillMaxWidth()) {
      Column(
          modifier = Modifier.padding(TallybookTheme.spacing.space16),
          verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      ) {
        TallybookText(
            stringResource(Res.string.history_period_balance),
            style = TallybookTheme.typography.label,
        )
        Column(
            modifier = Modifier.alpha(if (showLoading) 1f else 0f),
            verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
        ) {
          TallybookSkeletonLine("$ 000.000", TallybookTheme.typography.headline)
          Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
            TallybookSkeletonLine(
                stringResource(Res.string.home_income) + ": $ 000.000",
                TallybookTheme.typography.bodyEmphasis,
            )
            TallybookSkeletonLine(
                stringResource(Res.string.home_expenses) + ": $ 000.000",
                TallybookTheme.typography.bodyEmphasis,
            )
          }
        }
      }
    }
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

@Composable
private fun HistoryPeriodTotals(summary: MovimientoSummary) {
  Column(
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
  ) {
    TallybookText(summary.balance.format(), style = TallybookTheme.typography.headline)
    Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
      TallybookText(
          stringResource(Res.string.home_income) +
              ": " +
              signedMovimientoAmountText(summary.ingresos, TipoMovimiento.INGRESO),
          color = TallybookTheme.colors.status.success.base,
          style = TallybookTheme.typography.bodyEmphasis,
      )
      TallybookText(
          stringResource(Res.string.home_expenses) +
              ": " +
              signedMovimientoAmountText(summary.gastos, TipoMovimiento.GASTO),
          style = TallybookTheme.typography.bodyEmphasis,
      )
    }
  }
}
