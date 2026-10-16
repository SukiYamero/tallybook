package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.CategoryHistoryRow
import com.kurobello.tallybook.core.data.DeviceLocaleProvider
import com.kurobello.tallybook.core.data.entry.CategoryHistoryEntry
import com.kurobello.tallybook.core.data.entry.CategorySuggester
import com.kurobello.tallybook.core.data.entry.CategorySuggestion
import com.kurobello.tallybook.core.data.entry.CategorySuggestionIndex
import com.kurobello.tallybook.core.data.entry.MovimientoPhraseParser
import com.kurobello.tallybook.core.data.entry.SuggestionConfidence
import com.kurobello.tallybook.core.data.entry.incomeCategoryIds
import com.kurobello.tallybook.core.data.resolveDeviceLocale
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.ParsedMovimientoPhrase
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

internal const val MAX_SUGGESTION_CHIPS = 3

// The selected category is filtered out of the ranking, so keep one more to still fill the chips.
private const val RANKING_LIMIT = MAX_SUGGESTION_CHIPS + 1

internal data class PhraseContext(val moneda: Moneda, val locale: AppLocale, val today: LocalDate)

internal data class SuggestionQuery(val subject: String, val tipo: TipoMovimiento)

internal data class SuggestionCandidates(
    val availableIds: Set<String>,
    val excludedIds: Set<String>,
)

internal data class PhraseInterpretation(
    val parsed: ParsedMovimientoPhrase,
    val tipo: TipoMovimiento,
    val query: SuggestionQuery,
    val ranking: List<CategorySuggestion>,
    val learnedCategoriaId: String?,
) {
  val preselectedCategoriaId: String?
    get() =
        learnedCategoriaId
            ?: ranking
                .firstOrNull()
                ?.takeIf { it.confidence == SuggestionConfidence.HIGH }
                ?.categoriaId
}

/** Pure sentence interpretation; only the history indexing and live preview leave the caller. */
internal class MovimientoEntryEngine(
    private val parser: MovimientoPhraseParser,
    private val suggester: CategorySuggester,
    private val localeProvider: DeviceLocaleProvider,
    private val computationDispatcher: CoroutineDispatcher,
) {

  fun resolveLocale(preferred: AppLocale?): AppLocale =
      preferred ?: resolveDeviceLocale(localeProvider.currentLanguageTag()).appLocale

  fun dictionaryIndex(now: Instant): CategorySuggestionIndex = suggester.index(emptyList(), now)

  suspend fun buildIndex(
      history: List<CategoryHistoryRow>,
      context: PhraseContext,
      now: Instant,
  ): CategorySuggestionIndex =
      withContext(computationDispatcher) {
        suggester.index(
            history.map { row ->
              CategoryHistoryEntry(
                  subject =
                      parser
                          .parse(row.titulo, context.moneda, context.locale, context.today)
                          .subject,
                  categoriaId = row.categoriaId,
                  createdAt = row.createdAt,
              )
            },
            now,
        )
      }

  suspend fun interpretOffMain(
      text: String,
      context: PhraseContext,
      index: CategorySuggestionIndex,
      candidates: SuggestionCandidates,
  ): PhraseInterpretation =
      withContext(computationDispatcher) { interpret(text, context, index, candidates) }

  fun interpret(
      text: String,
      context: PhraseContext,
      index: CategorySuggestionIndex,
      candidates: SuggestionCandidates,
  ): PhraseInterpretation {
    val parsed = parser.parse(text, context.moneda, context.locale, context.today)
    val tipo = parsed.tipo ?: impliedTipo(parsed.subject, index, candidates)
    val query = SuggestionQuery(parsed.subject, tipo)
    val learned =
        index.learnedCategoria(query.subject, candidates.availableIds, candidates.excludedIds)
    return PhraseInterpretation(parsed, tipo, query, rank(index, query, candidates), learned)
  }

  fun rank(
      index: CategorySuggestionIndex,
      query: SuggestionQuery,
      candidates: SuggestionCandidates,
  ): List<CategorySuggestion> =
      index.suggest(
          subject = query.subject,
          tipo = query.tipo,
          availableIds = candidates.availableIds,
          excludedIds = candidates.excludedIds,
          limit = RANKING_LIMIT,
      )

  // Without a verb the sentence has no direction; a confident income category is the only hint.
  // Anything weaker must not imply it, because the income boost would then promote it to confident.
  private fun impliedTipo(
      subject: String,
      index: CategorySuggestionIndex,
      candidates: SuggestionCandidates,
  ): TipoMovimiento {
    val top =
        index
            .suggest(subject, null, candidates.availableIds, candidates.excludedIds, limit = 1)
            .firstOrNull()
            ?.takeIf { it.confidence == SuggestionConfidence.HIGH }
    return if (top?.categoriaId in incomeCategoryIds) TipoMovimiento.INGRESO
    else TipoMovimiento.GASTO
  }
}
