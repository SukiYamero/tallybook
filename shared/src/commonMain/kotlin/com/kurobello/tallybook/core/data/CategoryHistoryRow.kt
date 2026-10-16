package com.kurobello.tallybook.core.data

import kotlin.time.Instant

data class CategoryHistoryRow(
    val titulo: String,
    val categoriaId: String,
    val createdAt: Instant,
)
