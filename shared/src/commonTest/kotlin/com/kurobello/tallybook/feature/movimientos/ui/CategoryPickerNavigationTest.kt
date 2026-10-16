package com.kurobello.tallybook.feature.movimientos.ui

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CategoryPickerNavigationTest {

  private val tree = buildCategoryTree(listOf(comida, supermercado, transporte))

  @Test
  fun aRootWithChildrenOpensItsChildrenInsteadOfBeingChosen() {
    assertTrue(
        tree.shouldOpenChildren(comida, allowSubcategories = true, isSearching = false, null)
    )
  }

  @Test
  fun aRootWithoutChildrenIsChosenDirectly() {
    assertFalse(
        tree.shouldOpenChildren(transporte, allowSubcategories = true, isSearching = false, null)
    )
  }

  @Test
  fun theParentRowInsideItsChildrenListIsChosenDirectly() {
    assertFalse(
        tree.shouldOpenChildren(comida, allowSubcategories = true, isSearching = false, comida)
    )
    assertFalse(
        tree.shouldOpenChildren(
            supermercado,
            allowSubcategories = true,
            isSearching = false,
            comida,
        )
    )
  }

  @Test
  fun searchResultsAreChosenDirectly() {
    assertFalse(
        tree.shouldOpenChildren(comida, allowSubcategories = true, isSearching = true, null)
    )
  }

  @Test
  fun aParentOnlyPickerNeverOpensChildren() {
    assertFalse(
        tree.shouldOpenChildren(comida, allowSubcategories = false, isSearching = false, null)
    )
  }

  private companion object {
    val comida = categoria("comida")
    val supermercado = categoria("supermercado", comida.id)
    val transporte = categoria("transporte")

    fun categoria(id: String, padreId: String? = null) =
        Categoria(
            id = id,
            nombre = id,
            padreId = padreId,
            icono = CategoryIcon.UTENSILS,
            color = CategoryTint.AMBER,
        )
  }
}
