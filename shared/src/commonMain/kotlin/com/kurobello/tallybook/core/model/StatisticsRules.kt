package com.kurobello.tallybook.core.model

import kotlin.math.absoluteValue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.yearMonth

internal enum class Direction {
  Up,
  Down,
  Flat,
}

/** [percent] is the size of the variation, rounded half away from zero; [direction] its sign. */
internal data class Change(val percent: Int, val direction: Direction)

/**
 * Null when there is nothing to compare against: no [previous], or a [previous] that is not
 * positive.
 */
internal fun change(current: Money, previous: Money?): Change? {
  if (previous == null || previous.minorUnits <= 0) return null
  val difference = (current - previous).minorUnits
  val percent =
      roundedRatio(difference.absoluteValue, PERCENT, previous.minorUnits)
          .coerceAtMost(Int.MAX_VALUE.toLong())
          .toInt()
  val direction =
      when {
        difference > 0 -> Direction.Up
        difference < 0 -> Direction.Down
        else -> Direction.Flat
      }
  return Change(percent, direction)
}

/** A slice of a distribution; a null [key] is "Otras", which has no [change]. */
internal data class Share(
    val key: StatisticsKey?,
    val amount: Money,
    val basisPoints: Int,
    val change: Change?,
)

/**
 * Groups with current gasto, by amount DESC then id, measured against their own sum so the basis
 * points add up to exactly 10,000: the largest slice absorbs the rounding remainder. Past
 * [maxSlices] groups, the first `maxSlices - 1` stay and the rest join in "Otras".
 */
internal fun exclusiveShares(
    totals: List<DimensionTotal>,
    maxSlices: Int = MAX_SLICES,
): List<Share> {
  require(maxSlices >= 2) { "A distribution needs room for a group and Otras" }
  val ranked = ranked(totals)
  if (ranked.isEmpty()) return emptyList()
  val sum = ranked.map { it.current }.reduce(Money::plus)
  val shown = if (ranked.size > maxSlices) ranked.take(maxSlices - 1) else ranked
  val otras = ranked.drop(shown.size).map { it.current }.reduceOrNull(Money::plus)
  val measured =
      shown.map {
        Share(it.key, it.current, basisPointsOf(it.current, sum), change(it.current, it.previous))
      } + listOfNotNull(otras?.let { Share(null, it, basisPointsOf(it, sum), null) })
  val largest = measured.indices.maxBy { measured[it].amount }
  val remainder = BASIS_POINTS - measured.sumOf { it.basisPoints }
  return measured.mapIndexed { index, share ->
    if (index == largest) share.copy(basisPoints = share.basisPoints + remainder) else share
  }
}

/**
 * Groups with current gasto, by amount DESC then id, each measured against [total] on its own: a
 * movimiento counts under every one of its tags, so the basis points do not add up to a whole.
 */
internal fun overlappingShares(totals: List<DimensionTotal>, total: Money): List<Share> {
  if (total.minorUnits <= 0) return emptyList()
  return ranked(totals).map {
    Share(it.key, it.current, basisPointsOf(it.current, total), change(it.current, it.previous))
  }
}

/**
 * [cumulative] holds the gasto up to each day of the month, [averageMonth] the mean of up to the
 * three latest whole months with gasto, and [projection] where the month ends at this rate.
 */
internal data class Pace(
    val cumulative: List<Money>,
    val averageMonth: Money,
    val projection: Money,
    val change: Change,
)

/**
 * Null unless [daily] runs from the first day of [today]'s month to [today] and a whole month had
 * gasto.
 */
internal fun pace(daily: List<DayGasto>, months: List<MonthTotals>, today: LocalDate): Pace? {
  val currentMonth = today.yearMonth
  val isCurrentMonth =
      daily.firstOrNull()?.fecha == currentMonth.firstDay && daily.last().fecha == today
  val history =
      months
          .filter { it.month < currentMonth && it.summary.gastos.minorUnits > 0 }
          .sortedBy { it.month }
          .takeLast(PACE_MONTHS)
          .map { it.summary.gastos }
  if (!isCurrentMonth || history.isEmpty()) return null
  val moneda = history.first().moneda
  val average =
      Money(roundedRatio(history.reduce(Money::plus).minorUnits, 1, history.size.toLong()), moneda)
  val cumulative = daily.map { it.gasto }.runningReduce(Money::plus)
  val projection =
      Money(
          roundedRatio(
              cumulative.last().minorUnits,
              currentMonth.numberOfDays.toLong(),
              today.day.toLong(),
          ),
          moneda,
      )
  return Pace(cumulative, average, projection, checkNotNull(change(projection, average)))
}

internal sealed interface Insight {
  /** [difference] is the size of the rise. */
  data class CategoryRose(
      val key: StatisticsKey.Categoria,
      val change: Change,
      val difference: Money,
  ) : Insight

