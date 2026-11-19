package com.kurobello.tallybook.feature.home

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoSummary

/**
 * [summary] holds this civil month's ingresos and gastos; [totalBalance] is the primary currency's
 * total over every live account, archived included, and zero while none is in that currency.
 * [statistics] is null while the card is hidden. [pendingRecurrentes] counts occurrences due
 * through today, never money: it changes no total. [budgetsToReview] counts budgets, in any
 * currency, that are not on track.
 */
internal data class HomeContent(
    val summary: MovimientoSummary,
    val recent: List<Movimiento>,
    val categoriasById: Map<String, Categoria>,
    val totalBalance: Money,
    val statistics: HomeStatistics? = null,
    val pendingRecurrentes: Int = 0,
    val budgetsToReview: Int = 0,
)
