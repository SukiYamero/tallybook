package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoMovimiento

internal enum class CuentaFieldError {
  MISSING,

  /** The repository refused the cuenta, e.g. it was archived while the draft was open. */
  UNAVAILABLE,
}

/**
 * The Medio and Account sections every movimiento surface shows. [options] are the cuentas the
 * picker offers for [medio]; [selected] is the cuenta the movimiento would be saved with, which is
 * the only option when there is just one. [primaryMoneda] marks which cuentas need their code.
 */
internal data class MovimientoCuentaUi(
    val medio: MedioPago?,
    val medioOptions: List<MedioPago>,
    val selected: Cuenta?,
    val options: List<Cuenta>,
    val suggestions: List<Cuenta> = emptyList(),
    val primaryMoneda: Moneda? = null,
    val isMedioMissing: Boolean = false,
    val cuentaError: CuentaFieldError? = null,
) {
  /** The medio saved with [selected]: an undecided one follows the cuenta's type. */
  fun effectiveMedio(tipo: TipoMovimiento): MedioPago? = medio ?: selected?.tipo?.defaultMedio(tipo)
}

/**
 * Builds the field from a selection. A [kept] cuenta (an editor's untouched archived cuenta) is
 * offered even though it is no longer active.
 */
@Suppress("LongParameterList")
internal fun movimientoCuentaUi(
    selection: CuentaSelectionState,
    cuentas: List<Cuenta>,
    tipo: TipoMovimiento,
    submitAttempted: Boolean,
    cuentaRejected: Boolean,
    moneda: Moneda? = null,
    primaryMoneda: Moneda? = null,
    kept: Cuenta? = null,
    suggestionIds: List<String> = emptyList(),
): MovimientoCuentaUi {
  val active = compatibleCuentas(cuentas, selection.medio, moneda)
  val options =
      if (kept != null && kept !in active && kept.id == selection.cuentaId) listOf(kept) + active
      else active
  val selected = options.firstOrNull { it.id == selection.cuentaId } ?: options.singleOrNull()
  val medio = selection.medio
  return MovimientoCuentaUi(
      medio = medio,
      medioOptions = medioOptions(tipo),
      selected = selected,
      options = options,
      suggestions =
          suggestionIds
              .mapNotNull { id -> cuentas.firstOrNull { it.id == id && !it.archivada } }
              .filter { it.id != selected?.id && (moneda == null || it.moneda == moneda) },
      primaryMoneda = primaryMoneda,
      isMedioMissing = submitAttempted && medio == null && selected == null,
      cuentaError =
          when {
            cuentaRejected -> CuentaFieldError.UNAVAILABLE
            submitAttempted && selected == null -> CuentaFieldError.MISSING
            else -> null
          },
  )
}
