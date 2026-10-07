package com.kurobello.tallybook.feature.home

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.home_search_placeholder

class HomeSearchFeedbackTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun pressUsesOwnFeedbackWithoutInheritedIndicationAndReleaseClicksOnce() {
    val indication = RecordingHomeSearchIndication()
    val label = runBlocking { getString(Res.string.home_search_placeholder) }
    var clicks = 0
    composeRule.setContent {
      TallybookTheme {
        CompositionLocalProvider(LocalIndication provides indication) {
          HomeSearch(onClick = { clicks++ })
        }
      }
    }
    val search = composeRule.onNodeWithText(label)
    search.performTouchInput { down(center) }
    composeRule.mainClock.advanceTimeByFrame()
    composeRule.runOnIdle {
      assertEquals(0, indication.createdNodes)
      assertEquals(0, clicks)
    }
    search.performTouchInput { up() }
    composeRule.runOnIdle {
      assertEquals(0, indication.createdNodes)
      assertEquals(1, clicks)
    }
  }

  @Test
  fun presentationalSearchHasNoClickAction() {
    val label = runBlocking { getString(Res.string.home_search_placeholder) }
    composeRule.setContent { TallybookTheme { HomeSearch() } }
    composeRule.onNodeWithText(label).assertHasNoClickAction()
  }
}

private class RecordingHomeSearchIndication : IndicationNodeFactory {
  var createdNodes = 0
    private set

  override fun create(interactionSource: InteractionSource): DelegatableNode {
    createdNodes++
    return object : Modifier.Node(), DrawModifierNode {
      override fun ContentDrawScope.draw() = drawContent()
    }
  }

  override fun equals(other: Any?): Boolean = this === other

  override fun hashCode(): Int = super.hashCode()
}
