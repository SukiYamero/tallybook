package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.ui.component.TallybookDateChipPicker
import com.kurobello.tallybook.core.ui.component.TallybookDateChipSize
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
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
  Column(
      modifier = modifier,
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    TallybookText(
        text = stringResource(Res.string.date),
        style = TallybookTheme.typography.label,
        color = TallybookTheme.colors.foreground.tertiary,
    )
    TallybookDateChipPicker(
        value = state.fecha,
        onValueChange = onFechaChanged,
        label = stringResource(Res.string.date),
        size = TallybookDateChipSize.M,
        enabled = state.enabled,
        maxDate = state.maxSelectableDate,
    )
  }
}
