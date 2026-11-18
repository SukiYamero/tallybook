package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money

/** The winning amount covering tokens `[start, endExclusive)`. */
internal data class PhraseAmount(
    val money: Money,
    val start: Int,
    val endExclusive: Int,
    val isAmbiguous: Boolean,
)

internal data class PhraseAmountCandidate(val money: Money, val start: Int, val endExclusive: Int)

/** Every amount candidate except small counts before a thing ("2 hamburguesas"). */
internal fun scanStatedAmounts(cursor: PhraseCursor, moneda: Moneda): List<PhraseAmountCandidate> =
    PhraseAmountScanner(cursor, moneda).scan().filterNot { isQuantityShaped(cursor, it) }

// A bare 1-2 digit number covers exactly one token: a factor, symbol or currency would widen it.
// A part connector after it ("propina 20 y almuerzo 18") closes a movimiento rather than naming a
// thing, and reading it as a quantity would let that part re-parse to a different amount.
private fun isQuantityShaped(cursor: PhraseCursor, candidate: PhraseAmountCandidate): Boolean {
  val token = cursor.tokenAt(candidate.start)
  val next = cursor.wordAt(candidate.endExclusive)
  return token != null &&
      candidate.endExclusive == candidate.start + 1 &&
      token.kind == PhraseTokenKind.NUMBER &&
      token.text.length <= MAX_QUANTITY_DIGITS &&
      token.text.all(Char::isDigit) &&
      next != null &&
      next !in suggestionStopWords &&
      next !in partConnectors &&
      next !in digitMultipliers &&
      next !in adjacentMultipliers &&
      next !in currencyWords
}

private const val MAX_QUANTITY_DIGITS = 2

/**
 * The largest stated amount, or the largest count when the sentence states none. A dropped count at
 * least as large as the amount ("45 dinner, 5 dollars tip") may have been the amount, so it keeps
 * the result ambiguous.
 */
internal fun findPhraseAmount(cursor: PhraseCursor, moneda: Moneda): PhraseAmount? {
  val (quantities, stated) =
      PhraseAmountScanner(cursor, moneda).scan().partition { isQuantityShaped(cursor, it) }
  val kept = stated.ifEmpty { quantities }
  val winner = kept.maxByOrNull { it.money.minorUnits } ?: return null
  val isAmbiguous =
      kept.size > 1 ||
          (stated.isNotEmpty() && quantities.any { it.money.minorUnits >= winner.money.minorUnits })
  return PhraseAmount(winner.money, winner.start, winner.endExclusive, isAmbiguous)
}

private class PhraseAmountScanner(private val cursor: PhraseCursor, private val moneda: Moneda) {

  fun scan(): List<PhraseAmountCandidate> {
    val candidates = mutableListOf<PhraseAmountCandidate>()
    var index = 0
    while (index < cursor.tokens.size) {
      val candidate = candidateAt(index)
      if (candidate != null) candidates += candidate
      index = candidate?.endExclusive ?: (index + 1)
    }
    return candidates
  }

  private fun candidateAt(index: Int): PhraseAmountCandidate? {
    val hasLeadingSymbol = cursor.tokenAt(index)?.kind == PhraseTokenKind.SYMBOL
    val coreStart = if (hasLeadingSymbol) index + 1 else index
    val core = numeric(coreStart) ?: spelled(coreStart, hasLeadingSymbol)
    return core?.let {
      val hasTrailingCurrency = isCurrencyAt(it.endExclusive)
      PhraseAmountCandidate(it.money, index, it.endExclusive + (if (hasTrailingCurrency) 1 else 0))
    }
  }

  private fun isCurrencyAt(index: Int): Boolean =
      cursor.tokenAt(index)?.kind == PhraseTokenKind.SYMBOL || cursor.wordAt(index) in currencyWords

  private fun numeric(index: Int): PhraseAmountCandidate? {
    val token = cursor.tokenAt(index)?.takeIf { it.kind == PhraseTokenKind.NUMBER }
    val factor = multiplierAfter(index)
    val money = token?.let { splitDecimal(it.norm) }?.toMoney(factor ?: 1L, moneda)
    return money?.let {
      PhraseAmountCandidate(it, index, index + 1 + (if (factor != null) 1 else 0))
    }
  }

  private fun multiplierAfter(index: Int): Long? {
    val word = cursor.wordAt(index + 1)
    return digitMultipliers[word]
        ?: adjacentMultipliers[word]?.takeIf {
          cursor.tokens[index + 1].isAdjacentAfter(cursor.tokens[index])
        }
  }

  private fun spelled(index: Int, hasLeadingSymbol: Boolean): PhraseAmountCandidate? {
    // A multiplier right after digits belongs to them, even when that number was rejected.
    val followsDigits = cursor.tokens.getOrNull(index - 1)?.kind == PhraseTokenKind.NUMBER
    val startsWithBareMagnitude = cursor.wordAt(index) in bareMagnitudeWords
    return if (followsDigits || startsWithBareMagnitude) null
    else spelledAt(index, hasLeadingSymbol)
  }

  private fun spelledAt(index: Int, hasLeadingSymbol: Boolean): PhraseAmountCandidate? {
    val words =
        generateSequence(index) { it + 1 }
            .map(::spelledWordAt)
            .takeWhile { it != null }
            .filterNotNull()
            .toList()
    val number = evaluateNumberWords(words) ?: return null
    val slangIndex = index + number.wordCount
    val slangFactor = slangMultipliers[cursor.wordAt(slangIndex)]
    val endExclusive = slangIndex + (if (slangFactor != null) 1 else 0)
    val isMarked = hasLeadingSymbol || slangFactor != null || isCurrencyAt(endExclusive)
    val money = DecimalDigits(number.amount.toString(), "").toMoney(slangFactor ?: 1L, moneda)
    return money
        ?.takeUnless { number.isLoneUnit && !isMarked }
        ?.let { PhraseAmountCandidate(it, index, endExclusive) }
  }

  private fun spelledWordAt(index: Int): String? =
      cursor.wordAt(index)?.takeIf {
        it in numberWordValues || it in scaleWords || it in numberWordConnectors
      }
}
