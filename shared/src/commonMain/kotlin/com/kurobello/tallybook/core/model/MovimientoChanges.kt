package com.kurobello.tallybook.core.model

import kotlinx.datetime.LocalDate

data class MovimientoChanges(
    val tipo: TipoMovimiento,
    val monto: Money,
    val fecha: LocalDate,
    val categoriaIds: List<String>,
    val titulo: String?,
    val descripcion: String?,
)
