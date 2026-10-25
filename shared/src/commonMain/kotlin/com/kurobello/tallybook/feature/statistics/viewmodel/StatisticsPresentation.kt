package com.kurobello.tallybook.feature.statistics.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryStatisticsRequest
import com.kurobello.tallybook.core.model.CategoryStatisticsSnapshot
import com.kurobello.tallybook.core.model.DimensionTotal
import com.kurobello.tallybook.core.model.Insight
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Share
import com.kurobello.tallybook.core.model.StatisticsKey
import com.kurobello.tallybook.core.model.StatisticsRequest
import com.kurobello.tallybook.core.model.StatisticsScope
import com.kurobello.tallybook.core.model.StatisticsSnapshot
import com.kurobello.tallybook.core.model.change
import com.kurobello.tallybook.core.model.exclusiveShares
import com.kurobello.tallybook.core.model.insights
import com.kurobello.tallybook.core.model.isCurrentStatisticsPeriod
import com.kurobello.tallybook.core.model.overlappingShares
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

/**
 * [main] is the screen's content for the same read, which gives the root its color and share of
 * the period. Throws [ArithmeticException] when a total leaves the Long range.
 */
internal fun presentCategoryDetail(
    detail: CategoryStatisticsSnapshot,
    rootId: String,
    context: StatisticsContext,
    catalog: StatisticsCatalog,
    main: StatisticsContent?,
): CategoryDetailContent {
  val key = StatisticsKey.Categoria(rootId)
  val row = main?.distributions?.categorias?.rows?.firstOrNull { it.key == key }
  val color = row?.color ?: StatisticsColor.Rank(0)
  val labels = StatisticsLabels(catalog)
  val partialMonth = context.request.trend.end.takeIf { it == context.today }?.yearMonth
  val months =
      detail.months.map { (month, total) ->
        CategoryMonth(month, total.chartValue(), total.format(), month == partialMonth)
      }
  val complete = detail.months.filter { (month, _) -> month != partialMonth }.map { it.second }
  return CategoryDetailContent(
      categoria = catalog.categorias[rootId],
      label = labels.of(key),
      color = color,
      scope = context.scope,
      summary =
          CategoryDetailSummary(
              total = detail.current.format(),
              shareBasisPoints = row?.basisPoints,
              count = detail.count,
              change = change(detail.current, detail.previous),
              previous = detail.previous?.takeIf { it.minorUnits > 0 }?.format(),
              comparison = context.comparison(),
          ),
      months = months,
      average = complete.averageOrNull()?.let { CategoryAverage(it.chartValue(), it.format()) },
      subcategorias =
          everyShare(detail.subcategorias).map { share ->
            val subKey = share.key as StatisticsKey
            share.toRow(
                if (subKey == key) StatisticsLabel.General else labels.of(subKey),
                color,
            )
          },
      largest = detail.largest.map { CategoryLargest(it, catalog.categorias[it.categoriaId]) },
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

private fun StatisticsContext.comparison(): StatisticsComparison? {
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

/**
 * The rows list every group so each one stays reachable; those the donut folds into "Otras" take
 * its color, so a row's dot always matches its slice.
 */
private fun exclusiveDistribution(
    totals: List<DimensionTotal>,
    gastos: Money,
    labels: StatisticsLabels,
): StatisticsDistribution {
  val slices = exclusiveShares(totals)
  val named = slices.count { it.key != null }
  return StatisticsDistribution(
      rows =
          everyShare(totals).mapIndexed { rank, share ->
            share.toRow(
                labels.of(share.key as StatisticsKey),
                if (rank < named) StatisticsColor.Rank(rank) else StatisticsColor.Otras,
            )
          },
      donut =
          slices.mapIndexed { rank, share ->
            val key = share.key
            if (key == null)
                StatisticsSlice(StatisticsLabel.Otras, share.basisPoints, StatisticsColor.Otras)
            else StatisticsSlice(labels.of(key), share.basisPoints, StatisticsColor.Rank(rank))
          },
      total = gastos.format(),
  )
}

private fun overlappingDistribution(
    totals: List<DimensionTotal>,
    gastos: Money,
    labels: StatisticsLabels,
): StatisticsDistribution =
    StatisticsDistribution(
        rows =
            overlappingShares(totals, gastos).mapIndexed { rank, share ->
              share.toRow(labels.of(share.key as StatisticsKey), StatisticsColor.Rank(rank))
            },
        donut = null,
        total = gastos.format(),
    )

private fun everyShare(totals: List<DimensionTotal>): List<Share> =
    exclusiveShares(totals, maxSlices = maxOf(MIN_SLICES, totals.size))

private fun Share.toRow(label: StatisticsLabel, color: StatisticsColor) =
    StatisticsShareRow(
        key = key as StatisticsKey,
        label = label,
        amount = amount.format(),
        basisPoints = basisPoints,
        change = change,
        color = color,
    )

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
  if (context.scope != StatisticsScope.Month || !context.isCurrent || spent.minorUnits <= 0)
      return null
  val pace = pace(snapshot.daily, snapshot.months, context.today) ?: return null
  return StatisticsPace(
      today = context.today,
      cumulative = pace.cumulative.map { it.chartValue() },
      averageMonth = pace.averageMonth.chartValue(),
      projection = pace.projection.chartValue(),
      daysInMonth = context.today.yearMonth.numberOfDays,
      change = pace.change,
      spentText = spent.format(),
      averageText = pace.averageMonth.format(),
      projectionText = pace.projection.format(),
  )
}

private fun Insight.present(labels: StatisticsLabels): StatisticsInsight =
    when (this) {
      is Insight.CategoryRose ->
          StatisticsInsight.CategoryRose(labels.of(key), change.percent, difference.format())
      is Insight.CategoryFell ->
          StatisticsInsight.CategoryFell(labels.of(key), change.percent, difference.format())
      is Insight.BiggestDay -> StatisticsInsight.BiggestDay(fecha, gasto.format())
    }

private class StatisticsLabels(private val catalog: StatisticsCatalog) {
  fun of(key: StatisticsKey): StatisticsLabel =
      when (key) {
        is StatisticsKey.Categoria ->
            catalog.categorias[key.id]?.let { StatisticsLabel.Named(it.nombre) }
                ?: StatisticsLabel.UnknownCategoria
        is StatisticsKey.Etiqueta ->
            catalog.etiquetas[key.id]?.let(StatisticsLabel::Named)
                ?: StatisticsLabel.UnknownEtiqueta
        is StatisticsKey.Cuenta ->
            catalog.cuentas[key.id]?.let(StatisticsLabel::Named) ?: StatisticsLabel.UnknownCuenta
        is StatisticsKey.Medio -> StatisticsLabel.Medio(key.medio)
      }
}

private fun List<Money>.averageOrNull(): Money? {
  if (isEmpty()) return null
  val sum = reduce(Money::plus)
  val quotient = sum.minorUnits / size
  val remainder = sum.minorUnits % size
  return Money(if (remainder * 2 >= size) quotient + 1 else quotient, sum.moneda)
}

private fun Money.signedFormat(): String = if (minorUnits > 0) "+" + format() else format()

private fun Money.chartValue(): Double = minorUnits / TEN.pow(moneda.scale)

private const val SKIPPED_PALETTE_INDEX = 3
private const val MIN_SLICES = 2
private const val TEN = 10.0
