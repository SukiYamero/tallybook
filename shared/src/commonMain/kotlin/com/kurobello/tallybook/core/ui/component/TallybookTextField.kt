package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.kurobello.tallybook.core.ui.theme.TallybookSpacing
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlinx.coroutines.yield

@Composable
@Suppress("LongParameterList")
internal fun TallybookTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    message: TallybookFieldMessage? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = TallybookTheme.typography.body,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
  val state = rememberTallybookTextFieldState(value = value, onValueChange = onValueChange)
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()
  val isError = message is TallybookFieldMessage.Error
  val inputDescription = label ?: placeholder
  val appearance =
      tallybookTextFieldAppearance(enabled = enabled, isFocused = isFocused, isError = isError)
  val decoration =
      TallybookTextFieldDecorationParameters(
          state = state,
          placeholder = placeholder,
          textStyle = textStyle,
          appearance = appearance,
          leadingContent = leadingContent,
          trailingContent = trailingContent,
      )

  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(TallybookSpacing.space4),
  ) {
    if (label != null) {
      TallybookText(
          text = label,
          color = appearance.labelColor,
          style = TallybookTheme.typography.label,
      )
    }
    BasicTextField(
        state = state,
        modifier =
            Modifier.semantics {
              if (inputDescription != null) {
                contentDescription = inputDescription
              }
              if (message is TallybookFieldMessage.Error) {
                error(message.text)
              }
            },
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle.merge(color = appearance.valueColor),
        keyboardOptions = keyboardOptions,
        lineLimits =
            if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.Default,
        interactionSource = interactionSource,
        decorator = { innerTextField ->
          TallybookTextFieldDecoration(
              parameters = decoration,
              innerTextField = innerTextField,
          )
        },
    )
    if (message != null) {
      TallybookFieldMessageContent(message = message)
    }
  }
}

@Composable
private fun rememberTallybookTextFieldState(
    value: String,
    onValueChange: (String) -> Unit,
): TextFieldState {
  val state = rememberTextFieldState(value)
  val currentOnValueChange by rememberUpdatedState(onValueChange)
  val currentValue by rememberUpdatedState(value)
  LaunchedEffect(state) {
    snapshotFlow { state.text.toString() }
        .collect { editedValue ->
          if (editedValue != currentValue) {
            currentOnValueChange(editedValue)
            // Yield lets a synchronous parent response become the source of truth before
            // reconciliation.
            yield()
            if (state.text.toString() != currentValue) {
              state.edit { replace(0, length, currentValue) }
            }
          }
        }
  }
  LaunchedEffect(value) {
    if (state.text.toString() != value) {
      state.edit { replace(0, length, value) }
    }
  }
  return state
}

@Composable
private fun TallybookTextFieldDecoration(
    parameters: TallybookTextFieldDecorationParameters,
    innerTextField: @Composable () -> Unit,
) {
  val shape = RoundedCornerShape(TallybookTheme.radii.radius12)
  Row(
      modifier =
          Modifier.defaultMinSize(minHeight = TallybookTheme.dimensions.fieldMinimumHeight)
              .clip(shape)
              .background(parameters.appearance.containerColor)
              .border(
                  width = TallybookTheme.dimensions.fieldBorderWidth,
                  color = parameters.appearance.borderColor,
                  shape = shape,
              )
              .padding(
                  horizontal = TallybookTheme.dimensions.fieldHorizontalPadding,
                  vertical = TallybookTheme.dimensions.fieldVerticalPadding,
              ),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    if (parameters.leadingContent != null) {
      Box(modifier = Modifier.padding(end = TallybookSpacing.space8)) {
        parameters.leadingContent()
      }
    }
    Box(modifier = Modifier.weight(1f)) {
      if (parameters.state.text.isEmpty() && parameters.placeholder != null) {
        TallybookText(
            text = parameters.placeholder,
            color = parameters.appearance.placeholderColor,
            style = parameters.textStyle,
        )
      }
      innerTextField()
    }
    if (parameters.trailingContent != null) {
      Box(modifier = Modifier.padding(start = TallybookSpacing.space8)) {
        parameters.trailingContent()
      }
    }
  }
}

private data class TallybookTextFieldDecorationParameters(
    val state: TextFieldState,
    val placeholder: String?,
    val textStyle: TextStyle,
    val appearance: TallybookTextFieldAppearance,
    val leadingContent: (@Composable () -> Unit)?,
    val trailingContent: (@Composable () -> Unit)?,
)

private data class TallybookTextFieldAppearance(
    val containerColor: Color,
    val borderColor: Color,
    val valueColor: Color,
    val labelColor: Color,
    val placeholderColor: Color,
)

@Composable
private fun tallybookTextFieldAppearance(
    enabled: Boolean,
    isFocused: Boolean,
    isError: Boolean,
): TallybookTextFieldAppearance {
  val colors = TallybookTheme.colors
  return TallybookTextFieldAppearance(
      containerColor = colors.surface.card,
      borderColor =
          when {
            isError -> colors.status.danger.base
            enabled && isFocused -> colors.border.ring
            else -> colors.border.base
          },
      valueColor = if (enabled) colors.foreground.primary else colors.foreground.disabled,
      labelColor = if (isError) colors.status.danger.base else colors.foreground.secondary,
      placeholderColor = if (enabled) colors.foreground.tertiary else colors.foreground.disabled,
  )
}

@Composable
internal fun TallybookFieldMessageContent(
    message: TallybookFieldMessage,
    modifier: Modifier = Modifier,
) {
  val isError = message is TallybookFieldMessage.Error
  val color =
      if (isError) TallybookTheme.colors.status.danger.base
      else TallybookTheme.colors.foreground.tertiary
  TallybookText(
      text = message.text,
      modifier =
          modifier.then(
              if (isError) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier
          ),
      color = color,
      style = TallybookTheme.typography.caption,
  )
}
