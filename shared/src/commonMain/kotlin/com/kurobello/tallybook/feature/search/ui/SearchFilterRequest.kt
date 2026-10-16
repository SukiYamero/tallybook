package com.kurobello.tallybook.feature.search.ui

import androidx.compose.runtime.Immutable

/**
 * Asks Search to replace its filters with [categoryIds] and [etiquetaIds]; [token] tells repeated
 * requests for the same ids apart.
 */
@Immutable
internal data class SearchFilterRequest(
    val token: Long,
    val categoryIds: Set<String> = emptySet(),
    val etiquetaIds: Set<String> = emptySet(),
)
