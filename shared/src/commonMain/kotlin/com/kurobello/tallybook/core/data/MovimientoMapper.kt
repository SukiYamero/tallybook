package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.database.Movimiento_con_etiquetas as DatabaseMovimiento
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

private const val POSITION_SEPARATOR = '\u001F'
private const val ENTRY_SEPARATOR = '\u001E'

/** The view joins ids with these characters, so an id holding one would corrupt the parsed list. */
internal val VIEW_SEPARATORS = charArrayOf(ENTRY_SEPARATOR, POSITION_SEPARATOR)

internal fun DatabaseMovimiento.toDomain(): Movimiento {
  require(monto_minor > 0) { "Persisted movimiento amount must be positive" }
  require(categoria_id.isNotEmpty()) { "Persisted movimiento must have a categoria" }
  return Movimiento(
      id = id,
      tipo = TipoMovimiento.valueOf(tipo),
      monto = Money(monto_minor, Moneda.valueOf(monto_moneda)),
      fecha = LocalDate.parse(fecha),
      categoriaId = categoria_id,
      titulo = titulo,
      descripcion = descripcion,
      createdAt = Instant.parse(created_at),
      cuentaId = cuenta_id,
      medio = MedioPago.valueOf(medio_pago),
      etiquetaIds = etiqueta_ids.toOrderedIds(),
  )
}

// The view concatenates "posicion<US>id" entries in unspecified order; posicion decides the order.
private fun String.toOrderedIds(): List<String> =
    split(ENTRY_SEPARATOR)
        .filter(String::isNotEmpty)
        .map { entry ->
          entry.substringBefore(POSITION_SEPARATOR).toInt() to
              entry.substringAfter(POSITION_SEPARATOR)
        }
        .sortedBy { (posicion, _) -> posicion }
        .map { (_, id) -> id }
