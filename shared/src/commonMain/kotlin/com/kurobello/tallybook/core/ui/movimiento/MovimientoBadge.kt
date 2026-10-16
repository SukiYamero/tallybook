package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceStyle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookSpacing
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.core.ui.theme.TallybookTypography

/** [Large] is for the review sheet, where tipo and fecha lead the content. */
internal enum class MovimientoBadgeSize(
    val iconSize: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val textStyle: (TallybookTypography) -> TextStyle,
) {
  Regular(
      iconSize = TallybookSpacing.space16,
      horizontalPadding = TallybookSpacing.space10,
      verticalPadding = TallybookSpacing.space6,
      textStyle = TallybookTypography::label,
  ),
  Large(
      iconSize = TallybookSpacing.space20,
      horizontalPadding = TallybookSpacing.space14,
      verticalPadding = TallybookSpacing.space10,
      textStyle = TallybookTypography::title,
  ),
}

@Immutable internal data class MovimientoBadgeColors(val content: Color, val container: Color)

@Composable
internal fun MovimientoBadge(
    icon: ImageVector,
    text: String,
    colors: MovimientoBadgeColors,
    size: MovimientoBadgeSize,
    modifier: Modifier = Modifier,
) {
  TallybookSurface(
      modifier = modifier.semantics(mergeDescendants = true) {},
      style =
          TallybookSurfaceStyle(
              color = colors.container,
              contentColor = colors.content,
              shape = RoundedCornerShape(TallybookTheme.radii.radius20),
              border = null,
          ),
  ) {
    Row(
        modifier =
            Modifier.padding(
                horizontal = size.horizontalPadding,
                vertical = size.verticalPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Image(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(size.iconSize),
          colorFilter = ColorFilter.tint(colors.content),
      )
      TallybookText(
          text = text,
          style = size.textStyle(TallybookTheme.typography),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
