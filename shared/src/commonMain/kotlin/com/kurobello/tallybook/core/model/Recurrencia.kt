package com.kurobello.tallybook.core.model

import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/**
 * The template of the movimiento each occurrence creates once confirmed; an occurrence is never
 * money until then. [proximaFecha] is the oldest occurrence neither confirmed nor skipped, and
 * every occurrence is computed from [fechaAncla] through [RecurrenciaRules].
 */
data class Recurrencia(
    val id: String,
    val tipo: TipoMovimiento,
    /** The currency of [monto] is this cuenta's; the database stores it only there. */
    val monto: Money,
    val categoriaId: String,
    val titulo: String?,
    val descripcion: String?,
    val cuentaId: String,
    val medio: MedioPago,
    val etiquetaIds: List<String>,
    val frecuencia: Frecuencia,
    val fechaAncla: LocalDate,
    val proximaFecha: LocalDate,
    val createdAt: Instant,
) {
  val draft: RecurrenciaDraft
    get() =
        RecurrenciaDraft(
            tipo = tipo,
            monto = monto,
            categoriaId = categoriaId,
            titulo = titulo,
            descripcion = descripcion,
            cuentaId = cuentaId,
            medio = medio,
            etiquetaIds = etiquetaIds,
            frecuencia = frecuencia,
            proximaFecha = proximaFecha,
        )

  fun pendingThrough(today: LocalDate): List<LocalDate> =
      RecurrenciaRules.pending(fechaAncla, frecuencia, proximaFecha, today)
}

/** [pending] holds the due dates through the day it was read for, oldest first. */
data class RecurrenciaWithPending(val recurrencia: Recurrencia, val pending: List<LocalDate>)
