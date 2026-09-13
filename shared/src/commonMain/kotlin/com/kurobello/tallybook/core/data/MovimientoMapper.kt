package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.database.Movimiento as DatabaseMovimiento
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.datetime.LocalDate

internal fun DatabaseMovimiento.toDomain(): Movimiento =
    Movimiento(
        id = id,
        tipo = TipoMovimiento.valueOf(tipo),
        monto = Money(monto_minor, Moneda.valueOf(monto_moneda)),
        fecha = LocalDate.parse(fecha),
        categoriaId = categoria_id,
        descripcion = descripcion,
    )
