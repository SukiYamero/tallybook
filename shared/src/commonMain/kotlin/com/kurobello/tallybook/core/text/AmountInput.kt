package com.kurobello.tallybook.core.text

import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money

internal sealed interface AmountInputResult {

  data class Valid(val money: Money) : AmountInputResult

  data class Invalid(val reason: AmountInputError) : AmountInputResult
}

internal enum class AmountInputError {
  EMPTY,
  MALFORMED,
  NOT_POSITIVE,
  TOO_MANY_DECIMALS,
  OVERFLOW,
}

internal fun parseAmountInput(raw: String, moneda: Moneda): AmountInputResult {
  val input = raw.trim()
  return if (input.isEmpty()) {
    AmountInputResult.Invalid(AmountInputError.EMPTY)
  } else {
    parseNonEmptyAmount(input, moneda)
  }
}

/**
 * A valid amount moves to [to]'s decimal separator; invalid text stays as typed so the user still
 * sees what they wrote.
 */
internal fun String.reformatAmountInput(from: Moneda, to: Moneda): String =
    if (parseAmountInput(this, from) is AmountInputResult.Valid)
        replace(from.decimalSeparator, to.decimalSeparator)
    else this

internal fun Money.toAmountInput(): String {
  val digits = minorUnits.toString().padStart(moneda.scale + 1, '0')
  return if (moneda.scale == 0) digits
  else digits.dropLast(moneda.scale) + moneda.decimalSeparator + digits.takeLast(moneda.scale)
}

private fun parseNonEmptyAmount(input: String, moneda: Moneda): AmountInputResult {
  val isNegative = input.startsWith('-')
  val body = if (isNegative) input.drop(1) else input
  val parts = body.toDecimalParts(moneda.decimalSeparator.single())
  return when {
    parts == null -> AmountInputResult.Invalid(AmountInputError.MALFORMED)
    isNegative -> AmountInputResult.Invalid(AmountInputError.NOT_POSITIVE)
    parts.decimals.length > moneda.scale ->
        AmountInputResult.Invalid(AmountInputError.TOO_MANY_DECIMALS)
    else -> parts.toAmountResult(moneda)
  }
}

private fun String.toDecimalParts(separator: Char): DecimalParts? {
  val separatorIndex = indexOf(separator)
  val integerPart = if (separatorIndex >= 0) take(separatorIndex) else this
  val decimals = if (separatorIndex >= 0) drop(separatorIndex + 1) else ""
  val hasValidShape =
      integerPart.isNotEmpty() &&
          integerPart.all(Char::isAsciiDigit) &&
          decimals.all(Char::isAsciiDigit) &&
          (separatorIndex < 0 || decimals.isNotEmpty()) &&
          indexOf(separator, separatorIndex + 1) < 0
  return DecimalParts(integerPart, decimals).takeIf { hasValidShape }
}

private fun DecimalParts.toAmountResult(moneda: Moneda): AmountInputResult {
  val minorUnits = (integerPart + decimals.padEnd(moneda.scale, '0')).toLongOrNull()
  return when {
    minorUnits == null -> AmountInputResult.Invalid(AmountInputError.OVERFLOW)
    minorUnits <= 0 -> AmountInputResult.Invalid(AmountInputError.NOT_POSITIVE)
    else -> AmountInputResult.Valid(Money(minorUnits, moneda))
  }
}

private data class DecimalParts(val integerPart: String, val decimals: String)
