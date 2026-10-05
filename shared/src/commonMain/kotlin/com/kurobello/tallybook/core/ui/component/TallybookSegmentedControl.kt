package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.kurobello.tallybook.core.ui.theme.TallybookColors
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

internal enum class TallybookSegmentTone {
  Neutral,
  Income,
  Expense,
}

internal enum class TallybookSegmentedControlStyle {
  Tonal,
  Neutral,
}

@Immutable
internal data class TallybookSegmentOption<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
    val tone: TallybookSegmentTone = TallybookSegmentTone.Neutral,
)

@Immutable
private data class TallybookSegmentedControlColors(
    val selectedContainer: Color,
    val selectedContent: Color,
    val unselectedContainer: Color,
    val unselectedContent: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
)

private object TallybookSegmentedControlDefaults {
  @Composable
  fun colors(
      style: TallybookSegmentedControlStyle,
      tone: TallybookSegmentTone,
  ): TallybookSegmentedControlColors {
    val colors = TallybookTheme.colors
    if (style == TallybookSegmentedControlStyle.Neutral) {
      return TallybookSegmentedControlColors(
          selectedContainer = neutralSelectedSegmentContainer(colors),
          selectedContent = colors.foreground.primary,
          unselectedContainer = Color.Transparent,
          unselectedContent = colors.foreground.tertiary,
          disabledContainer = Color.Transparent,
          disabledContent = colors.foreground.disabled,
      )
    }
    val status =
        when (tone) {
          TallybookSegmentTone.Neutral -> null
          TallybookSegmentTone.Income -> colors.status.success
          TallybookSegmentTone.Expense -> colors.status.danger
        }
    val selectedContent = status?.base ?: colors.brand.secondary
    return TallybookSegmentedControlColors(
        selectedContainer = selectedContent.copy(alpha = SEGMENT_TONE_CONTAINER_ALPHA),
        selectedContent = selectedContent,
        unselectedContainer = Color.Transparent,
        unselectedContent = colors.foreground.tertiary,
        disabledContainer = Color.Transparent,
        disabledContent = colors.foreground.disabled,
    )
  }
}

@Composable
@Suppress("LongParameterList")
internal fun <T> TallybookSegmentedControl(
    options: List<TallybookSegmentOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: TallybookSegmentedControlStyle = TallybookSegmentedControlStyle.Tonal,
    labelStyle: TextStyle = TallybookTheme.typography.label,
) {
  require(options.isNotEmpty()) { "Segmented control options must not be empty." }

  val trackShape = RoundedCornerShape(TallybookTheme.radii.radius16)
  Row(
      modifier =
          modifier
              .clip(trackShape)
              .background(TallybookTheme.colors.surface.deep)
              .padding(TallybookTheme.spacing.space4)
              .selectableGroup(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space0),
  ) {
    options.forEach { option ->
      TallybookSegment(
          isSelected = option.value == selected,
          colors = TallybookSegmentedControlDefaults.colors(style, option.tone),
          enabled = enabled,
          onSelect = { onSelect(option.value) },
      ) { contentColor ->
        TallybookSegmentContent(
            option = option,
            contentColor = contentColor,
            labelStyle = labelStyle,
        )
      }
    }
  }
}

private const val SEGMENT_TONE_CONTAINER_ALPHA = 0.15f
private const val NEUTRAL_SELECTED_CONTAINER_DARKENING_ALPHA = 0.04f

internal fun neutralSelectedSegmentContainer(colors: TallybookColors): Color =
    colors.foreground.primary
        .copy(alpha = NEUTRAL_SELECTED_CONTAINER_DARKENING_ALPHA)
        .compositeOver(colors.surface.highest)

@Composable
private fun RowScope.TallybookSegment(
    isSelected: Boolean,
    colors: TallybookSegmentedControlColors,
    enabled: Boolean,
    onSelect: () -> Unit,
    content: @Composable (Color) -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val containerColor =
      when {
        !enabled -> colors.disabledContainer
        isSelected -> colors.selectedContainer
        else -> colors.unselectedContainer
      }
  val contentColor =
      when {
        !enabled -> colors.disabledContent
        isSelected -> colors.selectedContent
        else -> colors.unselectedContent
      }
  val selectionModifier =
      if (enabled) {
        Modifier.selectable(
            selected = isSelected,
            interactionSource = interactionSource,
            indication = null,
            role = Role.RadioButton,
            onClick = onSelect,
        )
      } else {
        Modifier.semantics(mergeDescendants = true) {
          this.selected = isSelected
          role = Role.RadioButton
          disabled()
        }
      }
  TallybookSurface(
      modifier =
          Modifier.weight(1f)
              .defaultMinSize(
                  minWidth = TallybookTheme.dimensions.minimumTouchTarget,
                  minHeight = TallybookTheme.dimensions.segmentedControlMinimumHeight,
              )
              .then(selectionModifier),
      style =
          TallybookSurfaceStyle(
              color = containerColor,
              contentColor = contentColor,
              shape = RoundedCornerShape(TallybookTheme.radii.radius12),
              border = null,
              contentAlignment = Alignment.Center,
          ),
  ) {
    content(contentColor)
  }
}

@Composable
private fun TallybookSegmentContent(
    option: TallybookSegmentOption<*>,
    contentColor: Color,
    labelStyle: TextStyle,
) {
  Row(
      horizontalArrangement =
          Arrangement.spacedBy(
              if (option.icon == null) TallybookTheme.spacing.space0
              else TallybookTheme.spacing.space6
          ),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    if (option.icon != null) {
      androidx.compose.foundation.Image(
          imageVector = option.icon,
          contentDescription = null,
          modifier = Modifier.size(TallybookTheme.dimensions.movementSummaryIcon),
          colorFilter = ColorFilter.tint(contentColor),
      )
    }
    TallybookText(
        text = option.label,
        style = labelStyle,
    )
  }
}
