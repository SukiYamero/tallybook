package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.entry.CorrectionRules
import com.kurobello.tallybook.core.data.entry.RuleOutcome
import com.kurobello.tallybook.core.data.entry.SentenceCuenta
import com.kurobello.tallybook.core.data.entry.SentenceMedio
import com.kurobello.tallybook.core.data.entry.SentenceSelection
import com.kurobello.tallybook.core.data.entry.resolveSentenceMedio
import com.kurobello.tallybook.core.model.CampoCorreccion
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.model.MOVIMIENTO_TITLE_MAX_LENGTH
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.ParsedMovimientoPhrase
import com.kurobello.tallybook.core.model.PhraseReviewFlag
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.enums.EnumEntries
import kotlinx.datetime.LocalDate

/**
 * Per compared campo except título and structure: the parser's own reading (`leido`, before any
 * learned rule) and what the app proposed (`propuesto`), encoded as correction records store them.
 */
internal data class FieldReadings(
    val leido: Map<CampoCorreccion, String?>,
    val propuesto: Map<CampoCorreccion, String?>,
) {
  fun with(campo: CampoCorreccion, leido: String?, propuesto: String?): FieldReadings =
      FieldReadings(this.leido + (campo to leido), this.propuesto + (campo to propuesto))
}

/** Values a single agreeing correction (or disagreeing rules) offers without selecting them. */
internal data class OfferedValues(
    val categoriaIds: List<String>,
    val medios: Set<MedioPago>,
    val cuentaIds: List<String>,
)

/** The selection the parser read and the one learned rules turned it into. */
internal data class LearnedSelection(
    val read: SentenceSelection,
    val learned: SentenceSelection,
    val offeredMedios: Set<MedioPago>,
    val offeredCuentaIds: List<String>,
)

internal fun RuleOutcome.appliedValue(): String? = (this as? RuleOutcome.Applied)?.value

internal fun RuleOutcome.offeredValues(): Set<String> =
    (this as? RuleOutcome.Offered)?.values.orEmpty()

internal fun <E : Enum<E>> RuleOutcome.applied(entries: EnumEntries<E>): E? =
    appliedValue()?.let { value -> entries.firstOrNull { it.name == value } }

internal fun Money.encoded(): String = "$minorUnits ${moneda.name}"

/**
 * The cuenta rule runs first because a cuenta's type implies the medio, which then outranks a medio
 * rule just like it outranks the sentence's own method words.
 */
internal fun learnedSelection(
    parsed: ParsedMovimientoPhrase,
    tipo: TipoMovimiento,
    readCuenta: SentenceCuenta,
    cuentas: List<Cuenta>,
    outcomeOf: (CampoCorreccion, String?) -> RuleOutcome,
): LearnedSelection {
  val read =
      SentenceSelection(readCuenta, resolveSentenceMedio(parsed, tipo, readCuenta.cuenta?.tipo))
  val live = cuentas.filterNot(Cuenta::archivada).associateBy(Cuenta::id)
  val cuentaOutcome = outcomeOf(CampoCorreccion.CUENTA, readCuenta.cuenta?.id)
  val cuenta = cuentaOutcome.appliedValue()?.let(live::get) ?: readCuenta.cuenta
  val offeredCuentaIds = cuentaOutcome.offeredValues().filter { it in live && it != cuenta?.id }
  val learnedCuenta =
      SentenceCuenta(
          cuenta,
          (listOfNotNull(readCuenta.cuenta?.id) + readCuenta.alternativeIds + offeredCuentaIds)
              .distinct()
              .filter { it != cuenta?.id },
      )
  val resolvedMedio = resolveSentenceMedio(parsed, tipo, cuenta?.tipo)
  val medioOutcome = outcomeOf(CampoCorreccion.MEDIO, read.medio.medio?.name)
  val appliedMedio = medioOutcome.applied(MedioPago.entries)?.forTipo(tipo)
  val offeredMedios =
      medioOutcome
          .offeredValues()
          .mapNotNull { value -> MedioPago.entries.firstOrNull { it.name == value }?.forTipo(tipo) }
          .toSet()
  val learnedMedio =
      when {
        cuenta != null -> resolvedMedio
        appliedMedio != null -> SentenceMedio(appliedMedio, emptySet())
        resolvedMedio.medio != null -> resolvedMedio
        else -> SentenceMedio(null, resolvedMedio.candidates + offeredMedios)
      }
  return LearnedSelection(
      read = read,
      learned = SentenceSelection(learnedCuenta, learnedMedio),
      offeredMedios = if (learnedMedio.medio == null) offeredMedios else emptySet(),
      offeredCuentaIds = offeredCuentaIds,
  )
}

/** A recurrence the parser could not represent is a reading of its own, not an undecided one. */
internal fun ParsedMovimientoPhrase.withLearnedFrecuencia(
    rules: CorrectionRules,
    words: List<String>,
): ParsedMovimientoPhrase {
  if (PhraseReviewFlag.UNSUPPORTED_RECURRENCE in reviewFlags) return this
  val learned =
      rules.outcome(words, CampoCorreccion.FRECUENCIA, frecuencia?.name).applied(Frecuencia.entries)
  return if (learned == null) this else copy(frecuencia = learned)
}

/** Every campo but categoría, which the caller adds from its own choice. */
internal fun sentenceReadings(
    read: ParsedMovimientoPhrase,
    parsed: ParsedMovimientoPhrase,
    tipo: TipoMovimiento,
    selection: LearnedSelection,
    today: LocalDate,
): FieldReadings {
  val monto = parsed.monto?.encoded()
  return FieldReadings(
      leido =
          mapOf(
              CampoCorreccion.TIPO to read.tipo?.name,
              CampoCorreccion.MONTO to monto,
              CampoCorreccion.FECHA to parsed.fecha?.toString(),
              CampoCorreccion.MEDIO to selection.read.medio.medio?.name,
              CampoCorreccion.CUENTA to selection.read.cuenta.cuenta?.id,
              CampoCorreccion.FRECUENCIA to read.frecuencia?.name,
          ),
      propuesto =
          mapOf(
              CampoCorreccion.TIPO to tipo.name,
              CampoCorreccion.MONTO to monto,
              CampoCorreccion.FECHA to (parsed.fecha ?: today).toString(),
              CampoCorreccion.MEDIO to selection.learned.medio.medio?.name,
              CampoCorreccion.CUENTA to selection.learned.cuenta.cuenta?.id,
              CampoCorreccion.FRECUENCIA to parsed.frecuencia?.name,
          ),
  )
}

internal fun partInterpretation(
    text: String,
    parsed: ParsedMovimientoPhrase,
    shared: FieldReadings,
    category: CategoryChoice,
): PartInterpretation {
  val monto = parsed.monto?.encoded()
  return PartInterpretation(
      titulo = text.trim().take(MOVIMIENTO_TITLE_MAX_LENGTH),
      monto = parsed.monto,
      amountIsAmbiguous = PhraseReviewFlag.AMBIGUOUS_AMOUNT in parsed.reviewFlags,
      query = category.query,
      ranking = category.ranking,
      learnedCategoriaId = category.history,
      preselectedCategoriaId = category.preselected,
      readings =
          shared
              .with(CampoCorreccion.MONTO, monto, monto)
              .with(CampoCorreccion.CATEGORIA, category.read, category.preselected),
      offered = OfferedValues(category.query.offeredCategoriaIds, emptySet(), emptyList()),
  )
}
