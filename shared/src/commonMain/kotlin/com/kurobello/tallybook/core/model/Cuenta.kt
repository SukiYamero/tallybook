package com.kurobello.tallybook.core.model

import kotlin.time.Instant

enum class TipoCuenta {
  EFECTIVO,
  BANCO,
  CREDITO;

  /** The medio implied when only the account is known, e.g. a sentence naming "Nequi". */
  fun defaultMedio(tipo: TipoMovimiento): MedioPago = defaultMedios.getValue(this).forTipo(tipo)

  /** The color a new account of this type starts with until the user picks another. */
  val defaultColor: CategoryTint
    get() = defaultColors.getValue(this)
}

// A map instead of a constructor argument: MedioPago's entries already reference TipoCuenta.
private val defaultMedios: Map<TipoCuenta, MedioPago> =
    mapOf(
        TipoCuenta.EFECTIVO to MedioPago.EFECTIVO,
        TipoCuenta.BANCO to MedioPago.DEBITO,
        TipoCuenta.CREDITO to MedioPago.CREDITO,
    )

private val defaultColors: Map<TipoCuenta, CategoryTint> =
    mapOf(
        TipoCuenta.EFECTIVO to CategoryTint.EMERALD,
        TipoCuenta.BANCO to CategoryTint.BLUE,
        TipoCuenta.CREDITO to CategoryTint.PURPLE,
    )

/**
 * [saldoInicial] is signed and stored as received: a credit card's opening debt is already negative
 * here. [diaCorte] is the statement day of a [TipoCuenta.CREDITO] account, see [creditCycle].
 */
data class Cuenta(
    val id: String,
    val nombre: String,
    val tipo: TipoCuenta,
    val saldoInicial: Money,
    val diaCorte: Int?,
    val archivada: Boolean,
    val createdAt: Instant,
    val color: CategoryTint,
) {
  val moneda: Moneda
    get() = saldoInicial.moneda

  /** The seeded cash cuenta of its currency, whose moneda never changes. */
  val isDefaultCash: Boolean
    get() = id == defaultCashId(moneda)

  /**
   * Why a movimiento cannot point at this cuenta with [medio] in [moneda], or null when it can. An
   * archived cuenta only passes as [storedCuentaId], the one an edited movimiento already keeps.
   */
  fun rejectionFor(
      medio: MedioPago,
      moneda: Moneda,
      storedCuentaId: String? = null,
  ): CuentaRejection? =
      when {
        archivada && id != storedCuentaId -> CuentaRejection.ARCHIVED
        tipo !in medio.compatibleTipos -> CuentaRejection.MEDIO_MISMATCH
        moneda != this.moneda -> CuentaRejection.MONEDA_MISMATCH
        else -> null
      }

  fun accepts(medio: MedioPago, moneda: Moneda, storedCuentaId: String? = null): Boolean =
      rejectionFor(medio, moneda, storedCuentaId) == null

  companion object {
    /** Deterministic so two installations seeding the same default produce the same entity. */
    fun defaultCashId(moneda: Moneda): String = "cuenta_efectivo_${moneda.name.lowercase()}"
  }
}

enum class CuentaRejection {
  ARCHIVED,
  MEDIO_MISMATCH,
  MONEDA_MISMATCH,
}
