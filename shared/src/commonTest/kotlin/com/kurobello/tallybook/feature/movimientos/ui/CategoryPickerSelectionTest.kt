package com.kurobello.tallybook.feature.movimientos.ui

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryPickerSelectionTest {
  private val catalog =
      listOf(category("food"), category("groceries", parent = "food"), category("health"))

  @Test
  fun choosingAnUnselectedCategoryAddsIt() {
    val updates = mutableListOf<List<String>>()
    val selection = selection(selected = listOf("health"), updates = updates)

    selection.choose("food", catalog)

    assertEquals(listOf(listOf("health", "food")), updates)
  }

  @Test
  fun choosingASelectedCategoryRemovesItWhenAnotherRemains() {
    val updates = mutableListOf<List<String>>()
    val selection = selection(selected = listOf("health", "food"), updates = updates)

    selection.choose("health", catalog)

    assertEquals(listOf(listOf("food")), updates)
  }

  @Test
  fun choosingTheLastCategoryReportsNothing() {
    val updates = mutableListOf<List<String>>()
    val selection = selection(selected = listOf("health"), updates = updates)

    selection.choose("health", catalog)

    assertEquals(emptyList(), updates)
  }

  @Test
  fun choosingAChildReplacesItsParent() {
    val updates = mutableListOf<List<String>>()
    val selection = selection(selected = listOf("food", "health"), updates = updates)

    selection.choose("groceries", catalog)

    assertEquals(listOf(listOf("health", "groceries")), updates)
  }

  @Test
  fun choosingAtTheLimitReportsNothing() {
    val updates = mutableListOf<List<String>>()
    val selection = selection(selected = listOf("food", "health"), max = 2, updates = updates)

    selection.choose("other", catalog)

    assertEquals(emptyList(), updates)
  }

  private fun selection(
      selected: List<String>,
      updates: MutableList<List<String>>,
      max: Int = 6,
  ) = CategoryPickerSelection(selected, max) { updates += it }

  private fun category(id: String, parent: String? = null) =
      Categoria(id, id, parent, CategoryIcon.CIRCLE_DASHED, CategoryTint.NEUTRAL)
}
