package com.kurobello.tallybook.feature.statistics.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Change
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.StatisticsScope
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import kotlinx.datetime.YearMonth

/**
 * The open category sheet. While [isClosing] it keeps the last [content] it rendered so the exit
 * animation never empties it; [pendingSearch] is emitted once the sheet reports it is hidden.
 */
internal data class CategoryDetailState(
    val rootId: String,
    val content: RetainedLoadState<CategoryDetailContent, StatisticsFailure> =
        RetainedLoadState.Loading(),
    val isClosing: Boolean = false,
    val pendingSearch: StatisticsEffect.OpenSearch? = null,
)

/**
 * The sheet's own copy of the read it shows, so it keeps describing that period while it closes
 * after a change. [average] covers the complete months shown and is null while only the month in
 * progress is shown.
 */
internal data class CategoryDetailContent(
    val categoria: Categoria?,
    val label: StatisticsLabel,
    val color: StatisticsColor,
    val scope: StatisticsScope,
    val period: MovimientoDateRange,
    val partial: Boolean,
    val summary: CategoryDetailSummary,
    val months: List<CategoryMonth>,
    val average: CategoryAverage?,
    val subcategorias: List<StatisticsShareRow>,
    val largest: List<CategoryLargest>,
)

/** [shareBasisPoints] is the root's share of the period's gasto. */
internal data class CategoryDetailSummary(
    val total: String,
    val shareBasisPoints: Int?,
    val count: Long,
    val change: Change?,
    val previous: String?,
    val comparison: StatisticsComparison?,
)

internal data class CategoryMonth(
    val month: YearMonth,
    val value: Double,
    val text: String,
    val partial: Boolean,
)

internal data class CategoryAverage(val value: Double, val text: String)

/** [categoria] is the movimiento's own one, a subcategoria or the root. */
internal data class CategoryLargest(val movimiento: Movimiento, val categoria: Categoria?)
