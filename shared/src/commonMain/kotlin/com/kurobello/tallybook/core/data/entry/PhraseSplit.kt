package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.Moneda

internal const val MAX_PHRASE_PARTS = 10

private val partEdgeCharacters: Set<Char> = setOf(',', ';', '.', '+')

/**
 * Cuts [text] at a connector run before each amount after the first, and drops the shared tail
 * after the last amount. Null unless the sentence has 2 to [MAX_PHRASE_PARTS] amounts that are not
 * quantities, with a connector between each pair.
 */
internal fun splitPhraseParts(text: String, reading: PhraseReading, moneda: Moneda): List<String>? {
  val amounts = scanStatedAmounts(reading.cursor, moneda)
  val cuts = amounts.zipWithNext { a, b -> reading.cutBetween(a.endExclusive, b.start) }
  return if (amounts.size in 2..MAX_PHRASE_PARTS && null !in cuts) {
    partTexts(text, reading, cuts.filterNotNull(), amounts.last().endExclusive)
  } else {
    null
  }
}

/** [cuts] are connector runs; the text between consecutive runs is one part. */
private fun partTexts(
    text: String,
    reading: PhraseReading,
    cuts: List<IntRange>,
    lastAmountEnd: Int,
): List<String> {
  val tokens = reading.cursor.tokens
  val tailStart = sharedTailStart(reading, lastAmountEnd)
  val starts = listOf(0) + cuts.map { tokens[it.last].end }
  val ends =
      cuts.map { tokens[it.first].start } + (tailStart?.let { tokens[it].start } ?: text.length)
  return starts.zip(ends) { start, end -> cleanPartText(text.substring(start, end)) }
}

/**
 * The run right before the next amount when only stop words or tipo verbs follow it ("cine y
 * palomitas y 40 mil", "y pagué 40 mil"), otherwise the first run ("20 mil y comida y bebida 40
 * mil").
 */
private fun PhraseReading.cutBetween(from: Int, to: Int): IntRange? {
  val runs = cursor.connectorRuns(from, to)
  return runs.lastOrNull { run -> (run.last + 1 until to).all(::isCutFiller) } ?: runs.firstOrNull()
}

private fun PhraseReading.isCutFiller(index: Int): Boolean {
  val token = cursor.tokens[index]
  return tipo.verbTokens[index] ||
      (token.kind == PhraseTokenKind.WORD && token.norm in suggestionStopWords)
}

/**
 * After the last amount, each connector run opens a chunk that ends at the next run. The tail
 * starts at the first run whose own chunk holds a shared value, so "y bebida" stays in the last
 * part even when "y lo pagué con débito" follows it.
 */
private fun sharedTailStart(reading: PhraseReading, from: Int): Int? {
  val size = reading.cursor.tokens.size
  val runs = reading.cursor.connectorRuns(from, size)
  val chunkEnds = runs.drop(1).map { it.first } + size
  return runs
      .zip(chunkEnds)
      .firstOrNull { (run, end) -> (run.last + 1 until end).any { reading.sharedConsumed[it] } }
      ?.let { (run, _) -> run.first }
}

/** Adjacent connectors ("y también", ", y") act as one, so none is left at a part's edge. */
private fun PhraseCursor.connectorRuns(from: Int, to: Int): List<IntRange> =
    (from until to).filter(::isPartConnector).fold(mutableListOf()) { runs, index ->
      val last = runs.lastOrNull()
      if (last != null && last.last == index - 1) runs[runs.lastIndex] = last.first..index
      else runs += index..index
      runs
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
