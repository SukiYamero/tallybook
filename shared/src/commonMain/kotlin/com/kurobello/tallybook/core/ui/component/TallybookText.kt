package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

internal val LocalTallybookContentColor = staticCompositionLocalOf { Color.Unspecified }

@Suppress("LongParameterList")
@Composable
internal fun TallybookText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = TallybookTheme.typography.body,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
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
      maxLines = maxLines,
      overflow = overflow,
  )
}
