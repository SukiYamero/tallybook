package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.entry.SentenceSelection
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.datetime.LocalDate

/** Layers an interpretation over the draft; values the user changed by hand stay theirs. */
internal fun AddMovimientoUiState.withDetection(
    interpretation: PhraseInterpretation,
    today: LocalDate,
): AddMovimientoUiState =
    copy(
        preview =
            PhrasePreviewUi(
                tipo = interpretation.tipo,
                monto = interpretation.parsed.monto,
                fecha = interpretation.parsed.fecha ?: today,
                frecuencia = interpretation.parsed.frecuencia,
                parts = interpretation.partPreviews(parts, categorias),
            ),
        suggestionQuery = interpretation.query,
        categoriaId = if (categoriasEdited) categoriaId else interpretation.preselectedCategoriaId,
        etiquetaIds = if (etiquetasEdited) etiquetaIds else interpretation.etiquetaIds,
        sentenceSelection = interpretation.sentence,
        cuentaChoice = sentenceChoice(interpretation.sentence, interpretation.tipo),
    )

internal fun AddMovimientoUiState.sentenceCuentas() =
    SentenceCuentas(cuentas, primaryMoneda ?: moneda) { sentence, tipo ->
      monedaOf(sentenceChoice(sentence, tipo), tipo)
    }

private fun AddMovimientoUiState.sentenceChoice(
    sentence: SentenceSelection,
    tipo: TipoMovimiento,
): CuentaSelectionState =
    cuentaChoice.withSentence(sentence, cuentas, cuentaDefaults, tipo).forTipo(tipo)
