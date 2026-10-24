package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.text.normalizeSearchText

/** A search-normalized word of a raw text and its character range `[start, endExclusive)`. */
internal data class SearchWord(val norm: String, val start: Int, val endExclusive: Int)

// Combining marks stay inside the word so a decomposed "é" or "ñ" folds like a composed one.
private val WORD = Regex("[\\p{L}\\p{N}][\\p{L}\\p{N}\\p{Mn}]*")

internal fun searchWords(text: String): List<SearchWord> =
    WORD.findAll(text)
        .map { SearchWord(normalizeSearchText(it.value), it.range.first, it.range.last + 1) }
        .toList()

/** The normalized words of a user-given name, the form [startsOf] compares against. */
internal fun nameWords(name: String): List<String> = searchWords(name).map(SearchWord::norm)

/** Word indices where [sequence] appears as consecutive whole words. */
internal fun List<SearchWord>.startsOf(sequence: List<String>): List<Int> {
  if (sequence.isEmpty() || sequence.size > size) return emptyList()
  return (0..size - sequence.size).filter { start ->
    sequence.indices.all { offset -> this[start + offset].norm == sequence[offset] }
  }
}
