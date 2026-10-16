package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.ParsedMovimientoPhrase
import com.kurobello.tallybook.core.model.PhraseReviewFlag
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.datetime.LocalDate

private val whitespaceRuns = Regex("\\s+")

/**
 * Derives tipo, monto, fecha and subject from one sentence. Pure and deterministic: the vocabulary
 * is the union of every supported language and [AppLocale] only decides numeric date order.
 */
class MovimientoPhraseParser {

  fun parse(
      text: String,
      moneda: Moneda,
      locale: AppLocale,
      today: LocalDate,
  ): ParsedMovimientoPhrase {
    val tokens = tokenizePhrase(text)
    val consumed = BooleanArray(tokens.size)
    val cursor = PhraseCursor(tokens, consumed)
    val tipo = consumeTipoVerbs(cursor)
    val date =
        findPhraseDate(cursor, today, locale)?.also { cursor.consume(it.start, it.endExclusive) }
    val amount =
        findPhraseAmount(cursor, moneda)?.also { cursor.consume(it.start, it.endExclusive) }
    val isFuture = date != null && date.date > today
    return ParsedMovimientoPhrase(
        tipo = tipo,
        monto = amount?.money,
        fecha = date?.date?.takeUnless { isFuture },
        subject = remainingText(text, tokens, consumed),
        reviewFlags =
            setOfNotNull(
                PhraseReviewFlag.AMBIGUOUS_AMOUNT.takeIf { amount?.isAmbiguous == true },
                PhraseReviewFlag.FUTURE_DATE_IGNORED.takeIf { isFuture },
            ),
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
