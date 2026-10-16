package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.Composable
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.calendar_date_content_description
import tallybook.shared.generated.resources.date_chip_value
import tallybook.shared.generated.resources.date_picker_months
import tallybook.shared.generated.resources.date_picker_weekday_names
import tallybook.shared.generated.resources.movimiento_relative_today
import tallybook.shared.generated.resources.movimiento_relative_yesterday

private const val LAST_WEEKDAY_OFFSET = 6

internal sealed interface RelativeDay {
  data object Today : RelativeDay

  data object Yesterday : RelativeDay

  data class Weekday(val dayOfWeek: DayOfWeek) : RelativeDay

  data class Calendar(val date: LocalDate, val showsYear: Boolean) : RelativeDay
}

/** A weekday name is unambiguous only within the last week; older and future dates show the day. */
internal fun relativeDayOf(fecha: LocalDate, today: LocalDate): RelativeDay {
  val daysAgo = fecha.daysUntil(today)
  return when {
    daysAgo == 0 -> RelativeDay.Today
    daysAgo == 1 -> RelativeDay.Yesterday
    daysAgo in 2..LAST_WEEKDAY_OFFSET -> RelativeDay.Weekday(fecha.dayOfWeek)
    else -> RelativeDay.Calendar(fecha, showsYear = fecha.year != today.year)
  }
}

@Composable
internal fun relativeDateLabel(fecha: LocalDate, today: LocalDate): String =
    when (val day = relativeDayOf(fecha, today)) {
      RelativeDay.Today -> stringResource(Res.string.movimiento_relative_today)
      RelativeDay.Yesterday -> stringResource(Res.string.movimiento_relative_yesterday)
      is RelativeDay.Weekday ->
          stringArrayResource(Res.array.date_picker_weekday_names)[day.dayOfWeek.ordinal]
              .replaceFirstChar(Char::titlecase)
      is RelativeDay.Calendar -> calendarDayLabel(day)
    }

@Composable
private fun calendarDayLabel(day: RelativeDay.Calendar): String {
  val month = stringArrayResource(Res.array.date_picker_months)[day.date.month.ordinal]
  return if (day.showsYear) {
    stringResource(Res.string.calendar_date_content_description, day.date.day, month, day.date.year)
  } else {
    stringResource(Res.string.date_chip_value, day.date.day, month)
  }
}
