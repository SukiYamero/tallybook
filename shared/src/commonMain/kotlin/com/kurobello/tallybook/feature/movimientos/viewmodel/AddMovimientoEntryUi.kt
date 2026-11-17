package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.Frecuencia
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
    val frecuencia: Frecuencia? = null,
)

internal data class MovimientoSentenceUi(
    val text: String,
    val detected: PhrasePreviewUi,
    val today: LocalDate,
    val moneda: Moneda,
    val categoria: Categoria?,
    val categoriaIsPending: Boolean,
    val suggestions: List<Categoria>,
    val etiquetas: List<Etiqueta>,
    val etiquetasLoad: EtiquetasLoad,
    val cuenta: MovimientoCuentaUi,
    val isEnabled: Boolean,
    val actionsEnabled: Boolean,
    val voice: VoiceInputUi = VoiceInputUi(),
) {
  val canContinue: Boolean
    get() = isEnabled && actionsEnabled && text.isNotBlank()

  // While the microphone is held the card keeps the visibility it had when the press began, so
  // the button never slides from under the finger as dictated words arrive.
  val showsDetected: Boolean
    get() = if (voice.isListening) voice.detectedShownAtStart else text.isNotBlank()
}

internal data class MovimientoSummaryUi(
    val tipo: TipoMovimiento,
    val fecha: LocalDate,
    val monto: Money?,
    val amountNeedsReview: Boolean,
    val titulo: String,
    val categoria: Categoria?,
    val categoriaIsPending: Boolean,
    val suggestions: List<Categoria>,
    val etiquetas: List<Etiqueta>,
    val etiquetasLoad: EtiquetasLoad,
    val cuenta: MovimientoCuentaUi,
    val isSubmitting: Boolean,
)
