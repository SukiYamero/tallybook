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
