package com.kurobello.tallybook.feature.statistics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.StatisticsScope
import com.kurobello.tallybook.core.ui.account.MonedaSelector
import com.kurobello.tallybook.core.ui.component.TallybookPeriodOption
import com.kurobello.tallybook.core.ui.component.TallybookPeriodPickerHeader
import com.kurobello.tallybook.core.ui.component.TallybookPeriodPickerRow
import com.kurobello.tallybook.core.ui.component.TallybookSegmentOption
import com.kurobello.tallybook.core.ui.component.TallybookSegmentedControl
import com.kurobello.tallybook.core.ui.component.TallybookSegmentedControlStyle
import com.kurobello.tallybook.core.ui.component.TallybookYearMenu
import com.kurobello.tallybook.core.ui.component.rememberLoadingPresentation
import com.kurobello.tallybook.core.ui.layout.TallybookScreenFrame
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsActions
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsContent
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsSelection
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsUiState
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.statistics_month_of_year
import tallybook.shared.generated.resources.statistics_period_description
import tallybook.shared.generated.resources.statistics_scope_month
import tallybook.shared.generated.resources.statistics_scope_year
import tallybook.shared.generated.resources.statistics_title
import tallybook.shared.generated.resources.statistics_until_day
import tallybook.shared.generated.resources.statistics_year_description

private const val SKELETON_CARDS = 2

private data class PendingStatisticsRead(
    val selection: StatisticsSelection,
    val today: LocalDate,
    val moneda: Moneda?,
)

private val ScopeLabels =
    mapOf(
        StatisticsScope.Month to Res.string.statistics_scope_month,
        StatisticsScope.Year to Res.string.statistics_scope_year,
    )

@Composable
internal fun StatisticsScreen(
    uiState: StatisticsUiState,
    actions: StatisticsActions,
    clearance: Dp,
    modifier: Modifier = Modifier,
) {
  require(clearance >= TallybookTheme.spacing.space0)
  val presentation by
      rememberLoadingPresentation(
          uiState,
          isLoading = { it.content.isInitialLoading },
          requestKey = { PendingStatisticsRead(it.selection, it.today, it.moneda) },
      )
  val state = presentation.state
  val listDescription = stringResource(Res.string.statistics_title)
  TallybookScreenFrame(
      modifier = modifier,
      windowInsets =
          WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
      bottomPaddingFloor = TallybookTheme.spacing.space0,
  ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().semantics { contentDescription = listDescription },
        contentPadding =
            PaddingValues(
                top = TallybookTheme.spacing.space20,
                bottom = clearance + TallybookTheme.spacing.space16,
            ),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
    ) {
      statisticsItems(state, actions, presentation.showLoading)
    }
  }
  CategoryStatisticsSheet(state.detail, state.content.cachedValue, actions)
}

private fun LazyListScope.statisticsItems(
    state: StatisticsUiState,
    actions: StatisticsActions,
    showLoading: Boolean,
) {
  val content = state.content.cachedValue
  item(key = "statistics-header") { StatisticsHeader(state, actions) }
  val moneda = state.moneda
  if (state.showsMonedaSelector && moneda != null)
      item(key = "statistics-currency") {
        MonedaSelector(state.monedas, moneda, actions::onMonedaSelected)
      }
  if (content != null && !content.hasAnyMovimiento) {
    item(key = "statistics-empty") { StatisticsEmpty(actions::onAddMovimiento) }
    return
  }
  item(key = "statistics-scope") { StatisticsScopeControl(state, actions) }
  item(key = "statistics-periods") { StatisticsPeriodRow(state, actions) }
  when {
    content != null -> statisticsCards(state, content, actions)
    state.content is RetainedLoadState.Failed ->
        item(key = "statistics-error") { StatisticsError(state.content.failure, actions::onRetry) }
    else ->
        items(SKELETON_CARDS, key = { "statistics-skeleton-$it" }) {
          StatisticsSkeleton(showLoading)
        }
  }
}

