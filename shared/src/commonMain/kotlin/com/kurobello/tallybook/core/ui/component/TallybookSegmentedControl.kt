package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Immutable internal data class TallybookSegmentOption<T>(val value: T, val label: String)

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
  fun colors(): TallybookSegmentedControlColors =
      TallybookSegmentedControlColors(
          selectedContainer = TallybookTheme.colors.brand.primary,
          selectedContent = TallybookTheme.colors.brand.onPrimary,
          unselectedContainer = TallybookTheme.colors.surface.raised,
          unselectedContent = TallybookTheme.colors.foreground.primary,
          disabledContainer = TallybookTheme.colors.surface.high,
          disabledContent = TallybookTheme.colors.foreground.disabled,
      )
}

@Composable
internal fun <T> TallybookSegmentedControl(
    options: List<TallybookSegmentOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  require(options.isNotEmpty()) { "Segmented control options must not be empty." }

  val colors = TallybookSegmentedControlDefaults.colors()
  Row(
      modifier = modifier.selectableGroup(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    options.forEach { option ->
      val isSelected = option.value == selected
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
      TallybookSurface(
          modifier =
              Modifier.weight(1f)
                  .defaultMinSize(
                      minHeight = TallybookTheme.dimensions.segmentedControlMinimumHeight
                  )
                  .selectable(
                      selected = isSelected,
                      enabled = enabled,
                      role = Role.RadioButton,
                      onClick = { onSelect(option.value) },
                  ),
          style =
              TallybookSurfaceStyle(
                  color = containerColor,
                  contentColor = contentColor,
                  shape = RoundedCornerShape(TallybookTheme.radii.radius12),
                  border = null,
              ),
      ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          TallybookText(text = option.label, style = TallybookTheme.typography.label)
        }
      }
    }
  }
}
