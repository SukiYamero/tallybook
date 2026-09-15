package com.kurobello.tallybook.core.model

import kotlin.time.Instant
import kotlinx.datetime.LocalDate

const val MOVIMIENTO_DESCRIPTION_MAX_LENGTH: Int = 180

data class Movimiento(
    val id: String,
    val tipo: TipoMovimiento,
    val monto: Money,
    val fecha: LocalDate,
    val categoriaId: String,
    val descripcion: String?,
    val createdAt: Instant,
)
