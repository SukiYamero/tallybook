package com.kurobello.tallybook.feature.search.viewmodel

// Explicit callbacks keep the filter transaction and synchronous shell guards under one owner.
@Suppress("TooManyFunctions")
internal interface SearchActions {
  val hasActiveFilterSession: Boolean

  fun onQueryChanged(query: String)

  fun onClearQuery()

  fun onRemoveDateFilter()

  fun onRemoveTypeFilter()

  fun onRemoveCategoryFilter(id: String)

  fun onRemoveTagFilter(id: String)

  /**
   * Resets the query and filters to All history, every type and exactly [categoryIds] and
   * [etiquetaIds]. Blank IDs are ignored, and a request without any ID keeps the current input.
   */
  fun onShowFilters(categoryIds: Set<String>, etiquetaIds: Set<String>)

  fun onRetry()

  fun onResume()

  fun onPresentationChanged(unknownCategoryLabel: String, localeTag: String = "")

  fun onOpenFilters(): SearchFilterSessionId?

  fun onFilterDraftChanged(id: SearchFilterSessionId, filters: SearchFilters)

  fun onClearFilters(id: SearchFilterSessionId)

  fun onApplyFilters(id: SearchFilterSessionId)

  fun onCancelFilters(id: SearchFilterSessionId)

  fun onFiltersHidden(id: SearchFilterSessionId)
}
