package com.kurobello.tallybook.core.ui.category

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_tag_add_description
import tallybook.shared.generated.resources.category_tag_field_counter_description
import tallybook.shared.generated.resources.category_tag_field_suggestions
import tallybook.shared.generated.resources.category_tag_remove

@OptIn(ExperimentalResourceApi::class)
class CategoryTagTest {
  @get:Rule val composeRule = createComposeRule()

  private val cinema = Categoria("cinema", "Cinema", null, CategoryIcon.FILM, CategoryTint.PURPLE)
  private val food = Categoria("food", "Food", null, CategoryIcon.UTENSILS, CategoryTint.AMBER)
  private val transport =
      Categoria("transport", "Transport", null, CategoryIcon.CAR, CategoryTint.BLUE)

  @Before
  fun setUp() {
    setResourceReaderAndroidContext(InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun selectedTagIsSelectedContentWithoutAClickAction() {
    composeRule.setContent { TallybookTheme { CategoryTag(cinema) } }

    composeRule.onNodeWithText("Cinema").assertIsSelected().assertHasNoClickAction()
  }

  @Test
  fun removeActionIsAButtonLabelledWithTheCategoryName() {
    var removals = 0
    composeRule.setContent { TallybookTheme { CategoryTag(cinema, onRemove = { removals++ }) } }

    composeRule
        .onNodeWithContentDescription(string(Res.string.category_tag_remove, "Cinema"))
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        .performClick()
    composeRule.runOnIdle { assertEquals(1, removals) }
  }

  @Test
  fun disabledTagRejectsRemoval() {
    var removals = 0
    composeRule.setContent {
      TallybookTheme { CategoryTag(cinema, enabled = false, onRemove = { removals++ }) }
    }

    composeRule
        .onNodeWithContentDescription(string(Res.string.category_tag_remove, "Cinema"))
        .assertIsNotEnabled()
    composeRule.runOnIdle { assertEquals(0, removals) }
  }

  @Test
  fun suggestionTagIsAPlainButtonThatReportsClicks() {
    var clicks = 0
    composeRule.setContent {
      TallybookTheme { CategorySuggestionTag(cinema, onClick = { clicks++ }) }
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
  fun disabledSuggestionTagRejectsClicks() {
    var clicks = 0
    composeRule.setContent {
      TallybookTheme { CategorySuggestionTag(cinema, onClick = { clicks++ }, enabled = false) }
    }

    composeRule.onNodeWithText("Cinema").assertIsNotEnabled()
    composeRule.runOnIdle { assertEquals(0, clicks) }
  }

  @Test
  fun fieldAnnouncesTheCounterAndOffersAddUntilTheLimit() {
    var adds = 0
    composeRule.setContent {
      TallybookTheme {
        CategoryTagField(
            categorias = listOf(cinema, food),
            max = LIMIT,
            onRemove = {},
            onAdd = { adds++ },
        )
      }
    }

    composeRule.onNodeWithText("2/$LIMIT").assertExists()
    composeRule
        .onNodeWithContentDescription(
            string(Res.string.category_tag_field_counter_description, 2, LIMIT)
        )
        .assertExists()
    composeRule
        .onNodeWithContentDescription(string(Res.string.category_tag_add_description))
        .performClick()
    composeRule.runOnIdle { assertEquals(1, adds) }
  }

  @Test
  fun fieldHidesAddAndSuggestionsAtTheLimit() {
    composeRule.setContent {
      TallybookTheme {
        CategoryTagField(
            categorias = listOf(cinema, food),
            max = 2,
            onRemove = {},
            onAdd = {},
            suggestions = listOf(transport),
        )
      }
    }

    composeRule
        .onAllNodesWithContentDescription(string(Res.string.category_tag_add_description))
        .assertCountEquals(0)
    composeRule
        .onNodeWithText(string(Res.string.category_tag_field_suggestions))
        .assertDoesNotExist()
    composeRule.onNodeWithText("Transport").assertDoesNotExist()
  }

  @Test
  fun fieldRemovesEveryTagExceptTheLastOne() {
    var removed: Categoria? = null
    composeRule.setContent {
      TallybookTheme {
        CategoryTagField(
            categorias = listOf(cinema, food),
            max = LIMIT,
            onRemove = { removed = it },
            onAdd = {},
        )
      }
    }

    composeRule
        .onNodeWithContentDescription(string(Res.string.category_tag_remove, "Food"))
        .performClick()
    composeRule.runOnIdle { assertEquals(food, removed) }
  }

  @Test
  fun fieldWithASingleCategoryOffersNoRemoval() {
    composeRule.setContent {
      TallybookTheme {
        CategoryTagField(categorias = listOf(cinema), max = LIMIT, onRemove = {}, onAdd = {})
      }
    }

    composeRule
        .onAllNodesWithContentDescription(string(Res.string.category_tag_remove, "Cinema"))
        .assertCountEquals(0)
  }

  @Test
  fun fieldOffersOnlySuggestionsThatAreNotSelected() {
    var chosen: Categoria? = null
    composeRule.setContent {
      TallybookTheme {
        CategoryTagField(
            categorias = listOf(cinema),
            max = LIMIT,
            onRemove = {},
            onAdd = {},
            suggestions = listOf(cinema, food),
            onSuggestion = { chosen = it },
        )
      }
    }

    composeRule.onNodeWithText(string(Res.string.category_tag_field_suggestions)).assertExists()
    composeRule.onNodeWithText("Food").performClick()
    composeRule.runOnIdle { assertEquals(food, chosen) }
  }

  private fun string(resource: StringResource, vararg args: Any): String = runBlocking {
    getString(resource, *args)
  }

  private companion object {
    const val LIMIT = 6
  }
}
