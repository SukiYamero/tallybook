package com.kurobello.tallybook.core.ui.recurrencia

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.ui.component.TallybookSwitchRow
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.recurrencia_repeat_body
import tallybook.shared.generated.resources.recurrencia_repeat_label

/** A null [frecuencia] means the movimiento does not repeat. */
@Composable
internal fun RepetirField(
    frecuencia: Frecuencia?,
    onRepetirChanged: (Boolean) -> Unit,
    onFrecuenciaChanged: (Frecuencia) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    TallybookSwitchRow(
        label = stringResource(Res.string.recurrencia_repeat_label),
        checked = frecuencia != null,
        onCheckedChange = onRepetirChanged,
        supportingText = stringResource(Res.string.recurrencia_repeat_body),
        enabled = enabled,
    )
    AnimatedVisibility(visible = frecuencia != null) {
      FrecuenciaField(
          selected = frecuencia ?: Frecuencia.MENSUAL,
          onSelect = onFrecuenciaChanged,
          enabled = enabled,
      )
    }
  }
}

@Preview
@Composable
internal fun RepetirFieldPreview() {
  TallybookTheme {
    Column {
      RepetirField(frecuencia = null, onRepetirChanged = {}, onFrecuenciaChanged = {})
      RepetirField(frecuencia = Frecuencia.MENSUAL, onRepetirChanged = {}, onFrecuenciaChanged = {})
    }
  }
}
