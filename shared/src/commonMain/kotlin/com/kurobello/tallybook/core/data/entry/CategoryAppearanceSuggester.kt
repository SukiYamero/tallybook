package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.data.CategorySeed
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint

data class CategoryAppearance(val icono: CategoryIcon, val color: CategoryTint)

/**
 * Borrows the icon and tint of the seed whose dictionary evidence best matches a category name.
 * Ties go to strong evidence, then to the earlier seed in [seeds], so the result never depends on
 * map iteration order. Otros is never borrowed: it means "uncategorized", not a look.
 */
class CategoryAppearanceSuggester(
    private val dictionary: CategoryKeywordDictionary,
    seeds: List<CategorySeed>,
) {

  private val rankedSeeds: Map<String, IndexedValue<CategorySeed>> =
      seeds.withIndex().filter { it.value.id != CATEGORIA_OTROS_ID }.associateBy { it.value.id }

  fun suggest(nombre: String): CategoryAppearance {
    val best =
        dictionary
            .evidence(suggestionTokens(nombre))
            .mapNotNull { (id, evidence) -> rankedSeeds[id]?.let { Candidate(it, evidence) } }
            .minWithOrNull(
                compareByDescending<Candidate> { it.evidence.score }
                    .thenByDescending { it.evidence.strong }
                    .thenBy { it.seed.index }
            ) ?: return UNMATCHED
    return CategoryAppearance(best.seed.value.icono, best.seed.value.color)
  }

  private class Candidate(val seed: IndexedValue<CategorySeed>, val evidence: DictionaryEvidence)
}

private val UNMATCHED = CategoryAppearance(CategoryIcon.TAG, CategoryTint.NEUTRAL)
