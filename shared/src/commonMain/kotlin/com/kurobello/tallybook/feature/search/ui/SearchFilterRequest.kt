package com.kurobello.tallybook.feature.search.ui

import androidx.compose.runtime.Immutable
import com.kurobello.tallybook.core.model.Moneda

/**
 * Asks Search to replace its filters with [categoryIds], [etiquetaIds] and [cuentaIds], first
 * switching to [moneda] when given; [token] tells repeated requests for the same ids apart.
 */
@Immutable
internal data class SearchFilterRequest(
    val token: Long,
    val categoryIds: Set<String> = emptySet(),
    val etiquetaIds: Set<String> = emptySet(),
    val cuentaIds: Set<String> = emptySet(),
    val moneda: Moneda? = null,
)
