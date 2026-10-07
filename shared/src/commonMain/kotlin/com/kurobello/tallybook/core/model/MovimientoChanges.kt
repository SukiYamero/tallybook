package com.kurobello.tallybook.core.model

import kotlinx.datetime.LocalDate

data class MovimientoChanges(
    val tipo: TipoMovimiento,
    val monto: Money,
    val fecha: LocalDate,
    val categoriaId: String,
    val descripcion: String?,
)
