package com.kurobello.tallybook.core.ui.recurrencia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.ui.component.TallybookInputLabel
import com.kurobello.tallybook.core.ui.component.TallybookSegmentOption
import com.kurobello.tallybook.core.ui.component.TallybookSegmentedControl
import com.kurobello.tallybook.core.ui.component.TallybookSegmentedControlStyle
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.frecuencia_anual
import tallybook.shared.generated.resources.frecuencia_mensual
import tallybook.shared.generated.resources.frecuencia_semanal
import tallybook.shared.generated.resources.recurrencia_frequency_label

private val labels: Map<Frecuencia, StringResource> =
    mapOf(
        Frecuencia.SEMANAL to Res.string.frecuencia_semanal,
        Frecuencia.MENSUAL to Res.string.frecuencia_mensual,
        Frecuencia.ANUAL to Res.string.frecuencia_anual,
    )

internal val Frecuencia.labelResource: StringResource
  get() = labels.getValue(this)

/** Mensual leads because it is the default and the most common choice. */
private val FRECUENCIA_ORDER = listOf(Frecuencia.MENSUAL, Frecuencia.SEMANAL, Frecuencia.ANUAL)

@Composable
internal fun FrecuenciaField(
    selected: Frecuencia,
    onSelect: (Frecuencia) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.dimensions.fieldLabelSpacing),
  ) {
    TallybookInputLabel(text = stringResource(Res.string.recurrencia_frequency_label))
    TallybookSegmentedControl(
        options =
            FRECUENCIA_ORDER.map { TallybookSegmentOption(it, stringResource(it.labelResource)) },
        selected = selected,
        onSelect = onSelect,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        style = TallybookSegmentedControlStyle.Neutral,
        labelStyle = TallybookTheme.typography.controlValue,
    )
  }
}

@Preview
@Composable
internal fun FrecuenciaFieldPreview() {
  TallybookTheme { FrecuenciaField(selected = Frecuencia.MENSUAL, onSelect = {}) }
}
