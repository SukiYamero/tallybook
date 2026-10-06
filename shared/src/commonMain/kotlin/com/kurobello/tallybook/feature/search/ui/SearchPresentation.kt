package com.kurobello.tallybook.feature.search.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.ui.component.LoadingPresentation
import com.kurobello.tallybook.core.ui.component.deferLoading
import com.kurobello.tallybook.core.ui.component.rememberLoadingPresentation
import com.kurobello.tallybook.core.ui.text.normalizeSearchText
import com.kurobello.tallybook.feature.search.viewmodel.SearchUiState
import com.kurobello.tallybook.feature.search.viewmodel.SearchWork
import kotlin.time.TimeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal enum class SearchFeedbackKind {
  Skeleton,
  Progress,
}

internal data class SearchFeedbackRequest(
    val criteria: MovimientoReadCriteria?,
    val kind: SearchFeedbackKind?,
    val normalizedQuery: String?,
)

internal data class SearchFeedback(val request: SearchFeedbackRequest) {
  val kind: SearchFeedbackKind?
    get() = request.kind
}

internal typealias SearchPresentation = LoadingPresentation<SearchFeedback>

internal fun searchFeedback(state: SearchUiState): SearchFeedback {
  val kind =
      when {
        state.work == null || state.results.failure != null -> null
        state.work == SearchWork.WaitingText -> null
        state.displayResults == null -> SearchFeedbackKind.Skeleton
        else -> SearchFeedbackKind.Progress
      }
  return SearchFeedback(
      SearchFeedbackRequest(
          state.criteria,
          kind,
          if (kind == SearchFeedbackKind.Progress) normalizeSearchText(state.rawQuery) else null,
      )
  )
}

internal fun Flow<SearchUiState>.deferSearchLoading(
    timeSource: TimeSource = TimeSource.Monotonic,
): Flow<SearchPresentation> =
    map(::searchFeedback)
        .deferLoading(
            isLoading = { it.kind != null },
            requestKey = { it.request },
            timeSource = timeSource,
        )

@Composable
internal fun rememberSearchPresentation(state: SearchUiState): State<SearchPresentation> =
    key(state.criteria) {
      rememberLoadingPresentation(
          searchFeedback(state),
          isLoading = { it.kind != null },
          requestKey = { it.request },
      )
    }
