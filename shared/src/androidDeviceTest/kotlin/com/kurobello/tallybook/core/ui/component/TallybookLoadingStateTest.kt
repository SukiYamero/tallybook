package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class TallybookLoadingStateTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun loadingIndicatorIsCenteredInsideItsOwnedRegion() {
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      TallybookTheme {
        Box(modifier = Modifier.size(width = 320.dp, height = 240.dp).testTag("loading_region")) {
          TallybookLoadingState(
              contentDescription = "Loading catalog",
              modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }

    val region = composeRule.onNodeWithTag("loading_region").fetchSemanticsNode().boundsInRoot
    val indicator =
        composeRule
            .onNodeWithContentDescription("Loading catalog")
            .fetchSemanticsNode()
            .boundsInRoot

    assertEquals(region.center.x, indicator.center.x, absoluteTolerance = 1f)
    assertEquals(region.center.y, indicator.center.y, absoluteTolerance = 1f)
    composeRule
        .onNodeWithContentDescription("Loading catalog")
        .assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo.Indeterminate,
            )
        )
  }

  @Test
  fun loadingStateCentersIndicatorAndKeepsSupportingTextInsideRegionAtDoubleFontScale() {
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      val density = LocalDensity.current
      CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
        TallybookTheme {
          Box(modifier = Modifier.size(width = 320.dp, height = 240.dp).testTag("loading_region")) {
            TallybookLoadingState(
                contentDescription = "Loading movements",
                modifier = Modifier.fillMaxSize(),
                supportingText = "Loading your movements",
            )
          }
        }
      }
    }

    val region = composeRule.onNodeWithTag("loading_region").fetchSemanticsNode().boundsInRoot
    val indicator =
        composeRule
            .onNodeWithContentDescription("Loading movements")
            .fetchSemanticsNode()
            .boundsInRoot
    val supportingText =
        composeRule.onNodeWithText("Loading your movements").fetchSemanticsNode().boundsInRoot

    composeRule.onNodeWithContentDescription("Loading movements").assertIsDisplayed()
    composeRule.onNodeWithText("Loading your movements").assertIsDisplayed()
    assertEquals(region.center.x, indicator.center.x, absoluteTolerance = 1f)
    assertEquals(region.center.y, indicator.center.y, absoluteTolerance = 1f)
    assertTrue(supportingText.left >= region.left)
    assertTrue(supportingText.right <= region.right)
    assertTrue(supportingText.top >= region.top)
    assertTrue(supportingText.bottom <= region.bottom)
  }
}
