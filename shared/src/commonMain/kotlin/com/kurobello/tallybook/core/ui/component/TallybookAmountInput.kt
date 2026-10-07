@file:Suppress("LongMethod", "LongParameterList")

package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Composable
internal fun TallybookAmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    message: TallybookFieldMessage? = null,
    focusRequester: FocusRequester? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    inputModifier: Modifier = Modifier,
    labelContent: (@Composable () -> Unit)? = null,
) {
  val appearance = tallybookAmountInputAppearance(enabled = enabled, message = message)
  Column(
      modifier = modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.dimensions.fieldLabelSpacing),
  ) {
    if (labelContent != null) {
      labelContent()
    } else {
      TallybookInputLabel(text = label, color = appearance.labelColor)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      TallybookAmountSymbol(symbol = currencySymbol, color = appearance.symbolColor)
      BasicTextField(
          value = value,
          onValueChange = onValueChange,
          modifier =
              Modifier.widthIn(
                      min = TallybookTheme.dimensions.amountInputMinimumWidth,
                      max = TallybookTheme.dimensions.amountInputMaximumWidth,
                  )
                  .then(
                      if (focusRequester == null) Modifier
                      else Modifier.focusRequester(focusRequester)
                  )
                  .then(inputModifier)
                  .semantics {
                    contentDescription = label
                    if (message is TallybookFieldMessage.Error) {
                      error(message.text)
                    }
                  },
          enabled = enabled,
          singleLine = true,
          textStyle =
              TallybookTheme.typography.amount.copy(
                  color = appearance.valueColor,
                  textAlign = TextAlign.Center,
              ),
          keyboardOptions =
              KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
          keyboardActions = keyboardActions,
          cursorBrush = SolidColor(TallybookTheme.colors.input.cursor),
          decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center) {
              if (value.isEmpty()) {
                TallybookText(
                    text = "0",
                    color = appearance.placeholderColor,
                    style = TallybookTheme.typography.amount,
                )
              }
              innerTextField()
            }
          },
      )
      TallybookAmountSymbol(
          symbol = currencySymbol,
          color = appearance.symbolColor,
          invisible = true,
      )
    }
    if (message != null) {
      TallybookFieldMessageContent(message = message)
    }
  }
}

@Composable
private fun TallybookAmountSymbol(
    symbol: String,
    color: androidx.compose.ui.graphics.Color,
    invisible: Boolean = false,
) {
  TallybookText(
      text = symbol,
      color = if (invisible) color.copy(alpha = 0f) else color,
      style = TallybookTheme.typography.amountSymbol,
  )
}

private data class TallybookAmountInputAppearance(
    val labelColor: androidx.compose.ui.graphics.Color,
    val symbolColor: androidx.compose.ui.graphics.Color,
    val valueColor: androidx.compose.ui.graphics.Color,
    val placeholderColor: androidx.compose.ui.graphics.Color,
)

@Composable
private fun tallybookAmountInputAppearance(
    enabled: Boolean,
    message: TallybookFieldMessage?,
): TallybookAmountInputAppearance {
  val colors = TallybookTheme.colors
  val isError = message is TallybookFieldMessage.Error
  val valueColor =
      when {
        !enabled -> colors.foreground.disabled
        isError -> colors.status.danger.base
        else -> colors.foreground.primary
      }
  return TallybookAmountInputAppearance(
      labelColor = if (isError) colors.status.danger.base else colors.foreground.tertiary,
      symbolColor = if (enabled) colors.foreground.faint else colors.foreground.disabled,
      valueColor = valueColor,
      placeholderColor = if (enabled) colors.foreground.faint else colors.foreground.disabled,
  )
}
