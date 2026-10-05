package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.database.Movimiento as DatabaseMovimiento
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

internal fun DatabaseMovimiento.toDomain(): Movimiento {
  require(monto_minor > 0) { "Persisted movimiento amount must be positive" }
  return Movimiento(
      id = id,
      tipo = TipoMovimiento.valueOf(tipo),
      monto = Money(monto_minor, Moneda.valueOf(monto_moneda)),
      fecha = LocalDate.parse(fecha),
      categoriaId = categoria_id,
      descripcion = descripcion,
      createdAt = Instant.parse(created_at),
  )
}
