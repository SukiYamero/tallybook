package com.kurobello.tallybook.feature.statistics.viewmodel

import com.kurobello.tallybook.core.model.Change
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.StatisticsKey
import com.kurobello.tallybook.core.model.StatisticsScope
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

/**
 * One presented Statistics snapshot: amounts already formatted in [moneda] and colors resolved by
 * rank, so the screen only picks localized templates.
 */
internal data class StatisticsContent(
    val moneda: Moneda,
    val scope: StatisticsScope,
    val hasAnyMovimiento: Boolean,
    val summary: StatisticsSummary,
    val distributions: StatisticsDistributions,
    val trend: List<StatisticsTrendMonth>,
    val pace: StatisticsPace?,
    val insights: List<StatisticsInsight>,
)

/**
 * [partial] marks a period in progress, compared with the same stretch of the previous one;
 * [change] and [previousGastos] are null when that comparison had no gasto.
 */
internal data class StatisticsSummary(
    val period: MovimientoDateRange,
    val partial: Boolean,
    val gastos: String,
    val ingresos: String,
    val balance: String,
    val hasGasto: Boolean,
    val change: Change?,
    val previousGastos: String?,
    val comparison: StatisticsComparison?,
)

/** The stretch a period is compared against, named the way the copy reads it. */
internal sealed interface StatisticsComparison {
  data class WholeMonth(val month: YearMonth) : StatisticsComparison

  data class WholeYear(val year: Int) : StatisticsComparison

  /** From the first day of [month] to [end]. */
  data class MonthStretch(val month: YearMonth, val end: LocalDate) : StatisticsComparison

  /** From January 1st of [end]'s year to [end]. */
  data class YearStretch(val end: LocalDate) : StatisticsComparison
}

internal data class StatisticsDistributions(
    val categorias: StatisticsDistribution,
    val etiquetas: StatisticsDistribution,
    val cuentas: StatisticsDistribution,
    val medios: StatisticsDistribution,
) {
  operator fun get(dimension: StatisticsDimension): StatisticsDistribution =
      when (dimension) {
        StatisticsDimension.Categorias -> categorias
        StatisticsDimension.Etiquetas -> etiquetas
        StatisticsDimension.Cuentas -> cuentas
        StatisticsDimension.Medios -> medios
      }
}

/**
 * Groups with gasto in the period. [donut] is null for tags: a movimiento counts under each of its
 * tags, so their shares never make a whole.
 */
internal data class StatisticsDistribution(
    val rows: List<StatisticsShareRow>,
    val donut: List<StatisticsSlice>?,
    val total: String,
)

internal data class StatisticsShareRow(
    val key: StatisticsKey,
    val label: StatisticsLabel,
    val amount: String,
    val basisPoints: Int,
    val change: Change?,
    val color: StatisticsColor,
)

internal data class StatisticsSlice(
    val label: StatisticsLabel,
    val basisPoints: Int,
    val color: StatisticsColor,
)

internal sealed interface StatisticsLabel {
  data class Named(val nombre: String) : StatisticsLabel

  data class Medio(val medio: MedioPago) : StatisticsLabel

  data object UnknownCategoria : StatisticsLabel

  data object UnknownEtiqueta : StatisticsLabel

  data object UnknownCuenta : StatisticsLabel

  /** The slice grouping every group past the donut's named ones. */
  data object Otras : StatisticsLabel

  /** A root categoria's own movimientos among its subcategorias. */
  data object General : StatisticsLabel
}

internal sealed interface StatisticsColor {
  /** The [rank]-th chart color, see [chartPaletteIndex]. */
  data class Rank(val rank: Int) : StatisticsColor

  data object Otras : StatisticsColor
}

/** [partial] marks the month in progress; [partialEnd] is its last counted day. */
internal data class StatisticsTrendMonth(
    val month: YearMonth,
    val gastos: Double,
    val ingresos: Double,
    val partialEnd: LocalDate?,
    val gastosText: String,
    val ingresosText: String,
    val balanceText: String,
) {
  val partial: Boolean
    get() = partialEnd != null
}

internal data class StatisticsPace(
    val today: LocalDate,
    val cumulative: List<Double>,
    val averageMonth: Double,
    val projection: Double,
    val daysInMonth: Int,
    val change: Change,
    val spentText: String,
    val averageText: String,
    val projectionText: String,
)

internal sealed interface StatisticsInsight {
  data class CategoryRose(val label: StatisticsLabel, val percent: Int, val difference: String) :
      StatisticsInsight

  data class CategoryFell(val label: StatisticsLabel, val percent: Int, val difference: String) :
      StatisticsInsight

  data class BiggestDay(val fecha: LocalDate, val gasto: String) : StatisticsInsight
}
