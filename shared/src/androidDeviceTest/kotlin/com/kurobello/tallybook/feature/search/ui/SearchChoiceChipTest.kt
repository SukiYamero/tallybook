package com.kurobello.tallybook.feature.search.ui

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchChoiceChipTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun compactPresetCentersItsLabelWithinAFullSizeSelectedTapTarget() {
    var clicks = 0
    composeRule.setContent {
      TallybookTheme { SearchChoiceChip("Last 7 days", true, { clicks++ }) }
    }
    val target = composeRule.onNodeWithText("Last 7 days").assertIsSelected()
    val targetBounds = target.getUnclippedBoundsInRoot()
    val textBounds =
        composeRule.onNodeWithText("Last 7 days", useUnmergedTree = true).getUnclippedBoundsInRoot()
    assertTrue(targetBounds.bottom - targetBounds.top >= 48.dp)
    val targetCenter = (targetBounds.top + targetBounds.bottom) / 2
    val textCenter = (textBounds.top + textBounds.bottom) / 2
    assertTrue(abs((targetCenter - textCenter).value) <= 1f)
    target.performClick()
    composeRule.runOnIdle { assertEquals(1, clicks) }
  }
}
