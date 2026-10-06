package com.kurobello.tallybook.feature.home

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoSummary

internal data class HomeContent(
    val summary: MovimientoSummary,
    val recent: List<Movimiento>,
    val categoriasById: Map<String, Categoria>,
)
