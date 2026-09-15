package com.kurobello.tallybook.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MovimientoSummaryTest {

  @Test
  fun balanceSubtractsGastosFromIngresos() {
    val summary =
        MovimientoSummary(
            ingresos = Money(2_500, Moneda.COP),
            gastos = Money(750, Moneda.COP),
        )

    assertEquals(Money(1_750, Moneda.COP), summary.balance)
  }
}
