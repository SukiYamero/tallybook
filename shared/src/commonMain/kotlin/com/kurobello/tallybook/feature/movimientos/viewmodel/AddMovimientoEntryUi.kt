package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.datetime.LocalDate

internal enum class AddMovimientoStep {
  PHRASE,
  SUMMARY,
  FORM,
}

/** What the sentence says so far; [tipo] and [fecha] fall back to expense and today. */
internal data class PhrasePreviewUi(
    val tipo: TipoMovimiento,
    val monto: Money?,
    val fecha: LocalDate,
)

internal data class MovimientoSentenceUi(
    val text: String,
    val detected: PhrasePreviewUi,
    val today: LocalDate,
    val moneda: Moneda,
    val categorias: List<Categoria>,
    val categoriasArePending: Boolean,
    val suggestions: List<Categoria>,
    val isEnabled: Boolean,
    val actionsEnabled: Boolean,
) {
  val canContinue: Boolean
    get() = isEnabled && actionsEnabled && text.isNotBlank()
}

internal data class MovimientoSummaryUi(
    val tipo: TipoMovimiento,
    val fecha: LocalDate,
    val monto: Money?,
    val amountNeedsReview: Boolean,
    val descripcion: String,
    val categorias: List<Categoria>,
    val categoriasArePending: Boolean,
    val suggestions: List<Categoria>,
    val isSubmitting: Boolean,
)
