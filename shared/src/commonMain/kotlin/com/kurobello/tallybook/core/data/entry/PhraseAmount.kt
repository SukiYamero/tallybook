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

private data class AmountCandidate(val money: Money, val start: Int, val endExclusive: Int)

internal fun findPhraseAmount(cursor: PhraseCursor, moneda: Moneda): PhraseAmount? {
  val candidates = PhraseAmountScanner(cursor, moneda).scan()
  return candidates
      .maxByOrNull { it.money.minorUnits }
      ?.let { PhraseAmount(it.money, it.start, it.endExclusive, isAmbiguous = candidates.size > 1) }
}

private class PhraseAmountScanner(private val cursor: PhraseCursor, private val moneda: Moneda) {

  fun scan(): List<AmountCandidate> {
    val candidates = mutableListOf<AmountCandidate>()
    var index = 0
    while (index < cursor.tokens.size) {
      val candidate = candidateAt(index)
      if (candidate != null) candidates += candidate
      index = candidate?.endExclusive ?: (index + 1)
    }
    return candidates
  }

  private fun candidateAt(index: Int): AmountCandidate? {
    val hasLeadingSymbol = cursor.tokenAt(index)?.kind == PhraseTokenKind.SYMBOL
    val coreStart = if (hasLeadingSymbol) index + 1 else index
    val core = numeric(coreStart) ?: spelled(coreStart, hasLeadingSymbol)
    return core?.let {
      val hasTrailingCurrency = isCurrencyAt(it.endExclusive)
      AmountCandidate(it.money, index, it.endExclusive + (if (hasTrailingCurrency) 1 else 0))
    }
  }

  private fun isCurrencyAt(index: Int): Boolean =
      cursor.tokenAt(index)?.kind == PhraseTokenKind.SYMBOL || cursor.wordAt(index) in currencyWords

  private fun numeric(index: Int): AmountCandidate? {
    val token = cursor.tokenAt(index)?.takeIf { it.kind == PhraseTokenKind.NUMBER }
    val factor = multiplierAfter(index)
    val money = token?.let { splitDecimal(it.text) }?.toMoney(factor ?: 1L, moneda)
    return money?.let { AmountCandidate(it, index, index + 1 + (if (factor != null) 1 else 0)) }
  }

  private fun multiplierAfter(index: Int): Long? {
    val word = cursor.wordAt(index + 1)
    return digitMultipliers[word]
        ?: adjacentMultipliers[word]?.takeIf {
          cursor.tokens[index + 1].isAdjacentAfter(cursor.tokens[index])
        }
  }

  private fun spelled(index: Int, hasLeadingSymbol: Boolean): AmountCandidate? {
    // A multiplier right after digits belongs to them, even when that number was rejected.
    val followsDigits = cursor.tokens.getOrNull(index - 1)?.kind == PhraseTokenKind.NUMBER
    val startsWithBareMagnitude = cursor.wordAt(index) in bareMagnitudeWords
    return if (followsDigits || startsWithBareMagnitude) null
    else spelledAt(index, hasLeadingSymbol)
  }

  private fun spelledAt(index: Int, hasLeadingSymbol: Boolean): AmountCandidate? {
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
        ?.let { AmountCandidate(it, index, endExclusive) }
  }

  private fun spelledWordAt(index: Int): String? =
      cursor.wordAt(index)?.takeIf {
        it in numberWordValues || it in scaleWords || it in numberWordConnectors
      }
}
