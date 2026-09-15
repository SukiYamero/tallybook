package com.kurobello.tallybook.feature.movimientos.ui

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryTreeTest {

  @Test
  fun buildsStableRootsAndChildren() {
    val tree = buildCategoryTree(listOf(comida, supermercado, transporte, gasolina))

    assertEquals(listOf(comida, transporte), tree.roots)
    assertEquals(listOf(supermercado), tree.childrenByParent.getValue(comida.id))
    assertEquals(listOf(gasolina), tree.childrenByParent.getValue(transporte.id))
  }

  @Test
  fun treatsAnOrphanAsARoot() {
    val orphan = categoria(id = "orphan", padreId = "missing")

    val tree = buildCategoryTree(listOf(supermercado, orphan, comida))

    assertEquals(listOf(orphan, comida), tree.roots)
    assertEquals(listOf(supermercado), tree.childrenByParent.getValue(comida.id))
  }

  @Test
  fun keepsEmptyInputEmpty() {
    val tree = buildCategoryTree(emptyList())

    assertEquals(emptyList(), tree.roots)
    assertEquals(emptyMap(), tree.childrenByParent)
  }

  private companion object {
    val comida = categoria(id = "comida")
    val supermercado = categoria(id = "supermercado", padreId = comida.id)
    val transporte = categoria(id = "transporte")
    val gasolina = categoria(id = "gasolina", padreId = transporte.id)

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
