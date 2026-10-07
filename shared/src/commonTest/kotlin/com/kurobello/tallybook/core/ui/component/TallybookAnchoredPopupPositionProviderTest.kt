package com.kurobello.tallybook.core.ui.component

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

class TallybookAnchoredPopupPositionProviderTest {

  @Test
  fun compactCalendarsFitWithinBothEdgesAndSafeInsets() {
    listOf(320, 360).forEach { windowWidth ->
      val popupWidth = windowWidth - 32
      val provider = TallybookAnchoredPopupPositionProvider(verticalOffset = 8, edgeMargin = 16)
      listOf(0, windowWidth - 40).forEach { anchorLeft ->
        val position =
            provider.calculatePosition(
                IntRect(anchorLeft, 100, anchorLeft + 40, 148),
                IntSize(windowWidth, 800),
                LayoutDirection.Ltr,
                IntSize(popupWidth, 300),
            )
        assertEquals(16, position.x)
        assertEquals(windowWidth - 16, position.x + popupWidth)
      }
    }
    val provider =
        TallybookAnchoredPopupPositionProvider(
            verticalOffset = 8,
            edgeMargin = 16,
            leftSafeInset = 24,
            rightSafeInset = 8,
        )
    val position =
        provider.calculatePosition(
            IntRect(0, 100, 40, 148),
            IntSize(360, 800),
            LayoutDirection.Rtl,
            IntSize(296, 300),
        )
    assertEquals(40, position.x)
    assertEquals(336, position.x + 296)
  }

  @Test
  fun positionsTheCalendarBelowWhenTheViewportHasEnoughTrailingSpace() {
    val provider = TallybookAnchoredPopupPositionProvider(verticalOffset = 8, edgeMargin = 16)

    assertEquals(
        IntOffset(100, 140),
        provider.calculatePosition(
            anchorBounds = IntRect(left = 150, top = 100, right = 250, bottom = 132),
            windowSize = IntSize(width = 400, height = 800),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(width = 200, height = 300),
        ),
    )
  }

  @Test
  fun flipsTheCalendarAboveWhenTheTrailingViewportSpaceIsInsufficient() {
    val provider = TallybookAnchoredPopupPositionProvider(verticalOffset = 8, edgeMargin = 16)

    assertEquals(
        IntOffset(100, 292),
        provider.calculatePosition(
            anchorBounds = IntRect(left = 150, top = 600, right = 250, bottom = 632),
            windowSize = IntSize(width = 400, height = 800),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(width = 200, height = 300),
        ),
    )
  }

  @Test
  fun yearSelectorKeepsTheHistoricalBoundaryAndGrowsWithTheMaximumDate() {
    assertEquals(
        2019..2027,
        selectableYears(
            selectedDate = LocalDate(2027, 1, 1),
            maxDate = LocalDate(2027, 1, 1),
        ),
    )
  }

  @Test
  fun yearSelectorKeepsAnExistingLegacySelectedDateReachable() {
    assertEquals(
        2016..2027,
        selectableYears(
            selectedDate = LocalDate(2016, 8, 15),
            maxDate = LocalDate(2027, 1, 1),
        ),
    )
  }

  @Test
  fun unlimitedYearSelectorPreservesItsOneYearForwardWindow() {
    assertEquals(
        2019..2027,
        selectableYears(selectedDate = LocalDate(2026, 10, 6), maxDate = null),
    )
  }
}
