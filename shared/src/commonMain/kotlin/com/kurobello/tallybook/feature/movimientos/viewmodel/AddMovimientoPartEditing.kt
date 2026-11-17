package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.map
import com.kurobello.tallybook.core.text.AmountInputResult
import com.kurobello.tallybook.core.text.normalizeOptionalText
import com.kurobello.tallybook.core.text.parseAmountInput
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * The review's single-draft fields, set aside while a part's form holds that part: the shared
 * values plus the single sentence's own amount, título and category, which the review keeps apart.
 */
internal data class SharedDraft(
    val tipo: TipoMovimiento,
    val fecha: LocalDate,
    val cuentaChoice: CuentaSelectionState,
    val cuentaRejected: Boolean,
    val etiquetaIds: List<String>,
    val etiquetasEdited: Boolean,
    val amountInput: String,
    val amountEdited: Boolean,
    val amountNeedsReview: Boolean,
    val titulo: String,
    val descripcion: String,
    val categoriaId: String?,
    val categoriasEdited: Boolean,
    val suggestionQuery: SuggestionQuery?,
    val dismissedSuggestionIds: Set<String>,
    val submitAttempted: Boolean,
)

internal sealed interface PartsSave {
  data class Ready(val movimientos: List<Movimiento>) : PartsSave

  /** [form] is the first invalid part's form, showing its field errors. */
  data class Invalid(val form: AddMovimientoUiState) : PartsSave
}

/** The part's form: its own values with its overrides applied; Repetir stays the review's. */
internal fun AddMovimientoUiState.withPartForm(index: Int): AddMovimientoUiState {
  val part = parts[index]
  val overrides = part.overrides
  val partTipo = overrides.tipo ?: tipo
  return copy(
      step = AddMovimientoStep.FORM,
      editingPart = index,
      reviewShared = sharedDraft(),
      formOpenedFromSummary = false,
      tipo = partTipo,
      fecha = overrides.fecha ?: fecha,
      cuentaChoice = (overrides.cuentaChoice ?: cuentaChoice).forTipo(partTipo),
      cuentaRejected = false,
      etiquetaIds = overrides.etiquetaIds ?: etiquetaIds,
      amountInput = part.amountInput,
      amountEdited = part.amountEdited,
      amountNeedsReview = part.amountNeedsReview,
      titulo = part.titulo,
      descripcion = part.descripcion,
      categoriaId = part.categoriaId,
      categoriasEdited = part.categoriasEdited,
      suggestionQuery = part.suggestionQuery,
      suggestions = part.suggestions,
      submitAttempted = false,
  )
}

/**
 * Done keeps an invalid part in its form, like Save. A valid one goes back to the review, following
 * the shared values only where it still holds them.
 */
internal fun AddMovimientoUiState.withPartDone(today: LocalDate): AddMovimientoUiState {
  val index = editingPart
  val shared = reviewShared
  return when {
    index == null || shared == null || isSubmitting -> this
    savableOn(today) == null -> copy(submitAttempted = true)
    else ->
        copy(
                parts =
                    parts.mapIndexed { i, part ->
                      if (i == index) editedPart(part, shared) else part
                    }
            )
            .withShared(shared)
  }
}

internal fun AddMovimientoUiState.withoutPartEdits(): AddMovimientoUiState =
    reviewShared?.let(::withShared) ?: this

/**
 * Every part as its form would save it. The first part gets the save instant and each next one a
 * millisecond less, so History, newest first, lists them in the sentence's order.
 */
internal fun AddMovimientoUiState.partsToSave(
    idGenerator: IdGenerator,
    createdAt: Instant,
    timeZone: TimeZone,
): PartsSave {
  val forms = parts.indices.map { withPartForm(it) }
  val movimientos = forms.map { it.toMovimientoOrNull(idGenerator, createdAt, timeZone) }
  val invalid = movimientos.indexOf(null)
  return if (invalid >= 0) PartsSave.Invalid(forms[invalid].copy(submitAttempted = true))
  else
      PartsSave.Ready(
          movimientos.filterNotNull().mapIndexed { i, movimiento ->
            movimiento.copy(createdAt = createdAt - i.milliseconds)
          }
      )
}

