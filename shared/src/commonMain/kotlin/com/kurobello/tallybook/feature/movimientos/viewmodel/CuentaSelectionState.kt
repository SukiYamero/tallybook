package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.entry.SentenceSelection
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaSelection
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoMovimiento

/**
 * Who decided a draft's medio and cuenta. [DEFAULT] follows the repository's resolved selection,
 * [SENTENCE] is replaced by every interpretation and [USER] survives both, like the tags' owners.
 */
internal enum class SelectionOwner {
  DEFAULT,
  SENTENCE,
  USER,
}

internal data class CuentaSelectionState(
    val medio: MedioPago?,
    val cuentaId: String?,
    val owner: SelectionOwner,
) {
  companion object {
    val NONE = CuentaSelectionState(medio = null, cuentaId = null, owner = SelectionOwner.DEFAULT)
  }
}

/**
 * The repository's resolved selections cached per medio, so the selection rules stay synchronous.
 * They are read for gastos, which allow every medio; [baseSelection] adapts them to the tipo.
 */
internal data class CuentaDefaults(
    val base: CuentaSelection?,
    val byMedio: Map<MedioPago, CuentaSelection?>,
)

/**
 * Layers ownership only; [default] is already resolved by MovimientoRepository.defaultSelection.
 */
internal fun preselect(default: CuentaSelection): CuentaSelectionState =
    CuentaSelectionState(default.medio, default.cuentaId, SelectionOwner.DEFAULT)

internal fun baseSelection(defaults: CuentaDefaults?, tipo: TipoMovimiento): CuentaSelectionState =
    defaults?.base?.let(::preselect)?.forTipo(tipo) ?: CuentaSelectionState.NONE

internal fun medioOptions(tipo: TipoMovimiento): List<MedioPago> =
    MedioPago.entries.filter { it.allows(tipo) }

/** Live, active cuentas a [medio] can use; without a medio any type fits. */
internal fun compatibleCuentas(
    cuentas: List<Cuenta>,
    medio: MedioPago?,
    moneda: Moneda? = null,
): List<Cuenta> = cuentas.filter { cuenta ->
  !cuenta.archivada &&
      (medio == null || cuenta.tipo in medio.compatibleTipos) &&
      (moneda == null || cuenta.moneda == moneda)
}

internal fun CuentaSelectionState.forTipo(tipo: TipoMovimiento): CuentaSelectionState =
    copy(medio = medio?.forTipo(tipo))

/**
 * A compatible active cuenta stays; otherwise the cached default for [medio] takes over, empty when
 * there is none or it is not in the required [moneda].
 */
internal fun CuentaSelectionState.withMedio(
    medio: MedioPago,
    cuentas: List<Cuenta>,
    defaults: CuentaDefaults?,
    owner: SelectionOwner = SelectionOwner.USER,
    moneda: Moneda? = null,
): CuentaSelectionState {
  val current = cuentas.firstOrNull { it.id == cuentaId }
  val keeps = current != null && current in compatibleCuentas(listOf(current), medio, moneda)
  val replacement =
      defaults?.byMedio?.get(medio)?.takeIf { moneda == null || it.moneda == moneda }?.cuentaId
  return CuentaSelectionState(medio, if (keeps) cuentaId else replacement, owner)
}

internal fun CuentaSelectionState.withCuenta(
    cuenta: Cuenta,
    tipo: TipoMovimiento,
): CuentaSelectionState {
  val kept = medio?.takeIf { cuenta.tipo in it.compatibleTipos && it.allows(tipo) }
  return CuentaSelectionState(
      kept ?: cuenta.tipo.defaultMedio(tipo),
      cuenta.id,
      SelectionOwner.USER,
  )
}

/**
 * A named cuenta wins, a medio alone keeps or replaces the default's cuenta, silence is default.
 */
internal fun CuentaSelectionState.withSentence(
    sentence: SentenceSelection,
    cuentas: List<Cuenta>,
    defaults: CuentaDefaults?,
    tipo: TipoMovimiento,
): CuentaSelectionState {
  if (owner == SelectionOwner.USER) return this
  val base = baseSelection(defaults, tipo)
  val cuenta = sentence.cuenta.cuenta
  val medio = sentence.medio.medio
  return when {
    cuenta != null ->
        CuentaSelectionState(
            medio ?: cuenta.tipo.defaultMedio(tipo),
            cuenta.id,
            SelectionOwner.SENTENCE,
        )
    medio != null -> base.withMedio(medio, cuentas, defaults, SelectionOwner.SENTENCE)
    else -> base
  }
}

internal fun CuentaSelectionState.withoutSentence(
    defaults: CuentaDefaults?,
    tipo: TipoMovimiento,
): CuentaSelectionState = if (owner == SelectionOwner.USER) this else baseSelection(defaults, tipo)
