package com.kurobello.tallybook.core.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.kurobello.tallybook.core.ui.component.clearFocusOnUnconsumedTap
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Composable
internal fun TallybookScreenFrame(
    modifier: Modifier = Modifier,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
  val contentPadding =
      screenContentPadding(
          safeInsets = WindowInsets.safeDrawing.asPaddingValues(),
          layoutDirection = LocalLayoutDirection.current,
      )
  Box(
      modifier =
          modifier
              .fillMaxSize()
              .clearFocusOnUnconsumedTap()
              .background(TallybookTheme.colors.canvas)
  ) {
    Box(
        modifier =
            Modifier.align(Alignment.TopCenter)
                .widthIn(max = TallybookTheme.dimensions.screenContentMaxWidth)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(contentPadding),
        content = content,
    )
    if (bottomBar != null) {
      Box(
          modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
      ) {
        bottomBar()
      }
    }
  }
}

internal fun screenContentPadding(
    safeInsets: PaddingValues,
    layoutDirection: LayoutDirection,
): PaddingValues {
  val safeStart =
      if (layoutDirection == LayoutDirection.Ltr) {
        safeInsets.calculateLeftPadding(layoutDirection)
      } else {
        safeInsets.calculateRightPadding(layoutDirection)
      }
  val safeEnd =
      if (layoutDirection == LayoutDirection.Ltr) {
        safeInsets.calculateRightPadding(layoutDirection)
      } else {
        safeInsets.calculateLeftPadding(layoutDirection)
      }
  return PaddingValues(
      start = safeStart + TallybookTheme.dimensions.screenHorizontal,
      top = maxOf(safeInsets.calculateTopPadding(), TallybookTheme.dimensions.screenTopFloor),
      end = safeEnd + TallybookTheme.dimensions.screenHorizontal,
      bottom =
          maxOf(safeInsets.calculateBottomPadding(), TallybookTheme.dimensions.screenBottomFloor),
  )
}
