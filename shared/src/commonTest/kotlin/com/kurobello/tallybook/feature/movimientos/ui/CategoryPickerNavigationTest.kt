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
    assertTrue(tree.shouldOpenChildren(comida, isSearching = false, parent = null))
  }

  @Test
  fun aRootWithoutChildrenIsChosenDirectly() {
    assertFalse(tree.shouldOpenChildren(transporte, isSearching = false, parent = null))
  }

  @Test
  fun theParentRowInsideItsChildrenListIsChosenDirectly() {
    assertFalse(tree.shouldOpenChildren(comida, isSearching = false, parent = comida))
    assertFalse(tree.shouldOpenChildren(supermercado, isSearching = false, parent = comida))
  }

  @Test
  fun searchResultsAreChosenDirectly() {
    assertFalse(tree.shouldOpenChildren(comida, isSearching = true, parent = null))
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
