package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import kotlin.math.absoluteValue

private const val GROUP_SIZE = 3
private const val DECIMAL_BASE = 10L

internal data class DecimalDigits(val integer: String, val fraction: String)

/**
 * Splits `20.000`, `20,000`, `12,50` and `1.234,56`: a separator followed by exactly three digits
 * groups, any other separator is decimal, and a mix of both characters makes the last one decimal.
 * Grouping also needs a leading group of one to three digits without a leading zero, so `0,500` and
 * `1234.567` are decimals (and then exceed every moneda scale) instead of 500 and 1,234,567.
 */
internal fun splitDecimal(text: String): DecimalDigits? {
  val segments = text.split('.', ',')
  val separators = text.filter { it == '.' || it == ',' }.toSet()
  val lastIsDecimal =
      when {
        separators.isEmpty() -> false
        separators.size > 1 -> true
        segments.size > 2 -> false
        else -> !(segments.last().length == GROUP_SIZE && segments.first().isLeadingGroup())
      }
  val grouped = if (lastIsDecimal) segments.dropLast(1) else segments
  val isWellFormed =
      grouped.size == 1 ||
          (grouped.first().isLeadingGroup() && grouped.drop(1).all { it.length == GROUP_SIZE })
  return DecimalDigits(grouped.joinToString(""), if (lastIsDecimal) segments.last() else "")
      .takeIf { isWellFormed }
}

private fun String.isLeadingGroup(): Boolean = length in 1..GROUP_SIZE && first() != '0'

/**
 * Exact minor units of `digits * factor`, or null when the value is not positive, overflows a Long
 * or needs more decimals than the moneda holds. Without a multiplier the decimals must fit the
 * scale as typed; with one ("1,5 millones") only the resulting value has to be exact.
 */
internal fun DecimalDigits.toMoney(factor: Long, moneda: Moneda): Money? {
  val digits = (integer + fraction).toLongOrNull()
  val isTypedExactly = factor != 1L || fraction.length <= moneda.scale
  val minorUnits =
      if (digits != null && isTypedExactly) {
        scaleByPowerOfTen(multiplyExact(digits, factor), moneda.scale - fraction.length)
      } else {
        null
      }
  return minorUnits?.takeIf { it > 0 }?.let { Money(it, moneda) }
}

private fun scaleByPowerOfTen(value: Long?, exponent: Int): Long? {
  val power = powerOfTen(exponent.absoluteValue)
  return when {
    value == null || power == null -> null
    exponent >= 0 -> multiplyExact(value, power)
    value % power == 0L -> value / power
    else -> null
  }
}

private fun powerOfTen(exponent: Int): Long? =
    (1..exponent).fold<Int, Long?>(1L) { acc, _ -> acc?.let { multiplyExact(it, DECIMAL_BASE) } }

private fun multiplyExact(a: Long, b: Long): Long? =
    if (a == 0L || b == 0L || a <= Long.MAX_VALUE / b) a * b else null

private fun multiplyExact(a: Long?, b: Long): Long? = a?.let { multiplyExact(it, b) }

private fun addExact(a: Long, b: Long): Long? = if (a <= Long.MAX_VALUE - b) a + b else null

/** [isLoneUnit] marks a single word below ten, which only counts as an amount with a marker. */
internal data class SpelledNumber(val amount: Long, val wordCount: Int, val isLoneUnit: Boolean)

/**
 * Reads the longest valid prefix of number words ("veinte mil", "two hundred and five"). The prefix
 * is rejected as a whole when its value does not fit a Long.
 */
internal fun evaluateNumberWords(words: List<String>): SpelledNumber? {
  var state = SpelledState(total = 0, current = 0)
  var used = 0
  for ((index, word) in words.withIndex()) {
    state = state.accept(word, words.getOrNull(index + 1)) ?: break
    used = index + 1
  }
  val amount = state.value
  return if (used == 0 || amount == null) {
    null
  } else {
    SpelledNumber(amount, used, isLoneUnit = used == 1 && words.first() in numberWordUnits)
  }
}

// An overflow is sticky so the remaining words are still consumed and the whole prefix is rejected.
private data class SpelledState(
    val total: Long,
    val current: Long,
    val isOverflowed: Boolean = false,
) {
  val value: Long?
    get() = if (isOverflowed) null else addExact(total, current)

  fun accept(word: String, next: String?): SpelledState? =
      when {
        word in numberWordValues ->
            updated(total, addExact(current, numberWordValues.getValue(word)))
        word in scaleWords -> applyScale(scaleWords.getValue(word))
        word in numberWordConnectors && isConnectorAllowed(next) -> this
        else -> null
      }

  // "veinte y cinco", "ciento y veinte" and "mil e quinhentos" continue a round value with a
  // smaller one; "veinte y veinte" and "cinco y cinco" are two separate numbers.
  private fun isConnectorAllowed(next: String?): Boolean {
    val nextValue = numberWordValues[next] ?: return false
    return nextValue < joinGranularity
  }

  // The magnitude of the lowest round place of the accumulated value, 0 when it ends in units.
  private val joinGranularity: Long
    get() =
        when {
          isOverflowed -> 0
          current > 0 -> granularityOf(current, HUNDRED_FACTOR, DECIMAL_BASE)
          else -> granularityOf(total, MILLION_FACTOR, THOUSAND_FACTOR)
        }

  private fun granularityOf(value: Long, vararg magnitudes: Long): Long =
      if (value <= 0) 0 else magnitudes.firstOrNull { value % it == 0L } ?: 0

  private fun applyScale(factor: Long): SpelledState {
    val scaledCurrent = multiplyExact(current.coerceAtLeast(1), factor)
    return when (factor) {
      HUNDRED_FACTOR -> updated(total, scaledCurrent)
      THOUSAND_FACTOR -> updated(scaledCurrent?.let { addExact(total, it) }, 0)
      else -> updated(value?.coerceAtLeast(1)?.let { multiplyExact(it, factor) }, 0)
    }
  }

  private fun updated(total: Long?, current: Long?): SpelledState =
      if (isOverflowed || total == null || current == null) {
        copy(isOverflowed = true)
      } else {
        copy(total = total, current = current)
      }
}
