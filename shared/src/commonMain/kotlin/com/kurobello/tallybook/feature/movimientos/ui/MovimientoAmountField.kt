package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.ui.component.TallybookAmountInput
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessage
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.amount

internal data class MovimientoAmountFieldState(
    val value: String,
    val moneda: Moneda,
    val error: String?,
)

@Composable
internal fun MovimientoAmountField(
    state: MovimientoAmountFieldState,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
  TallybookAmountInput(
      value = state.value,
      onValueChange = onValueChange,
      label = stringResource(Res.string.amount),
      currencySymbol = state.moneda.symbol,
      modifier = modifier,
      enabled = enabled,
      message = state.error?.let(TallybookFieldMessage::Error),
      focusRequester = focusRequester,
  )
}
