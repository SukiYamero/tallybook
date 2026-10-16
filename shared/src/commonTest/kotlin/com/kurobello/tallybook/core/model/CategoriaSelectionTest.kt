package com.kurobello.tallybook.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CategoriaSelectionTest {
  private val catalog =
      listOf(
          categoria("transport"),
          categoria("taxi", parent = "transport"),
          categoria("bus", parent = "transport"),
          categoria("food"),
          categoria("groceries", parent = "food"),
          categoria("leisure"),
          categoria("cinema", parent = "leisure"),
          categoria("health"),
          categoria("home"),
          categoria("gifts"),
          categoria("pets"),
          categoria(CATEGORIA_OTROS_ID),
      )
  private val full = listOf("transport", "food", "leisure", "health", "home", "gifts")

  @Test
  fun addAppendsToTheEnd() {
    assertEquals(listOf("food", "health"), add(listOf("food"), "health"))
  }

  @Test
  fun addIgnoresAnIdAlreadySelected() {
    assertEquals(listOf("food", "health"), add(listOf("food", "health"), "food"))
  }

  @Test
  fun addIgnoresAnIdAtTheLimit() {
    assertEquals(full, add(full, "pets"))
  }

  @Test
  fun addAcceptsTheLastFreeSlot() {
    val five = full.dropLast(1)

    assertEquals(full, add(five, "gifts"))
  }

  @Test
  fun addingAChildReplacesItsParent() {
    assertEquals(listOf("food", "taxi"), add(listOf("transport", "food"), "taxi"))
  }

  @Test
  fun addingAParentReplacesEveryChildOfIt() {
    assertEquals(listOf("food", "transport"), add(listOf("taxi", "food", "bus"), "transport"))
  }

  @Test
  fun replacingAtTheLimitSucceedsBecauseTheCountDoesNotGrow() {
    assertEquals(listOf("food", "leisure", "health", "home", "gifts", "taxi"), add(full, "taxi"))
  }

  @Test
  fun siblingsCoexist() {
    assertEquals(listOf("taxi", "bus"), add(listOf("taxi"), "bus"))
  }

  @Test
  fun aChildOfAnotherParentCoexistsWithAParent() {
    assertEquals(listOf("transport", "groceries"), add(listOf("transport"), "groceries"))
  }

  @Test
  fun addingARealCategoryDropsTheOtrosFallback() {
    assertEquals(listOf("food"), add(listOf(CATEGORIA_OTROS_ID), "food"))
  }

  @Test
  fun addingOtrosToRealCategoriesLeavesThemUntouched() {
    assertEquals(listOf("food"), add(listOf("food"), CATEGORIA_OTROS_ID))
  }

  @Test
  fun unknownIdsAreTreatedAsStandaloneCategories() {
    assertEquals(listOf("food", "ghost"), add(listOf("food"), "ghost"))
  }

  @Test
  fun removeDropsTheGivenId() {
    assertEquals(
        listOf("food", "home"),
        CategoriaSelection.remove(listOf("food", "health", "home"), "health"),
    )
  }

  @Test
  fun removeKeepsTheLastRealCategory() {
    assertEquals(listOf("food"), CategoriaSelection.remove(listOf("food"), "food"))
  }

  @Test
  fun removeIgnoresAnIdThatIsNotSelected() {
    assertEquals(
        listOf("food", "home"),
        CategoriaSelection.remove(listOf("food", "home"), "health"),
    )
  }

  @Test
  fun removeKeepsTheOtrosFallbackWhenItIsAlone() {
    assertEquals(
        listOf(CATEGORIA_OTROS_ID),
        CategoriaSelection.remove(listOf(CATEGORIA_OTROS_ID), CATEGORIA_OTROS_ID),
    )
  }

  @Test
  fun normalizedDropsOtrosWhenARealCategoryIsPresent() {
    assertEquals(
        listOf("food", "home"),
        CategoriaSelection.normalized(listOf("food", CATEGORIA_OTROS_ID, "home")),
    )
  }

  @Test
  fun normalizedKeepsOtrosWhenItIsTheOnlyCategory() {
    assertEquals(
        listOf(CATEGORIA_OTROS_ID),
        CategoriaSelection.normalized(listOf(CATEGORIA_OTROS_ID, CATEGORIA_OTROS_ID)),
    )
  }

  @Test
  fun normalizedRemovesDuplicatesKeepingTheFirstOccurrence() {
    assertEquals(
        listOf("food", "home"),
        CategoriaSelection.normalized(listOf("food", "home", "food")),
    )
  }

  @Test
  fun normalizedLeavesAnEmptyListEmpty() {
    assertEquals(emptyList(), CategoriaSelection.normalized(emptyList()))
  }

  private fun add(current: List<String>, id: String): List<String> =
      CategoriaSelection.add(current, id, catalog)

  private fun categoria(id: String, parent: String? = null) =
      Categoria(
          id = id,
          nombre = id,
          padreId = parent,
          icono = CategoryIcon.CIRCLE_DASHED,
          color = CategoryTint.NEUTRAL,
      )
}
