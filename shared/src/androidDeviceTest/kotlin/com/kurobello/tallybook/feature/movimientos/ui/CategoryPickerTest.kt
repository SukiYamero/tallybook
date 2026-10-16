package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_picker_current
import tallybook.shared.generated.resources.category_picker_single_title
import tallybook.shared.generated.resources.no_categories_found

class CategoryPickerTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun searchShowsFlatResultsAndSelectsAChild() {
    val selections = mutableListOf<String>()

    composeRule.setContent {
      TallybookTheme {
        CategoryPickerSheet(
            visible = true,
            categorias = categories,
            selectedId = null,
            onSelect = { selections += it },
            onDismiss = {},
        )
      }
    }

    composeRule.onNode(hasSetTextAction()).performTextInput("supermercado")
    composeRule.onCategoryOptionWithText(supermercado.nombre).performClick()

    composeRule.runOnIdle { assertEquals(listOf(supermercado.id), selections) }
  }

  @Test
  fun tappingACategorySelectsItOnceAndDismisses() {
    val selections = mutableListOf<String>()
    var dismissals = 0
    var visible by mutableStateOf(true)

    composeRule.setContent {
      TallybookTheme {
        CategoryPickerSheet(
            visible = visible,
            categorias = categories,
            selectedId = comida.id,
            onSelect = { selections += it },
            onDismiss = {
              dismissals++
              visible = false
            },
        )
      }
    }

    composeRule
        .onNodeWithText(resourceString(Res.string.category_picker_single_title))
        .assertExists()
    composeRule.onCategoryOptionWithText(transporte.nombre).performClick()

    composeRule.runOnIdle {
      assertEquals(listOf(transporte.id), selections)
      assertEquals(1, dismissals)
    }
    composeRule.onCategoryOptionWithText(transporte.nombre).assertDoesNotExist()
  }

  @Test
  fun tappingTheCurrentCategoryOnlyDismisses() {
    val selections = mutableListOf<String>()
    var dismissals = 0

    composeRule.setContent {
      TallybookTheme {
        CategoryPickerSheet(
            visible = true,
            categorias = categories,
            selectedId = comida.id,
            onSelect = { selections += it },
            onDismiss = { dismissals++ },
        )
      }
    }

    composeRule.onCategoryOptionWithText(comida.nombre).assertIsSelected().performClick()

    composeRule.runOnIdle {
      assertEquals(emptyList(), selections)
      assertEquals(1, dismissals)
    }
  }

  @Test
  fun theCurrentCategoryStaysPinnedWhileSearchingAndClosesWithoutChanging() {
    val selections = mutableListOf<String>()
    var dismissals = 0

    composeRule.setContent {
      TallybookTheme {
        CategoryPickerSheet(
            visible = true,
            categorias = categories,
            selectedId = supermercado.id,
            onSelect = { selections += it },
            onDismiss = { dismissals++ },
        )
      }
    }

    composeRule
        .onNodeWithTag(CATEGORY_PICKER_CURRENT_TAG)
        .assert(hasAnyDescendant(hasText(resourceString(Res.string.category_picker_current))))
    composeRule.onCurrentCategory(supermercado.nombre).assertIsSelected()
    composeRule.onNode(hasSetTextAction()).performTextInput(transporte.nombre)
    composeRule.onCategoryOptionWithText(supermercado.nombre).assertDoesNotExist()
    composeRule.onCurrentCategory(supermercado.nombre).assertIsSelected()

    composeRule.runOnIdle {
      assertEquals(emptyList(), selections)
      assertEquals(0, dismissals)
    }
  }

  @Test
  fun nothingIsPinnedForOtrosOrAMissingCategory() {
    var selectedId by mutableStateOf<String?>(otros.id)

    composeRule.setContent {
      TallybookTheme {
        CategoryPickerSheet(
            visible = true,
            categorias = categories + otros,
            selectedId = selectedId,
            onSelect = {},
            onDismiss = {},
        )
      }
    }

    composeRule.onNodeWithTag(CATEGORY_PICKER_CURRENT_TAG).assertDoesNotExist()
    selectedId = "missing"
    composeRule.onNodeWithTag(CATEGORY_PICKER_CURRENT_TAG).assertDoesNotExist()
  }

  @Test
  fun theResultsOpenScrolledToTheSelectedFamily() {
    val families = (1..FAMILY_COUNT).map { categoria(id = "family_$it", nombre = "Family $it") }

    composeRule.setContent {
      TallybookTheme {
        CategoryPickerSheet(
            visible = true,
            categorias = families,
            selectedId = families.last().id,
            onSelect = {},
            onDismiss = {},
        )
      }
    }

    composeRule.onCategoryOptionWithText(families.last().nombre).assertIsDisplayed()
    composeRule.onCategoryOptionWithText(families.first().nombre).assertDoesNotExist()
  }

  @Test
  fun familiesShowTheParentThenItsChildrenInOneLevelAndSeparateBlocks() {
    composeRule.setContent {
      TallybookTheme {
        CategoryPickerSheet(
            visible = true,
            categorias = categories,
            selectedId = null,
            onSelect = {},
            onDismiss = {},
        )
      }
    }

    val parent = composeRule.onCategoryOptionWithText(comida.nombre).boundsInRoot()
    val child = composeRule.onCategoryOptionWithText(supermercado.nombre).boundsInRoot()
    val nextFamily = composeRule.onCategoryOptionWithText(transporte.nombre).boundsInRoot()

    assertTrue(parent.left < child.left, "Parent $parent should precede child $child")
    assertEquals(parent.top, child.top)
    assertTrue(nextFamily.top > parent.bottom, "Next family $nextFamily should start below $parent")
  }

  @Test
  fun searchShowsEmptyResultAndMarksTheSelectedCategory() {
    composeRule.setContent {
      TallybookTheme {
        CategoryPickerSheet(
            visible = true,
            categorias = categories,
            selectedId = supermercado.id,
            onSelect = {},
            onDismiss = {},
        )
      }
    }

    composeRule.onCategoryOptionWithText(supermercado.nombre).assertIsSelected()
    composeRule.onNode(hasSetTextAction()).performTextInput("no-match")
    composeRule.onNodeWithText(resourceString(Res.string.no_categories_found)).assertExists()
  }

  @Test
  fun searchWithoutMatchesKeepsTheResultsHeight() {
    composeRule.setContent {
      TallybookTheme {
        CategoryPickerSheet(
            visible = true,
            categorias = categories,
            selectedId = null,
            onSelect = {},
            onDismiss = {},
        )
      }
    }

    val initialHeight =
        composeRule
            .onNodeWithTag("category_picker_content")
            .fetchSemanticsNode()
            .boundsInRoot
            .height
    composeRule.onNode(hasSetTextAction()).performTextInput("no-match")
    val emptyResultHeight =
        composeRule
            .onNodeWithTag("category_picker_content")
            .fetchSemanticsNode()
            .boundsInRoot
            .height

    assertTrue(abs(initialHeight - emptyResultHeight) <= 1f)
  }

  @Test
  fun searchWithOneMatchKeepsTheResultsHeight() {
    composeRule.setContent {
      TallybookTheme {
        CategoryPickerSheet(
            visible = true,
            categorias = categories,
            selectedId = null,
            onSelect = {},
            onDismiss = {},
        )
      }
    }

    val initialHeight =
        composeRule
            .onNodeWithTag("category_picker_content")
            .fetchSemanticsNode()
            .boundsInRoot
            .height
    composeRule.onNode(hasSetTextAction()).performTextInput("supermercado")
    val filteredHeight =
        composeRule
            .onNodeWithTag("category_picker_content")
            .fetchSemanticsNode()
            .boundsInRoot
            .height

    assertTrue(abs(initialHeight - filteredHeight) <= 1f)
  }

  @Test
  fun excludedIdsAreHiddenFromRootsAndSearch() {
    composeRule.setContent {
      TallybookTheme {
        CategoryPickerSheet(
            visible = true,
            categorias = categories,
            selectedId = null,
            onSelect = {},
            onDismiss = {},
            excludedIds = setOf(transporte.id, supermercado.id),
        )
      }
    }

    composeRule.onNodeWithText(transporte.nombre).assertDoesNotExist()
    composeRule.onNodeWithText(comida.nombre).assertExists()
    composeRule.onNode(hasSetTextAction()).performTextInput(supermercado.nombre)
    composeRule.onNodeWithText(resourceString(Res.string.no_categories_found)).assertExists()
  }

  private fun resourceString(resource: StringResource, vararg args: Any): String = runBlocking {
    getString(resource, *args)
  }

  private fun SemanticsNodeInteractionsProvider.onCategoryOptionWithText(text: String) =
      onNode(
          hasText(text) and
              hasClickAction() and
              isSelectable() and
              hasAnyAncestor(hasTestTag("category_picker_results"))
      )

  private fun SemanticsNodeInteractionsProvider.onCurrentCategory(text: String) =
      onNode(hasText(text) and hasAnyAncestor(hasTestTag(CATEGORY_PICKER_CURRENT_TAG)))

  private fun SemanticsNodeInteraction.boundsInRoot() = fetchSemanticsNode().boundsInRoot

  private companion object {
    val comida = categoria(id = "comida", nombre = "Comida")
    val supermercado = categoria(id = "supermercado", nombre = "Supermercado", padreId = comida.id)
    val transporte = categoria(id = "transporte", nombre = "Transporte")
    val otros = categoria(id = CATEGORIA_OTROS_ID, nombre = "Otros")
    const val FAMILY_COUNT = 20

    val categories = listOf(comida, supermercado, transporte)

    fun categoria(id: String, nombre: String, padreId: String? = null) =
        Categoria(
            id = id,
            nombre = nombre,
            padreId = padreId,
            icono = CategoryIcon.UTENSILS,
            color = CategoryTint.AMBER,
        )
  }
}
