package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.ParsedMovimientoPhrase
import com.kurobello.tallybook.core.model.PhraseReviewFlag
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.datetime.LocalDate

internal val whitespaceRuns = Regex("\\s+")

/**
 * Everything read from a sentence before its amount. [sharedConsumed] marks the tokens of medio,
 * reserved spans, recurrence and date: the attributes every part of a split sentence shares.
 */
internal class PhraseReading(
    val cursor: PhraseCursor,
    val sharedConsumed: BooleanArray,
    val tipo: TipoMovimiento?,
    val medio: PhraseMedio,
    val recurrence: PhraseRecurrence?,
    val date: PhraseDate?,
)

/**
 * Derives tipo, monto, fecha, medio, frecuencia and subject from one sentence. Pure and
 * deterministic: the vocabulary is the union of every supported language and [AppLocale] only
 * decides numeric date order. The medio is the words' own reading; the final tipo and a named
 * account may still change it, see [resolveSentenceMedio].
 */
class MovimientoPhraseParser {

  /**
   * [reservedSpans] are character ranges of [text] that another matcher already owns, such as an
   * account name: they never become a date or an amount and they leave the subject.
   */
  fun parse(
      text: String,
      moneda: Moneda,
      locale: AppLocale,
      today: LocalDate,
      reservedSpans: List<IntRange> = emptyList(),
  ): ParsedMovimientoPhrase {
    val reading = read(text, locale, today, reservedSpans)
    val cursor = reading.cursor
    val date = reading.date
    val recurrence = reading.recurrence
    val amount =
        findPhraseAmount(cursor, moneda)?.also { cursor.consume(it.start, it.endExclusive) }
    val isFuture = date != null && date.date > today
    return ParsedMovimientoPhrase(
        tipo = reading.tipo,
        monto = amount?.money,
        fecha = if (date != null) date.date.takeUnless { isFuture } else recurrence?.anchor,
        subject = remainingText(text, cursor),
        reviewFlags =
            setOfNotNull(
                PhraseReviewFlag.AMBIGUOUS_AMOUNT.takeIf { amount?.isAmbiguous == true },
                PhraseReviewFlag.FUTURE_DATE_IGNORED.takeIf { isFuture },
                PhraseReviewFlag.UNSUPPORTED_RECURRENCE.takeIf {
                  recurrence != null && recurrence.frecuencia == null
                },
            ),
        medio = reading.medio.medio,
        medioCandidates = reading.medio.candidates,
        frecuencia = recurrence?.frecuencia,
    )
  }

  /**
   * The texts of the movimientos [text] names, one per amount and in order, without the shared
   * tail; null when the sentence is a single movimiento. Takes the same arguments as [parse].
   */
  fun split(
      text: String,
      moneda: Moneda,
      locale: AppLocale,
      today: LocalDate,
      reservedSpans: List<IntRange> = emptyList(),
  ): List<String>? = splitPhraseParts(text, read(text, locale, today, reservedSpans), moneda)

  private fun read(
      text: String,
      locale: AppLocale,
      today: LocalDate,
      reservedSpans: List<IntRange>,
  ): PhraseReading {
    val tokens = tokenizePhrase(text)
    val consumed = BooleanArray(tokens.size)
    val cursor = PhraseCursor(tokens, consumed)
    val tipo = consumeTipoVerbs(cursor)
    val consumedByTipo = consumed.copyOf()
    // Before the reserved spans: the seeded cash account is named "Efectivo", and reserving it
    // first would leave the "en" of "en efectivo" behind in the subject.
    val medio = consumePhraseMedio(cursor)
    consumeReserved(cursor, reservedSpans)
    // Before the date and the amount: "cada lunes" is not a date and the 30 of "cada 30 días" is
    // not the amount.
    val recurrence = consumePhraseRecurrence(cursor, today)
    val date =
        findPhraseDate(cursor, today, locale)?.also { cursor.consume(it.start, it.endExclusive) }
    val sharedConsumed = BooleanArray(tokens.size) { consumed[it] && !consumedByTipo[it] }
    return PhraseReading(cursor, sharedConsumed, tipo, medio, recurrence, date)
  }

  /** The first verb decides the tipo; every verb leaves the subject. */
  private fun consumeTipoVerbs(cursor: PhraseCursor): TipoMovimiento? {
    var tipo: TipoMovimiento? = null
    var index = 0
    while (index < cursor.tokens.size) {
      val hit = tipoVerbs.matchAt(cursor, index)
      if (hit != null) {
        tipo = tipo ?: hit.value
        cursor.consume(index, index + hit.length)
      }
      index += hit?.length ?: 1
    }
    return tipo
  }

  private fun consumeReserved(cursor: PhraseCursor, spans: List<IntRange>) {
    cursor.tokens.forEachIndexed { index, token ->
      if (spans.any { token.start <= it.last && it.first < token.end }) {
        cursor.consume(index, index + 1)
      }
    }
  }

  /** Keeps the user's words, spacing and inner punctuation; only the edges are cleaned. */
  private fun remainingText(text: String, cursor: PhraseCursor): String {
    val tokens = cursor.tokens
    val segments = mutableListOf<String>()
    var segmentStart = -1
    for (index in 0..tokens.size) {
      val isRemaining = cursor.isFree(index)
      if (isRemaining && segmentStart < 0) segmentStart = index
      if (!isRemaining && segmentStart >= 0) {
        segments += text.substring(tokens[segmentStart].start, tokens[index - 1].end)
        segmentStart = -1
      }
    }
    return segments
        .map { it.trim { character -> !character.isLetterOrDigit() }.replace(whitespaceRuns, " ") }
        .filter(String::isNotEmpty)
        .joinToString(" ")
  }
}
