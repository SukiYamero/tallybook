package com.kurobello.tallybook.feature.statistics.viewmodel

import com.kurobello.tallybook.core.model.CategoryStatisticsSnapshot
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.StatisticsKey
import com.kurobello.tallybook.core.model.change
import kotlinx.datetime.yearMonth

/**
 * [main] is the screen's content for the same read, which gives the root its color and share of the
 * period. Throws [ArithmeticException] when a total leaves the Long range.
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
      period = context.request.period,
      partial = context.isCurrent,
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

private fun List<Money>.averageOrNull(): Money? {
  if (isEmpty()) return null
  val sum = reduce(Money::plus)
  val quotient = sum.minorUnits / size
  val remainder = sum.minorUnits % size
  return Money(if (remainder * 2 >= size) quotient + 1 else quotient, sum.moneda)
}
