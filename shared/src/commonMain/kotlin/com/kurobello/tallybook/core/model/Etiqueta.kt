package com.kurobello.tallybook.core.model

import kotlin.time.Instant

const val ETIQUETA_DESCRIPCION_MAX: Int = 140

/**
 * Tag names share [CategoriaRules.NOMBRE_MAX] and are unique among tags by
 * [CategoriaRules.nombreKey]. [descripcion] is at most [ETIQUETA_DESCRIPCION_MAX] characters.
 */
data class Etiqueta(
    val id: String,
    val nombre: String,
    val createdAt: Instant,
    val descripcion: String? = null,
)
