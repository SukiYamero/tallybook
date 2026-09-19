package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.ui.theme.TallybookSpacing
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Composable
@Suppress("LongParameterList")
internal fun TallybookSelectionField(
    label: String,
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    message: TallybookFieldMessage? = null,
    leadingContent: (@Composable () -> Unit)? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  val appearance = tallybookSelectionFieldAppearance(enabled = enabled, message = message)
  Column(
      modifier =
          modifier
              .then(
                  if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = {
                          tapFeedback.trigger()
                          onClick()
                        },
                    )
                  } else Modifier
              )
              .semantics(mergeDescendants = true) {
                role = Role.Button
                if (!enabled) {
                  disabled()
                }
                if (message is TallybookFieldMessage.Error) {
                  error(message.text)
                }
              },
      verticalArrangement = Arrangement.spacedBy(TallybookSpacing.space4),
  ) {
    TallybookText(
        text = label,
        color = appearance.labelColor,
        style = TallybookTheme.typography.label,
    )
    TallybookSelectionFieldContainer(
        value = value,
        placeholder = placeholder,
        appearance = appearance,
        leadingContent = leadingContent,
        isPressed = isPressed || tapFeedback.isActive,
    )
    if (message != null) {
      TallybookFieldMessageContent(message = message)
    }
  }
}

@Composable
private fun TallybookSelectionFieldContainer(
    value: String?,
    placeholder: String,
    appearance: TallybookSelectionFieldAppearance,
    leadingContent: (@Composable () -> Unit)?,
    isPressed: Boolean,
) {
  TallybookSurface(
      style =
          TallybookSurfaceStyle(
              color = appearance.containerColor,
              contentColor = appearance.valueColor,
              shape = RoundedCornerShape(TallybookTheme.radii.radius12),
              border =
                  BorderStroke(
                      width = TallybookTheme.dimensions.fieldBorderWidth,
                      color = appearance.borderColor,
                  ),
          ),
      isPressed = isPressed,
  ) {
    Row(
        modifier =
            Modifier.defaultMinSize(minHeight = TallybookTheme.dimensions.fieldMinimumHeight)
                .padding(
                    horizontal = TallybookTheme.dimensions.fieldHorizontalPadding,
                    vertical = TallybookTheme.dimensions.fieldVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      if (leadingContent != null) {
        Box(modifier = Modifier.padding(end = TallybookSpacing.space8)) { leadingContent() }
      }
      Box(modifier = Modifier.weight(1f)) {
        TallybookText(
            text = value ?: placeholder,
            color = if (value == null) appearance.placeholderColor else appearance.valueColor,
            style = TallybookTheme.typography.body,
        )
      }
      Image(
          imageVector = Lucide.ChevronRight,
          contentDescription = null,
          modifier =
              Modifier.padding(start = TallybookSpacing.space8)
                  .size(TallybookTheme.dimensions.fieldIconSize),
          colorFilter = ColorFilter.tint(appearance.iconColor),
      )
    }
  }
}

private data class TallybookSelectionFieldAppearance(
    val containerColor: Color,
    val borderColor: Color,
    val valueColor: Color,
    val labelColor: Color,
    val placeholderColor: Color,
    val iconColor: Color,
)

@Composable
private fun tallybookSelectionFieldAppearance(
    enabled: Boolean,
    message: TallybookFieldMessage?,
): TallybookSelectionFieldAppearance {
  val colors = TallybookTheme.colors
  val isError = message is TallybookFieldMessage.Error
  return TallybookSelectionFieldAppearance(
      containerColor = colors.surface.card,
      borderColor = if (isError) colors.status.danger.base else colors.border.base,
      valueColor = if (enabled) colors.foreground.primary else colors.foreground.disabled,
      labelColor = if (isError) colors.status.danger.base else colors.foreground.secondary,
      placeholderColor = if (enabled) colors.foreground.tertiary else colors.foreground.disabled,
      iconColor = if (enabled) colors.foreground.tertiary else colors.foreground.disabled,
  )
}
