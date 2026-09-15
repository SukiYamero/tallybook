package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.back
import tallybook.shared.generated.resources.no_categories_found

class CategoryPickerTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun rootWithChildrenOpensDrillInAndSelectsTheGeneralCategory() {
    var selectedId: String? = null
    var dismissCount = 0

    composeRule.setContent {
      TallybookTheme {
        CategoryPickerSheet(
            visible = true,
            categorias = categories,
            selectedId = null,
            onSelect = { selectedId = it },
            onDismiss = { dismissCount += 1 },
        )
      }
    }

    composeRule.onCategoryOptionWithText(comida.nombre).performClick()
    composeRule.onNodeWithText(supermercado.nombre).assertExists()
    composeRule.onCategoryOptionWithText(comida.nombre).performClick()

    composeRule.runOnIdle {
      assertEquals(comida.id, selectedId)
      assertEquals(1, dismissCount)
    }
  }

  @Test
  fun searchShowsFlatResultsAndSelectsAChild() {
    var selectedId: String? = null
    var dismissCount = 0

    composeRule.setContent {
      TallybookTheme {
        CategoryPickerSheet(
            visible = true,
            categorias = categories,
            selectedId = null,
            onSelect = { selectedId = it },
            onDismiss = { dismissCount += 1 },
        )
      }
    }

    composeRule.onNode(hasSetTextAction()).performTextInput("supermercado")
    composeRule.onNodeWithText(supermercado.nombre).performClick()

    composeRule.runOnIdle {
      assertEquals(supermercado.id, selectedId)
      assertEquals(1, dismissCount)
    }
  }

  @Test
  fun backFromDrillInReturnsToRoots() {
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

    composeRule.onCategoryOptionWithText(comida.nombre).performClick()
    composeRule.onNodeWithText(resourceString(Res.string.back)).performClick()

    composeRule.onNodeWithText(supermercado.nombre).assertDoesNotExist()
    composeRule.onNodeWithText(transporte.nombre).assertExists()
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

    composeRule.onCategoryOptionWithText(comida.nombre).performClick()
    composeRule.onNodeWithText(supermercado.nombre).assertIsSelected()
    composeRule.onNode(hasSetTextAction()).performTextInput("no-match")
    composeRule.onNodeWithText(resourceString(Res.string.no_categories_found)).assertExists()
  }

  private fun resourceString(resource: StringResource): String = runBlocking { getString(resource) }

  private fun SemanticsNodeInteractionsProvider.onCategoryOptionWithText(text: String) =
      onNode(hasText(text) and hasClickAction())

  private companion object {
    val comida = categoria(id = "comida", nombre = "Comida")
    val supermercado = categoria(id = "supermercado", nombre = "Supermercado", padreId = comida.id)
    val transporte = categoria(id = "transporte", nombre = "Transporte")

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
