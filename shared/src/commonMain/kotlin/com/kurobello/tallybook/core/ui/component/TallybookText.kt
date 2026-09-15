package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

internal val LocalTallybookContentColor = staticCompositionLocalOf { Color.Unspecified }

@Composable
internal fun TallybookText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = TallybookTheme.typography.body,
) {
  val localColor = LocalTallybookContentColor.current
  val resolvedColor =
      when {
        color != Color.Unspecified -> color
        localColor != Color.Unspecified -> localColor
        else -> TallybookTheme.colors.foreground.primary
      }
  BasicText(
      text = text,
      modifier = modifier,
      style = style.merge(color = resolvedColor),
  )
}
