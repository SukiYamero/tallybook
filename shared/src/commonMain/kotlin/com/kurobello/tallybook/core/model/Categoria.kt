package com.kurobello.tallybook.core.model

data class Categoria(
    val id: String,
    val nombre: String,
    val padreId: String?,
    val icono: CategoryIcon,
    val color: CategoryTint,
)