  /** [difference] is the size of the fall. */
  data class CategoryFell(
      val key: StatisticsKey.Categoria,
      val change: Change,
      val difference: Money,
  ) : Insight

  data class BiggestDay(val fecha: LocalDate, val gasto: Money) : Insight
}

/**
 * Up to three findings, in this order: the categoria that rose the most and the one that fell the
 * most, each only when it moved at least 10% and by at least 5% of the period's gasto, and the day
 * with the most gasto when at least two days had any.
 */
internal fun insights(snapshot: StatisticsSnapshot): List<Insight> {
  val movements = snapshot.categorias.mapNotNull { notableMovement(it, snapshot.current.gastos) }
  val byDifference =
      compareByDescending<CategoryMovement> { it.difference.minorUnits.absoluteValue }
          .thenBy { it.key.id }
  val rose =
      movements
          .filter { it.difference.minorUnits > 0 }
          .sortedWith(byDifference)
          .firstOrNull()
          ?.let { Insight.CategoryRose(it.key, it.change, it.difference) }
  val fell =
      movements
          .filter { it.difference.minorUnits < 0 }
          .sortedWith(byDifference)
          .firstOrNull()
          ?.let { Insight.CategoryFell(it.key, it.change, -it.difference) }
  val biggestDay =
      snapshot.daily
          .filter { it.gasto.minorUnits > 0 }
          .takeIf { it.size >= MIN_INSIGHT_DAYS }
          ?.sortedWith(compareByDescending<DayGasto> { it.gasto }.thenBy { it.fecha })
          ?.first()
          ?.let { Insight.BiggestDay(it.fecha, it.gasto) }
  return listOfNotNull(rose, fell, biggestDay)
}

private data class CategoryMovement(
    val key: StatisticsKey.Categoria,
    val difference: Money,
    val change: Change,
)

private fun notableMovement(total: DimensionTotal, periodGasto: Money): CategoryMovement? {
  val key = total.key as? StatisticsKey.Categoria
  val previous = total.previous
  val change = change(total.current, previous)
  if (key == null || previous == null || change == null) return null
  val difference = total.current - previous
  val size = difference.minorUnits.absoluteValue
  val notable =
      timesExact(size, PERCENT) >= timesExact(previous.minorUnits, MIN_CHANGE_PERCENT) &&
          timesExact(size, PERCENT) >= timesExact(periodGasto.minorUnits, MIN_SHARE_PERCENT)
  return if (notable) CategoryMovement(key, difference, change) else null
}

private fun ranked(totals: List<DimensionTotal>): List<DimensionTotal> =
    totals.filter { it.current.minorUnits > 0 }.sortedWith(DimensionTotalOrder)

private fun basisPointsOf(amount: Money, total: Money): Int =
    roundedRatio(amount.minorUnits, BASIS_POINTS.toLong(), total.minorUnits).toInt()

/**
 * `value * multiplier / divisor` rounded half up; operands are non-negative, [divisor] positive.
 * The product is never formed: a total near [Money.MAX_MINOR_UNITS] times 10,000 basis points
 * overflows Long although the ratio itself is small, so the remainder is scaled by shift-and-add.
 */
private fun roundedRatio(value: Long, multiplier: Long, divisor: Long): Long {
  val part = value % divisor
  var quotient = 0L
  var remainder = 0L
  for (bit in Long.SIZE_BITS - 1 - multiplier.countLeadingZeroBits() downTo 0) {
    // Invariant: quotient * divisor + remainder = part * (the bits of multiplier read so far).
    quotient *= 2
    if (remainder >= divisor - remainder) {
      remainder -= divisor - remainder
      quotient += 1
    } else {
      remainder *= 2
    }
    if ((multiplier shr bit) and 1L == 1L) {
      if (remainder >= divisor - part) {
        remainder -= divisor - part
        quotient += 1
      } else {
        remainder += part
      }
    }
  }
  // The fraction and its rounding add at most multiplier, so this is the only overflow left.
  val whole = timesExact(value / divisor, multiplier)
  if (whole > Long.MAX_VALUE - multiplier) throw ArithmeticException("Monto out of Long range")
  val rounded = whole + quotient
  return if (remainder >= divisor - remainder) rounded + 1 else rounded
}

private fun timesExact(a: Long, b: Long): Long {
  if (a != 0L && b > Long.MAX_VALUE / a) throw ArithmeticException("Monto out of Long range")
  return a * b
}

private const val BASIS_POINTS = 10_000
private const val PERCENT = 100L
private const val MAX_SLICES = 6
private const val PACE_MONTHS = 3
private const val MIN_CHANGE_PERCENT = 10L
private const val MIN_SHARE_PERCENT = 5L
private const val MIN_INSIGHT_DAYS = 2
