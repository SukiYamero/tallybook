package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.datetime.LocalDate

internal enum class AddMovimientoStep {
  PHRASE,
  SUMMARY,
  FORM,
}

internal data class PhrasePreviewUi(
    val tipo: TipoMovimiento?,
    val monto: Money?,
    val fecha: LocalDate?,
    val categoria: Categoria?,
) {
  companion object {
    val Empty = PhrasePreviewUi(tipo = null, monto = null, fecha = null, categoria = null)
  }
}

internal data class MovimientoSummaryUi(
    val tipo: TipoMovimiento,
    val fecha: LocalDate,
    val monto: Money?,
    val amountNeedsReview: Boolean,
    val descripcion: String,
    val categoria: Categoria,
    val categoriaIsPending: Boolean,
    val suggestions: List<Categoria>,
    val isSubmitting: Boolean,
)
