package com.kurobello.tallybook.core.ui.date

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.calendar_date_content_description
import tallybook.shared.generated.resources.date_picker_months

@Composable
internal fun fullCivilDate(date: LocalDate): String =
    stringResource(
        Res.string.calendar_date_content_description,
        date.day,
        stringArrayResource(Res.array.date_picker_months)[date.month.ordinal],
        date.year,
    )

@Composable
internal fun CivilDayHeader(date: LocalDate, modifier: Modifier = Modifier) {
  TallybookText(
      fullCivilDate(date),
      modifier = modifier.semantics { heading() },
      style = TallybookTheme.typography.dayHeading,
  )
}