/** Repetir is shared, so one save never mixes plain movimientos and recurrencias. */
internal suspend fun AddMovimientoRepositories.saveAll(
    movimientos: List<Movimiento>,
    frecuencia: Frecuencia?,
    today: LocalDate,
): DataOutcome<Unit> =
    if (frecuencia == null) this.movimientos.insertAll(movimientos)
    else recurrencias.createAll(movimientos, frecuencia, today).map {}

internal fun AddMovimientoUiState.toMovimientoOrNull(
    idGenerator: IdGenerator,
    createdAt: Instant,
    timeZone: TimeZone,
): Movimiento? =
    savableOn(createdAt.toLocalDateTime(timeZone).date)?.let { savable ->
      Movimiento(
          id = idGenerator.next(),
          tipo = tipo,
          monto = savable.monto,
          fecha = fecha,
          categoriaId = savable.categoriaId,
          titulo = titulo.normalizeOptionalText(),
          descripcion = descripcion.normalizeOptionalText(),
          createdAt = createdAt,
          cuentaId = savable.cuentaId,
          medio = savable.medio,
          etiquetaIds = selectedEtiquetas.map(Etiqueta::id),
      )
    }

private class SavableFields(
    val monto: Money,
    val categoriaId: String,
    val cuentaId: String,
    val medio: MedioPago,
)

private fun AddMovimientoUiState.savableOn(today: LocalDate): SavableFields? {
  val amount = (parseAmountInput(amountInput, moneda) as? AmountInputResult.Valid)?.money
  val categoriaId = effectiveCategoriaId
  val cuenta = cuentaField.selected
  val medio = cuentaField.effectiveMedio(tipo)
  val isValid = fecha <= today && !isLoading && error == null
  if (amount == null || categoriaId == null || !isValid) return null
  return if (cuenta == null || medio == null) null
  else SavableFields(amount, categoriaId, cuenta.id, medio)
}

private fun AddMovimientoUiState.editedPart(part: EntryPart, shared: SharedDraft): EntryPart =
    part.copy(
        amountInput = amountInput,
        amountEdited = amountEdited,
        amountNeedsReview = amountNeedsReview,
        titulo = titulo,
        descripcion = descripcion,
        categoriaId = categoriaId,
        categoriasEdited = categoriasEdited,
        overrides =
            PartOverrides(
                tipo = tipo.takeIf { it != shared.tipo },
                fecha = fecha.takeIf { it != shared.fecha },
                cuentaChoice = cuentaChoice.takeIf { it != shared.cuentaChoice },
                etiquetaIds = etiquetaIds.takeIf { it != shared.etiquetaIds },
            ),
    )

private fun AddMovimientoUiState.sharedDraft() =
    SharedDraft(
        tipo = tipo,
        fecha = fecha,
        cuentaChoice = cuentaChoice,
        cuentaRejected = cuentaRejected,
        etiquetaIds = etiquetaIds,
        etiquetasEdited = etiquetasEdited,
        amountInput = amountInput,
        amountEdited = amountEdited,
        amountNeedsReview = amountNeedsReview,
        titulo = titulo,
        descripcion = descripcion,
        categoriaId = categoriaId,
        categoriasEdited = categoriasEdited,
        suggestionQuery = suggestionQuery,
        dismissedSuggestionIds = dismissedSuggestionIds,
        submitAttempted = submitAttempted,
    )

private fun AddMovimientoUiState.withShared(shared: SharedDraft) =
    copy(
        step = AddMovimientoStep.REVIEW,
        editingPart = null,
        reviewShared = null,
        formOpenedFromSummary = false,
        tipo = shared.tipo,
        fecha = shared.fecha,
        cuentaChoice = shared.cuentaChoice,
        cuentaRejected = shared.cuentaRejected,
        etiquetaIds = shared.etiquetaIds,
        etiquetasEdited = shared.etiquetasEdited,
        amountInput = shared.amountInput,
        amountEdited = shared.amountEdited,
        amountNeedsReview = shared.amountNeedsReview,
        titulo = shared.titulo,
        descripcion = shared.descripcion,
        categoriaId = shared.categoriaId,
        categoriasEdited = shared.categoriasEdited,
        suggestionQuery = shared.suggestionQuery,
        dismissedSuggestionIds = shared.dismissedSuggestionIds,
        submitAttempted = shared.submitAttempted,
    )
