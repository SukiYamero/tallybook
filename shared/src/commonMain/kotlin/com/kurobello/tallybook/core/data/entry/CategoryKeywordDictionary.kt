package com.kurobello.tallybook.core.data.entry

/** [STRONG] words point at one category; [WEAK] ones are ambiguous and list several targets. */
enum class KeywordWeight {
  STRONG,
  WEAK,
}

/** A word, brand or phrase (any supported language) that hints at [categoriaId]. */
data class CategoryKeywordEntry(
    val phrase: String,
    val categoriaId: String,
    val weight: KeywordWeight,
)

internal data class DictionaryEvidence(val score: Double, val strong: Boolean)

class CategoryKeywordDictionary(entries: List<CategoryKeywordEntry>) {

  private val targetsByKey: Map<String, List<Target>>
  private val longestPhrase: Int

  init {
    val grouped = mutableMapOf<String, MutableList<Target>>()
    var longest = 1
    entries.forEach { entry ->
      val tokens = suggestionTokens(entry.phrase)
      if (tokens.isNotEmpty()) {
        longest = maxOf(longest, tokens.size)
        grouped
            .getOrPut(tokens.joinToString(" ")) { mutableListOf() }
            .add(Target(entry.categoriaId, entry.weight, tokens.size > 1))
      }
    }
    targetsByKey = grouped
    longestPhrase = longest
  }

  /**
   * Longest phrases claim their words first, so "cambio de aceite" is never also scored as the
   * ambiguous word "aceite". Evidence of one category sums across the hits that point to it.
   */
  internal fun evidence(tokens: List<String>): Map<String, DictionaryEvidence> {
    val claimed = BooleanArray(tokens.size)
    val totals = mutableMapOf<String, DictionaryEvidence>()
    for (length in minOf(longestPhrase, tokens.size) downTo 1) {
      for (start in 0..tokens.size - length) {
        val targets = findUnclaimed(tokens, start, length, claimed) ?: continue
        claimed.fill(true, start, start + length)
        targets.forEach { target ->
          val previous = totals[target.categoriaId]
          totals[target.categoriaId] =
              DictionaryEvidence(
                  score = (previous?.score ?: 0.0) + target.score,
                  strong = target.weight == KeywordWeight.STRONG || previous?.strong == true,
              )
        }
      }
    }
    return totals
  }

  private fun findUnclaimed(
      tokens: List<String>,
      start: Int,
      length: Int,
      claimed: BooleanArray,
  ): List<Target>? =
      if ((start until start + length).any { claimed[it] }) {
        null
      } else {
        targetsByKey[tokens.subList(start, start + length).joinToString(" ")]
      }

  private class Target(
      val categoriaId: String,
      val weight: KeywordWeight,
      isPhrase: Boolean,
  ) {
    val score: Double =
        when {
          weight == KeywordWeight.WEAK -> WEAK_SCORE
          isPhrase -> PHRASE_SCORE
          else -> WORD_SCORE
        }
  }

  companion object {
    val builtIn: CategoryKeywordDictionary = CategoryKeywordDictionary(builtInKeywords)
  }
}

private const val PHRASE_SCORE = 6.0
private const val WORD_SCORE = 4.0
private const val WEAK_SCORE = 1.2
