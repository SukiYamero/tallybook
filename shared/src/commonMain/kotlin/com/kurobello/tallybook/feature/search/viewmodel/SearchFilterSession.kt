package com.kurobello.tallybook.feature.search.viewmodel

import kotlin.jvm.JvmInline
import kotlinx.datetime.LocalDate

@JvmInline internal value class SearchFilterSessionId(val value: Long)

internal sealed interface SearchFilterSession {
  data object Hidden : SearchFilterSession

  data class Editing(
      val sessionId: SearchFilterSessionId,
      val draft: SearchFilters,
      val dateFailure: SearchDateFailure? = null,
  ) : SearchFilterSession

  data class Closing(
      val sessionId: SearchFilterSessionId,
      val renderSnapshot: SearchFilters,
  ) : SearchFilterSession
}

internal data class SearchFilterTransition(
    val session: SearchFilterSession,
    val committed: SearchFilters? = null,
)

internal fun SearchFilterSession.open(
    id: SearchFilterSessionId,
    applied: SearchFilters,
): SearchFilterSession =
    if (this == SearchFilterSession.Hidden) SearchFilterSession.Editing(id, applied.copy())
    else this

internal fun SearchFilterSession.edit(
    id: SearchFilterSessionId,
    transform: (SearchFilters) -> SearchFilters,
): SearchFilterSession =
    if (this is SearchFilterSession.Editing && sessionId == id)
        copy(draft = transform(draft), dateFailure = null)
    else this

internal fun SearchFilterSession.clear(
    id: SearchFilterSessionId,
    today: LocalDate,
): SearchFilterTransition = edit(id) { SearchFilters.initial(today) }.apply(id, today)

internal fun SearchFilterSession.apply(
    id: SearchFilterSessionId,
    today: LocalDate,
): SearchFilterTransition {
  if (this !is SearchFilterSession.Editing || sessionId != id) return SearchFilterTransition(this)
  return when (val resolution = resolveSearchFilters(draft, today)) {
    is SearchFilterResolution.Valid ->
        SearchFilterTransition(
            SearchFilterSession.Closing(id, resolution.filters),
            resolution.filters,
        )
    is SearchFilterResolution.Invalid ->
        SearchFilterTransition(copy(dateFailure = resolution.failure))
  }
}

internal fun SearchFilterSession.cancel(id: SearchFilterSessionId): SearchFilterSession =
    if (this is SearchFilterSession.Editing && sessionId == id)
        SearchFilterSession.Closing(id, draft)
    else this

internal fun SearchFilterSession.hidden(id: SearchFilterSessionId): SearchFilterSession =
    if (this is SearchFilterSession.Closing && sessionId == id) SearchFilterSession.Hidden else this
