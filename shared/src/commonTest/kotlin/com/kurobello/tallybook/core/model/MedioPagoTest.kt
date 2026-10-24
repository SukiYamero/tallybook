package com.kurobello.tallybook.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MedioPagoTest {
  private data class Row(
      val medio: MedioPago,
      val tipos: Set<TipoCuenta>,
      val movimientos: Set<TipoMovimiento>,
  )

  private val table =
      listOf(
          Row(
              MedioPago.EFECTIVO,
              setOf(TipoCuenta.EFECTIVO),
              setOf(TipoMovimiento.GASTO, TipoMovimiento.INGRESO),
          ),
          Row(MedioPago.DEBITO, setOf(TipoCuenta.BANCO), setOf(TipoMovimiento.GASTO)),
          Row(
              MedioPago.TRANSFERENCIA,
              setOf(TipoCuenta.BANCO),
              setOf(TipoMovimiento.GASTO, TipoMovimiento.INGRESO),
          ),
          Row(
              MedioPago.CREDITO,
              setOf(TipoCuenta.CREDITO),
              setOf(TipoMovimiento.GASTO, TipoMovimiento.INGRESO),
          ),
      )

  @Test
  fun everyMedioAndCuentaAndMovimientoCombinationFollowsTheTable() {
    assertEquals(MedioPago.entries.toSet(), table.map { it.medio }.toSet())
    for (row in table) {
      for (tipoCuenta in TipoCuenta.entries) {
        for (tipo in TipoMovimiento.entries) {
          val expected = tipoCuenta in row.tipos && tipo in row.movimientos
          val actual = tipoCuenta in row.medio.compatibleTipos && row.medio.allows(tipo)
          assertEquals(expected, actual, "${row.medio} $tipoCuenta $tipo")
        }
      }
    }
  }

  @Test
  fun switchingToIngresoTurnsDebitoIntoTransferenciaAndKeepsTheRest() {
    val expected =
        mapOf(
            (MedioPago.EFECTIVO to TipoMovimiento.INGRESO) to MedioPago.EFECTIVO,
            (MedioPago.DEBITO to TipoMovimiento.INGRESO) to MedioPago.TRANSFERENCIA,
            (MedioPago.TRANSFERENCIA to TipoMovimiento.INGRESO) to MedioPago.TRANSFERENCIA,
            (MedioPago.CREDITO to TipoMovimiento.INGRESO) to MedioPago.CREDITO,
            (MedioPago.EFECTIVO to TipoMovimiento.GASTO) to MedioPago.EFECTIVO,
            (MedioPago.DEBITO to TipoMovimiento.GASTO) to MedioPago.DEBITO,
            (MedioPago.TRANSFERENCIA to TipoMovimiento.GASTO) to MedioPago.TRANSFERENCIA,
            (MedioPago.CREDITO to TipoMovimiento.GASTO) to MedioPago.CREDITO,
        )

    for ((input, result) in expected) {
      val (medio, tipo) = input
      assertEquals(result, medio.forTipo(tipo), "$medio $tipo")
    }
  }

  @Test
  fun switchedMedioIsAllowedAndStaysOnTheSameAccountType() {
    for (medio in MedioPago.entries) {
      for (tipo in TipoMovimiento.entries) {
        val switched = medio.forTipo(tipo)
        assertTrue(switched.allows(tipo), "$medio $tipo")
        assertEquals(medio.compatibleTipos, switched.compatibleTipos, "$medio $tipo")
      }
    }
  }

  @Test
  fun everyTipoCuentaHasADefaultMedioPerTipoMovimiento() {
    val expected =
        mapOf(
            (TipoCuenta.EFECTIVO to TipoMovimiento.GASTO) to MedioPago.EFECTIVO,
            (TipoCuenta.EFECTIVO to TipoMovimiento.INGRESO) to MedioPago.EFECTIVO,
            (TipoCuenta.BANCO to TipoMovimiento.GASTO) to MedioPago.DEBITO,
            (TipoCuenta.BANCO to TipoMovimiento.INGRESO) to MedioPago.TRANSFERENCIA,
            (TipoCuenta.CREDITO to TipoMovimiento.GASTO) to MedioPago.CREDITO,
            (TipoCuenta.CREDITO to TipoMovimiento.INGRESO) to MedioPago.CREDITO,
        )

    for ((input, medio) in expected) {
      val (tipoCuenta, tipo) = input
      val actual = tipoCuenta.defaultMedio(tipo)
      assertEquals(medio, actual, "$tipoCuenta $tipo")
      assertTrue(tipoCuenta in actual.compatibleTipos && actual.allows(tipo), "$tipoCuenta $tipo")
    }
  }
}
