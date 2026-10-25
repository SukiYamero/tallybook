package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.Movimiento
import kotlinx.datetime.LocalDate

internal data class EditMovimientoDraftState(
    val original: Movimiento,
    val draft: MovimientoDraft,
    val maxSelectableDate: LocalDate,
    val submitAttempted: Boolean = false,
    val error: MovimientoFormError? = null,
    val validationDate: LocalDate = maxSelectableDate,
    val failedEtiquetaNombre: String? = null,
    val cuentaRejected: Boolean = false,
)
