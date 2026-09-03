package com.kurobello.tallybook.core.model

/**
 * @property icono nombre de un ícono Lucide, ej. "shopping-cart".
 * @property color tinte con nombre (amber, blue...) que se resuelve contra los tokens del tema; no
 *   es un hex, para no acoplar la categoría a un tema concreto.
 */
data class Categoria(
    val id: String,
    val nombre: String,
    val padreId: String?,
    val icono: String,
    val color: String,
)
