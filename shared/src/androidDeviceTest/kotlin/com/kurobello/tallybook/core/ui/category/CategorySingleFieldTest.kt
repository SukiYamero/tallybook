package com.kurobello.tallybook.core.ui.category

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
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
import tallybook.shared.generated.resources.category_picker_single_title
import tallybook.shared.generated.resources.category_suggestion_replaces
import tallybook.shared.generated.resources.category_tag_remove
import tallybook.shared.generated.resources.movimiento_category_section
import tallybook.shared.generated.resources.movimiento_pending_label

@OptIn(ExperimentalResourceApi::class)
class CategorySingleFieldTest {
  @get:Rule val composeRule = createComposeRule()

  private val chosen = mutableListOf<Categoria>()
  private var deselections = 0
  private var opens = 0

  @Before
  fun setUp() {
    setResourceReaderAndroidContext(InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun selectingASuggestionWhilePendingEmitsIt() {
    show(categoria = otros, pending = true, deselectable = true)

    composeRule.onNodeWithText(string(Res.string.movimiento_pending_label)).assertExists()
    composeRule
        .onNodeWithContentDescription(string(Res.string.category_tag_remove, otros.nombre))
        .assertDoesNotExist()
    composeRule
        .onNodeWithText(food.nombre)
        .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
        .performClick()

    composeRule.runOnIdle { assertEquals(listOf(food), chosen) }
  }

  @Test
  fun withASelectionOtherSuggestionsAreDimmedButStillSwitchTheCategory() {
    show(categoria = cinema, deselectable = false)

    composeRule.onNodeWithText(string(Res.string.movimiento_category_section)).assert(isHeading())
    composeRule.onNodeWithText(cinema.nombre).assertIsSelected()
    composeRule
        .onNodeWithText(food.nombre)
        .assertHasClickAction()
        .assertIsEnabled()
        .assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                string(Res.string.category_suggestion_replaces, cinema.nombre),
            )
        )
        .performClick()

    composeRule.runOnIdle { assertEquals(listOf(food), chosen) }
  }

  @Test
  fun theSelectedCategoryIsNotRepeatedAmongTheSuggestions() {
    show(categoria = cinema, deselectable = false, suggestions = listOf(cinema, food))

    composeRule.onNodeWithText(cinema.nombre).assertIsSelected()
    composeRule.onNodeWithText(food.nombre).assertExists()
  }

  @Test
  fun aDeselectableFieldClearsTheSelectedCategory() {
    show(categoria = cinema, deselectable = true)

    composeRule
        .onNodeWithContentDescription(string(Res.string.category_tag_remove, cinema.nombre))
        .performClick()

    composeRule.runOnIdle { assertEquals(1, deselections) }
  }

  @Test
  fun aFieldThatIsNotDeselectableOffersNoDeselection() {
    show(categoria = cinema, deselectable = false)

    composeRule
        .onNodeWithContentDescription(string(Res.string.category_tag_remove, cinema.nombre))
        .assertDoesNotExist()
  }

  @Test
  fun chooseOpensThePicker() {
    show(categoria = cinema, deselectable = false)

    composeRule
        .onNodeWithContentDescription(string(Res.string.category_picker_single_title))
        .performClick()

    composeRule.runOnIdle { assertEquals(1, opens) }
  }

  private fun show(
      categoria: Categoria,
      deselectable: Boolean,
      pending: Boolean = false,
      suggestions: List<Categoria> = listOf(food),
  ) {
    composeRule.setContent {
      TallybookTheme {
        CategorySingleField(
            categoria = categoria,
            deselectable = deselectable,
            onChoose = { opens++ },
            onSuggestion = { chosen += it },
            onDeselect = { deselections++ },
            pending = pending,
            suggestions = suggestions,
        )
      }
    }
  }

  private fun string(resource: StringResource, vararg args: Any): String = runBlocking {
    getString(resource, *args)
  }

  private companion object {
    val cinema = Categoria("cinema", "Cinema", null, CategoryIcon.FILM, CategoryTint.PURPLE)
    val food = Categoria("food", "Food", null, CategoryIcon.UTENSILS, CategoryTint.AMBER)
    val otros =
        Categoria(
            CATEGORIA_OTROS_ID,
            "Otros",
            null,
            CategoryIcon.CIRCLE_DASHED,
            CategoryTint.NEUTRAL,
        )
  }
}
