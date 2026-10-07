package com.kurobello.tallybook.feature.history.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.history.viewmodel.HistoryActions
import com.kurobello.tallybook.feature.history.viewmodel.HistoryProjection
import com.kurobello.tallybook.feature.history.viewmodel.HistoryScope
import com.kurobello.tallybook.feature.history.viewmodel.HistorySelection
import com.kurobello.tallybook.feature.history.viewmodel.HistoryUiState
import kotlinx.datetime.LocalDate

class HistoryLoadingPreviewActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val today = LocalDate(2026, 10, 6)
    val scope =
        HistoryScope.entries.firstOrNull { it.name == intent.getStringExtra("scope") }
            ?: HistoryScope.Day
    val anchor = intent.getStringExtra("date")?.let(LocalDate::parse) ?: today
    val pending = HistoryUiState(HistorySelection(scope, anchor), today, Moneda.USD)
    val state =
        if (intent.getBooleanExtra("empty", false) || intent.getBooleanExtra("cached", false))
            pending.copy(
                period =
                    if (intent.getBooleanExtra("cached", false))
                        RetainedLoadState.Loading(
                            HistoryProjection.Content(
                                MovimientoSummary(Money(0, Moneda.USD), Money(0, Moneda.USD)),
                                emptyList(),
                            )
                        )
                    else
                        RetainedLoadState.Ready(
                            HistoryProjection.Content(
                                MovimientoSummary(Money(0, Moneda.USD), Money(0, Moneda.USD)),
                                emptyList(),
                            )
                        )
            )
        else pending
    val dark = intent.getBooleanExtra("dark", false)
    val fontScale = intent.getFloatExtra("fontScale", 1f)
    setContent {
      val density = LocalDensity.current
      CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
        TallybookTheme(darkTheme = dark) {
          HistoryScreen(state, NoActions, HistoryNavigationCallbacks({}), 0.dp)
        }
      }
    }
  }

  private object NoActions : HistoryActions {
    override fun onScopeChanged(scope: HistoryScope) = Unit

    override fun onYearSelected(year: Int) = Unit

    override fun onPeriodSelected(period: MovimientoDateRange) = Unit

    override fun onRetry() = Unit

    override fun onResume() = Unit

    override fun onPresentationChanged(unknownCategoryLabel: String) = Unit
  }
}
