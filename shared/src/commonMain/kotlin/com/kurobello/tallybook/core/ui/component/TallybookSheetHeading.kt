package com.kurobello.tallybook.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Composable
internal fun TallybookSheetTitle(text: String, modifier: Modifier = Modifier) {
  TallybookText(
      text = text,
      modifier = modifier.semantics { heading() },
      color = TallybookTheme.colors.foreground.primary,
      style = TallybookTheme.typography.sheetTitle,
  )
}

@Composable
internal fun TallybookSectionHeading(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TallybookTheme.colors.foreground.tertiary,
) {
  TallybookText(
      text = text,
      modifier = modifier.semantics { heading() },
      color = color,
      style = TallybookTheme.typography.sectionHeading,
  )
}
