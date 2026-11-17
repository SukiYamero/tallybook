package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.MOVIMIENTO_TITLE_MAX_LENGTH
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.ParsedMovimientoPhrase
import com.kurobello.tallybook.core.model.PhraseReviewFlag
import com.kurobello.tallybook.core.text.reformatAmountInput
import com.kurobello.tallybook.core.text.toAmountInput

private const val JOINED_TITULO_SEPARATOR = ", "
private const val JOINED_DESCRIPCION_SEPARATOR = "\n"

/** The parts of a new interpretation; a category picked by hand stays with a part of its título. */
internal fun List<EntryPart>.rebuiltFrom(interpreted: List<PartInterpretation>): List<EntryPart> {
  val picks = filter(EntryPart::categoriasEdited).associate { it.titulo to it.categoriaId }
  return interpreted.map { part ->
    val picked = part.titulo in picks
    EntryPart(
        titulo = part.titulo,
        amountInput = part.monto?.toAmountInput().orEmpty(),
        amountNeedsReview = part.monto != null && part.amountIsAmbiguous,
        categoriaId = if (picked) picks[part.titulo] else part.preselectedCategoriaId,
        categoriasEdited = picked,
        suggestionQuery = part.query,
    )
  }
}

/** The Detected card's rows, with the categories Continue would give each part. */
internal fun PhraseInterpretation.partPreviews(
    previous: List<EntryPart>,
    categorias: List<Categoria>,
): List<PartPreviewUi> =
    parts.zip(previous.rebuiltFrom(parts)) { interpreted, part ->
      PartPreviewUi(
          monto = interpreted.monto,
          titulo = part.titulo,
          categoria = categorias.firstOrNull { it.id == part.categoriaId },
      )
    }

internal fun AddMovimientoUiState.withPartCategoria(index: Int, id: String?): AddMovimientoUiState =
    copy(
        parts =
            parts.mapIndexed { i, part ->
              if (i == index) part.copy(categoriaId = id, categoriasEdited = true) else part
            }
    )

internal fun AddMovimientoUiState.withoutPart(index: Int): AddMovimientoUiState {
  val remaining = parts.filterIndexed { i, _ -> i != index }
  return remaining.singleOrNull()?.let { summaryOf(it) } ?: copy(parts = remaining)
}

/**
 * The whole sentence reads as its largest amount, never as the sum, so the joined amount is marked
 * edited: another cuenta's currency then reformats it instead of re-deriving it from the sentence.
 */
internal fun AddMovimientoUiState.joined(): AddMovimientoUiState {
  val categoria = parts.map(EntryPart::categoriaId).distinct().singleOrNull()
  // The Detected card keeps every part the sentence was split into; fewer here means a removal.
  val titulo =
      if (parts.size == detected.parts.size) phrase.trim()
      else parts.joinToString(JOINED_TITULO_SEPARATOR, transform = EntryPart::titulo)
  return copy(
      step = AddMovimientoStep.SUMMARY,
      parts = emptyList(),
      amountInput =
          parts.mapNotNull { partAmount(it) }.fold(Money(0, moneda), Money::plus).toAmountInput(),
      amountEdited = true,
      amountNeedsReview = parts.any(EntryPart::amountNeedsReview),
      titulo = titulo.take(MOVIMIENTO_TITLE_MAX_LENGTH),
      descripcion =
          parts
              .map(EntryPart::descripcion)
              .filter(String::isNotBlank)
              .joinToString(JOINED_DESCRIPCION_SEPARATOR)
              .take(MOVIMIENTO_DESCRIPTION_MAX_LENGTH),
      categoriaId = categoria,
      categoriasEdited = categoria != null && parts.any(EntryPart::categoriasEdited),
  )
}

/** Like [joined], the sentence no longer reads as this part's amount, so it counts as edited. */
private fun AddMovimientoUiState.summaryOf(part: EntryPart): AddMovimientoUiState {
  val overrides = part.overrides
  val partTipo = overrides.tipo ?: tipo
  return copy(
      step = AddMovimientoStep.SUMMARY,
      parts = emptyList(),
      tipo = partTipo,
      fecha = overrides.fecha ?: fecha,
      cuentaChoice = (overrides.cuentaChoice ?: cuentaChoice).forTipo(partTipo),
      etiquetaIds = overrides.etiquetaIds ?: etiquetaIds,
      etiquetasEdited = etiquetasEdited || overrides.etiquetaIds != null,
      amountInput = part.amountInput,
      amountEdited = true,
      amountNeedsReview = part.amountNeedsReview,
      titulo = part.titulo,
      descripcion = part.descripcion,
      categoriaId = part.categoriaId,
      categoriasEdited = part.categoriasEdited,
      suggestionQuery = part.suggestionQuery,
  )
}

/**
 * The shared cuenta moved from [from] to [to]: a part the sentence still owns reads its own título
 * again, an edited one is reformatted, and one with its own cuenta keeps its currency.
 */
internal fun List<EntryPart>.withRederivedAmounts(
    from: Moneda,
    to: Moneda,
    parse: (String) -> ParsedMovimientoPhrase,
): List<EntryPart> = map { part ->
  when {
    from == to || part.overrides.cuentaChoice != null -> part
    part.amountEdited -> part.copy(amountInput = part.amountInput.reformatAmountInput(from, to))
    else -> {
      val parsed = parse(part.titulo)
      part.copy(
          amountInput = parsed.monto?.toAmountInput().orEmpty(),
          amountNeedsReview =
              parsed.monto != null && PhraseReviewFlag.AMBIGUOUS_AMOUNT in parsed.reviewFlags,
      )
    }
  }
}
