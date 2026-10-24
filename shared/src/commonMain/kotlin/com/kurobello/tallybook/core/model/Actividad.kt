package com.kurobello.tallybook.core.model

import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/**
 * One row of a currency's activity: a movimiento, or a transfer with at least one side in that
 * currency. Activity reads order both kinds together by [fecha], [createdAt] and [id], newest
 * first.
 */
sealed interface Actividad {
  val fecha: LocalDate
  val createdAt: Instant
  val id: String

  data class DeMovimiento(val movimiento: Movimiento) : Actividad {
    override val fecha: LocalDate
      get() = movimiento.fecha

    override val createdAt: Instant
      get() = movimiento.createdAt

    override val id: String
      get() = movimiento.id
  }

  /** [monto] is this transfer's side in the requested currency; both sides when both match it. */
  data class DeTransferencia(
      val transferencia: Transferencia,
      val monto: Money,
      val direccion: Direccion,
  ) : Actividad {
    override val fecha: LocalDate
      get() = transferencia.fecha

    override val createdAt: Instant
      get() = transferencia.createdAt

    override val id: String
      get() = transferencia.id

    companion object {
      /** Null when neither side of [transferencia] is in [moneda]. */
      fun of(transferencia: Transferencia, moneda: Moneda): DeTransferencia? {
        val sale = transferencia.montoOrigen.moneda == moneda
        val entra = transferencia.montoDestino.moneda == moneda
        return when {
          sale && entra ->
              DeTransferencia(transferencia, transferencia.montoOrigen, Direccion.INTERNA)
          sale -> DeTransferencia(transferencia, transferencia.montoOrigen, Direccion.SALIDA)
          entra -> DeTransferencia(transferencia, transferencia.montoDestino, Direccion.ENTRADA)
          else -> null
        }
      }
    }
  }
}

/** Relative to the requested currency: money enters it, leaves it, or both sides are in it. */
enum class Direccion {
  ENTRADA,
  SALIDA,
  INTERNA,
}

/** The newest-first order every activity read shares with `movimiento_recent`. */
val ActividadRecentOrder: Comparator<Actividad> =
    compareByDescending<Actividad> { it.fecha }
        .thenByDescending { it.createdAt }
        .thenByDescending { it.id }
