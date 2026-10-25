package com.kurobello.tallybook.core.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.kurobello.tallybook.core.ui.component.TallybookTapFeedback
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

internal enum class ChangeTone {
  Unfavorable,
  Favorable,
  Neutral,
}

@Immutable internal data class ChangeIndicator(val label: String, val tone: ChangeTone)

private const val LABEL_MAX_LINES = 2

@Suppress("LongParameterList")
@Composable
internal fun TallybookDistributionRow(
    label: String,
    amount: String,
    share: String,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    change: ChangeIndicator? = null,
    onClick: (() -> Unit)? = null,
) {
  val interactions = remember { MutableInteractionSource() }
  val isPressed by interactions.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  val pressed = onClick != null && (isPressed || tapFeedback.isActive)
  Column(
      modifier =
          modifier
              .clip(RoundedCornerShape(TallybookTheme.radii.radius12))
              .background(
                  if (pressed) TallybookTheme.colors.interaction.pressedOverlay
                  else Color.Transparent
              )
              .then(distributionRowClick(onClick, interactions, tapFeedback))
              .heightIn(min = TallybookTheme.dimensions.minimumTouchTarget)
              .padding(vertical = TallybookTheme.spacing.space10),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
          Modifier.size(TallybookTheme.dimensions.chartLegendDot)
              .background(color = color, shape = CircleShape)
      )
      Row(
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space10),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        TallybookText(
            text = label,
            modifier = Modifier.weight(1f, fill = false),
            style = TallybookTheme.typography.balanceLabel,
            maxLines = LABEL_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        if (change != null) {
          TallybookText(
              text = change.label,
              color = change.tone.color(),
              style = TallybookTheme.typography.caption,
          )
        }
      }
      TallybookText(text = amount, style = TallybookTheme.typography.bodyEmphasis)
      TallybookText(
          text = share,
          modifier = Modifier.width(TallybookTheme.dimensions.chartDistributionShareWidth),
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.controlValue.copy(textAlign = TextAlign.End),
      )
    }
    DistributionBar(fraction = fraction, color = color)
  }
}

@Composable
private fun DistributionBar(fraction: Float, color: Color) {
  val safeFraction = if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 1f)
  Box(
      Modifier.fillMaxWidth()
          .height(TallybookTheme.dimensions.chartDistributionBarHeight)
          .background(color = TallybookChartTheme.colors.track, shape = CircleShape)
  ) {
    if (safeFraction > 0f) {
      Box(
          Modifier.fillMaxWidth(safeFraction)
              .height(TallybookTheme.dimensions.chartDistributionBarHeight)
              .background(color = color, shape = CircleShape)
      )
    }
  }
}

@Composable
private fun ChangeTone.color(): Color =
    when (this) {
      ChangeTone.Unfavorable -> TallybookTheme.colors.status.danger.base
      ChangeTone.Favorable -> TallybookTheme.colors.status.success.base
      ChangeTone.Neutral -> TallybookTheme.colors.foreground.tertiary
    }

private fun distributionRowClick(
    onClick: (() -> Unit)?,
    interactions: MutableInteractionSource,
    tapFeedback: TallybookTapFeedback,
): Modifier =
    if (onClick == null) Modifier.semantics(mergeDescendants = true) {}
    else
        Modifier.clickable(
            interactionSource = interactions,
            indication = null,
            role = Role.Button,
            onClick = {
              tapFeedback.trigger()
              onClick()
            },
        )
