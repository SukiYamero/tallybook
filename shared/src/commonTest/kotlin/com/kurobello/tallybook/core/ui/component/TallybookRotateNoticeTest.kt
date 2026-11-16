package com.kurobello.tallybook.core.ui.component

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class TallybookRotateNoticeTest {
  private val phoneMaximum = 600.dp

  private fun suggests(width: Int, height: Int) =
      shouldSuggestPortrait(width.dp, height.dp, phoneMaximum)

  @Test
  fun portraitPhoneIsNotNudged() {
    assertEquals(false, suggests(width = 393, height = 852))
  }

  @Test
  fun landscapePhoneIsNudged() {
    assertEquals(true, suggests(width = 852, height = 393))
  }

  @Test
  fun landscapeTabletIsNotNudged() {
    assertEquals(false, suggests(width = 1280, height = 800))
  }

  @Test
  fun shortestSideAtTheMaximumCountsAsTablet() {
    assertEquals(true, suggests(width = 900, height = 599))
    assertEquals(false, suggests(width = 900, height = 600))
  }

  @Test
  fun squareWindowIsNotLandscape() {
    assertEquals(false, suggests(width = 500, height = 500))
  }
}
