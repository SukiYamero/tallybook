package com.kurobello.tallybook.feature.movimientos.ui

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import kotlin.test.Test
import kotlin.test.assertEquals

class CategorySearchTest {

  @Test
  fun findsRootsAndChildrenIgnoringCaseAndDiacritics() {
    val categories = listOf(comida, cafe, transporte, publico)

    assertEquals(listOf(cafe), searchCategorias(categories, "CAFÉ"))
    assertEquals(listOf(cafe), searchCategorias(categories, "cafe"))
    assertEquals(listOf(transporte, publico), searchCategorias(categories, "TRANSPORTE"))
  }

  @Test
  fun trimsTheQueryAndKeepsInputOrder() {
    val categories = listOf(comida, cafe, transporte)

    assertEquals(listOf(comida), searchCategorias(categories, "  comida  "))
  }

  @Test
  fun matchesPortugueseDiacritics() {
    val poupanca = categoria(id = "poupanca", nombre = "Poupança")

    assertEquals(listOf(poupanca), searchCategorias(listOf(poupanca), "poupanca"))
  }

  @Test
  fun matchesComposedAndDecomposedAccentsInEitherDirection() {
    val categories =
        listOf(
            categoria(id = "cafe", nombre = "Cafe\u0301"),
            categoria(id = "acao", nombre = "Ac\u0327a\u0303o"),
        )

    assertEquals(listOf(categories[0]), searchCategorias(categories, "  CAFÉ  "))
    assertEquals(listOf(categories[1]), searchCategorias(categories, "AÇÃO"))
    assertEquals(listOf(cafe), searchCategorias(listOf(cafe), "cafe\u0301"))
  }

  @Test
  fun preservesEnyeAndDoesNotSearchCategoryIds() {
    val category = categoria(id = "hidden-id", nombre = "An\u0303o")

    assertEquals(listOf(category), searchCategorias(listOf(category), "AÑO"))
    assertEquals(emptyList(), searchCategorias(listOf(category), "ano"))
    assertEquals(emptyList(), searchCategorias(listOf(category), "hidden-id"))
  }

  @Test
  fun returnsNoResultsForBlankOrUnknownQuery() {
    val categories = listOf(comida, cafe)

    assertEquals(emptyList(), searchCategorias(categories, ""))
    assertEquals(emptyList(), searchCategorias(categories, " "))
    assertEquals(emptyList(), searchCategorias(categories, "viaje"))
  }

  private companion object {
    val comida = categoria(id = "comida", nombre = "Comida")
    val cafe = categoria(id = "cafe", nombre = "Café", padreId = comida.id)
    val transporte = categoria(id = "transporte", nombre = "Transporte")
    val publico = categoria(id = "publico", nombre = "Transporte público", padreId = transporte.id)

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
