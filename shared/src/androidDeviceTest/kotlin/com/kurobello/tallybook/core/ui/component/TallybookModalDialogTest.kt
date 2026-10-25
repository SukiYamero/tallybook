package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class TallybookModalDialogTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun contentIsShownOnlyWhileVisible() {
    composeRule.setContent {
      TallybookTheme {
        TallybookModalDialog(visible = false, paneTitle = TITLE, onDismissRequest = {}) {
          TallybookText(BODY)
        }
      }
    }

    composeRule.onNodeWithText(BODY).assertDoesNotExist()
  }

  @Test
  fun dialogIsCenteredVerticallyWhenItOpens() {
    composeRule.setContent {
      TallybookTheme {
        TallybookModalDialog(visible = true, paneTitle = TITLE, onDismissRequest = {}) {
          TallybookText(BODY, modifier = Modifier.fillMaxWidth())
        }
      }
    }

    // The dialog is its own full-screen window, so its root is the screen it centers in.
    val screen =
        composeRule
            .onNode(isRoot() and hasAnyDescendant(hasText(BODY)))
            .fetchSemanticsNode()
            .boundsInRoot
    val body = composeRule.onNodeWithText(BODY).fetchSemanticsNode().boundsInRoot
    val offset = abs(body.center.y - screen.center.y)
    assertTrue(offset < screen.height * CENTER_TOLERANCE, "Body is $offset off the center")
  }

  @Test
  fun growingContentKeepsTheTopEdgeAndExtendsDownward() {
    var extra by mutableStateOf(false)
    composeRule.setContent {
      TallybookTheme {
        TallybookModalDialog(visible = true, paneTitle = TITLE, onDismissRequest = {}) {
          TallybookText(BODY, modifier = Modifier.fillMaxWidth())
          if (extra) Spacer(Modifier.fillMaxWidth().height(GROWTH))
          TallybookText(FOOTER, modifier = Modifier.fillMaxWidth())
        }
      }
    }
    val bodyTop = composeRule.onNodeWithText(BODY).fetchSemanticsNode().boundsInRoot.top
    val footerTop = composeRule.onNodeWithText(FOOTER).fetchSemanticsNode().boundsInRoot.top

    composeRule.runOnIdle { extra = true }
    composeRule.waitForIdle()

    val grownBodyTop = composeRule.onNodeWithText(BODY).fetchSemanticsNode().boundsInRoot.top
    val grownFooterTop = composeRule.onNodeWithText(FOOTER).fetchSemanticsNode().boundsInRoot.top
    assertEquals(bodyTop, grownBodyTop)
    assertTrue(grownFooterTop > footerTop, "Footer moved from $footerTop to $grownFooterTop")
  }

  @Test
  fun hiddenIsReportedOnceAfterTheDialogLeaves() {
    var visible by mutableStateOf(true)
    var hidden = 0
    composeRule.setContent {
      TallybookTheme {
        TallybookModalDialog(
            visible = visible,
            paneTitle = TITLE,
            onDismissRequest = {},
            onHidden = { hidden += 1 },
        ) {
          TallybookText(BODY)
        }
      }
    }
    composeRule.onNodeWithText(BODY).assertExists()
    composeRule.runOnIdle { assertEquals(0, hidden) }

    composeRule.runOnIdle { visible = false }
    composeRule.waitUntil(5_000) { hidden == 1 }
    composeRule.waitForIdle()

    composeRule.runOnIdle { assertEquals(1, hidden) }
  }

  private companion object {
    const val TITLE = "Dialog title"
    const val BODY = "Dialog body"
    const val FOOTER = "Dialog footer"
    const val CENTER_TOLERANCE = 0.1f
    val GROWTH = 120.dp
  }
}
