package com.kurobello.tallybook.core.model

import kotlin.time.Instant

/**
 * Tag names share [CategoriaRules.NOMBRE_MAX] and are unique among tags by
 * [CategoriaRules.nombreKey].
 */
data class Etiqueta(val id: String, val nombre: String, val createdAt: Instant)
