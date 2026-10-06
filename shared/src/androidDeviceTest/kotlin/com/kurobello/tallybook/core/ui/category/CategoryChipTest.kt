package com.kurobello.tallybook.core.ui.category

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class CategoryChipTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun disabledSelectedChipKeepsSelectionAndRejectsClicks() {
    var clicks = 0
    composeRule.setContent {
      TallybookTheme {
        CategoryChip(
            Categoria("food", "Food", null, CategoryIcon.UTENSILS, CategoryTint.AMBER),
            true,
            { clicks++ },
            enabled = false,
        )
      }
    }
    composeRule.onNodeWithText("Food").assertIsSelected().assertIsNotEnabled()
    composeRule.runOnIdle { assertEquals(0, clicks) }
  }
}
