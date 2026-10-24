package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.CategoryHistoryRow
import com.kurobello.tallybook.core.data.DeviceLocaleProvider
import com.kurobello.tallybook.core.data.entry.CategoryHistoryEntry
import com.kurobello.tallybook.core.data.entry.CategorySuggester
import com.kurobello.tallybook.core.data.entry.CategorySuggestion
import com.kurobello.tallybook.core.data.entry.CategorySuggestionIndex
import com.kurobello.tallybook.core.data.entry.CuentaMatcher
import com.kurobello.tallybook.core.data.entry.EtiquetaMatcher
import com.kurobello.tallybook.core.data.entry.MovimientoPhraseParser
import com.kurobello.tallybook.core.data.entry.SentenceSelection
import com.kurobello.tallybook.core.data.entry.SuggestionConfidence
import com.kurobello.tallybook.core.data.entry.incomeCategoryIds
import com.kurobello.tallybook.core.data.entry.resolveSentenceCuenta
import com.kurobello.tallybook.core.data.entry.resolveSentenceMedio
import com.kurobello.tallybook.core.data.resolveDeviceLocale
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.MAX_ETIQUETAS_POR_MOVIMIENTO
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

/** [moneda] reads the amount until the sentence's own cuenta decides another currency. */
internal data class PhraseContext(val moneda: Moneda, val locale: AppLocale, val today: LocalDate)

/** The user's own names a sentence may mention: tags and accounts. */
internal class SentenceCatalogs(val etiquetas: List<Etiqueta>, val cuentas: SentenceCuentas)

/**
 * The live cuentas a sentence may name. [monedaFor] is the currency the draft ends up in once the
 * caller layers the detected medio and cuenta over its own selection.
 */
internal class SentenceCuentas(
    val cuentas: List<Cuenta>,
    val primary: Moneda,
    val monedaFor: (SentenceSelection, TipoMovimiento) -> Moneda,
)

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
    val etiquetaIds: List<String>,
    val sentence: SentenceSelection,
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
    private val etiquetaMatcher: EtiquetaMatcher,
    private val cuentaMatcher: CuentaMatcher,
    private val localeProvider: DeviceLocaleProvider,
    private val computationDispatcher: CoroutineDispatcher,
) {

  fun resolveLocale(preferred: AppLocale?): AppLocale =
      preferred ?: resolveDeviceLocale(localeProvider.currentLanguageTag()).appLocale

  fun dictionaryIndex(now: Instant): CategorySuggestionIndex = suggester.index(emptyList(), now)

  /** History subjects drop account names like live ones do, so learned subjects still match. */
  suspend fun buildIndex(
      history: List<CategoryHistoryRow>,
      context: PhraseContext,
      now: Instant,
      cuentas: List<Cuenta>,
  ): CategorySuggestionIndex =
      withContext(computationDispatcher) {
        suggester.index(
            history.map { row ->
              CategoryHistoryEntry(
                  subject = parse(row.titulo, context, context.moneda, cuentas).subject,
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
      catalogs: SentenceCatalogs,
  ): PhraseInterpretation =
      withContext(computationDispatcher) { interpret(text, context, index, candidates, catalogs) }

  /**
   * Account names are matched before parsing so their words never become an amount or a date. The
   * amount is read again when the layered selection lands in another currency than [context]'s.
   */
  fun interpret(
      text: String,
      context: PhraseContext,
      index: CategorySuggestionIndex,
      candidates: SuggestionCandidates,
      catalogs: SentenceCatalogs,
  ): PhraseInterpretation {
    val cuentas = catalogs.cuentas
    val matches = cuentaMatcher.match(text, cuentas.cuentas)
    val reserved = matches.map { it.start until it.endExclusive }
    val sentenceCuenta = resolveSentenceCuenta(matches, cuentas.cuentas, cuentas.primary)
    val first = parser.parse(text, context.moneda, context.locale, context.today, reserved)
    val tipo = first.tipo ?: impliedTipo(first.subject, index, candidates)
    val sentence =
        SentenceSelection(
            sentenceCuenta,
            resolveSentenceMedio(first, tipo, sentenceCuenta.cuenta?.tipo),
        )
    val moneda = cuentas.monedaFor(sentence, tipo)
    val parsed =
        if (moneda == context.moneda) first
        else parser.parse(text, moneda, context.locale, context.today, reserved)
    val query = SuggestionQuery(parsed.subject, tipo)
    val learned =
        index.learnedCategoria(query.subject, candidates.availableIds, candidates.excludedIds)
    return PhraseInterpretation(
        parsed = parsed,
        tipo = tipo,
        query = query,
        ranking = rank(index, query, candidates),
        learnedCategoriaId = learned,
        etiquetaIds =
            etiquetaMatcher.match(text, catalogs.etiquetas).take(MAX_ETIQUETAS_POR_MOVIMIENTO),
        sentence = sentence,
    )
  }

  /** The sentence read in [moneda], e.g. to re-derive its amount for another cuenta. */
  fun parse(
      text: String,
      context: PhraseContext,
      moneda: Moneda,
      cuentas: List<Cuenta>,
  ): ParsedMovimientoPhrase {
    val reserved = cuentaMatcher.match(text, cuentas).map { it.start until it.endExclusive }
    return parser.parse(text, moneda, context.locale, context.today, reserved)
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
