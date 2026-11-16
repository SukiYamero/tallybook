package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.database.Recurrencia_con_etiquetas as DatabaseRecurrencia
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.Recurrencia
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

internal fun DatabaseRecurrencia.toDomain(): Recurrencia =
    Recurrencia(
        id = id,
        tipo = TipoMovimiento.valueOf(tipo),
        monto = Money(monto_minor, Moneda.valueOf(monto_moneda)),
        categoriaId = categoria_id,
        titulo = titulo,
        descripcion = descripcion,
        cuentaId = cuenta_id,
        medio = MedioPago.valueOf(medio_pago),
        etiquetaIds = etiqueta_ids.toOrderedIds(),
        frecuencia = Frecuencia.valueOf(frecuencia),
        fechaAncla = LocalDate.parse(fecha_ancla),
        proximaFecha = LocalDate.parse(proxima_fecha),
        createdAt = Instant.parse(created_at),
    )

internal fun Recurrencia.movimiento(
    id: String,
    monto: Money,
    fecha: LocalDate,
    createdAt: Instant,
): Movimiento =
    Movimiento(
        id = id,
        tipo = tipo,
        monto = monto,
        fecha = fecha,
        categoriaId = categoriaId,
        titulo = titulo,
        descripcion = descripcion,
        createdAt = createdAt,
        cuentaId = cuentaId,
        medio = medio,
        etiquetaIds = etiquetaIds,
    )

/** Strict like every insert: an existing id, live or tombstoned, fails the write. */
internal fun TallybookDatabase.insertRecurrenciaRow(recurrencia: Recurrencia, stamp: ChangeStamp) {
  check(
      recurrenciaQueries
          .insertRecurrencia(
              id = recurrencia.id,
              tipo = recurrencia.tipo.name,
              monto_minor = recurrencia.monto.minorUnits,
              titulo = recurrencia.titulo,
              descripcion = recurrencia.descripcion,
              categoria_id = recurrencia.categoriaId,
              cuenta_id = recurrencia.cuentaId,
              medio_pago = recurrencia.medio.name,
              frecuencia = recurrencia.frecuencia.name,
              fecha_ancla = recurrencia.fechaAncla.toString(),
              proxima_fecha = recurrencia.proximaFecha.toString(),
              created_at = recurrencia.createdAt.toString(),
              updated_at = stamp.value,
          )
          .value == 1L
  )
  insertRecurrenciaEtiquetas(recurrencia.id, recurrencia.etiquetaIds)
}

internal fun TallybookDatabase.updateRecurrenciaRow(recurrencia: Recurrencia, stamp: ChangeStamp) {
  check(
      recurrenciaQueries
          .updateRecurrencia(
              tipo = recurrencia.tipo.name,
              monto_minor = recurrencia.monto.minorUnits,
              titulo = recurrencia.titulo,
              descripcion = recurrencia.descripcion,
              categoria_id = recurrencia.categoriaId,
              cuenta_id = recurrencia.cuentaId,
              medio_pago = recurrencia.medio.name,
              frecuencia = recurrencia.frecuencia.name,
              fecha_ancla = recurrencia.fechaAncla.toString(),
              proxima_fecha = recurrencia.proximaFecha.toString(),
              updated_at = stamp.value,
              id = recurrencia.id,
          )
          .value == 1L
  )
  recurrenciaQueries.deleteRecurrenciaEtiquetas(recurrencia.id)
  insertRecurrenciaEtiquetas(recurrencia.id, recurrencia.etiquetaIds)
}

private fun TallybookDatabase.insertRecurrenciaEtiquetas(
    recurrenciaId: String,
    etiquetaIds: List<String>,
) {
  etiquetaIds.forEachIndexed { posicion, etiquetaId ->
    check(
        recurrenciaQueries
            .insertRecurrenciaEtiqueta(recurrenciaId, etiquetaId, posicion.toLong())
            .value == 1L
    )
  }
}