private fun LazyListScope.statisticsCards(
    state: StatisticsUiState,
    content: StatisticsContent,
    actions: StatisticsActions,
) {
  val loadState = state.content
  item(key = "statistics-summary") {
    StatisticsSummaryCard(content.summary, stale = loadState is RetainedLoadState.Failed)
  }
  if (loadState is RetainedLoadState.Failed)
      item(key = "statistics-refresh-error") {
        StatisticsError(loadState.failure, actions::onRetry)
      }
  item(key = "statistics-distribution") {
    StatisticsDistributionCard(
        distribution = content.distributions[state.dimension],
        dimension = state.dimension,
        hasGasto = content.summary.hasGasto,
        actions = actions,
    )
  }
  if (content.trend.isNotEmpty())
      item(key = "statistics-trend") {
        StatisticsTrendCard(
            months = content.trend,
            year =
                content.summary.period.start.year.takeIf { content.scope == StatisticsScope.Year },
            selected = state.trendSelection ?: content.trend.lastIndex,
            onSelect = actions::onTrendMonthSelected,
        )
      }
  content.pace?.let { pace -> item(key = "statistics-pace") { StatisticsPaceCard(pace) } }
  if (content.insights.isNotEmpty())
      item(key = "statistics-insights") {
        StatisticsInsightsCard(content.insights, content.summary.comparison)
      }
}

@Composable
private fun StatisticsHeader(state: StatisticsUiState, actions: StatisticsActions) {
  val period = state.selectedPeriod
  val year = state.selection.anchor.year
  TallybookPeriodPickerHeader(
      title = statisticsPeriodTitle(state.selection.scope, period),
      supportingText =
          if (state.today in period.start..period.end) statisticsUntil(state.today) else null,
      titleStyle = TallybookTheme.typography.headline,
      yearControl = {
        if (state.selection.scope == StatisticsScope.Month) {
          TallybookYearMenu(
              state.years,
              year,
              actions::onYearSelected,
              stringResource(Res.string.statistics_year_description, year),
          )
        }
      },
  )
}

@Composable
private fun StatisticsScopeControl(state: StatisticsUiState, actions: StatisticsActions) {
  TallybookSegmentedControl(
      options =
          StatisticsScope.entries.map {
            TallybookSegmentOption(it, stringResource(ScopeLabels.getValue(it)))
          },
      selected = state.selection.scope,
      onSelect = actions::onScopeChanged,
      modifier = Modifier.fillMaxWidth(),
      style = TallybookSegmentedControlStyle.Neutral,
  )
}

@Composable
private fun StatisticsPeriodRow(state: StatisticsUiState, actions: StatisticsActions) {
  val scope = state.selection.scope
  val options =
      state.periods.map { period ->
        TallybookPeriodOption(
            value = period,
            label =
                when (scope) {
                  StatisticsScope.Month -> shortMonthName(period.start.month)
                  StatisticsScope.Year -> period.start.year.toString()
                },
            contentDescription = statisticsPeriodDescription(scope, period),
        )
      }
  TallybookPeriodPickerRow(
      options,
      state.selectedPeriod,
      actions::onPeriodSelected,
      stringResource(Res.string.statistics_period_description),
      modifier = Modifier.fillMaxWidth(),
  )
}

@Composable
internal fun statisticsPeriodTitle(scope: StatisticsScope, period: MovimientoDateRange): String =
    when (scope) {
      StatisticsScope.Month -> monthName(period.start.month).replaceFirstChar(Char::titlecase)
      StatisticsScope.Year -> period.start.year.toString()
    }

@Composable
internal fun statisticsUntil(day: LocalDate): String =
    stringResource(Res.string.statistics_until_day, day.day, monthName(day.month))

@Composable
private fun statisticsPeriodDescription(scope: StatisticsScope, period: MovimientoDateRange) =
    when (scope) {
      StatisticsScope.Month ->
          stringResource(
              Res.string.statistics_month_of_year,
              monthName(period.start.month),
              period.start.year,
          )
      StatisticsScope.Year -> period.start.year.toString()
    }
