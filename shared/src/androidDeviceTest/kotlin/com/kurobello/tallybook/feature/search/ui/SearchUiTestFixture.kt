package com.kurobello.tallybook.feature.search.ui

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.feature.search.viewmodel.SearchActions
import com.kurobello.tallybook.feature.search.viewmodel.SearchCategoryState
import com.kurobello.tallybook.feature.search.viewmodel.SearchEtiquetaState
import com.kurobello.tallybook.feature.search.viewmodel.SearchFilterSessionId
import com.kurobello.tallybook.feature.search.viewmodel.SearchFilters
import com.kurobello.tallybook.feature.search.viewmodel.SearchUiState
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

internal val searchTestToday = LocalDate(2026, 10, 6)
internal val searchTestCategory =
    Categoria("food", "Food", null, CategoryIcon.UTENSILS, CategoryTint.AMBER)
internal val searchTestEtiqueta =
    Etiqueta("t_empresa", "Empresa", Instant.parse("2026-01-01T00:00:00Z"))
internal val searchTestMovement =
    Movimiento(
        "coffee",
        TipoMovimiento.GASTO,
        Money(450, Moneda.USD),
        searchTestToday,
        "food",
        "Morning coffee",
        null,
        Instant.parse("2026-10-06T12:00:00Z"),
    )
internal val searchTestResults =
    SearchResultsUi(
        SearchRequest(MovimientoReadCriteria(Moneda.USD), ""),
        listOf(SearchResultRowUi(searchTestMovement, searchTestCategory, "Morning coffee", "Food")),
        SearchCurrencyYears(Moneda.USD, listOf(2026)),
    )
internal val searchTestState =
    SearchUiState(
        "",
        SearchFilters.initial(searchTestToday),
        searchTestToday,
        Moneda.USD,
        categories = SearchCategoryState(listOf(searchTestCategory), false),
        etiquetas = SearchEtiquetaState(listOf(searchTestEtiqueta)),
        years = searchTestResults.years,
        results = RetainedLoadState.Ready(searchTestResults),
        work = null,
    )

internal open class SearchTestActions : SearchActions {
  val events = mutableListOf<String>()
  var lastDraft: SearchFilters? = null
  override val hasActiveFilterSession = false

  override fun onQueryChanged(query: String) {
    events += "query:$query"
  }

  override fun onClearQuery() {
    events += "clear-query"
  }

  override fun onRemoveDateFilter() {
    events += "remove-date"
  }

  override fun onRemoveTypeFilter() {
    events += "remove-type"
  }

  override fun onRemoveCategoryFilter(id: String) {
    events += "remove-category:$id"
  }

  override fun onRemoveTagFilter(id: String) {
    events += "remove-tag:$id"
  }

  override fun onShowFilters(categoryIds: Set<String>, etiquetaIds: Set<String>) {
    events += "show-filters:$categoryIds:$etiquetaIds"
  }

  override fun onRetry() {
    events += "retry"
  }

  override fun onResume() = Unit

  override fun onPresentationChanged(unknownCategoryLabel: String, localeTag: String) = Unit

  override fun onOpenFilters(): SearchFilterSessionId? = SearchFilterSessionId(1)

  override fun onFilterDraftChanged(id: SearchFilterSessionId, filters: SearchFilters) {
    events += "draft:${filters.preset}"
    lastDraft = filters
  }

  override fun onClearFilters(id: SearchFilterSessionId) {
    events += "clear-filters"
  }

  override fun onApplyFilters(id: SearchFilterSessionId) {
    events += "apply-filters"
  }

  override fun onCancelFilters(id: SearchFilterSessionId) {
    events += "cancel-filters"
  }

  override fun onFiltersHidden(id: SearchFilterSessionId) {
    events += "hidden-filters"
  }
}
