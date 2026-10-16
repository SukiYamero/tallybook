package com.kurobello.tallybook.feature.movimientos.ui

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryPickerModeTest {
  private val catalog =
      listOf(category("food"), category("groceries", parent = "food"), category("health"))

  @Test
  fun singleModeReportsTheChoiceAndDismisses() {
    var chosen: String? = null
    var dismissals = 0
    val mode = CategoryPickerMode.Single(selectedId = null, onSelect = { chosen = it })

    mode.choose("health", catalog) { dismissals++ }

    assertEquals("health", chosen)
    assertEquals(1, dismissals)
  }

  @Test
  fun multipleModeAddsAnUnselectedCategoryWithoutDismissing() {
    val updates = mutableListOf<List<String>>()
    var dismissals = 0
    val mode = multiple(selected = listOf("health"), updates = updates)

    mode.choose("food", catalog) { dismissals++ }

    assertEquals(listOf(listOf("health", "food")), updates)
    assertEquals(0, dismissals)
  }

  @Test
  fun multipleModeRemovesASelectedCategoryWhenAnotherRemains() {
    val updates = mutableListOf<List<String>>()
    val mode = multiple(selected = listOf("health", "food"), updates = updates)

    mode.choose("health", catalog) {}

    assertEquals(listOf(listOf("food")), updates)
  }

  @Test
  fun multipleModeReportsNothingWhenTheLastCategoryIsTapped() {
    val updates = mutableListOf<List<String>>()
    val mode = multiple(selected = listOf("health"), updates = updates)

    mode.choose("health", catalog) {}

    assertEquals(emptyList(), updates)
  }

  @Test
  fun multipleModeReplacesTheParentWhenAChildIsChosen() {
    val updates = mutableListOf<List<String>>()
    val mode = multiple(selected = listOf("food", "health"), updates = updates)

    mode.choose("groceries", catalog) {}

    assertEquals(listOf(listOf("health", "groceries")), updates)
  }

  @Test
  fun multipleModeIgnoresAChoiceAtTheLimit() {
    val updates = mutableListOf<List<String>>()
    val mode = multiple(selected = listOf("food", "health"), max = 2, updates = updates)

    mode.choose("other", catalog) {}

    assertEquals(emptyList(), updates)
  }

  private fun multiple(
      selected: List<String>,
      updates: MutableList<List<String>>,
      max: Int = 6,
  ) = CategoryPickerMode.Multiple(selected, max) { updates += it }

  private fun category(id: String, parent: String? = null) =
      Categoria(id, id, parent, CategoryIcon.CIRCLE_DASHED, CategoryTint.NEUTRAL)
}
