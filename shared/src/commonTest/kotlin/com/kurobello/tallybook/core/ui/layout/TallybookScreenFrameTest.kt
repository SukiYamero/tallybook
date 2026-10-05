package com.kurobello.tallybook.core.ui.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class TallybookScreenFrameTest {

  @Test
  fun hostedFrameLeavesBottomSpaceToThePublishedBarClearance() {
    val padding =
        screenContentPadding(
            PaddingValues(top = 40.dp, start = 8.dp, end = 10.dp),
            LayoutDirection.Ltr,
            bottomPaddingFloor = 0.dp,
        )
    assertEquals(0.dp, padding.calculateBottomPadding())
    assertEquals(40.dp, padding.calculateTopPadding())
    assertEquals(28.dp, padding.calculateLeftPadding(LayoutDirection.Ltr))
    assertEquals(30.dp, padding.calculateRightPadding(LayoutDirection.Ltr))
  }

  @Test
  fun zeroSafeInsetsUseScreenFloors() {
    val padding = screenContentPadding(PaddingValues(0.dp), LayoutDirection.Ltr)

    assertEquals(20.dp, padding.calculateLeftPadding(LayoutDirection.Ltr))
    assertEquals(24.dp, padding.calculateTopPadding())
    assertEquals(20.dp, padding.calculateRightPadding(LayoutDirection.Ltr))
    assertEquals(16.dp, padding.calculateBottomPadding())
  }

  @Test
  fun safeInsetsAreAddedHorizontallyAndMaximizedVertically() {
    val safeInsets = PaddingValues(start = 8.dp, top = 40.dp, end = 10.dp, bottom = 32.dp)

    val padding = screenContentPadding(safeInsets, LayoutDirection.Ltr)

    assertEquals(28.dp, padding.calculateLeftPadding(LayoutDirection.Ltr))
    assertEquals(40.dp, padding.calculateTopPadding())
    assertEquals(30.dp, padding.calculateRightPadding(LayoutDirection.Ltr))
    assertEquals(32.dp, padding.calculateBottomPadding())
  }

  @Test
  fun horizontalSafeInsetsFollowRightToLeftLayoutDirection() {
    val safeInsets = PaddingValues(start = 8.dp, end = 10.dp)

    val padding = screenContentPadding(safeInsets, LayoutDirection.Rtl)

    assertEquals(28.dp, padding.calculateRightPadding(LayoutDirection.Rtl))
    assertEquals(30.dp, padding.calculateLeftPadding(LayoutDirection.Rtl))
  }
}
