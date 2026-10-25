package com.kurobello.tallybook.feature.statistics.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryStatisticsRequest
import com.kurobello.tallybook.core.model.Insight
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.StatisticsRequest
import com.kurobello.tallybook.core.model.StatisticsScope
import com.kurobello.tallybook.core.model.StatisticsSnapshot
import com.kurobello.tallybook.core.model.change
import com.kurobello.tallybook.core.model.insights
import com.kurobello.tallybook.core.model.isCurrentStatisticsPeriod
import com.kurobello.tallybook.core.model.pace
import kotlin.math.pow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.yearMonth

/** Names a distribution key can take; etiquetas and cuentas by id. */
internal data class StatisticsCatalog(
    val categorias: Map<String, Categoria>,
    val etiquetas: Map<String, String>,
    val cuentas: Map<String, String>,
)

/** What one read covers: the selection's [scope], its [request] and the day it was built on. */
internal data class StatisticsContext(
    val scope: StatisticsScope,
    val request: StatisticsRequest,
    val today: LocalDate,
) {
  val moneda: Moneda
    get() = request.moneda

  val isCurrent: Boolean
    get() = isCurrentStatisticsPeriod(scope, request.period.start, today)

  fun categoryRequest(rootId: String): CategoryStatisticsRequest =
      CategoryStatisticsRequest(
          moneda = request.moneda,
          rootId = rootId,
          period = request.period,
          comparison = request.comparison,
          trend = request.trend,
      )

  companion object {
    fun of(selection: StatisticsSelection, moneda: Moneda, today: LocalDate): StatisticsContext =
        StatisticsContext(selection.scope, selection.request(moneda, today), today)
  }
}

/**
 * The palette entry of the [rank]-th group, skipping the red one that reads as gasto; null once the
 * palette runs out, where the group takes the "Otras" color.
 */
internal fun chartPaletteIndex(rank: Int, paletteSize: Int): Int? {
  val index = if (rank < SKIPPED_PALETTE_INDEX) rank else rank + 1
  return index.takeIf { rank >= 0 && it < paletteSize }
}

/** Throws [ArithmeticException] when a total leaves the Long range. */
internal fun presentStatistics(
    snapshot: StatisticsSnapshot,
    context: StatisticsContext,
    catalog: StatisticsCatalog,
): StatisticsContent {
  val gastos = snapshot.current.gastos
  val labels = StatisticsLabels(catalog)
  return StatisticsContent(
      moneda = context.moneda,
      scope = context.scope,
      hasAnyMovimiento = snapshot.hasAnyMovimiento,
      summary = presentSummary(snapshot, context),
      distributions =
          StatisticsDistributions(
              categorias = exclusiveDistribution(snapshot.categorias, gastos, labels),
              etiquetas = overlappingDistribution(snapshot.etiquetas, gastos, labels),
              cuentas = exclusiveDistribution(snapshot.cuentas, gastos, labels),
              medios = exclusiveDistribution(snapshot.medios, gastos, labels),
          ),
      trend = presentTrend(snapshot, context),
      pace = presentPace(snapshot, context),
      insights = insights(snapshot).map { it.present(labels) },
  )
}

private fun presentSummary(
    snapshot: StatisticsSnapshot,
    context: StatisticsContext,
): StatisticsSummary {
  val gastos = snapshot.current.gastos
  val previous = snapshot.previous?.gastos?.takeIf { it.minorUnits > 0 }
  return StatisticsSummary(
      period = context.request.period,
      partial = context.isCurrent,
      gastos = gastos.format(),
      ingresos = snapshot.current.ingresos.signedFormat(),
      balance = snapshot.current.balance.signedFormat(),
      hasGasto = gastos.minorUnits > 0,
      change = change(gastos, previous),
      previousGastos = previous?.format(),
      comparison = context.comparison(),
  )
}

internal fun StatisticsContext.comparison(): StatisticsComparison? {
  val range = request.comparison ?: return null
  return when (scope) {
    StatisticsScope.Month ->
        if (isCurrent) StatisticsComparison.MonthStretch(range.start.yearMonth, range.end)
        else StatisticsComparison.WholeMonth(range.start.yearMonth)
    StatisticsScope.Year ->
        if (isCurrent) StatisticsComparison.YearStretch(range.end)
        else StatisticsComparison.WholeYear(range.start.year)
  }
}

private fun presentTrend(
    snapshot: StatisticsSnapshot,
    context: StatisticsContext,
): List<StatisticsTrendMonth> {
  val trendEnd = context.request.trend.end
  val partialEnd = trendEnd.takeIf { it == context.today }
  return snapshot.months.map { month ->
    val summary = month.summary
    StatisticsTrendMonth(
        month = month.month,
        gastos = summary.gastos.chartValue(),
        ingresos = summary.ingresos.chartValue(),
        partialEnd = partialEnd?.takeIf { month.month == it.yearMonth },
        gastosText = summary.gastos.format(),
        ingresosText = summary.ingresos.signedFormat(),
        balanceText = summary.balance.signedFormat(),
    )
  }
}

private fun presentPace(snapshot: StatisticsSnapshot, context: StatisticsContext): StatisticsPace? {
  val spent = snapshot.current.gastos
  val inProgress =
      context.scope == StatisticsScope.Month && context.isCurrent && spent.minorUnits > 0
  val pace = if (inProgress) pace(snapshot.daily, snapshot.months, context.today) else null
  return pace?.let {
    StatisticsPace(
        today = context.today,
        cumulative = it.cumulative.map { day -> day.chartValue() },
        averageMonth = it.averageMonth.chartValue(),
        projection = it.projection.chartValue(),
        daysInMonth = context.today.yearMonth.numberOfDays,
        change = it.change,
        spentText = spent.format(),
        averageText = it.averageMonth.format(),
        projectionText = it.projection.format(),
    )
  }
}

private fun Insight.present(labels: StatisticsLabels): StatisticsInsight =
    when (this) {
      is Insight.CategoryRose ->
          StatisticsInsight.CategoryRose(labels.of(key), change.percent, difference.format())
      is Insight.CategoryFell ->
          StatisticsInsight.CategoryFell(labels.of(key), change.percent, difference.format())
      is Insight.BiggestDay -> StatisticsInsight.BiggestDay(fecha, gasto.format())
    }

private fun Money.signedFormat(): String = if (minorUnits > 0) "+" + format() else format()

internal fun Money.chartValue(): Double = minorUnits / TEN.pow(moneda.scale)

private const val SKIPPED_PALETTE_INDEX = 3
private const val TEN = 10.0
