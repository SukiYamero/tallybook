package com.kurobello.tallybook.core.ui.category

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.component.TallybookInputLabel
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.component.resolveTallybookSurfaceContainerColor
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource

/**
 * One choice per [CategoryTint], each previewing [imageVector] as the avatar it would become, so
 * the user picks the color on the icon it will actually wear.
 */
@Composable
@Suppress("LongParameterList")
internal fun CategoryTintPicker(
    label: String,
    selected: CategoryTint,
    imageVector: ImageVector,
    onSelect: (CategoryTint) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    TallybookInputLabel(label)
    FlowRow(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
    ) {
      CategoryTint.entries.forEach { tint ->
        key(tint) {
          CategoryTintOption(
              tint = tint,
              imageVector = imageVector,
              selected = tint == selected,
              enabled = enabled,
              onClick = { onSelect(tint) },
          )
        }
      }
    }
  }
}

@Composable
private fun CategoryTintOption(
    tint: CategoryTint,
    imageVector: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
  val colors = tint.tintColors()
  val disabled = TallybookTheme.colors.foreground.disabled
  CategoryOptionTarget(
      label = stringResource(tint.labelResource()),
      selected = selected,
      enabled = enabled,
      onClick = onClick,
      visualSize = TallybookTheme.dimensions.categoryIconOptionVisualSize,
      containerColor = colors.container,
      borderColor = if (selected) colors.content else Color.Transparent,
  ) {
    CategoryOptionIcon(
        imageVector = imageVector,
        color = if (enabled) colors.content else disabled,
        size = TallybookTheme.dimensions.iconMedium,
    )
  }
}

/** A circular choice drawn at [visualSize] inside a minimum touch target, announced by [label]. */
@Composable
@Suppress("LongParameterList")
internal fun CategoryOptionTarget(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    visualSize: Dp,
    containerColor: Color,
    borderColor: Color,
    content: @Composable () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  Box(
      modifier =
          Modifier.size(TallybookTheme.dimensions.minimumTouchTarget)
              .selectable(
                  selected = selected,
                  interactionSource = interactionSource,
                  indication = null,
                  enabled = enabled,
                  role = Role.RadioButton,
                  onClick = {
                    tapFeedback.trigger()
                    onClick()
                  },
              )
              .semantics { contentDescription = label },
      contentAlignment = Alignment.Center,
  ) {
    Box(
        modifier =
            Modifier.size(visualSize)
                .clip(CircleShape)
                .background(
                    resolveTallybookSurfaceContainerColor(
                        containerColor = containerColor,
                        pressedOverlay = TallybookTheme.colors.interaction.pressedOverlay,
                        isPressed = isPressed,
                        hasTapFeedback = tapFeedback.isActive,
                    )
                )
                .border(
                    width =
                        if (selected) TallybookTheme.dimensions.categoryOptionSelectedBorderWidth
                        else TallybookTheme.dimensions.borderThin,
                    color = borderColor,
                    shape = CircleShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
      content()
    }
  }
}

@Composable
internal fun CategoryOptionIcon(imageVector: ImageVector, color: Color, size: Dp) {
  Image(
      imageVector = imageVector,
      contentDescription = null,
      modifier = Modifier.size(size),
      colorFilter = ColorFilter.tint(color),
  )
}
