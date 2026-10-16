package com.kurobello.tallybook.core.ui.movimiento

import com.kurobello.tallybook.core.model.Movimiento

internal data class MovimientoRowText(
    val primary: String,
    val secondary: String?,
    val categoryLabel: String,
)

internal val Movimiento.rowTitulo: String?
  get() = titulo?.takeUnless(String::isBlank)

internal val Movimiento.rowDescription: String?
  get() = descripcion?.takeUnless(String::isBlank)

internal fun Movimiento.rowTitle(categoryLabel: String): String = rowTitulo ?: categoryLabel
