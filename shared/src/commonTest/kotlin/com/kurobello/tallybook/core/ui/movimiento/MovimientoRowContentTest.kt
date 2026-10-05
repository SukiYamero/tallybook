package com.kurobello.tallybook.core.ui.movimiento

import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals

class MovimientoRowContentTest {
  @Test
  fun sharedAmountPresentationPreservesIncomeExpenseCurrencyAndScale() {
    assertEquals(
        "$\u00A0+1.234,00",
        signedMovimientoAmountText(Money(123400, Moneda.COP), TipoMovimiento.INGRESO),
    )
    assertEquals(
        "$\u00A0-567,00",
        signedMovimientoAmountText(Money(56700, Moneda.COP), TipoMovimiento.GASTO),
    )
    assertEquals("$-567", signedMovimientoAmountText(Money(567, Moneda.CLP), TipoMovimiento.GASTO))
    assertEquals(
        "$+12.34",
        signedMovimientoAmountText(Money(1234, Moneda.USD), TipoMovimiento.INGRESO),
    )
  }
}
