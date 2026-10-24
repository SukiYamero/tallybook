package com.kurobello.tallybook.feature.movimientos.ui

import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountDraftState
import com.kurobello.tallybook.feature.accounts.viewmodel.QuickCuentaActions
import com.kurobello.tallybook.feature.accounts.viewmodel.QuickCuentaUiState
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoCuentaUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.emptyFlow

class CuentaCreationTest {

  @Test
  fun startPinsTheCurrencyOnlyWhenTheSurfaceAsksForIt() {
    val actions = RecordingQuickCuentaActions()
    val field = MovimientoCuentaUi(MedioPago.DEBITO, MedioPago.entries, null, emptyList())

    CuentaCreation(QuickCuentaUiState(), actions, emptyFlow(), Moneda.USD).start(field)
    CuentaCreation(QuickCuentaUiState(), actions, emptyFlow(), Moneda.USD, pinsMoneda = true)
        .start(field)

    assertEquals(
        listOf(
            Triple(TipoCuenta.BANCO, Moneda.USD, false),
            Triple(TipoCuenta.BANCO, Moneda.USD, true),
        ),
        actions.starts,
    )
  }
}

private class RecordingQuickCuentaActions : QuickCuentaActions {
  val starts = mutableListOf<Triple<TipoCuenta, Moneda, Boolean>>()

  override fun onStart(tipo: TipoCuenta, moneda: Moneda, nombre: String, monedaPinned: Boolean) {
    starts += Triple(tipo, moneda, monedaPinned)
  }

  override fun onDraftChanged(transform: AccountDraftState.() -> AccountDraftState) = Unit

  override fun onSave() = Unit

  override fun onUseDuplicate() = Unit

  override fun onClose() = Unit
}
