package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.recurrencia.RepetirField
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoUiState
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.recurrencia_unsupported_interval

/** Repetir belongs to creation only: an existing movimiento never becomes recurrent. */
@Composable
internal fun EntryRepetirField(uiState: AddMovimientoUiState, actions: AddMovimientoActions) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    RepetirField(
        frecuencia = uiState.frecuencia,
        onRepetirChanged = actions::onRepetirChanged,
        onFrecuenciaChanged = actions::onFrecuenciaChanged,
        enabled = !uiState.isSubmitting,
    )
    if (uiState.recurrenceUnsupported) {
      TallybookText(
          text = stringResource(Res.string.recurrencia_unsupported_interval),
          color = TallybookTheme.colors.foreground.secondary,
          style = TallybookTheme.typography.caption,
      )
    }
  }
}
