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

enum class TransferenciaCuentaError {
  MISSING,
  SAME_AS_ORIGEN,
}

enum class TransferenciaMontoError {
  MISSING,
  WRONG_MONEDA,
  NOT_POSITIVE,
  DIFFERS_FROM_ORIGEN,
}

data class TransferenciaErrors(
    val cuentaOrigen: TransferenciaCuentaError? = null,
    val cuentaDestino: TransferenciaCuentaError? = null,
    val montoOrigen: TransferenciaMontoError? = null,
    val montoDestino: TransferenciaMontoError? = null,
    val fechaFuture: Boolean = false,
    val descripcionTooLong: Boolean = false,
) {
  val isValid: Boolean
    get() = this == TransferenciaErrors()
}

/**
 * Validates field values rather than a [Transferencia] so a form can check a draft whose accounts
 * or amounts are still empty (Pay card from another currency starts without [montoOrigen]).
 */
object TransferenciaRules {
  @Suppress("LongParameterList")
  fun validate(
      origen: Cuenta?,
      destino: Cuenta?,
      montoOrigen: Money?,
      montoDestino: Money?,
      fecha: LocalDate,
      descripcion: String?,
      today: LocalDate,
  ): TransferenciaErrors {
    val montoOrigenError = montoError(montoOrigen, origen)
    val montoDestinoError =
        montoError(montoDestino, destino)
            ?: TransferenciaMontoError.DIFFERS_FROM_ORIGEN.takeIf {
              montoOrigenError == null &&
                  origen != null &&
                  origen.moneda == destino?.moneda &&
                  montoDestino != montoOrigen
            }
    return TransferenciaErrors(
        cuentaOrigen = TransferenciaCuentaError.MISSING.takeIf { origen == null },
        cuentaDestino =
            when {
              destino == null -> TransferenciaCuentaError.MISSING
              destino.id == origen?.id -> TransferenciaCuentaError.SAME_AS_ORIGEN
              else -> null
            },
        montoOrigen = montoOrigenError,
        montoDestino = montoDestinoError,
        fechaFuture = fecha > today,
        descripcionTooLong = (descripcion?.length ?: 0) > TRANSFERENCIA_DESCRIPCION_MAX,
    )
  }

  /** Without its account the currency is unknown, so only presence and sign are checked. */
  private fun montoError(monto: Money?, cuenta: Cuenta?): TransferenciaMontoError? =
      when {
        monto == null -> TransferenciaMontoError.MISSING
        cuenta != null && monto.moneda != cuenta.moneda -> TransferenciaMontoError.WRONG_MONEDA
        monto.minorUnits <= 0 -> TransferenciaMontoError.NOT_POSITIVE
        else -> null
      }
}
