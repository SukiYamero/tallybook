package com.kurobello.tallybook.core.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals

class LastWhileVisibleTest {

  @Test
  fun followsTheCurrentValueWhileVisible() {
    val holder = LastWhileVisible("first")

    assertEquals("second", holder.resolve(visible = true, current = "second"))
    assertEquals("third", holder.resolve(visible = true, current = "third"))
  }

  @Test
  fun keepsTheLastVisibleValueOnceHidden() {
    val holder = LastWhileVisible("first")
    holder.resolve(visible = true, current = "second")

    assertEquals("second", holder.resolve(visible = false, current = "reset"))
    assertEquals("second", holder.resolve(visible = false, current = "reset again"))
  }

  @Test
  fun startsFromTheInitialValueUntilItIsFirstVisible() {
    val holder = LastWhileVisible("initial")

    assertEquals("initial", holder.resolve(visible = false, current = "hidden"))
    assertEquals("shown", holder.resolve(visible = true, current = "shown"))
  }
}
