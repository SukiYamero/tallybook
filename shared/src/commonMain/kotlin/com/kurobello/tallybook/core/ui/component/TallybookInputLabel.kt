package com.kurobello.tallybook.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Composable
internal fun TallybookInputLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TallybookTheme.colors.foreground.tertiary,
) {
  TallybookText(
      text = text,
      modifier = modifier,
      color = color,
      style = TallybookTheme.typography.inputLabel,
  )
}
