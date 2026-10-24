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

enum class TransferenciaMontoError {
  WRONG_MONEDA,
  NOT_POSITIVE,
  DIFFERS_FROM_ORIGEN,
}

data class TransferenciaErrors(
    val sameCuenta: Boolean = false,
    val montoOrigen: TransferenciaMontoError? = null,
    val montoDestino: TransferenciaMontoError? = null,
    val fechaFuture: Boolean = false,
    val descripcionTooLong: Boolean = false,
) {
  val isValid: Boolean
    get() = this == TransferenciaErrors()
}

object TransferenciaRules {
  fun validate(
      transferencia: Transferencia,
      origen: Cuenta,
      destino: Cuenta,
      today: LocalDate,
  ): TransferenciaErrors {
    require(
        origen.id == transferencia.cuentaOrigenId && destino.id == transferencia.cuentaDestinoId
    ) {
      "Accounts must be the transferencia's origin and destination"
    }
    val montoOrigen = transferencia.montoOrigen
    val montoDestino = transferencia.montoDestino
    return TransferenciaErrors(
        sameCuenta = transferencia.cuentaOrigenId == transferencia.cuentaDestinoId,
        montoOrigen = montoError(montoOrigen, origen.moneda),
        montoDestino =
            montoError(montoDestino, destino.moneda)
                ?: TransferenciaMontoError.DIFFERS_FROM_ORIGEN.takeIf {
                  origen.moneda == destino.moneda && montoDestino != montoOrigen
                },
        fechaFuture = transferencia.fecha > today,
        descripcionTooLong =
            (transferencia.descripcion?.length ?: 0) > TRANSFERENCIA_DESCRIPCION_MAX,
    )
  }

  private fun montoError(monto: Money, moneda: Moneda): TransferenciaMontoError? =
      when {
        monto.moneda != moneda -> TransferenciaMontoError.WRONG_MONEDA
        monto.minorUnits <= 0 -> TransferenciaMontoError.NOT_POSITIVE
        else -> null
      }
}
