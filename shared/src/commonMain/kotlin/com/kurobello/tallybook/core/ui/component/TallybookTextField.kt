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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.kurobello.tallybook.core.ui.theme.TallybookSpacing
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

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
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()
  val isError = message is TallybookFieldMessage.Error
  val inputDescription = label ?: placeholder
  val appearance =
      tallybookTextFieldAppearance(enabled = enabled, isFocused = isFocused, isError = isError)
  val decoration =
      TallybookTextFieldDecorationParameters(
          value = value,
          placeholder = placeholder,
          textStyle = textStyle,
          appearance = appearance,
          singleLine = singleLine,
          leadingContent = leadingContent,
          trailingContent = trailingContent,
      )

  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(TallybookSpacing.space4),
  ) {
    TallybookTextFieldLabel(label = label, color = appearance.labelColor)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
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
        cursorBrush = SolidColor(TallybookTheme.colors.input.cursor),
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
          TallybookTextFieldDecoration(
              parameters = decoration,
              innerTextField = innerTextField,
          )
        },
    )
    TallybookTextFieldMessage(message = message)
  }
}

@Composable
private fun TallybookTextFieldLabel(label: String?, color: Color) {
  if (label != null) {
    TallybookText(text = label, color = color, style = TallybookTheme.typography.label)
  }
}

@Composable
private fun TallybookTextFieldMessage(message: TallybookFieldMessage?) {
  if (message != null) TallybookFieldMessageContent(message = message)
}

@Composable
private fun TallybookTextFieldDecoration(
    parameters: TallybookTextFieldDecorationParameters,
    innerTextField: @Composable () -> Unit,
) {
  val shape = RoundedCornerShape(TallybookTheme.radii.radius12)
  Row(
      modifier =
          (if (parameters.singleLine) {
                Modifier.height(TallybookTheme.dimensions.fieldMinimumHeight)
              } else {
                Modifier.defaultMinSize(minHeight = TallybookTheme.dimensions.fieldMinimumHeight)
              })
              .clip(shape)
              .background(parameters.appearance.containerColor)
              .border(
                  width = TallybookTheme.dimensions.fieldBorderWidth,
                  color = parameters.appearance.borderColor,
                  shape = shape,
              )
              .padding(horizontal = TallybookTheme.dimensions.fieldHorizontalPadding)
              .then(
                  if (parameters.singleLine) Modifier
                  else Modifier.padding(vertical = TallybookTheme.dimensions.fieldVerticalPadding)
              ),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    if (parameters.leadingContent != null) {
      Box(modifier = Modifier.padding(end = TallybookSpacing.space8)) {
        parameters.leadingContent()
      }
    }
    Box(modifier = Modifier.weight(1f)) {
      if (parameters.value.isEmpty() && parameters.placeholder != null) {
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
    val value: String,
    val placeholder: String?,
    val textStyle: TextStyle,
    val appearance: TallybookTextFieldAppearance,
    val singleLine: Boolean,
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
