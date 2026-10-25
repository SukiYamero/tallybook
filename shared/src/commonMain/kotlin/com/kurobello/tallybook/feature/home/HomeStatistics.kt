package com.kurobello.tallybook.feature.home

import com.kurobello.tallybook.core.model.Change
import com.kurobello.tallybook.core.model.StatisticsKey
import com.kurobello.tallybook.core.model.StatisticsSnapshot
import com.kurobello.tallybook.core.model.exclusiveShares
import com.kurobello.tallybook.core.model.pace
import kotlinx.datetime.LocalDate

/**
 * This month's gasto by root categoria: the [top] three in rank order and [otherBasisPoints] for
 * every other one, adding up to 10,000. [pace] is null until a whole month before had gasto.
 */
internal data class HomeStatistics(
    val top: List<HomeCategoryShare>,
    val otherBasisPoints: Int,
    val pace: Change?,
)

internal data class HomeCategoryShare(val categoriaId: String, val basisPoints: Int)

/**
 * Null for a month without gasto. Throws [ArithmeticException] when a total leaves the Long range.
 */
internal fun StatisticsSnapshot.toHomeStatistics(today: LocalDate): HomeStatistics? {
  if (current.gastos.minorUnits <= 0) return null
  val top =
      exclusiveShares(categorias, maxSlices = HOME_SLICES).take(HOME_TOP).mapNotNull { share ->
        (share.key as? StatisticsKey.Categoria)?.let { HomeCategoryShare(it.id, share.basisPoints) }
      }
  return HomeStatistics(
      top = top,
      otherBasisPoints = BASIS_POINTS - top.sumOf { it.basisPoints },
      pace = pace(daily, months, today)?.change,
  )
}

private const val HOME_TOP = 3
private const val HOME_SLICES = HOME_TOP + 1
private const val BASIS_POINTS = 10_000
