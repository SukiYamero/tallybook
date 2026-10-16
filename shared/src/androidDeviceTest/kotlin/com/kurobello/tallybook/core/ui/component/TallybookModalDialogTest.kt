package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
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
  fun dialogIsAnchoredInTheUpperHalfOfTheScreen() {
    composeRule.setContent {
      TallybookTheme {
        TallybookModalDialog(visible = true, paneTitle = TITLE, onDismissRequest = {}) {
          TallybookText(BODY, modifier = Modifier.fillMaxWidth())
        }
      }
    }

    val screenHeight = composeRule.onRoot().fetchSemanticsNode().boundsInRoot.height
    val bodyTop = composeRule.onNodeWithText(BODY).fetchSemanticsNode().boundsInRoot.top
    assertTrue(bodyTop < screenHeight / 2, "Dialog body starts at $bodyTop of $screenHeight")
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
  }
}
