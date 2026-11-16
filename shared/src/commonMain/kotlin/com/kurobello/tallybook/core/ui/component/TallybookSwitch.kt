package com.kurobello.tallybook.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.recurrencia_repeat_body
import tallybook.shared.generated.resources.recurrencia_repeat_label

/**
 * A labeled on/off row: the whole row is the toggle target, so the switch track stays small while
 * the hit area spans the label and keeps at least the minimum touch height.
 */
@Composable
@Suppress("LongParameterList")
internal fun TallybookSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
) {
  val interactionSource = remember { MutableInteractionSource() }
  Row(
      modifier =
          modifier
              .fillMaxWidth()
              .heightIn(min = TallybookTheme.dimensions.minimumTouchTarget)
              .toggleable(
                  value = checked,
                  interactionSource = interactionSource,
                  indication = null,
                  enabled = enabled,
                  role = Role.Switch,
                  onValueChange = onCheckedChange,
              ),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space2),
    ) {
      TallybookText(
          label,
          color =
              if (enabled) TallybookTheme.colors.foreground.primary
              else TallybookTheme.colors.foreground.disabled,
          style = TallybookTheme.typography.bodyEmphasis,
      )
      supportingText?.let {
        TallybookText(
            it,
            color = TallybookTheme.colors.foreground.tertiary,
            style = TallybookTheme.typography.caption,
        )
      }
    }
    TallybookSwitchTrack(checked = checked, enabled = enabled)
  }
}

@Composable
private fun TallybookSwitchTrack(checked: Boolean, enabled: Boolean) {
  val colors = TallybookTheme.colors
  val dimensions = TallybookTheme.dimensions
  val travel =
      dimensions.switchTrackWidth - dimensions.switchThumbSize - dimensions.switchThumbInset * 2
  val offset by
      animateDpAsState(
          if (checked) travel else TallybookTheme.spacing.space0,
          tween(TallybookTheme.motion.fadeMillis),
          label = "switch thumb",
      )
  val trackColor by
      animateColorAsState(
          when {
            !enabled -> colors.surface.high
            checked -> colors.brand.primary
            else -> colors.surface.highest
          },
          tween(TallybookTheme.motion.fadeMillis),
          label = "switch track",
      )
  Box(
      modifier =
          Modifier.size(dimensions.switchTrackWidth, dimensions.switchTrackHeight)
              .background(trackColor, CircleShape)
              .padding(dimensions.switchThumbInset),
      contentAlignment = Alignment.CenterStart,
  ) {
    Box(
        Modifier.offset(x = offset)
            .size(dimensions.switchThumbSize)
            .background(
                when {
                  !enabled -> colors.foreground.disabled
                  checked -> colors.brand.onPrimary
                  else -> colors.foreground.tertiary
                },
                CircleShape,
            )
    )
  }
}

@Preview
@Composable
internal fun TallybookSwitchRowPreview() {
  TallybookTheme {
    Column {
      val label = stringResource(Res.string.recurrencia_repeat_label)
      TallybookSwitchRow(label = label, checked = true, onCheckedChange = {})
      TallybookSwitchRow(
          label = label,
          checked = false,
          onCheckedChange = {},
          supportingText = stringResource(Res.string.recurrencia_repeat_body),
      )
      TallybookSwitchRow(label = label, checked = true, onCheckedChange = {}, enabled = false)
    }
  }
}
