package com.kurobello.tallybook.feature.search.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.search.viewmodel.SearchActions
import com.kurobello.tallybook.feature.search.viewmodel.SearchCategoryState
import com.kurobello.tallybook.feature.search.viewmodel.SearchFailure
import com.kurobello.tallybook.feature.search.viewmodel.SearchFilterSessionId
import com.kurobello.tallybook.feature.search.viewmodel.SearchFilters
import com.kurobello.tallybook.feature.search.viewmodel.SearchSource
import com.kurobello.tallybook.feature.search.viewmodel.SearchUiState
import com.kurobello.tallybook.feature.search.viewmodel.SearchWork
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

@Preview
@Composable
internal fun SearchContentLightPreview() = SearchPreview(searchPreviewState(), dark = false)

@Preview
@Composable
internal fun SearchContentDarkPreview() = SearchPreview(searchPreviewState(), dark = true)

@Preview
@Composable
internal fun SearchEmptyPreview() {
  val state = searchPreviewState()
  SearchPreview(
      state.copy(
          results =
              RetainedLoadState.Ready(checkNotNull(state.displayResults).copy(rows = emptyList()))
      ),
      dark = false,
  )
}

@Preview
@Composable
internal fun SearchLoadingPreview() =
    SearchPreview(
        searchPreviewState().copy(results = RetainedLoadState.Loading(), work = SearchWork.Reading),
        dark = false,
    )

@Preview
@Composable
internal fun SearchErrorPreview() =
    SearchPreview(
        searchPreviewState()
            .copy(
                results =
                    RetainedLoadState.Failed(
                        SearchFailure.Repository(
                            SearchSource.Movimientos,
                            DataError.Local(IllegalStateException("Preview")),
                        )
                    ),
                work = null,
            ),
        dark = true,
    )

@Preview
@Composable
internal fun SearchLargeTextPreview() {
  val density = LocalDensity.current
  CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
    SearchContentLightPreview()
  }
}

@Composable
private fun SearchPreview(state: SearchUiState, dark: Boolean) {
  TallybookTheme(darkTheme = dark) { SearchScreen(state, SearchPreviewActions, {}, {}, 0.dp) }
}

@Suppress("MagicNumber")
private fun searchPreviewState(): SearchUiState {
  val date = LocalDate(2026, 10, 6)
  val category = Categoria("food", "Food", null, CategoryIcon.UTENSILS, CategoryTint.AMBER)
  val movement =
      Movimiento(
          "coffee",
          TipoMovimiento.GASTO,
          Money(450, Moneda.USD),
          date,
          listOf("food"),
          "Morning coffee",
          "Oat latte to go",
          Instant.parse("2026-10-06T12:00:00Z"),
      )
  val years = SearchCurrencyYears(Moneda.USD, listOf(2026))
  val content =
      SearchResultsUi(
          SearchRequest(MovimientoReadCriteria(Moneda.USD), ""),
          listOf(SearchResultRowUi(movement, category, "Morning coffee", "Food")),
          years,
      )
  return SearchUiState(
      "",
      SearchFilters.initial(date),
      date,
      Moneda.USD,
      categories = SearchCategoryState(listOf(category), false),
      years = years,
      results = RetainedLoadState.Ready(content),
      work = null,
  )
}

@Suppress("TooManyFunctions")
private object SearchPreviewActions : SearchActions {
  override val hasActiveFilterSession = false

  override fun onQueryChanged(query: String) = Unit

  override fun onClearQuery() = Unit

  override fun onRemoveDateFilter() = Unit

  override fun onRemoveTypeFilter() = Unit

  override fun onRemoveCategoryFilter(id: String) = Unit

  override fun onShowCategory(id: String) = Unit

  override fun onRetry() = Unit

  override fun onResume() = Unit

  override fun onPresentationChanged(unknownCategoryLabel: String, localeTag: String) = Unit

  override fun onOpenFilters(): SearchFilterSessionId? = null

  override fun onFilterDraftChanged(id: SearchFilterSessionId, filters: SearchFilters) = Unit

  override fun onClearFilters(id: SearchFilterSessionId) = Unit

  override fun onApplyFilters(id: SearchFilterSessionId) = Unit

  override fun onCancelFilters(id: SearchFilterSessionId) = Unit

  override fun onFiltersHidden(id: SearchFilterSessionId) = Unit
}
