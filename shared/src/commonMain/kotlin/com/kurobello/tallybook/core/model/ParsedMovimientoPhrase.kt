package com.kurobello.tallybook.core.model

import kotlinx.datetime.LocalDate

data class ParsedMovimientoPhrase(
    val tipo: TipoMovimiento?,
    val monto: Money?,
    val fecha: LocalDate?,
    val subject: String,
    val reviewFlags: Set<PhraseReviewFlag>,
)

enum class PhraseReviewFlag {
  AMBIGUOUS_AMOUNT,
  FUTURE_DATE_IGNORED,
}
