package com.kurobello.tallybook.core.ui.category

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class CategorySuggestionChipTest {
  @get:Rule val composeRule = createComposeRule()

  private val cinema = Categoria("cinema", "Cinema", null, CategoryIcon.FILM, CategoryTint.PURPLE)

  @Test
  fun chipIsAPlainButtonThatReportsClicks() {
    var clicks = 0
    composeRule.setContent {
      TallybookTheme { CategorySuggestionChip(cinema, onClick = { clicks++ }) }
    }

    composeRule
        .onNodeWithText("Cinema")
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
        .assertIsEnabled()
        .performClick()
    composeRule.runOnIdle { assertEquals(1, clicks) }
  }

  @Test
  fun disabledChipRejectsClicks() {
    var clicks = 0
    composeRule.setContent {
      TallybookTheme { CategorySuggestionChip(cinema, onClick = { clicks++ }, enabled = false) }
    }

    composeRule.onNodeWithText("Cinema").assertIsNotEnabled()
    composeRule.runOnIdle { assertEquals(0, clicks) }
  }
}
