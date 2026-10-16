package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.database.Movimiento_con_categorias as DatabaseMovimiento
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

private const val POSITION_SEPARATOR = '\u001F'
private const val ENTRY_SEPARATOR = '\u001E'

internal fun DatabaseMovimiento.toDomain(): Movimiento {
  require(monto_minor > 0) { "Persisted movimiento amount must be positive" }
  val categoriaIds = categoria_ids.toOrderedCategoriaIds()
  require(categoriaIds.isNotEmpty()) { "Persisted movimiento must have at least one categoria" }
  return Movimiento(
      id = id,
      tipo = TipoMovimiento.valueOf(tipo),
      monto = Money(monto_minor, Moneda.valueOf(monto_moneda)),
      fecha = LocalDate.parse(fecha),
      categoriaIds = categoriaIds,
      descripcion = descripcion,
      createdAt = Instant.parse(created_at),
  )
}

// The view concatenates "posicion<US>id" entries in unspecified order; posicion decides the order.
private fun String.toOrderedCategoriaIds(): List<String> =
    split(ENTRY_SEPARATOR)
        .filter(String::isNotEmpty)
        .map { entry ->
          entry.substringBefore(POSITION_SEPARATOR).toInt() to
              entry.substringAfter(POSITION_SEPARATOR)
        }
        .sortedBy { (posicion, _) -> posicion }
        .map { (_, categoriaId) -> categoriaId }
