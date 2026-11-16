package com.kurobello.tallybook.core.model

import kotlinx.datetime.LocalDate

/** The editable fields of a [Recurrencia], under the same rules as a movimiento's. */
data class RecurrenciaDraft(
    val tipo: TipoMovimiento,
    val monto: Money,
    val categoriaId: String,
    val titulo: String?,
    val descripcion: String?,
    val cuentaId: String,
    val medio: MedioPago,
    val etiquetaIds: List<String>,
    val frecuencia: Frecuencia,
    val proximaFecha: LocalDate,
)
