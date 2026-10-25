package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.input.ImeAction
import com.kurobello.tallybook.core.ui.testing.awaitSoftKeyboard
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.junit.Rule
import org.junit.Test

class TallybookSheetFormTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun closingTheKeyboardKeepsTheEditedFieldOfATallBodyInView() {
    composeRule.setContent {
      TallybookTheme {
        TallybookModalBottomSheet(visible = true, paneTitle = "Form", onDismiss = {}) {
          TallybookSheetForm(
              header = { TallybookSheetTitle("Form") },
              footer = { TallybookButton(onClick = {}) { TallybookText("Save") } },
          ) {
            Spacer(Modifier.fillMaxWidth().height(TallybookTheme.dimensions.sheetFormMaximumHeight))
            BasicTextField(
                value = LAST_FIELD,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().revealWhenFocused(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
          }
        }
      }
    }

    val field = composeRule.onNodeWithText(LAST_FIELD)
    field.performScrollTo().performClick()
    composeRule.awaitSoftKeyboard(visible = true)
    field.performImeAction()
    composeRule.awaitSoftKeyboard(visible = false)

    field.assertIsDisplayed()
    composeRule.onNodeWithText("Save").assertIsDisplayed()
  }

  private companion object {
    const val LAST_FIELD = "Last field"
  }
}
