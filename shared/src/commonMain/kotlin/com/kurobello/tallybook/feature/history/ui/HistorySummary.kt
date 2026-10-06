package com.kurobello.tallybook.feature.history.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.movimiento.signedMovimientoAmountText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.history.viewmodel.HistoryFailure
import com.kurobello.tallybook.feature.history.viewmodel.HistoryLoadState
import com.kurobello.tallybook.feature.history.viewmodel.HistoryProjection
import com.kurobello.tallybook.feature.history.viewmodel.cachedValue
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
    period: HistoryLoadState<HistoryProjection.Content>,
    onRetry: () -> Unit,
) {
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
        is HistoryLoadState.Loading ->
            TallybookText(
                stringResource(Res.string.loading),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        is HistoryLoadState.Failed -> {
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
        is HistoryLoadState.Ready -> Unit
      }
      if (period !is HistoryLoadState.Ready && period.cachedValue != null) HistoryStaleNotice()
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
  FlowRow(
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
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
