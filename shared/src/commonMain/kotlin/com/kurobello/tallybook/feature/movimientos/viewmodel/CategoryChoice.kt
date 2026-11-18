package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.entry.CategorySuggestion
import com.kurobello.tallybook.core.data.entry.CategorySuggestionIndex
import com.kurobello.tallybook.core.data.entry.CorrectionRules
import com.kurobello.tallybook.core.data.entry.SuggestionConfidence
import com.kurobello.tallybook.core.data.entry.suggestionTokens
import com.kurobello.tallybook.core.model.CampoCorreccion
import com.kurobello.tallybook.core.model.TipoMovimiento

// The selected category is filtered out of the ranking, so keep one more to still fill the chips.
private const val RANKING_LIMIT = MAX_SUGGESTION_CHIPS + 1

/**
 * [history] is the exact-subject match; [read] is what the app would preselect without rules;
 * [words] are the ones the rule was looked up with.
 */
internal data class CategoryChoice(
    val query: SuggestionQuery,
    val ranking: List<CategorySuggestion>,
    val history: String?,
    val read: String?,
    val preselected: String?,
    val words: List<String>,
)

internal fun CategorySuggestionIndex.ranked(
    query: SuggestionQuery,
    candidates: SuggestionCandidates,
): List<CategorySuggestion> =
    suggest(
        subject = query.subject,
        tipo = query.tipo,
        availableIds = candidates.availableIds,
        excludedIds = candidates.excludedIds,
        limit = RANKING_LIMIT,
    )

/** The chips' order: learned offers first, then the ranking. */
internal fun CategorySuggestionIndex.suggestedCategoriaIds(
    query: SuggestionQuery,
    candidates: SuggestionCandidates,
): List<String> =
    (query.offeredCategoriaIds.filter(candidates::allows) +
            ranked(query, candidates).map(CategorySuggestion::categoriaId))
        .distinct()
        .take(RANKING_LIMIT)

/**
 * Exact-subject history outranks a word rule, which outranks the dictionary. The rule is gated on
 * what the app would otherwise preselect, since that is the reading its records corrected; a rule
 * learned against another reading is only offered.
 */
internal fun CategorySuggestionIndex.categoryChoice(
    subject: String,
    tipo: TipoMovimiento,
    candidates: SuggestionCandidates,
    rules: CorrectionRules,
    words: List<String> = suggestionTokens(subject),
): CategoryChoice {
  val ranking = ranked(SuggestionQuery(subject, tipo), candidates)
  val history = learnedCategoria(subject, candidates.availableIds, candidates.excludedIds)
  val confident =
      ranking.firstOrNull()?.takeIf { it.confidence == SuggestionConfidence.HIGH }?.categoriaId
  val read = history ?: confident
  val outcome = rules.outcome(words, CampoCorreccion.CATEGORIA, read)
  val applied = outcome.appliedValue()?.takeIf(candidates::allows)
  return CategoryChoice(
      query = SuggestionQuery(subject, tipo, outcome.offeredValues().filter(candidates::allows)),
      ranking = ranking,
      history = history,
      read = read,
      preselected = history ?: applied ?: confident,
      words = words,
  )
}
