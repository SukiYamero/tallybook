package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.model.CorreccionFrase
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.map
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Save for [AddMovimientoViewModel]: the single draft, or every part of the review at once. */
internal class AddMovimientoSubmission(
    private val uiState: MutableStateFlow<AddMovimientoUiState>,
    private val repositories: AddMovimientoRepositories,
    private val idGenerator: IdGenerator,
    private val scope: CoroutineScope,
    private val effects: SendChannel<AddMovimientoEffect>,
) {

  fun submitDraft(createdAt: Instant, timeZone: TimeZone) {
    uiState.update { it.copy(submitAttempted = true, error = it.loadError) }
    val movement = uiState.value.toMovimientoOrNull(idGenerator, createdAt, timeZone)
    if (movement == null) {
      uiState.update { current ->
        if (current.step == AddMovimientoStep.SUMMARY) {
          current.copy(step = AddMovimientoStep.FORM, formOpenedFromSummary = true)
        } else {
          current
        }
      }
      return
    }
    val draft = uiState.value
    val frecuencia = draft.frecuencia
    val today = createdAt.toLocalDateTime(timeZone).date
    persist(
        write = {
          if (frecuencia == null) repositories.movimientos.insert(movement)
          else repositories.recurrencias.create(movement, frecuencia, today).map {}
        },
        corrections = { draft.draftCorrections(movement, idGenerator, createdAt) },
        onFailure = { error ->
          // Every refusal is invalid input; re-reading the cuenta tells a refused one apart.
          val rejected =
              error is DataError.InvalidInput &&
                  (repositories.cuentas.getById(movement.cuentaId) as? DataOutcome.Success)
                      ?.data
                      ?.accepts(movement.medio, movement.monto.moneda) != true
          uiState.update { current ->
            if (rejected) current.withRejectedCuenta()
            else current.copy(error = MovimientoFormError.SAVE_FAILED)
          }
        },
    )
  }

  /** Saves every part or none; an invalid part opens its form instead, as Save does the draft's. */
  fun submitParts(createdAt: Instant, timeZone: TimeZone) {
    uiState.update { it.copy(error = it.loadError) }
    when (val save = uiState.value.partsToSave(idGenerator, createdAt, timeZone)) {
      is PartsSave.Invalid -> uiState.value = save.form
      is PartsSave.Ready -> {
        val review = uiState.value
        val today = createdAt.toLocalDateTime(timeZone).date
        persist(
            write = { repositories.saveAll(save.movimientos, review.frecuencia, today) },
            corrections = { review.partsCorrections(save.movimientos, idGenerator, createdAt) },
            onFailure = { uiState.update { it.copy(error = MovimientoFormError.SAVE_FAILED) } },
        )
      }
    }
  }

  /**
   * The corrections are written apart from the save and only after it succeeded, before Saved
   * closes the screen and cancels its scope. A failed write is dropped: the movimiento is saved
   * either way, and a missing record only costs one lesson.
   */
  private fun persist(
      write: suspend () -> DataOutcome<Unit>,
      corrections: () -> List<CorreccionFrase>,
      onFailure: suspend (DataError) -> Unit,
  ) {
    uiState.update { it.copy(isSubmitting = true) }
    scope.launch {
      var saved = false
      try {
        when (val outcome = write()) {
          is DataOutcome.Success -> {
            corrections()
                .takeIf { it.isNotEmpty() }
                ?.let { repositories.correcciones.insertAll(it) }
            effects.send(AddMovimientoEffect.Saved)
            saved = true
          }
          is DataOutcome.Failure -> onFailure(outcome.error)
        }
      } finally {
        if (!saved) {
          uiState.update { it.copy(isSubmitting = false) }
        }
      }
    }
  }

  private fun AddMovimientoUiState.withRejectedCuenta(): AddMovimientoUiState =
      copy(
          cuentaRejected = true,
          step = if (step == AddMovimientoStep.SUMMARY) AddMovimientoStep.FORM else step,
          formOpenedFromSummary = formOpenedFromSummary || step == AddMovimientoStep.SUMMARY,
      )
}
