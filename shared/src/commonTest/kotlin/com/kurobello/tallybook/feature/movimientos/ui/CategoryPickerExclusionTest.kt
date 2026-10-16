package com.kurobello.tallybook.feature.movimientos.ui

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CategoryPickerExclusionTest {

  private val tree = buildCategoryTree(listOf(comida, supermercado, cafe, transporte, otros))

  @Test
  fun emptyExclusionKeepsTheTreeUntouched() {
    assertSame(tree, tree.without(emptySet()))
  }

  @Test
  fun excludedRootIsRemovedFromRoots() {
    val filtered = tree.without(setOf(otros.id))

    assertEquals(listOf(comida, transporte), filtered.roots)
    assertEquals(tree.childrenByParent, filtered.childrenByParent)
  }

  @Test
  fun excludedChildIsRemovedFromItsParent() {
    val filtered = tree.without(setOf(supermercado.id))

    assertEquals(listOf(cafe), filtered.childrenByParent.getValue(comida.id))
    assertEquals(tree.roots, filtered.roots)
  }

  @Test
  fun excludedParentNeverPromotesItsChildrenToRoots() {
    val filtered = tree.without(setOf(comida.id))

    assertEquals(listOf(transporte, otros), filtered.roots)
  }

  private companion object {
    val comida = categoria("comida")
    val supermercado = categoria("supermercado", comida.id)
    val cafe = categoria("cafe", comida.id)
    val transporte = categoria("transporte")
    val otros = categoria("cat_otros")

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
