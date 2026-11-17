package com.kurobello.tallybook.core.model

import kotlinx.datetime.LocalDate

data class ParsedMovimientoPhrase(
    val tipo: TipoMovimiento?,
    val monto: Money?,
    val fecha: LocalDate?,
    val subject: String,
    val reviewFlags: Set<PhraseReviewFlag>,
    val medio: MedioPago?,
    val medioCandidates: Set<MedioPago>,
    /**
     * Null both without a recurrence and with one [PhraseReviewFlag.UNSUPPORTED_RECURRENCE] marks.
     */
    val frecuencia: Frecuencia? = null,
)

enum class PhraseReviewFlag {
  AMBIGUOUS_AMOUNT,
  FUTURE_DATE_IGNORED,
  /** The sentence states a cadence with no [Frecuencia] equivalent, such as every 15 days. */
  UNSUPPORTED_RECURRENCE,
}
