package com.kurobello.tallybook.core.ui.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceStyle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Immutable
internal data class CategoryTagAppearance(
    val container: Color,
    val border: Color,
    val text: Color,
    val accent: Color,
    val padding: PaddingValues,
    val dashedBorder: Boolean = false,
)

internal fun avatarTagPadding(leading: Boolean = false, trailing: Boolean = false): PaddingValues =
    PaddingValues(
        start = if (leading) TallybookTheme.spacing.space10 else TallybookTheme.spacing.space4,
        end = if (trailing) TallybookTheme.spacing.space10 else TallybookTheme.spacing.space12,
        top = TallybookTheme.spacing.space4,
        bottom = TallybookTheme.spacing.space4,
    )

@Composable
internal fun CategoryTagButton(
    onClick: () -> Unit,
    enabled: Boolean,
    appearance: CategoryTagAppearance,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  Box(
      modifier =
          modifier
              .defaultMinSize(
                  minWidth = TallybookTheme.dimensions.minimumTouchTarget,
                  minHeight = TallybookTheme.dimensions.minimumTouchTarget,
              )
              .clickable(
                  enabled = enabled,
                  interactionSource = interactionSource,
                  indication = null,
                  role = Role.Button,
                  onClick = {
                    tapFeedback.trigger()
                    onClick()
                  },
              ),
      contentAlignment = Alignment.Center,
  ) {
    CategoryTagSurface(
        appearance = appearance,
        isPressed = isPressed,
        hasTapFeedback = tapFeedback.isActive,
        content = content,
    )
  }
}

@Composable
internal fun CategoryTagSurface(
    appearance: CategoryTagAppearance,
    modifier: Modifier = Modifier,
    isPressed: Boolean = false,
    hasTapFeedback: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
  val radius = TallybookTheme.radii.radius20
  val solidBorder = !appearance.dashedBorder
  TallybookSurface(
      modifier =
          modifier.defaultMinSize(minHeight = TallybookTheme.dimensions.categoryTagVisualHeight),
      style =
          TallybookSurfaceStyle(
              color = appearance.container,
              contentColor = appearance.text,
              shape = RoundedCornerShape(radius),
              border =
                  if (solidBorder) {
                    BorderStroke(TallybookTheme.dimensions.borderThin, appearance.border)
                  } else {
                    null
                  },
              contentAlignment = Alignment.Center,
          ),
      isPressed = isPressed,
      hasTapFeedback = hasTapFeedback,
  ) {
    Row(
        modifier =
            Modifier.then(
                    if (solidBorder) Modifier
                    else
                        Modifier.dashedPillBorder(
                            color = appearance.border,
                            width = TallybookTheme.dimensions.borderThin,
                            dash = TallybookTheme.spacing.space4,
                            radius = radius,
                        )
                )
                .padding(appearance.padding),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
  }
}

/**
 * Full-height tap area over the trailing "×": it is a sibling of the tag content so the tag keeps
 * its compact size while the remove action still reaches the minimum touch target.
 */
@Composable
internal fun CategoryTagRemoveTarget(
    description: String,
    enabled: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Row(modifier = modifier) {
    Spacer(Modifier.weight(1f))
    Box(
        modifier =
            Modifier.width(TallybookTheme.dimensions.categoryTagRemoveTargetWidth)
                .fillMaxHeight()
                .clickable(
                    enabled = enabled,
                    interactionSource = null,
                    indication = null,
                    role = Role.Button,
                    onClick = onRemove,
                )
                .semantics { contentDescription = description }
    )
  }
}

@Composable
internal fun RowScope.CategoryTagLabel(text: String) {
  TallybookText(
      text = text,
      modifier = Modifier.weight(1f, fill = false),
      style = TallybookTheme.typography.controlValue,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
  )
}

@Composable
internal fun CategoryTagIcon(imageVector: ImageVector, tint: Color) {
  Image(
      imageVector = imageVector,
      contentDescription = null,
      modifier = Modifier.size(TallybookTheme.dimensions.iconSmall),
      colorFilter = ColorFilter.tint(tint),
  )
}

private fun Modifier.dashedPillBorder(color: Color, width: Dp, dash: Dp, radius: Dp): Modifier =
    drawWithContent {
      drawContent()
      val strokeWidth = width.toPx()
      val inset = strokeWidth / 2
      drawRoundRect(
          color = color,
          topLeft = Offset(inset, inset),
          size = Size(size.width - strokeWidth, size.height - strokeWidth),
          cornerRadius = CornerRadius(minOf(radius.toPx(), size.height / 2)),
          style =
              Stroke(
                  width = strokeWidth,
                  pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), dash.toPx())),
              ),
      )
    }
