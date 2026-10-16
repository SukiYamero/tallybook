package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
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

  private companion object {
    const val TITLE = "Dialog title"
    const val BODY = "Dialog body"
  }
}
