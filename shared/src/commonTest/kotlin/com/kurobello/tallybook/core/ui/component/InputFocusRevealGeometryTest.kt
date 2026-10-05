package com.kurobello.tallybook.core.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals

class InputFocusRevealGeometryTest {

  @Test
  fun centersFocusedInputWithinTheVisibleViewport() {
    assertEquals(
        360,
        centeredScrollTarget(
            currentScroll = 120,
            maxScroll = 1_000,
            inputCenter = 840f,
            visibleViewportCenter = 600f,
        ),
    )
  }

  @Test
  fun doesNotMoveWhenTheContainerCannotScroll() {
    assertEquals(
        0,
        centeredScrollTarget(
            currentScroll = 0,
            maxScroll = 0,
            inputCenter = 840f,
            visibleViewportCenter = 600f,
        ),
    )
  }

  @Test
  fun clampsTheTargetToTheAvailableScrollRange() {
    assertEquals(
        800,
        centeredScrollTarget(
            currentScroll = 700,
            maxScroll = 800,
            inputCenter = 1_100f,
            visibleViewportCenter = 600f,
        ),
    )
  }

  @Test
  fun usesTheKeyboardBoundaryWhenTheViewportExtendsBehindTheKeyboard() {
    assertEquals(
        1_800f,
        visibleViewportBottom(
            viewportBottom = 2_400f,
            rootBottom = 2_400f,
            imeBottom = 600,
        ),
    )
  }

  @Test
  fun keepsTheViewportBoundaryWhenTheSheetIsAlreadyAboveTheKeyboard() {
    assertEquals(
        1_200f,
        visibleViewportBottom(
            viewportBottom = 1_200f,
            rootBottom = 2_400f,
            imeBottom = 600,
        ),
    )
  }
}
