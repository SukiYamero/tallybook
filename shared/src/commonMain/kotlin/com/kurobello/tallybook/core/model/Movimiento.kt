package com.kurobello.tallybook.core.model

import kotlin.time.Instant
import kotlinx.datetime.LocalDate

const val MOVIMIENTO_TITLE_MAX_LENGTH: Int = 180
const val MOVIMIENTO_DESCRIPTION_MAX_LENGTH: Int = 180
const val MAX_CATEGORIAS_POR_MOVIMIENTO: Int = 6
const val MAX_ETIQUETAS_POR_MOVIMIENTO: Int = 6

data class Movimiento(
    val id: String,
    val tipo: TipoMovimiento,
    val monto: Money,
    val fecha: LocalDate,
    val categoriaId: String,
    val titulo: String?,
    val descripcion: String?,
    val createdAt: Instant,
    /** Up to [MAX_ETIQUETAS_POR_MOVIMIENTO] distinct ids in the user's display order. */
    val etiquetaIds: List<String> = emptyList(),
)
