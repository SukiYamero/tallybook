package com.kurobello.tallybook.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Repeat
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceDefaults
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.pluralStringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.home_recurrentes_pending

/** Only while something is pending; it never touches the balance or recent movimientos above. */
internal fun LazyListScope.homeRecurrentes(uiState: HomeUiState, onOpen: (() -> Unit)?) {
  val count = uiState.pendingRecurrentes
  if (count <= 0 || onOpen == null) return
  item(key = "recurrentes") {
    HomeRecurrentesRow(
        count = count,
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().padding(bottom = TallybookTheme.spacing.space18),
    )
  }
}

@Composable
internal fun HomeRecurrentesRow(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  TallybookSurface(
      modifier =
          modifier
              .defaultMinSize(minHeight = TallybookTheme.dimensions.minimumTouchTarget)
              .clickable(
                  interactionSource = interactionSource,
                  indication = null,
                  role = Role.Button,
                  onClick = {
                    tapFeedback.trigger()
                    onClick()
                  },
              ),
      style =
          TallybookSurfaceDefaults.cardStyle()
              .copy(
                  border =
                      BorderStroke(
                          TallybookTheme.dimensions.borderThin,
                          TallybookTheme.colors.border.subtle,
                      )
              ),
      isPressed = isPressed,
      hasTapFeedback = tapFeedback.isActive,
  ) {
    Row(
        modifier =
            Modifier.padding(
                    horizontal = TallybookTheme.spacing.space14,
                    vertical = TallybookTheme.spacing.space12,
                )
                .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Image(
          imageVector = Lucide.Repeat,
          contentDescription = null,
          modifier = Modifier.size(TallybookTheme.dimensions.iconMedium),
          colorFilter = ColorFilter.tint(TallybookTheme.colors.brand.primary),
      )
      TallybookText(
          pluralStringResource(Res.plurals.home_recurrentes_pending, count, count),
          modifier = Modifier.weight(1f),
          style = TallybookTheme.typography.bodyEmphasis,
      )
      Image(
          imageVector = Lucide.ChevronRight,
          contentDescription = null,
          modifier = Modifier.size(TallybookTheme.dimensions.fieldIconSize),
          colorFilter = ColorFilter.tint(TallybookTheme.colors.foreground.tertiary),
      )
    }
  }
}

@Preview
@Composable
internal fun HomeRecurrentesRowPreview() {
  TallybookTheme { HomeRecurrentesRow(count = 2, onClick = {}) }
}
