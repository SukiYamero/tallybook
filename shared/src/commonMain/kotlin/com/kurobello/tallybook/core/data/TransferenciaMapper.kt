package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.database.Transferencia_con_monedas as DatabaseTransferencia
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Transferencia
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

internal fun DatabaseTransferencia.toDomain(): Transferencia =
    Transferencia(
        id = id,
        cuentaOrigenId = cuenta_origen_id,
        cuentaDestinoId = cuenta_destino_id,
        montoOrigen = Money(monto_origen_minor, Moneda.valueOf(moneda_origen)),
        montoDestino = Money(monto_destino_minor, Moneda.valueOf(moneda_destino)),
        fecha = LocalDate.parse(fecha),
        descripcion = descripcion,
        createdAt = Instant.parse(created_at),
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
