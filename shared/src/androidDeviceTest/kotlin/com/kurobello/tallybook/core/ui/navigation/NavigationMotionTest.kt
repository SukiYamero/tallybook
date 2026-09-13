package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import kotlin.test.assertTrue
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test

class NavigationMotionTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun incomingDestinationWaitsUntilTheOutgoingDestinationHasFaded() {
    lateinit var backStack: NavBackStack<TestDestination>
    composeRule.setContent {
      backStack = remember { NavBackStack(First) }
      Box(Modifier.fillMaxSize().background(Color.Black)) {
        NavDisplay(
            modifier = Modifier.fillMaxSize(),
            backStack = backStack,
            transitionSpec = { navigationContentTransform() },
            entryProvider =
                entryProvider {
                  entry<First> { Destination(Color.Red) }
                  entry<Second> { Destination(Color.Blue) }
                },
        )
      }
    }
    composeRule.waitForIdle()
    composeRule.mainClock.autoAdvance = false

    composeRule.runOnIdle { backStack.add(Second) }
    composeRule.mainClock.advanceTimeBy(80)
    composeRule.waitForIdle()

    val image = composeRule.onRoot().captureToImage().toPixelMap()
    val centerPixel = image[image.width / 2, image.height / 2]
    assertTrue(centerPixel.blue < 0.01f, "Incoming blue content overlaps the outgoing screen")
  }
}

@Composable
private fun Destination(color: Color) {
  Box(Modifier.fillMaxSize().background(color))
}

@Serializable private sealed interface TestDestination : NavKey

@Serializable private data object First : TestDestination

@Serializable private data object Second : TestDestination
