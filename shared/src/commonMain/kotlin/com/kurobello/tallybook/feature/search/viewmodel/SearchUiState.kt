package com.kurobello.tallybook.feature.search.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.text.normalizeSearchText
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.feature.search.ui.SearchCurrencyYears
import com.kurobello.tallybook.feature.search.ui.SearchResultsUi
import kotlinx.datetime.LocalDate

internal data class SearchUiState(
    val rawQuery: String,
    val filters: SearchFilters,
    val today: LocalDate,
    val moneda: Moneda? = null,
    val filterSession: SearchFilterSession = SearchFilterSession.Hidden,
    val sessionMinimumYear: Int? = null,
    val categories: SearchCategoryState = SearchCategoryState(),
    val etiquetas: SearchEtiquetaState = SearchEtiquetaState(),
    val years: SearchCurrencyYears? = null,
    val results: RetainedLoadState<SearchResultsUi, SearchFailure> = RetainedLoadState.Loading(),
    val work: SearchWork? = SearchWork.Reading,
    val preferenceFailure: DataError? = null,
) {
  val criteria: MovimientoReadCriteria?
    get() {
      val currency = moneda ?: return null
      val resolution =
          resolveSearchFilters(filters, today) as? SearchFilterResolution.Valid ?: return null
      return MovimientoReadCriteria(
          currency,
          resolution.range,
          filters.tipo,
          filters.categoryIds,
          filters.etiquetaIds,
      )
    }

  val isCurrent: Boolean
    get() =
        results is RetainedLoadState.Ready &&
            results.value.request.criteria == criteria &&
            results.value.request.normalizedQuery == normalizeSearchText(rawQuery)

  val currentCount: Int?
    get() = if (isCurrent) results.cachedValue?.count else null

  val displayResults: SearchResultsUi?
    get() = results.cachedValue?.takeIf { it.request.criteria == criteria }

  val isTextPending: Boolean
    get() = work == SearchWork.WaitingText

  val canOpenFilters: Boolean
    get() =
        moneda != null &&
            preferenceFailure == null &&
            categories.isReady &&
            etiquetas.isReady &&
            (years?.moneda == moneda ||
                resolveSearchFilters(filters, today) is SearchFilterResolution.Invalid)

  val availableYears: List<Int>
    get() {
      val rendered =
          when (val session = filterSession) {
            SearchFilterSession.Hidden -> filters
            is SearchFilterSession.Editing -> session.draft
            is SearchFilterSession.Closing -> session.renderSnapshot
          }
      return rendered.availableYears(
          years?.persistedYears.orEmpty(),
          today,
          setOfNotNull(filters.customStart.year, filters.customEnd.year, sessionMinimumYear),
      )
    }
}

internal data class SearchCategoryState(
    val values: List<Categoria>? = null,
    val isLoading: Boolean = true,
    val failure: DataError? = null,
) {
  val isReady: Boolean
    get() = values != null && failure == null
}

internal data class SearchEtiquetaState(
    val values: List<Etiqueta>? = null,
    val failure: DataError? = null,
) {
  val isReady: Boolean
    get() = values != null && failure == null
}

internal enum class SearchWork {
  Reading,
  WaitingText,
  Matching,
  Revalidating,
}
