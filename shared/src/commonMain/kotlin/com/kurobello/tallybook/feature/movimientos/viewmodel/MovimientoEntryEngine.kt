package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.CategoryHistoryRow
import com.kurobello.tallybook.core.data.DeviceLocaleProvider
import com.kurobello.tallybook.core.data.entry.CategoryHistoryEntry
import com.kurobello.tallybook.core.data.entry.CategorySuggester
import com.kurobello.tallybook.core.data.entry.CategorySuggestion
import com.kurobello.tallybook.core.data.entry.CategorySuggestionIndex
import com.kurobello.tallybook.core.data.entry.CorrectionRules
import com.kurobello.tallybook.core.data.entry.CuentaMatcher
import com.kurobello.tallybook.core.data.entry.EtiquetaMatcher
import com.kurobello.tallybook.core.data.entry.MovimientoPhraseParser
import com.kurobello.tallybook.core.data.entry.SentenceSelection
import com.kurobello.tallybook.core.data.entry.SuggestionConfidence
import com.kurobello.tallybook.core.data.entry.incomeCategoryIds
import com.kurobello.tallybook.core.data.entry.resolveSentenceCuenta
import com.kurobello.tallybook.core.data.entry.resolveSentenceMedio
import com.kurobello.tallybook.core.data.entry.suggestionTokens
import com.kurobello.tallybook.core.data.resolveDeviceLocale
import com.kurobello.tallybook.core.data.voice.dictationLanguageTag
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.CampoCorreccion
import com.kurobello.tallybook.core.model.CorreccionFrase
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.MAX_ETIQUETAS_POR_MOVIMIENTO
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.ParsedMovimientoPhrase
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

internal const val MAX_SUGGESTION_CHIPS = 3

/** [moneda] reads the amount until the sentence's own cuenta decides another currency. */
internal data class PhraseContext(val moneda: Moneda, val locale: AppLocale, val today: LocalDate)

/**
 * What the user's own data tells a sentence: the tags and accounts it may name, and the [rules]
 * learned from their corrections.
 */
internal class SentenceCatalogs(
    val etiquetas: List<Etiqueta>,
    val cuentas: SentenceCuentas,
    val rules: CorrectionRules,
)

/**
 * The live cuentas a sentence may name. [monedaFor] is the currency the draft ends up in once the
 * caller layers the detected medio and cuenta over its own selection.
 */
internal class SentenceCuentas(
    val cuentas: List<Cuenta>,
    val primary: Moneda,
    val monedaFor: (SentenceSelection, TipoMovimiento) -> Moneda,
)

/** [offeredCategoriaIds] are learned categories shown ahead of the ranked suggestions. */
internal data class SuggestionQuery(
    val subject: String,
    val tipo: TipoMovimiento,
    val offeredCategoriaIds: List<String> = emptyList(),
)

internal data class SuggestionCandidates(
    val availableIds: Set<String>,
    val excludedIds: Set<String>,
) {
  fun allows(categoriaId: String): Boolean =
      categoriaId in availableIds && categoriaId !in excludedIds
}

/** One movimiento of a sentence that states several amounts; the rest is shared by the sentence. */
internal data class PartInterpretation(
    val titulo: String,
    val monto: Money?,
    val amountIsAmbiguous: Boolean,
    val query: SuggestionQuery,
    val ranking: List<CategorySuggestion>,
    val learnedCategoriaId: String?,
    val preselectedCategoriaId: String?,
    /** This part's own words, which its categoría rule was looked up with. */
    val words: List<String>,
    /** The sentence's shared readings with this part's own monto and categoría. */
    val readings: FieldReadings,
)

/**
 * [parsed] already carries a learned frecuencia; [readings] keep what the parser itself read.
 * [learnedCategoriaId] is the exact-subject history match, which outranks any word rule. [words]
 * are the ones every learned rule was looked up with, so corrections must record these.
 */
internal data class PhraseInterpretation(
    val parsed: ParsedMovimientoPhrase,
    val tipo: TipoMovimiento,
    val query: SuggestionQuery,
    val ranking: List<CategorySuggestion>,
    val learnedCategoriaId: String?,
    val preselectedCategoriaId: String?,
    val etiquetaIds: List<String>,
    val sentence: SentenceSelection,
    val words: List<String>,
    val readings: FieldReadings,
    /** Empty when the sentence is a single movimiento. */
    val parts: List<PartInterpretation> = emptyList(),
)

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

  fun dictationLanguageFor(preferred: AppLocale?): String =
      dictationLanguageTag(resolveLocale(preferred))

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

  /** Records are never pruned, so the rules are built off the main thread like the index. */
  suspend fun learnRules(records: List<CorreccionFrase>, now: Instant): CorrectionRules =
      withContext(computationDispatcher) { CorrectionRules.from(records, now) }

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
   * amount is read again when the layered selection lands in another currency than [context]'s. A
   * learned tipo is decided first, since the ranking and the medio depend on it.
   */
  fun interpret(
      text: String,
      context: PhraseContext,
      index: CategorySuggestionIndex,
      candidates: SuggestionCandidates,
      catalogs: SentenceCatalogs,
  ): PhraseInterpretation {
    val cuentas = catalogs.cuentas
    val rules = catalogs.rules
    val matches = cuentaMatcher.match(text, cuentas.cuentas)
    val reserved = matches.map { it.start until it.endExclusive }
    val first = parser.parse(text, context.moneda, context.locale, context.today, reserved)
    val words = suggestionTokens(first.subject)
    val readTipo = first.tipo ?: impliedTipo(first.subject, index, candidates)
    val tipo =
        rules.outcome(words, CampoCorreccion.TIPO, first.tipo?.name).applied(TipoMovimiento.entries)
            ?: readTipo
    val readCuenta = resolveSentenceCuenta(matches, cuentas.cuentas, cuentas.primary)
    val readSelection =
        SentenceSelection(
            readCuenta,
            resolveSentenceMedio(first, readTipo, readCuenta.cuenta?.tipo),
        )
    val selection =
        learnedSelection(first, tipo, readSelection, cuentas.cuentas) { campo, leido ->
          rules.outcome(words, campo, leido)
        }
    val moneda = cuentas.monedaFor(selection.learned, tipo)
    val read =
        if (moneda == context.moneda) first
        else parser.parse(text, moneda, context.locale, context.today, reserved)
    val parsed = read.withLearnedFrecuencia(rules, words)
    val category = index.categoryChoice(parsed.subject, tipo, candidates, rules, words)
    val readings =
        sentenceReadings(read, parsed, tipo, selection, context.today)
            .with(CampoCorreccion.CATEGORIA, category.read, category.preselected)
    return PhraseInterpretation(
        parsed = parsed,
        tipo = tipo,
        query = category.query,
        ranking = category.ranking,
        learnedCategoriaId = category.history,
        preselectedCategoriaId = category.preselected,
        etiquetaIds =
            etiquetaMatcher.match(text, catalogs.etiquetas).take(MAX_ETIQUETAS_POR_MOVIMIENTO),
        sentence = selection.learned,
        words = words,
        readings = readings,
        parts =
            parser.split(text, moneda, context.locale, context.today, reserved).orEmpty().map {
                partText ->
              val part = parse(partText, context, moneda, cuentas.cuentas)
              partInterpretation(
                  partText,
                  part,
                  readings,
                  index.categoryChoice(part.subject, tipo, candidates, rules),
              )
            },
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
