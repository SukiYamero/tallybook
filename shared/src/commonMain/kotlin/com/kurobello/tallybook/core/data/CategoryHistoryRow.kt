package com.kurobello.tallybook.core.data

import kotlin.time.Instant

data class CategoryHistoryRow(
    val movimientoId: String,
    val descripcion: String,
    val categoriaId: String,
    val createdAt: Instant,
)
