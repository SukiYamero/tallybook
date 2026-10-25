package com.kurobello.tallybook.feature.statistics.ui

import androidx.compose.runtime.Composable
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsComparison
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.date_picker_months
import tallybook.shared.generated.resources.date_picker_months_short
import tallybook.shared.generated.resources.date_picker_weekday_names
import tallybook.shared.generated.resources.statistics_in_month
import tallybook.shared.generated.resources.statistics_in_year
import tallybook.shared.generated.resources.statistics_month_stretch
import tallybook.shared.generated.resources.statistics_than_month
import tallybook.shared.generated.resources.statistics_than_month_stretch
import tallybook.shared.generated.resources.statistics_than_year
import tallybook.shared.generated.resources.statistics_than_year_stretch
import tallybook.shared.generated.resources.statistics_weekday_date
import tallybook.shared.generated.resources.statistics_year_stretch

@Composable
internal fun monthName(month: Month): String =
    stringArrayResource(Res.array.date_picker_months)[month.ordinal]

@Composable
internal fun shortMonthName(month: Month): String =
    stringArrayResource(Res.array.date_picker_months_short)[month.ordinal]

@Composable
internal fun weekdayDate(date: LocalDate): String =
    stringResource(
            Res.string.statistics_weekday_date,
            stringArrayResource(Res.array.date_picker_weekday_names)[date.dayOfWeek.ordinal],
            date.day,
            monthName(date.month),
        )
        .replaceFirstChar(Char::titlecase)

/** "del 1 al 24 de septiembre", "en 2025": what a total is compared against. */
@Composable
internal fun StatisticsComparison.phrase(): String =
    when (this) {
      is StatisticsComparison.WholeMonth ->
          stringResource(Res.string.statistics_in_month, monthName(month.month))
      is StatisticsComparison.WholeYear -> stringResource(Res.string.statistics_in_year, year)
      is StatisticsComparison.MonthStretch ->
          stringResource(Res.string.statistics_month_stretch, end.day, monthName(month.month))
      is StatisticsComparison.YearStretch ->
          stringResource(
              Res.string.statistics_year_stretch,
              end.day,
              monthName(end.month),
              end.year,
          )
    }

/** "que en el mismo tramo de septiembre": the tail of a "more than" sentence. */
@Composable
internal fun StatisticsComparison.thanPhrase(): String =
    when (this) {
      is StatisticsComparison.WholeMonth ->
          stringResource(Res.string.statistics_than_month, monthName(month.month))
      is StatisticsComparison.WholeYear -> stringResource(Res.string.statistics_than_year, year)
      is StatisticsComparison.MonthStretch ->
          stringResource(Res.string.statistics_than_month_stretch, monthName(month.month))
      is StatisticsComparison.YearStretch ->
          stringResource(Res.string.statistics_than_year_stretch, end.year)
    }
