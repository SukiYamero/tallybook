package com.kurobello.tallybook.core.model

/**
 * @property icono name of a Lucide icon, e.g. "shopping-cart".
 * @property color named tint (amber, blue...) resolved against the theme tokens; not a hex, so a
 *   categoria is never coupled to a concrete theme.
 */
data class Categoria(
    val id: String,
    val nombre: String,
    val padreId: String?,
    val icono: String,
    val color: String,
)
