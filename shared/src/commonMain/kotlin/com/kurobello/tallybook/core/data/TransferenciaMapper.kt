package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.database.SelectAllTransferencias
import com.kurobello.tallybook.core.database.SelectRecentTransferencias
import com.kurobello.tallybook.core.database.SelectTransferenciaById
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Transferencia
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

internal fun SelectTransferenciaById.toDomain(): Transferencia =
    transferenciaOf(
        id,
        cuenta_origen_id,
        cuenta_destino_id,
        Money(monto_origen_minor, Moneda.valueOf(moneda_origen)),
        Money(monto_destino_minor, Moneda.valueOf(moneda_destino)),
        fecha,
        descripcion,
        created_at,
    )

internal fun SelectAllTransferencias.toDomain(): Transferencia =
    transferenciaOf(
        id,
        cuenta_origen_id,
        cuenta_destino_id,
        Money(monto_origen_minor, Moneda.valueOf(moneda_origen)),
        Money(monto_destino_minor, Moneda.valueOf(moneda_destino)),
        fecha,
        descripcion,
        created_at,
    )

internal fun SelectRecentTransferencias.toDomain(): Transferencia =
    transferenciaOf(
        id,
        cuenta_origen_id,
        cuenta_destino_id,
        Money(monto_origen_minor, Moneda.valueOf(moneda_origen)),
        Money(monto_destino_minor, Moneda.valueOf(moneda_destino)),
        fecha,
        descripcion,
        created_at,
    )

@Suppress("LongParameterList")
private fun transferenciaOf(
    id: String,
    cuentaOrigenId: String,
    cuentaDestinoId: String,
    montoOrigen: Money,
    montoDestino: Money,
    fecha: String,
    descripcion: String?,
    createdAt: String,
): Transferencia =
    Transferencia(
        id = id,
        cuentaOrigenId = cuentaOrigenId,
        cuentaDestinoId = cuentaDestinoId,
        montoOrigen = montoOrigen,
        montoDestino = montoDestino,
        fecha = LocalDate.parse(fecha),
        descripcion = descripcion,
        createdAt = Instant.parse(createdAt),
    )

/** The amounts' currencies are not written: each side's cuenta owns its currency. */
internal fun TallybookDatabase.insertTransferenciaRow(
    transferencia: Transferencia,
    stamp: ChangeStamp,
) {
  check(
      transferenciaQueries
          .insertTransferencia(
              id = transferencia.id,
              cuenta_origen_id = transferencia.cuentaOrigenId,
              cuenta_destino_id = transferencia.cuentaDestinoId,
              monto_origen_minor = transferencia.montoOrigen.minorUnits,
              monto_destino_minor = transferencia.montoDestino.minorUnits,
              fecha = transferencia.fecha.toString(),
              descripcion = transferencia.descripcion,
              created_at = transferencia.createdAt.toString(),
              updated_at = stamp.value,
          )
          .value == 1L
  )
}
