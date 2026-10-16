package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_picker_done
import tallybook.shared.generated.resources.category_picker_keep_one
import tallybook.shared.generated.resources.category_picker_limit_reached
import tallybook.shared.generated.resources.category_picker_selected_label
import tallybook.shared.generated.resources.category_tag_remove

@OptIn(ExperimentalResourceApi::class)
class CategoryMultiPickerTest {
  @get:Rule val composeRule = createComposeRule()

  private var selection by mutableStateOf(listOf<String>())
  private var dismissals = 0

  @Before
  fun setUp() {
    setResourceReaderAndroidContext(InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun tappingRowsTogglesThemAndDoneReportsTheCount() {
    show(initial = listOf(leisure.id))

    composeRule.onCategoryOption(health.nombre).performClick()
    composeRule.runOnIdle { assertEquals(listOf(leisure.id, health.id), selection) }
    composeRule.onCategoryOption(health.nombre).assertIsSelected()
    composeRule.onNodeWithText(string(Res.string.category_picker_done, 2)).assertExists()

    composeRule.onCategoryOption(health.nombre).performClick()
    composeRule.runOnIdle { assertEquals(listOf(leisure.id), selection) }
  }

  @Test
  fun lastSelectedCategoryIsDisabledAndTheReasonIsShown() {
    show(initial = listOf(leisure.id))

    composeRule.onCategoryOption(leisure.nombre).assertIsNotEnabled().assertIsSelected()
    composeRule.onNodeWithText(string(Res.string.category_picker_keep_one)).assertExists()

    composeRule.onCategoryOption(leisure.nombre).performClick()

    composeRule.runOnIdle { assertEquals(listOf(leisure.id), selection) }
  }

  @Test
  fun selectedRowsAreEnabledAgainOnceAnotherCategoryIsSelected() {
    show(initial = listOf(leisure.id, health.id))

    composeRule.onCategoryOption(leisure.nombre).assertIsEnabled()
    composeRule.onNodeWithText(string(Res.string.category_picker_keep_one)).assertDoesNotExist()
  }

  @Test
  fun parentAndChildReplaceEachOtherAndTheTagsFollowImmediately() {
    show(initial = listOf(health.id))

    composeRule.onCategoryOption(groceries.nombre).performClick()
    composeRule.runOnIdle { assertEquals(listOf(health.id, groceries.id), selection) }
    composeRule.onCategoryOption(groceries.nombre).assertIsSelected()

    composeRule.onCategoryOption(food.nombre).performClick()
    composeRule.runOnIdle { assertEquals(listOf(health.id, food.id), selection) }
    composeRule.onCategoryOption(food.nombre).assertIsSelected()
    composeRule.onCategoryOption(groceries.nombre).assertIsNotSelected()
    composeRule.onNodeWithContentDescription(removeLabel(groceries)).assertDoesNotExist()

    composeRule.onCategoryOption(groceries.nombre).performClick()
    composeRule.runOnIdle { assertEquals(listOf(health.id, groceries.id), selection) }
    composeRule.onCategoryOption(food.nombre).assertIsNotSelected()
  }

  @Test
  fun selectedSectionListsTheSelectionAndTappingATagRemovesIt() {
    show(initial = listOf(leisure.id, health.id))

    composeRule.onNodeWithTag(CATEGORY_PICKER_SELECTED_TAG).assertExists()
    composeRule.onNodeWithText(string(Res.string.category_picker_selected_label)).assertExists()

    composeRule.onNodeWithContentDescription(removeLabel(health)).performClick()

    composeRule.runOnIdle { assertEquals(listOf(leisure.id), selection) }
    composeRule.onNodeWithContentDescription(removeLabel(health)).assertDoesNotExist()
    composeRule.onCategoryOption(health.nombre).assertIsNotSelected()
  }

  @Test
  fun selectedSectionIsHiddenWhileNothingIsSelected() {
    show(initial = emptyList())

    composeRule.onNodeWithTag(CATEGORY_PICKER_SELECTED_TAG).assertDoesNotExist()

    composeRule.onCategoryOption(health.nombre).performClick()

    composeRule.onNodeWithTag(CATEGORY_PICKER_SELECTED_TAG).assertExists()
  }

  @Test
  fun theLastSelectedTagCannotBeRemoved() {
    show(initial = listOf(leisure.id))

    composeRule.onNodeWithContentDescription(removeLabel(leisure)).assertIsNotEnabled()
    composeRule.onNodeWithContentDescription(removeLabel(leisure)).performClick()

    composeRule.runOnIdle { assertEquals(listOf(leisure.id), selection) }
  }

  @Test
  fun doneClosesTheSheet() {
    show(initial = listOf(leisure.id))

    composeRule.onNodeWithTag(CATEGORY_PICKER_DONE_TAG).performClick()

    composeRule.runOnIdle { assertEquals(1, dismissals) }
  }

  @Test
  fun atTheLimitOtherRowsAreDisabledAndTheLimitIsExplained() {
    show(initial = listOf(leisure.id, health.id), max = 2)

    composeRule.onCategoryOption(transport.nombre).assertIsNotEnabled()
    composeRule.onNodeWithText(string(Res.string.category_picker_limit_reached, 2)).assertExists()
  }

  private fun show(initial: List<String>, max: Int = 6) {
    selection = initial
    composeRule.setContent {
      TallybookTheme {
        CategoryPickerSheet(
            visible = true,
            categorias = categories,
            selectedIds = selection,
            onSelectionChange = { selection = it },
            onDismiss = { dismissals++ },
            max = max,
        )
      }
    }
  }

  private fun string(resource: org.jetbrains.compose.resources.StringResource, vararg args: Any) =
      runBlocking {
        getString(resource, *args)
      }

  // A selected category also appears as a removable tag, so a chip is told apart by being
  // selectable.
  private fun SemanticsNodeInteractionsProvider.onCategoryOption(text: String) =
      onNode(hasText(text) and hasClickAction() and isSelectable())

  private fun removeLabel(categoria: Categoria) =
      string(Res.string.category_tag_remove, categoria.nombre)

  private companion object {
    val food = categoria("food", "Food")
    val groceries = categoria("groceries", "Groceries", padreId = food.id)
    val leisure = categoria("leisure", "Leisure")
    val health = categoria("health", "Health")
    val transport = categoria("transport", "Transport")
    val categories = listOf(food, groceries, leisure, health, transport)

    fun categoria(id: String, nombre: String, padreId: String? = null) =
        Categoria(id, nombre, padreId, CategoryIcon.UTENSILS, CategoryTint.AMBER)
  }
}
