package com.kurobello.tallybook.feature.recurrentes.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Recurrencia
import com.kurobello.tallybook.core.text.AmountInputError
import com.kurobello.tallybook.core.text.AmountInputResult
import com.kurobello.tallybook.core.text.parseAmountInput
import com.kurobello.tallybook.core.text.toAmountInput
import com.kurobello.tallybook.feature.movimientos.viewmodel.EditMovimientoDraftState
import com.kurobello.tallybook.feature.movimientos.viewmodel.EditMovimientoUiState
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormError
import com.kurobello.tallybook.feature.movimientos.viewmodel.toDraft
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * The one open sheet of the section. A closing sheet keeps the shell modal-owned until it reports
 * it hid; switching from an occurrence to its recurrencia swaps the content in place.
 */
internal sealed interface RecurrentesSheet {
  val recurrencia: Recurrencia
  val isClosing: Boolean
}

internal enum class OccurrenceAction {
  CONFIRM,
  SKIP,
}

internal enum class OccurrenceError {
  CONFIRM_FAILED,

  /** The template's cuenta or categoria can no longer take a movimiento; fixed in its sheet. */
  TEMPLATE_INVALID,
  SKIP_FAILED,
}

/** Confirms or skips [dueDate], the oldest occurrence pending for [recurrencia]. */
internal data class OccurrenceSheet(
    override val recurrencia: Recurrencia,
    val dueDate: LocalDate,
    val today: LocalDate,
    val amountInput: String = recurrencia.monto.toAmountInput(),
    val fecha: LocalDate = dueDate,
    val submitAttempted: Boolean = false,
    val busy: OccurrenceAction? = null,
    val error: OccurrenceError? = null,
    override val isClosing: Boolean = false,
) : RecurrentesSheet {
  val moneda: Moneda
    get() = recurrencia.monto.moneda

  val monto: Money?
    get() = (parseAmountInput(amountInput, moneda) as? AmountInputResult.Valid)?.money

  val amountError: AmountInputError?
    get() =
        (parseAmountInput(amountInput, moneda) as? AmountInputResult.Invalid)?.reason?.takeIf {
          submitAttempted
        }

  val isEditable: Boolean
    get() = busy == null && !isClosing
}

/**
 * Edits [recurrencia] through the movimiento form, whose fecha is the next date. [scheduleRejected]
 * records that a save found an occurrence pending, which the list may not show yet.
 */
internal data class RecurrenciaSheet(
    override val recurrencia: Recurrencia,
    val edit: EditMovimientoDraftState,
    val frecuencia: Frecuencia = recurrencia.frecuencia,
    val scheduleRejected: Boolean = false,
    val isSubmitting: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false,
    val deleteFailed: Boolean = false,
    override val isClosing: Boolean = false,
) : RecurrentesSheet {
  val canInteract: Boolean
    get() = !isSubmitting && !isClosing && !isDeleteConfirmationVisible

  val hasChanges: Boolean
    get() = frecuencia != recurrencia.frecuencia || edit.draft != edit.original.toDraft()

  fun form(catalog: RecurrentesCatalog, categorias: List<Categoria>): EditMovimientoUiState =
      EditMovimientoUiState(
          original = edit.original,
          draft = edit.draft,
          categorias = categorias,
          maxSelectableDate = edit.maxSelectableDate,
          etiquetas = catalog.etiquetas,
          etiquetasLoad = catalog.etiquetasLoad,
          isLoading = catalog.isCuentasLoading,
          isSubmitting = isSubmitting,
          submitAttempted = edit.submitAttempted,
          error =
              if (catalog.isCuentasFailed) MovimientoFormError.ACCOUNTS_UNAVAILABLE else edit.error,
          validationDate = edit.validationDate,
          failedEtiquetaNombre = edit.failedEtiquetaNombre,
          cuentas = catalog.cuentas,
          cuentaRejected = edit.cuentaRejected,
      )

  companion object {
    fun of(recurrencia: Recurrencia, today: LocalDate): RecurrenciaSheet {
      val original = recurrencia.asMovimiento()
      return RecurrenciaSheet(
          recurrencia = recurrencia,
          edit =
              EditMovimientoDraftState(
                  original = original,
                  draft = original.toDraft(),
                  maxSelectableDate = latestNextDate(recurrencia.proximaFecha, today),
              ),
      )
    }
  }
}

/** Far enough for an annual schedule to move a year past its current next date. */
private fun latestNextDate(proximaFecha: LocalDate, today: LocalDate): LocalDate =
    maxOf(proximaFecha, today).plus(NEXT_DATE_YEARS_AHEAD, DateTimeUnit.YEAR)

private const val NEXT_DATE_YEARS_AHEAD = 2
