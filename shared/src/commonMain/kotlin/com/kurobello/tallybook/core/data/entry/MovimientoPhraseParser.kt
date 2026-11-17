package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.ParsedMovimientoPhrase
import com.kurobello.tallybook.core.model.PhraseReviewFlag
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.datetime.LocalDate

private val whitespaceRuns = Regex("\\s+")

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
    val tokens = tokenizePhrase(text)
    val consumed = BooleanArray(tokens.size)
    val cursor = PhraseCursor(tokens, consumed)
    val tipo = consumeTipoVerbs(cursor)
    // Before the reserved spans: the seeded cash account is named "Efectivo", and reserving it
    // first would leave the "en" of "en efectivo" behind in the subject.
    val medio = consumePhraseMedio(cursor)
    consumeReserved(cursor, reservedSpans)
    // Before the date and the amount: "cada lunes" is not a date and the 30 of "cada 30 días" is
    // not the amount.
    val recurrence = consumePhraseRecurrence(cursor, today)
    val date =
        findPhraseDate(cursor, today, locale)?.also { cursor.consume(it.start, it.endExclusive) }
    val amount =
        findPhraseAmount(cursor, moneda)?.also { cursor.consume(it.start, it.endExclusive) }
    val isFuture = date != null && date.date > today
    return ParsedMovimientoPhrase(
        tipo = tipo,
        monto = amount?.money,
        fecha = if (date != null) date.date.takeUnless { isFuture } else recurrence?.anchor,
        subject = remainingText(text, tokens, consumed),
        reviewFlags =
            setOfNotNull(
                PhraseReviewFlag.AMBIGUOUS_AMOUNT.takeIf { amount?.isAmbiguous == true },
                PhraseReviewFlag.FUTURE_DATE_IGNORED.takeIf { isFuture },
                PhraseReviewFlag.UNSUPPORTED_RECURRENCE.takeIf {
                  recurrence != null && recurrence.frecuencia == null
                },
            ),
        medio = medio.medio,
        medioCandidates = medio.candidates,
        frecuencia = recurrence?.frecuencia,
    )
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
  private fun remainingText(
      text: String,
      tokens: List<PhraseToken>,
      consumed: BooleanArray,
  ): String {
    val segments = mutableListOf<String>()
    var segmentStart = -1
    for (index in 0..tokens.size) {
      val isRemaining = index < tokens.size && !consumed[index]
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
