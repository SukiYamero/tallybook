package com.kurobello.tallybook.feature.movimientos.ui

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoriaSelection
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryPickerGroupsTest {

  @Test
  fun familiesListTheParentFirstThenItsChildren() {
    assertEquals(
        listOf(
            listOf("food", "groceries"),
            listOf("transport", "gas", "taxi"),
            listOf("other"),
            listOf("movies"),
        ),
        ids(categoryPickerGroups(catalog, emptySet(), query = "")),
    )
  }

  @Test
  fun excludedCategoriesLeaveTheirFamily() {
    assertEquals(
        listOf(listOf("food", "groceries"), listOf("transport", "gas", "taxi"), listOf("movies")),
        ids(categoryPickerGroups(catalog, setOf("other"), query = "")),
    )
  }

  @Test
  fun anExcludedParentNeverPromotesItsChildrenIntoAnotherFamily() {
    assertEquals(
        listOf(
            listOf("groceries"),
            listOf("transport", "gas", "taxi"),
            listOf("other"),
            listOf("movies"),
        ),
        ids(categoryPickerGroups(catalog, setOf("food"), query = "")),
    )
  }

  @Test
  fun searchShowsParentsAndChildrenInOneFlatBlockInFamilyOrder() {
    assertEquals(
        listOf(listOf("groceries", "transport", "gas", "movies")),
        ids(categoryPickerGroups(catalog, emptySet(), query = "s")),
    )
  }

  @Test
  fun searchIgnoresExcludedCategories() {
    assertEquals(
        emptyList(),
        ids(categoryPickerGroups(catalog, setOf("other"), query = "other")),
    )
  }

  @Test
  fun searchWithoutMatchesHasNoBlocks() {
    assertEquals(emptyList(), ids(categoryPickerGroups(catalog, emptySet(), query = "zzz")))
  }

  @Test
  fun selectedCategoriesKeepSelectionOrderAndSkipUnknownIds() {
    assertEquals(
        listOf("movies", "food"),
        selectedCategorias(catalog, listOf("movies", "gone", "food")).map { it.id },
    )
  }

  @Test
  fun choosingAChildReplacesItsParentAndChoosingTheParentReplacesItsChildren() {
    val withChild = CategoriaSelection.add(listOf("movies", "transport"), "gas", catalog)
    assertEquals(listOf("movies", "gas"), withChild)

    val withParent = CategoriaSelection.add(withChild, "transport", catalog)
    assertEquals(listOf("movies", "transport"), withParent)
  }

  private fun ids(groups: List<List<Categoria>>) = groups.map { group -> group.map { it.id } }

  private companion object {
    val food = categoria("food", "Food")
    val transport = categoria("transport", "Transport")
    val groceries = categoria("groceries", "Groceries", food.id)
    val gas = categoria("gas", "Gas", transport.id)
    val taxi = categoria("taxi", "Taxi", transport.id)
    val other = categoria("other", "Other")
    val movies = categoria("movies", "Movies")
    val catalog = listOf(food, transport, groceries, gas, taxi, other, movies)

    fun categoria(id: String, nombre: String, padreId: String? = null) =
        Categoria(id, nombre, padreId, CategoryIcon.UTENSILS, CategoryTint.AMBER)
  }
}
