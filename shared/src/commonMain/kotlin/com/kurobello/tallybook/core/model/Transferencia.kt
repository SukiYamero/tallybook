package com.kurobello.tallybook.core.model

import kotlin.time.Instant
import kotlinx.datetime.LocalDate

const val TRANSFERENCIA_DESCRIPCION_MAX: Int = 180

data class Transferencia(
    val id: String,
    val cuentaOrigenId: String,
    val cuentaDestinoId: String,
    val montoOrigen: Money,
    val montoDestino: Money,
    val fecha: LocalDate,
    val descripcion: String?,
    val createdAt: Instant,
)

enum class TransferenciaError {
  SAME_CUENTA,
  MONTO_ORIGEN_MONEDA,
  MONTO_ORIGEN_NOT_POSITIVE,
  MONTO_DESTINO_MONEDA,
  MONTO_DESTINO_NOT_POSITIVE,
  MONTOS_DIFFER,
  FECHA_FUTURE,
  DESCRIPCION_TOO_LONG,
}

/** Returns the first failing field in form order, or null when the transfer can be saved. */
object TransferenciaRules {
  fun validate(
      transferencia: Transferencia,
      origen: Cuenta,
      destino: Cuenta,
      today: LocalDate,
  ): TransferenciaError? {
    require(
        origen.id == transferencia.cuentaOrigenId && destino.id == transferencia.cuentaDestinoId
    ) {
      "Accounts must be the transferencia's origin and destination"
    }
    val origenMonto = transferencia.montoOrigen
    val destinoMonto = transferencia.montoDestino
    return when {
      transferencia.cuentaOrigenId == transferencia.cuentaDestinoId ->
          TransferenciaError.SAME_CUENTA
      origenMonto.moneda != origen.moneda -> TransferenciaError.MONTO_ORIGEN_MONEDA
      origenMonto.minorUnits <= 0 -> TransferenciaError.MONTO_ORIGEN_NOT_POSITIVE
      destinoMonto.moneda != destino.moneda -> TransferenciaError.MONTO_DESTINO_MONEDA
      destinoMonto.minorUnits <= 0 -> TransferenciaError.MONTO_DESTINO_NOT_POSITIVE
      origen.moneda == destino.moneda && origenMonto != destinoMonto ->
          TransferenciaError.MONTOS_DIFFER
      transferencia.fecha > today -> TransferenciaError.FECHA_FUTURE
      (transferencia.descripcion?.length ?: 0) > TRANSFERENCIA_DESCRIPCION_MAX ->
          TransferenciaError.DESCRIPCION_TOO_LONG
      else -> null
    }
  }
}
