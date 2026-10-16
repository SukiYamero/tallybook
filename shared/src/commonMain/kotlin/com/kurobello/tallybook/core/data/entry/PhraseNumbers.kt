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
 */
internal fun splitDecimal(text: String): DecimalDigits? {
  val segments = text.split('.', ',')
  val separators = text.filter { it == '.' || it == ',' }.toSet()
  val lastIsDecimal =
      when {
        separators.isEmpty() -> false
        separators.size > 1 -> true
        else -> segments.last().length != GROUP_SIZE
      }
  val grouped = if (lastIsDecimal) segments.dropLast(1) else segments
  val isWellFormed = grouped.drop(1).all { it.length == GROUP_SIZE }
  return DecimalDigits(grouped.joinToString(""), if (lastIsDecimal) segments.last() else "")
      .takeIf { isWellFormed }
}

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

private fun saturatingMultiply(a: Long, b: Long): Long = multiplyExact(a, b) ?: Long.MAX_VALUE

private fun saturatingAdd(a: Long, b: Long): Long =
    if (a <= Long.MAX_VALUE - b) a + b else Long.MAX_VALUE

internal data class SpelledNumber(val amount: Long, val wordCount: Int)

/** Reads the longest valid prefix of number words ("veinte mil", "two hundred and five"). */
internal fun evaluateNumberWords(words: List<String>): SpelledNumber? {
  var state = SpelledState(total = 0, current = 0)
  var used = 0
  for ((index, word) in words.withIndex()) {
    state = state.accept(word, words.getOrNull(index + 1)) ?: break
    used = index + 1
  }
  val isLoneUnit = used == 1 && words.first() in singleUnitWords
  return if (used == 0 || isLoneUnit) null else SpelledNumber(state.value, used)
}

// Overflowing sequences saturate at Long.MAX_VALUE so the money conversion rejects them.
private data class SpelledState(val total: Long, val current: Long) {
  val value: Long
    get() = saturatingAdd(total, current)

  fun accept(word: String, next: String?): SpelledState? =
      when {
        word in numberWordValues -> copy(current = current + numberWordValues.getValue(word))
        word in scaleWords -> applyScale(scaleWords.getValue(word))
        word in numberWordConnectors && isConnectorAllowed(next) -> this
        else -> null
      }

  // "veinte y cinco" and "one hundred and five" join a round number to a unit, never two units.
  private fun isConnectorAllowed(next: String?): Boolean =
      next in numberWordUnits && value > 0 && current % DECIMAL_BASE == 0L

  private fun applyScale(factor: Long): SpelledState =
      when (factor) {
        HUNDRED_FACTOR -> copy(current = saturatingMultiply(current.coerceAtLeast(1), factor))
        THOUSAND_FACTOR ->
            SpelledState(
                saturatingAdd(total, saturatingMultiply(current.coerceAtLeast(1), factor)),
                0,
            )
        else -> SpelledState(saturatingMultiply(value.coerceAtLeast(1), factor), 0)
      }
}
