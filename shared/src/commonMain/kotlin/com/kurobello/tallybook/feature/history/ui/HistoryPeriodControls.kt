package com.kurobello.tallybook.feature.history.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.ui.component.TallybookPeriodOption
import com.kurobello.tallybook.core.ui.component.TallybookPeriodPickerHeader
import com.kurobello.tallybook.core.ui.component.TallybookPeriodPickerRow
import com.kurobello.tallybook.core.ui.component.TallybookSegmentOption
import com.kurobello.tallybook.core.ui.component.TallybookSegmentedControl
import com.kurobello.tallybook.core.ui.component.TallybookSegmentedControlStyle
import com.kurobello.tallybook.core.ui.component.TallybookYearMenu
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.history.viewmodel.HistoryActions
import com.kurobello.tallybook.feature.history.viewmodel.HistoryScope
import com.kurobello.tallybook.feature.history.viewmodel.HistoryUiState
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.calendar_date_content_description
import tallybook.shared.generated.resources.date_chip_value
import tallybook.shared.generated.resources.date_picker_months
import tallybook.shared.generated.resources.date_picker_weekday_names
import tallybook.shared.generated.resources.date_picker_weekdays
import tallybook.shared.generated.resources.history_day_title
import tallybook.shared.generated.resources.history_month_year
import tallybook.shared.generated.resources.history_period_description
import tallybook.shared.generated.resources.history_range_description
import tallybook.shared.generated.resources.history_scope_context
import tallybook.shared.generated.resources.history_scope_day
import tallybook.shared.generated.resources.history_scope_month
import tallybook.shared.generated.resources.history_scope_week
import tallybook.shared.generated.resources.history_week_chip
import tallybook.shared.generated.resources.history_week_title
import tallybook.shared.generated.resources.history_year_description

@Composable
internal fun HistoryPeriodHeader(state: HistoryUiState, actions: HistoryActions) {
  val selected = state.selectedPeriod
  val monthNames = stringArrayResource(Res.array.date_picker_months)
  val monthYear =
      stringResource(
          Res.string.history_month_year,
          monthNames[selected.start.month.ordinal],
          selected.start.year,
      )
  val title =
      when (state.selection.scope) {
        HistoryScope.Day ->
            stringResource(
                Res.string.history_day_title,
                stringArrayResource(Res.array.date_picker_weekday_names)[
                    selected.start.dayOfWeek.ordinal],
                selected.start.day,
            )
        HistoryScope.Week ->
            stringResource(
                Res.string.history_week_title,
                selected.start.day,
                selected.end.day,
                monthNames[selected.start.month.ordinal],
            )
        HistoryScope.Month -> monthYear
      }
  TallybookPeriodPickerHeader(
      title = title,
      supportingText =
          stringResource(
              Res.string.history_scope_context,
              historyScopeLabel(state.selection.scope),
              monthYear,
          ),
      titleStyle = TallybookTheme.typography.headline,
      yearControl = {
        TallybookYearMenu(
            state.availableYears,
            state.selection.anchor.year,
            actions::onYearSelected,
            stringResource(Res.string.history_year_description, state.selection.anchor.year),
        )
      },
  )
}

@Composable
internal fun HistoryScopeControl(state: HistoryUiState, actions: HistoryActions) {
  TallybookSegmentedControl(
      options = HistoryScope.entries.map { TallybookSegmentOption(it, historyScopeLabel(it)) },
      selected = state.selection.scope,
      onSelect = actions::onScopeChanged,
      modifier = Modifier.fillMaxWidth(),
      style = TallybookSegmentedControlStyle.Neutral,
  )
}

@Composable
internal fun HistoryPeriodRow(state: HistoryUiState, actions: HistoryActions) {
  val weekdays = stringArrayResource(Res.array.date_picker_weekdays)
  val months = stringArrayResource(Res.array.date_picker_months)
  val options =
      state.periods.map { period ->
        val label =
            when (state.selection.scope) {
              HistoryScope.Day -> period.start.day.toString()
              HistoryScope.Week ->
                  stringResource(Res.string.history_week_chip, period.start.day, period.end.day)
              HistoryScope.Month -> months[period.start.month.ordinal]
            }
        TallybookPeriodOption(
            value = period,
            label = label,
            caption =
                if (state.selection.scope == HistoryScope.Day)
                    weekdays[period.start.dayOfWeek.ordinal]
                else null,
            contentDescription = historyPeriodDescription(period),
        )
      }
  TallybookPeriodPickerRow(
      options,
      state.selectedPeriod,
      actions::onPeriodSelected,
      stringResource(Res.string.history_period_description),
      modifier = Modifier.fillMaxWidth(),
  )
}

@Composable
private fun historyScopeLabel(scope: HistoryScope): String =
    stringResource(
        when (scope) {
          HistoryScope.Day -> Res.string.history_scope_day
          HistoryScope.Week -> Res.string.history_scope_week
          HistoryScope.Month -> Res.string.history_scope_month
        }
    )

@Composable
internal fun historyDateSubtitle(date: LocalDate): String =
    stringResource(
        Res.string.date_chip_value,
        date.day,
        stringArrayResource(Res.array.date_picker_months)[date.month.ordinal],
    )

@Composable
internal fun historyFullDate(date: LocalDate): String =
    stringResource(
        Res.string.calendar_date_content_description,
        date.day,
        stringArrayResource(Res.array.date_picker_months)[date.month.ordinal],
        date.year,
    )

@Composable
private fun historyPeriodDescription(period: MovimientoDateRange): String =
    if (period.start == period.end) historyFullDate(period.start)
    else
        stringResource(
            Res.string.history_range_description,
            historyFullDate(period.start),
            historyFullDate(period.end),
        )
