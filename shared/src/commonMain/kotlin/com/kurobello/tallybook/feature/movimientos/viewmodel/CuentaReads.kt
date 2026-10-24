package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.CuentaRepository
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.model.CuentaSelection
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoMovimiento

/**
 * Reads [MovimientoRepository.defaultSelection] once without a medio and once per medio. A gasto
 * allows every medio, and [baseSelection] adapts the result to the draft's tipo.
 */
internal suspend fun MovimientoRepository.cuentaDefaults(
    primary: Moneda
): DataOutcome<CuentaDefaults> {
  val answers = mutableMapOf<MedioPago?, CuentaSelection?>()
  for (medio in listOf(null) + MedioPago.entries) {
    when (val outcome = defaultSelection(primary, TipoMovimiento.GASTO, medio)) {
      is DataOutcome.Failure -> return outcome
      is DataOutcome.Success -> answers[medio] = outcome.data
    }
  }
  return DataOutcome.Success(
      CuentaDefaults(base = answers[null], byMedio = MedioPago.entries.associateWith(answers::get))
  )
}

/**
 * Whether the repository would still take [cuentaId] for [medio] in [moneda]. Its refusals are all
 * invalid input, so this re-read tells a refused cuenta apart from any other rejected value. An
 * archived cuenta only passes as the [stored] one, which an untouched edit keeps.
 */
internal suspend fun CuentaRepository.accepts(
    cuentaId: String,
    medio: MedioPago,
    moneda: Moneda,
    stored: String? = null,
): Boolean {
  val cuenta = (getById(cuentaId) as? DataOutcome.Success)?.data ?: return false
  return (!cuenta.archivada || cuenta.id == stored) &&
      cuenta.tipo in medio.compatibleTipos &&
      cuenta.moneda == moneda
}
