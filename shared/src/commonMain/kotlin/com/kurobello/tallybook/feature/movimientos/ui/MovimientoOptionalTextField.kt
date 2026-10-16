package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessage
import com.kurobello.tallybook.core.ui.component.TallybookInputLabel
import com.kurobello.tallybook.core.ui.component.TallybookTextField

@Composable
@Suppress("LongParameterList")
internal fun MovimientoOptionalTextField(
    value: String,
    label: String,
    maxLength: Int,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  val focusManager = LocalFocusManager.current
  TallybookTextField(
      value = value,
      onValueChange = { onValueChange(it.take(maxLength)) },
      modifier = modifier.fillMaxWidth(),
      enabled = enabled,
      label = label,
      message = TallybookFieldMessage.Supporting("${value.length}/$maxLength"),
      singleLine = false,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
      labelContent = { TallybookInputLabel(label) },
  )
}
