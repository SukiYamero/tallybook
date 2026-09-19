package com.kurobello.tallybook.core.ui.component

import kotlin.test.Test
import kotlin.test.assertTrue

class TallybookModalBottomSheetLayoutTest {

  @Test
  fun fullHeightLayoutConstrainsTheSheetToTheAvailableHeight() {
    assertTrue(ModalBottomSheetLayout(fillAvailableHeight = true).sheetFillsAvailableHeight)
  }
}
