package com.kurobello.tallybook.feature.recurrentes.viewmodel

import androidx.compose.runtime.Stable
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.ui.feedback.ManagementOutcome
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormActions
import kotlinx.datetime.LocalDate

/**
 * The list, the occurrence sheet and the recurrencia sheet share one owner of the open sheet. The
 * recurrencia sheet is the movimiento form, so its field events are [MovimientoFormActions], with
 * [onFechaChanged] setting the next date and [onSubmit] saving the template.
 */
@Suppress("TooManyFunctions")
@Stable
internal interface RecurrentesActions : MovimientoFormActions {
  fun onRetry()

  fun onOpenOccurrence(recurrenciaId: String): Boolean

  fun onOpenRecurrencia(recurrenciaId: String): Boolean

  fun onOccurrenceAmountChanged(value: String)

  fun onOccurrenceFechaChanged(value: LocalDate)

  fun onPaid()

  fun onNotYet()

  fun onSkip()

  /** Swaps the occurrence sheet for its recurrencia's, where a refused template is fixed. */
  fun onEditFromOccurrence()

  fun onFrecuenciaChanged(frecuencia: Frecuencia)

  fun onRequestDelete()

  fun onConfirmDelete()

  fun onCancelDelete()

  fun onRequestCloseSheet()

  fun onSheetHidden()
}

internal sealed interface RecurrentesEffect {
  /** A committed write whose feedback, and Undo when it has one, the shell's queue shows. */
  data class Feedback(val outcome: ManagementOutcome) : RecurrentesEffect
}
