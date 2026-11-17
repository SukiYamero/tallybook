package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.Moneda

internal const val MAX_PHRASE_PARTS = 10

private val partEdgeCharacters: Set<Char> = setOf(',', ';', '.', '+')

/**
 * Cuts [text] at the connector before each amount after the first, and drops a trailing shared tail
 * (a connector followed by medio, account, date or cadence words). Null unless that yields 2 to
 * [MAX_PHRASE_PARTS] parts.
 */
internal fun splitPhraseParts(text: String, reading: PhraseReading, moneda: Moneda): List<String>? {
  val amounts = scanPhraseAmounts(reading.cursor, moneda)
  val cuts = amounts.zipWithNext { a, b -> reading.cursor.cutBetween(a.endExclusive, b.start) }
  return if (amounts.size in 2..MAX_PHRASE_PARTS && null !in cuts) {
    partTexts(text, reading, cuts.filterNotNull(), amounts.last().endExclusive)
  } else {
    null
  }
}

/** [cuts] are connector token indices; the text between consecutive cuts is one part. */
private fun partTexts(
    text: String,
    reading: PhraseReading,
    cuts: List<Int>,
    lastAmountEnd: Int,
): List<String> {
  val tokens = reading.cursor.tokens
  val tailStart = sharedTailStart(reading, lastAmountEnd)
  val starts = listOf(0) + cuts.map { tokens[it].end }
  val ends = cuts.map { tokens[it].start } + (tailStart?.let { tokens[it].start } ?: text.length)
  return starts.zip(ends) { start, end -> cleanPartText(text.substring(start, end)) }
}

/**
 * The connector right before the next amount when only consumed or stop words follow it ("cine y
 * palomitas y 40 mil"), otherwise the first one ("20 mil y comida y bebida 40 mil").
 */
private fun PhraseCursor.cutBetween(from: Int, to: Int): Int? {
  val connectors = (from until to).filter(::isPartConnector)
  return connectors.lastOrNull { connector ->
    (connector + 1 until to).all { !isFree(it) || wordAt(it) in suggestionStopWords }
  } ?: connectors.firstOrNull()
}

private fun sharedTailStart(reading: PhraseReading, from: Int): Int? {
  val size = reading.cursor.tokens.size
  return (from until size).firstOrNull { connector ->
    reading.cursor.isPartConnector(connector) &&
        (connector + 1 until size).any { reading.sharedConsumed[it] }
  }
}

private fun PhraseCursor.isPartConnector(index: Int): Boolean {
  val token = tokenAt(index) ?: return false
  return when (token.kind) {
    PhraseTokenKind.WORD -> token.norm in partConnectors
    PhraseTokenKind.PUNCT -> token.norm in partConnectorPunctuation
    else -> false
  }
}

private fun cleanPartText(part: String): String =
    part.trim { it.isWhitespace() || it in partEdgeCharacters }.replace(whitespaceRuns, " ")
