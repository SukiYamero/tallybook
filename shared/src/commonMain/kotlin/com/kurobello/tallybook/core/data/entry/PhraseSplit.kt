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
  val lastEnd = lastPartEnd(reading, lastAmountEnd)
  val starts = listOf(0) + cuts.map { tokens[it.last].end }
  val ends =
      cuts.map { tokens[it.first].start } + (lastEnd?.let { tokens[it].start } ?: text.length)
  return starts.zip(ends) { start, end -> cleanPartText(text.substring(start, end)) }
}

/**
 * The run right before the next amount when only filler follows it ("cine y palomitas y 40 mil", "y
 * pagué 40 mil", "y con débito 40 mil"), otherwise the first run ("20 mil y comida y bebida 40
 * mil").
 */
private fun PhraseReading.cutBetween(from: Int, to: Int): IntRange? {
  val runs = cursor.connectorRuns(from, to)
  return runs.lastOrNull { run -> (run.last + 1 until to).all(::isFiller) } ?: runs.firstOrNull()
}

/** Words already read as a tipo verb or a shared value, stop words and punctuation. */
private fun PhraseReading.isFiller(index: Int): Boolean {
  val token = cursor.tokens[index]
  return !cursor.isFree(index) ||
      token.kind == PhraseTokenKind.PUNCT ||
      (token.kind == PhraseTokenKind.WORD && token.norm in suggestionStopWords)
}

/** The token the last part ends before: the shared tail's run, or a run the sentence ends with. */
private fun lastPartEnd(reading: PhraseReading, from: Int): Int? {
  val tokens = reading.cursor.tokens
  val runs = reading.cursor.connectorRuns(from, tokens.size)
  val trailing =
      runs.lastOrNull()?.takeIf { run ->
        (run.last + 1 until tokens.size).all { tokens[it].kind == PhraseTokenKind.PUNCT }
      }
  return sharedTailStart(reading, runs) ?: trailing?.first
}

/**
 * After the last amount, each connector run opens a chunk that ends at the next run. The tail
 * starts at the first run whose chunk opens with filler or [sharedTailWords] and then a shared
 * value, so "y bebida ayer" stays in the last part while "más lo cargué con la tarjeta" is shared.
 */
private fun sharedTailStart(reading: PhraseReading, runs: List<IntRange>): Int? {
  val chunkEnds = runs.drop(1).map { it.first } + reading.cursor.tokens.size
  return runs
      .zip(chunkEnds)
      .firstOrNull { (run, end) -> reading.opensWithSharedValue(run.last + 1 until end) }
      ?.let { (run, _) -> run.first }
}

private fun PhraseReading.opensWithSharedValue(chunk: IntRange): Boolean {
  val firstShared = chunk.firstOrNull { sharedConsumed[it] } ?: return false
  return (chunk.first until firstShared).all {
    isFiller(it) || cursor.wordAt(it) in sharedTailWords
  }
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
