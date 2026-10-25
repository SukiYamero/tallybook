package com.kurobello.tallybook.core.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

/**
 * What one Statistics emission reads, all in [moneda]. [trend] starts on the first day of a month
 * so every bucket is a whole month up to the period's end, and [daily] spans at most 366 days.
 */
internal data class StatisticsRequest(
    val moneda: Moneda,
    val period: MovimientoDateRange,
    val comparison: MovimientoDateRange?,
    val trend: MovimientoDateRange,
    val daily: MovimientoDateRange,
)

/** A group of a distribution dimension; a categoria key is always a root (or an unknown id). */
internal sealed interface StatisticsKey {
  data class Categoria(val id: String) : StatisticsKey

  data class Etiqueta(val id: String) : StatisticsKey

  data class Cuenta(val id: String) : StatisticsKey

  data class Medio(val medio: MedioPago) : StatisticsKey
}

/**
 * Gasto of one group. [previous] is null without a comparison range and zero when the range had no
 * gasto in the group.
 */
internal data class DimensionTotal(val key: StatisticsKey, val current: Money, val previous: Money?)

internal data class MonthTotals(val month: YearMonth, val summary: MovimientoSummary)

internal data class DayGasto(val fecha: LocalDate, val gasto: Money)

/**
 * Transfers and tombstones never count. Dimension lists hold every group with gasto in the period
 * or the comparison, so a group that fell to zero is still present; they are ordered by current
 * DESC, then id. Categorias add up to [current] gastos; etiquetas do not, since a movimiento counts
 * whole under each of its tags.
 */
internal data class StatisticsSnapshot(
    val current: MovimientoSummary,
    val previous: MovimientoSummary?,
    val categorias: List<DimensionTotal>,
    val etiquetas: List<DimensionTotal>,
    val cuentas: List<DimensionTotal>,
    val medios: List<DimensionTotal>,
    val months: List<MonthTotals>,
    val daily: List<DayGasto>,
    val hasAnyMovimiento: Boolean,
)

internal data class CategoryStatisticsRequest(
    val moneda: Moneda,
    val rootId: String,
    val period: MovimientoDateRange,
    val comparison: MovimientoDateRange?,
    val trend: MovimientoDateRange,
)

/**
 * Gasto of a root categoria and its children. [count] matches Search filtered by the root, the
 * moneda, the period and [SearchTipo.GASTO]. [subcategorias] keys the root's own movimientos under
 * the root id and follows the [StatisticsSnapshot] list rules. [largest] holds up to three, by
 * monto DESC, fecha DESC, then id.
 */
internal data class CategoryStatisticsSnapshot(
    val current: Money,
    val previous: Money?,
    val count: Long,
    val subcategorias: List<DimensionTotal>,
    val months: List<Pair<YearMonth, Money>>,
    val largest: List<Movimiento>,
)
