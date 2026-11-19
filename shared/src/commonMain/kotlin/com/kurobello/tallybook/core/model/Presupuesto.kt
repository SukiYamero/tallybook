package com.kurobello.tallybook.core.model

import kotlin.time.Instant

data class Presupuesto(
    val id: String,
    val categoriaId: String,
    val monto: Money,
    val createdAt: Instant,
)
