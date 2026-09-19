package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.ui.component.TallybookDateChipPicker
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.date

internal data class MovimientoDateFieldState(
    val fecha: LocalDate,
    val enabled: Boolean,
    val maxSelectableDate: LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
)

@Composable
internal fun MovimientoDateField(
    state: MovimientoDateFieldState,
    onFechaChanged: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
  TallybookDateChipPicker(
      value = state.fecha,
      onValueChange = onFechaChanged,
      label = stringResource(Res.string.date),
      modifier = modifier,
      enabled = state.enabled,
      maxDate = state.maxSelectableDate,
  )
}
