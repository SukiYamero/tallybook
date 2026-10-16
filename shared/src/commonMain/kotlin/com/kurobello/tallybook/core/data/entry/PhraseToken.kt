package com.kurobello.tallybook.core.data.entry

internal enum class PhraseTokenKind {
  WORD,
  NUMBER,
  SYMBOL,
  SLASH_DATE,
  PUNCT,
}

/** [norm] is search-normalized for words and the raw text otherwise. */
internal data class PhraseToken(
    val kind: PhraseTokenKind,
    val start: Int,
    val end: Int,
    val text: String,
    val norm: String,
)

internal fun PhraseToken.isAdjacentAfter(previous: PhraseToken): Boolean = start == previous.end

/** Read access to tokens that no earlier rule has consumed. */
internal class PhraseCursor(val tokens: List<PhraseToken>, private val consumed: BooleanArray) {

  fun isFree(index: Int): Boolean = index in tokens.indices && !consumed[index]

  fun tokenAt(index: Int): PhraseToken? = tokens.getOrNull(index)?.takeIf { !consumed[index] }

  fun wordAt(index: Int): String? = tokenAt(index)?.takeIf { it.kind == PhraseTokenKind.WORD }?.norm

  fun consume(start: Int, endExclusive: Int) = consumed.fill(true, start, endExclusive)
}

internal data class PhraseHit<V>(val length: Int, val value: V)

/** Multi-word phrases keyed by their normalized words; longer phrases win over their prefixes. */
internal class PhraseTable<V>(private val entries: Map<List<String>, V>) {
  private val maxLength = entries.keys.maxOf { it.size }

  fun matchAt(cursor: PhraseCursor, index: Int): PhraseHit<V>? =
      (maxLength downTo 1).firstNotNullOfOrNull { length ->
        val words = (index until index + length).mapNotNull(cursor::wordAt)
        entries[words]?.takeIf { words.size == length }?.let { PhraseHit(length, it) }
      }
}
