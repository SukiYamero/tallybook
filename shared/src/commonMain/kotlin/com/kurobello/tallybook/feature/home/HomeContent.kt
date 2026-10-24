package com.kurobello.tallybook.feature.home

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoSummary

/**
 * [summary] holds this civil month's ingresos and gastos; [totalBalance] is the primary currency's
 * total over every live account, archived included, and zero while none is in that currency.
 */
internal data class HomeContent(
    val summary: MovimientoSummary,
    val recent: List<Movimiento>,
    val categoriasById: Map<String, Categoria>,
    val totalBalance: Money,
)
