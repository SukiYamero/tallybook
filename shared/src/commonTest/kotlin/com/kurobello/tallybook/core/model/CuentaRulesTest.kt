package com.kurobello.tallybook.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CuentaRulesTest {
  @Test
  fun nombreIsTrimmedBeforeItsLengthIsChecked() {
    val longest = " ${"x".repeat(CuentaRules.NOMBRE_MAX)} "

    assertEquals(CuentaErrors(), CuentaRules.validate(longest, TipoCuenta.BANCO, diaCorte = null))
    assertEquals(
        CuentaNombreError.TOO_LONG,
        CuentaRules.validate("x".repeat(CuentaRules.NOMBRE_MAX + 1), TipoCuenta.BANCO, null).nombre,
    )
  }

  @Test
  fun blankNombreIsRejected() {
    for (nombre in listOf("", "   ", "\t")) {
      assertEquals(
          CuentaNombreError.BLANK,
          CuentaRules.validate(nombre, TipoCuenta.EFECTIVO, null).nombre,
          "'$nombre'",
      )
    }
  }

  @Test
  fun nombreLimitFollowsTheCategoriaConvention() {
    assertEquals(CategoriaRules.NOMBRE_MAX, CuentaRules.NOMBRE_MAX)
  }

  @Test
  fun nombreKeyIgnoresCaseAccentsAndSurroundingSpacesWithinOneMoneda() {
    assertEquals(
        CuentaRules.nombreKey("Ahorros Davivienda", Moneda.COP),
        CuentaRules.nombreKey("  ahórros DAVIVIENDA ", Moneda.COP),
    )
    assertEquals(
        CategoriaRules.nombreKey("Ahórros") == CategoriaRules.nombreKey("ahorros"),
        CuentaRules.nombreKey("Ahórros", Moneda.COP) ==
            CuentaRules.nombreKey("ahorros", Moneda.COP),
    )
    assertNotEquals(
        CuentaRules.nombreKey("Año", Moneda.COP),
        CuentaRules.nombreKey("Ano", Moneda.COP),
    )
  }

  @Test
  fun sameNombreInAnotherMonedaIsADifferentKey() {
    assertNotEquals(
        CuentaRules.nombreKey("Efectivo", Moneda.COP),
        CuentaRules.nombreKey("Efectivo", Moneda.USD),
    )
  }

  @Test
  fun diaCorteIsAcceptedOnlyOnCreditoAndWithinTheMonth() {
    val cases =
        listOf(
            Triple(TipoCuenta.CREDITO, null, null),
            Triple(TipoCuenta.CREDITO, 1, null),
            Triple(TipoCuenta.CREDITO, 31, null),
            Triple(TipoCuenta.CREDITO, 0, DiaCorteError.OUT_OF_RANGE),
            Triple(TipoCuenta.CREDITO, 32, DiaCorteError.OUT_OF_RANGE),
            Triple(TipoCuenta.EFECTIVO, null, null),
            Triple(TipoCuenta.BANCO, null, null),
            Triple(TipoCuenta.EFECTIVO, 15, DiaCorteError.NOT_CREDITO),
            Triple(TipoCuenta.BANCO, 40, DiaCorteError.NOT_CREDITO),
        )

    for ((tipo, diaCorte, expected) in cases) {
      assertEquals(
          expected,
          CuentaRules.validate("NU", tipo, diaCorte).diaCorte,
          "$tipo $diaCorte",
      )
    }
  }

  @Test
  fun errorsOfDifferentFieldsAreReportedTogether() {
    val errors = CuentaRules.validate(" ", TipoCuenta.BANCO, diaCorte = 10)

    assertEquals(CuentaErrors(CuentaNombreError.BLANK, DiaCorteError.NOT_CREDITO), errors)
    assertTrue(CuentaRules.validate("NU", TipoCuenta.CREDITO, 15).isValid)
  }

  @Test
  fun defaultCashIdIsDeterministicPerMoneda() {
    assertEquals("cuenta_efectivo_cop", Cuenta.defaultCashId(Moneda.COP))
    assertEquals("cuenta_efectivo_usd", Cuenta.defaultCashId(Moneda.USD))
  }
}
