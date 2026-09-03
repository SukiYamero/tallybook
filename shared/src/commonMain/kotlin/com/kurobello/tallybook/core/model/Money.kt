package com.kurobello.tallybook.core.model

private const val GROUP_SIZE = 3

/**
 * Monto in the minor units of its moneda (cents at scale 2, the whole unit for CLP). Storing the
 * integer and not a decimal keeps `SUM()`/`GROUP BY` on SQLite's side and avoids every intermediate
 * rounding: the 8 supported monedas are scale 0 or 2.
 */
data class Money(val minorUnits: Long, val moneda: Moneda) : Comparable<Money> {

  operator fun plus(other: Money): Money {
    requireSameMoneda(other)
    return Money(addExact(minorUnits, other.minorUnits), moneda)
  }

  operator fun minus(other: Money): Money {
    requireSameMoneda(other)
    return Money(subtractExact(minorUnits, other.minorUnits), moneda)
  }

  operator fun unaryMinus(): Money = Money(subtractExact(0L, minorUnits), moneda)

  override fun compareTo(other: Money): Int {
    requireSameMoneda(other)
    return minorUnits.compareTo(other.minorUnits)
  }

  fun format(): String {
    val digits = minorUnits.toString().removePrefix("-").padStart(moneda.scale + 1, '0')
    // Sign glued to the digits, not before the symbol: the order moneta imposes on Intl's output in
    // attachSignToNumber (movimientoView.ts).
    val sign = if (minorUnits < 0) "-" else ""
    val cut = digits.length - moneda.scale
    val integerPart = groupDigits(digits.take(cut), moneda.groupSeparator)
    val decimals = if (moneda.scale == 0) "" else moneda.decimalSeparator + digits.drop(cut)
    return moneda.symbol + moneda.symbolSeparator + sign + integerPart + decimals
  }

  private fun requireSameMoneda(other: Money) {
    require(moneda == other.moneda) {
      "Cannot operate on montos of different monedas: $moneda and ${other.moneda}"
    }
  }

  companion object {

    /**
     * Reads a plain decimal with no group separator (`-?\d+(\.\d{1,scale})?`). Rejects — instead of
     * truncating — more decimals than the moneda admits: silently losing money on an import or a
     * form parse is worse than failing.
     */
    fun parseOrNull(text: String, moneda: Moneda): Money? {
      val body = text.removePrefix("-")
      val integerPart = body.substringBefore('.')
      val decimals = body.substringAfter('.', "")
      val wellFormed =
          integerPart.isNotEmpty() &&
              integerPart.all { it in '0'..'9' } &&
              decimals.all { it in '0'..'9' } &&
              decimals.length <= moneda.scale &&
              (!body.contains('.') || decimals.isNotEmpty())
      val minorUnits =
          if (wellFormed) (integerPart + decimals.padEnd(moneda.scale, '0')).toLongOrNull()
          else null
      return minorUnits?.let { Money(if (text.startsWith('-')) -it else it, moneda) }
    }

    fun parse(text: String, moneda: Moneda): Money =
        requireNotNull(parseOrNull(text, moneda)) { "Invalid monto for $moneda: '$text'" }
  }
}

private fun overflow(): Nothing = throw ArithmeticException("Monto out of Long range")

// The sign bit gives the wrap away: if both operands differ from the result, the sum overflowed.
private fun addExact(a: Long, b: Long): Long =
    (a + b).also { if (((a xor it) and (b xor it)) < 0) overflow() }

private fun subtractExact(a: Long, b: Long): Long =
    (a - b).also { if (((a xor b) and (a xor it)) < 0) overflow() }

private fun groupDigits(digits: String, separator: String): String = buildString {
  digits.forEachIndexed { index, digit ->
    if (index > 0 && (digits.length - index) % GROUP_SIZE == 0) append(separator)
    append(digit)
  }
}
