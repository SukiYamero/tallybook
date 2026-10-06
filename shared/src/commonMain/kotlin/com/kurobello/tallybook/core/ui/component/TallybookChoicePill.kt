package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Composable
internal fun TallybookChoicePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  val colors = TallybookTheme.colors
  val interactionSource = remember { MutableInteractionSource() }
  Box(
      modifier =
          modifier
              .defaultMinSize(
                  minWidth = TallybookTheme.dimensions.minimumTouchTarget,
                  minHeight = TallybookTheme.dimensions.minimumTouchTarget,
              )
              .selectable(
                  selected = selected,
                  interactionSource = interactionSource,
                  indication = null,
                  enabled = enabled,
                  role = Role.RadioButton,
                  onClick = onClick,
              ),
      contentAlignment = Alignment.Center,
  ) {
    TallybookSurface(
        modifier =
            Modifier.defaultMinSize(minHeight = TallybookTheme.dimensions.choicePillVisualHeight),
        style =
            TallybookSurfaceStyle(
                color = if (selected) colors.surface.deep else colors.surface.card,
                contentColor =
                    if (enabled) colors.foreground.primary else colors.foreground.disabled,
                shape = RoundedCornerShape(TallybookTheme.radii.radius12),
                border =
                    BorderStroke(
                        TallybookTheme.dimensions.borderThin,
                        if (selected) colors.brand.secondary else colors.border.subtle,
                    ),
                contentAlignment = Alignment.Center,
            ),
    ) {
      TallybookText(
          label,
          modifier =
              Modifier.padding(
                  horizontal = TallybookTheme.spacing.space12,
                  vertical = TallybookTheme.spacing.space4,
              ),
          style = TallybookTheme.typography.label,
      )
    }
  }
}
