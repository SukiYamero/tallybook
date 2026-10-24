package com.kurobello.tallybook.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class CuentaAcceptanceTest {

  @Test
  fun aLiveCuentaTakesACompatibleMedioInItsCurrency() {
    assertNull(nequi.rejectionFor(MedioPago.DEBITO, Moneda.COP))
    assertTrue(nequi.accepts(MedioPago.TRANSFERENCIA, Moneda.COP))
  }

  @Test
  fun anArchivedCuentaPassesOnlyAsTheStoredOne() {
    val archived = nequi.copy(archivada = true)

    assertEquals(CuentaRejection.ARCHIVED, archived.rejectionFor(MedioPago.DEBITO, Moneda.COP))
    assertEquals(
        CuentaRejection.ARCHIVED,
        archived.rejectionFor(MedioPago.DEBITO, Moneda.COP, storedCuentaId = "other"),
    )
    assertTrue(archived.accepts(MedioPago.DEBITO, Moneda.COP, storedCuentaId = nequi.id))
  }

  @Test
  fun aMedioOutsideTheCuentaTypeIsRejectedEvenForTheStoredCuenta() {
    assertEquals(
        CuentaRejection.MEDIO_MISMATCH,
        nequi.rejectionFor(MedioPago.CREDITO, Moneda.COP, storedCuentaId = nequi.id),
    )
    assertFalse(nequi.accepts(MedioPago.EFECTIVO, Moneda.COP))
  }

  @Test
  fun anotherCurrencyIsRejectedEvenForTheStoredCuenta() {
    assertEquals(
        CuentaRejection.MONEDA_MISMATCH,
        nequi.rejectionFor(MedioPago.DEBITO, Moneda.USD, storedCuentaId = nequi.id),
    )
  }

  @Test
  fun onlyTheDeterministicCashIdOfItsOwnCurrencyIsTheDefaultCash() {
    val cash = nequi.copy(id = Cuenta.defaultCashId(Moneda.COP), tipo = TipoCuenta.EFECTIVO)

    assertTrue(cash.isDefaultCash)
    assertFalse(nequi.isDefaultCash)
    assertFalse(cash.copy(saldoInicial = Money(0, Moneda.USD)).isDefaultCash)
  }

  private val nequi =
      Cuenta(
          id = "nequi",
          nombre = "Nequi",
          tipo = TipoCuenta.BANCO,
          saldoInicial = Money(0, Moneda.COP),
          diaCorte = null,
          archivada = false,
          createdAt = Instant.parse("2026-01-01T00:00:00Z"),
      )
}
