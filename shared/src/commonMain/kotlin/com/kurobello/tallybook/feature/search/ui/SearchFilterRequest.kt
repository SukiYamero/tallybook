package com.kurobello.tallybook.feature.search.ui

import androidx.compose.runtime.Immutable
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.SearchTipo

/**
 * Asks Search to replace its filters with [categoryIds], [etiquetaIds], [cuentaIds], [tipo] and a
 * Custom [range], first switching to [moneda] when given; [token] tells repeated requests for the
 * same filters apart.
 */
@Immutable
internal data class SearchFilterRequest(
    val token: Long,
    val categoryIds: Set<String> = emptySet(),
    val etiquetaIds: Set<String> = emptySet(),
    val cuentaIds: Set<String> = emptySet(),
    val moneda: Moneda? = null,
    val range: MovimientoDateRange? = null,
    val tipo: SearchTipo? = null,
)
