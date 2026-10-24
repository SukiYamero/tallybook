package com.kurobello.tallybook.feature.movimientos.ui

import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.TipoCuenta
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickCuentaTipoTest {

  @Test
  fun theMedioPresetsTheTypeANewCuentaNeeds() {
    assertEquals(
        mapOf(
            MedioPago.EFECTIVO to TipoCuenta.EFECTIVO,
            MedioPago.DEBITO to TipoCuenta.BANCO,
            MedioPago.TRANSFERENCIA to TipoCuenta.BANCO,
            MedioPago.CREDITO to TipoCuenta.CREDITO,
        ),
        MedioPago.entries.associateWith(::quickCuentaTipo),
    )
  }

  @Test
  fun anUndecidedMedioStartsACashCuenta() {
    assertEquals(TipoCuenta.EFECTIVO, quickCuentaTipo(null))
  }
}
