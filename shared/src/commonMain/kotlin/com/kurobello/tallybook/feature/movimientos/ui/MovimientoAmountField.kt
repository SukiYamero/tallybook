package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.Moneda
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
  Column(
      modifier = modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
        text = stringResource(Res.string.amount),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
          text = state.moneda.symbol,
          style = MaterialTheme.typography.headlineLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      OutlinedTextField(
          value = state.value,
          onValueChange = onValueChange,
          modifier =
              Modifier.widthIn(min = 120.dp, max = 240.dp)
                  .then(
                      if (focusRequester == null) Modifier
                      else Modifier.focusRequester(focusRequester)
                  )
                  .then(
                      if (state.error == null) Modifier
                      else Modifier.semantics { error(state.error) }
                  )
                  .testTag("movimiento_amount"),
          enabled = enabled,
          singleLine = true,
          isError = state.error != null,
          textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          supportingText =
              state.error?.let { message ->
                {
                  Text(
                      text = message,
                      modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                  )
                }
              },
      )
    }
  }
}
