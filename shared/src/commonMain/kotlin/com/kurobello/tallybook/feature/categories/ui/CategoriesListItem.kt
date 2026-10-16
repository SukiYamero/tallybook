package com.kurobello.tallybook.feature.categories.ui

import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesContent
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesUiState
import com.kurobello.tallybook.feature.categories.viewmodel.CategoryRow

/** One lazy item of the Categories screen; scrolling to a row reads the same order it renders. */
internal sealed interface CategoriesListItem {
  val key: String

  data object Loading : CategoriesListItem {
    override val key = "categories-loading"
  }

  data object Failed : CategoriesListItem {
    override val key = "categories-failed"
  }

  data class SearchEmpty(val query: String) : CategoriesListItem {
    override val key = "categories-search-empty"
  }

  data class Heading(val section: CategoriesSection) : CategoriesListItem {
    override val key = "categories-heading-${section.name}"
  }

  data object CustomEmpty : CategoriesListItem {
    override val key = "categories-custom-empty"
  }

  data class Row(val row: CategoryRow) : CategoriesListItem {
    override val key = "categories-row-${row.categoria.id}"
  }
}

internal enum class CategoriesSection {
  CUSTOM,
  PREDEFINED,
}

internal fun CategoriesUiState.listItems(): List<CategoriesListItem> =
    when (val content = content) {
      CategoriesContent.Loading -> listOf(CategoriesListItem.Loading)
      CategoriesContent.Failed -> listOf(CategoriesListItem.Failed)
      is CategoriesContent.Ready -> content.listItems(query.trim())
    }

private fun CategoriesContent.Ready.listItems(query: String): List<CategoriesListItem> = buildList {
  if (query.isNotEmpty() && isEmpty) {
    add(CategoriesListItem.SearchEmpty(query))
    return@buildList
  }
  if (query.isEmpty() || custom.isNotEmpty()) {
    add(CategoriesListItem.Heading(CategoriesSection.CUSTOM))
    if (custom.isEmpty()) add(CategoriesListItem.CustomEmpty)
    custom.mapTo(this, CategoriesListItem::Row)
  }
  if (predefined.isNotEmpty()) {
    add(CategoriesListItem.Heading(CategoriesSection.PREDEFINED))
    predefined.forEach { family -> family.mapTo(this, CategoriesListItem::Row) }
  }
}
