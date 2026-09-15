@file:OptIn(ExperimentalMaterial3Api::class)

package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.close
import tallybook.shared.generated.resources.confirm
import tallybook.shared.generated.resources.date

internal data class MovimientoDateFieldState(
    val fecha: LocalDate,
    val showPicker: Boolean,
    val enabled: Boolean,
    val maxSelectableDate: LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
)

@Composable
internal fun MovimientoDateField(
    state: MovimientoDateFieldState,
    onOpenPicker: () -> Unit,
    onDismissPicker: () -> Unit,
    onFechaChanged: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
  OutlinedButton(
      onClick = onOpenPicker,
      modifier = modifier.fillMaxWidth(),
      enabled = state.enabled,
  ) {
    Text("${stringResource(Res.string.date)}: ${state.fecha}")
  }

  if (state.showPicker) {
    val selectableDates =
        remember(state.maxSelectableDate) { DatesThrough(state.maxSelectableDate.toUtcMillis()) }
    val state =
        rememberDatePickerState(
            initialSelectedDateMillis = state.fecha.toUtcMillis(),
            selectableDates = selectableDates,
        )
    DatePickerDialog(
        onDismissRequest = onDismissPicker,
        confirmButton = {
          TextButton(
              onClick = {
                state.selectedDateMillis?.let { onFechaChanged(it.toUtcLocalDate()) }
                onDismissPicker()
              }
          ) {
            Text(stringResource(Res.string.confirm))
          }
        },
        dismissButton = {
          TextButton(onClick = onDismissPicker) { Text(stringResource(Res.string.close)) }
        },
    ) {
      DatePicker(state = state)
    }
  }
}

private class DatesThrough(private val maxDateMillis: Long) : SelectableDates {
  override fun isSelectableDate(utcTimeMillis: Long): Boolean =
      isDateSelectable(utcTimeMillis, maxDateMillis)
}

internal fun isDateSelectable(utcTimeMillis: Long, maxDateMillis: Long): Boolean =
    utcTimeMillis <= maxDateMillis

internal fun LocalDate.toUtcMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

internal fun Long.toUtcLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date
