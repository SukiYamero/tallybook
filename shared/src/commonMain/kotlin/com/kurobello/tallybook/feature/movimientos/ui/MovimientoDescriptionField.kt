package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessage
import com.kurobello.tallybook.core.ui.component.TallybookInputLabel
import com.kurobello.tallybook.core.ui.component.TallybookTextField
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormState
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.description_optional

@Composable
internal fun MovimientoDescriptionField(
    uiState: MovimientoFormState,
    onValueChange: (String) -> Unit,
    inputModifier: Modifier,
    enabled: Boolean,
) {
  val focusManager = LocalFocusManager.current
  TallybookTextField(
      value = uiState.descripcion,
      onValueChange = { onValueChange(it.take(MOVIMIENTO_DESCRIPTION_MAX_LENGTH)) },
      modifier = Modifier.fillMaxWidth().then(inputModifier),
      enabled = enabled,
      label = stringResource(Res.string.description_optional),
      message =
          TallybookFieldMessage.Supporting(
              "${uiState.descripcion.length}/$MOVIMIENTO_DESCRIPTION_MAX_LENGTH"
          ),
      singleLine = false,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
      labelContent = { TallybookInputLabel(stringResource(Res.string.description_optional)) },
  )
}
