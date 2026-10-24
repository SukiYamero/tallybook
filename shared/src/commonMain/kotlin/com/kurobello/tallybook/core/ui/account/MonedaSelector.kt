package com.kurobello.tallybook.core.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.ui.component.TallybookChoicePill
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.account_currency_label

/**
 * The currency History and Search show, as one choice per currency code. Callers render it only
 * while accounts exist in more than one currency.
 */
@Composable
internal fun MonedaSelector(
    monedas: List<Moneda>,
    selected: Moneda,
    onSelect: (Moneda) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  Row(
      modifier = modifier,
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    TallybookText(
        stringResource(Res.string.account_currency_label),
        style = TallybookTheme.typography.label,
        color = TallybookTheme.colors.foreground.secondary,
    )
    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
    ) {
      monedas.forEach { moneda ->
        TallybookChoicePill(
            moneda.name,
            moneda == selected,
            { if (moneda != selected) onSelect(moneda) },
            enabled = enabled,
        )
      }
    }
  }
}

@Preview
@Composable
internal fun MonedaSelectorLightPreview() {
  TallybookTheme(darkTheme = false) {
    MonedaSelector(listOf(Moneda.COP, Moneda.USD), Moneda.COP, {})
  }
}

@Preview
@Composable
internal fun MonedaSelectorDarkPreview() {
  TallybookTheme(darkTheme = true) {
    MonedaSelector(listOf(Moneda.COP, Moneda.USD, Moneda.BRL), Moneda.USD, {})
  }
}
