package com.kurobello.tallybook.core.ui.component

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TallybookViewportOverlayStateTest {

  @Test
  fun hidingADifferentOwnerKeepsTheActiveScrimVisible() {
    val state = TallybookViewportOverlayState()
    val activeOwner = Any()

    state.showScrim(activeOwner)
    state.hideScrim(Any())

    assertTrue(state.isScrimVisible)
    state.hideScrim(activeOwner)
    assertFalse(state.isScrimVisible)
  }
}
