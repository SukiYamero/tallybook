package com.kurobello.tallybook.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class TransferenciaRulesTest {
  private val createdAt = Instant.parse("2026-06-15T12:00:00Z")
  private val today = LocalDate(2026, 6, 15)
  private val nequi = cuenta("nequi", TipoCuenta.BANCO, Moneda.COP)
  private val nu = cuenta("nu", TipoCuenta.CREDITO, Moneda.COP)
  private val dolares = cuenta("dolares", TipoCuenta.EFECTIVO, Moneda.USD)

  private val valid =
      Transferencia(
          id = "t1",
          cuentaOrigenId = nequi.id,
          cuentaDestinoId = nu.id,
          montoOrigen = Money(50_000_00, Moneda.COP),
          montoDestino = Money(50_000_00, Moneda.COP),
          fecha = today,
          descripcion = null,
          createdAt = createdAt,
      )

  @Test
  fun sameCurrencyTransferWithEqualAmountsTodayIsValid() {
    val errors = TransferenciaRules.validate(valid, nequi, nu, today)

    assertEquals(TransferenciaErrors(), errors)
    assertTrue(errors.isValid)
  }

  @Test
  fun transferToTheSameAccountIsRejected() {
    val same = valid.copy(cuentaDestinoId = nequi.id)

    assertTrue(TransferenciaRules.validate(same, nequi, nequi, today).sameCuenta)
  }

  @Test
  fun zeroOrNegativeAmountsAreRejectedPerSide() {
    val zeroOrigen = valid.copy(montoOrigen = Money(0, Moneda.COP))
    val negativeOrigen = valid.copy(montoOrigen = Money(-1, Moneda.COP))
    val zeroDestino = valid.copy(cuentaDestinoId = dolares.id, montoDestino = Money(0, Moneda.USD))

    assertEquals(
        TransferenciaMontoError.NOT_POSITIVE,
        TransferenciaRules.validate(zeroOrigen, nequi, nu, today).montoOrigen,
    )
    assertEquals(
        TransferenciaMontoError.NOT_POSITIVE,
        TransferenciaRules.validate(negativeOrigen, nequi, nu, today).montoOrigen,
    )
    assertEquals(
        TransferenciaErrors(montoDestino = TransferenciaMontoError.NOT_POSITIVE),
        TransferenciaRules.validate(zeroDestino, nequi, dolares, today),
    )
  }

  @Test
  fun sameCurrencyAmountsMustMatch() {
    val differing = valid.copy(montoDestino = Money(49_000_00, Moneda.COP))

    assertEquals(
        TransferenciaErrors(montoDestino = TransferenciaMontoError.DIFFERS_FROM_ORIGEN),
        TransferenciaRules.validate(differing, nequi, nu, today),
    )
  }

  @Test
  fun amountsMustBeInTheirAccountsCurrency() {
    val wrongOrigen = valid.copy(montoOrigen = Money(50_000_00, Moneda.USD))
    val wrongDestino =
        valid.copy(cuentaDestinoId = dolares.id, montoDestino = Money(12_00, Moneda.COP))

    assertEquals(
        TransferenciaMontoError.WRONG_MONEDA,
        TransferenciaRules.validate(wrongOrigen, nequi, nu, today).montoOrigen,
    )
    assertEquals(
        TransferenciaErrors(montoDestino = TransferenciaMontoError.WRONG_MONEDA),
        TransferenciaRules.validate(wrongDestino, nequi, dolares, today),
    )
  }

  @Test
  fun crossCurrencyTransferKeepsItsOwnReceivedAmount() {
    val crossCurrency =
        valid.copy(
            cuentaOrigenId = dolares.id,
            cuentaDestinoId = nequi.id,
            montoOrigen = Money(100_00, Moneda.USD),
            montoDestino = Money(395_000_00, Moneda.COP),
        )

    assertTrue(TransferenciaRules.validate(crossCurrency, dolares, nequi, today).isValid)
  }

  @Test
  fun futureFechaIsRejected() {
    val tomorrow = valid.copy(fecha = LocalDate(2026, 6, 16))
    val yesterday = valid.copy(fecha = LocalDate(2026, 6, 14))

    assertEquals(
        TransferenciaErrors(fechaFuture = true),
        TransferenciaRules.validate(tomorrow, nequi, nu, today),
    )
    assertTrue(TransferenciaRules.validate(yesterday, nequi, nu, today).isValid)
  }

  @Test
  fun descripcionIsLimitedTo180Characters() {
    val longest = valid.copy(descripcion = "x".repeat(TRANSFERENCIA_DESCRIPCION_MAX))
    val tooLong = valid.copy(descripcion = "x".repeat(TRANSFERENCIA_DESCRIPCION_MAX + 1))

    assertEquals(180, TRANSFERENCIA_DESCRIPCION_MAX)
    assertTrue(TransferenciaRules.validate(longest, nequi, nu, today).isValid)
    assertEquals(
        TransferenciaErrors(descripcionTooLong = true),
        TransferenciaRules.validate(tooLong, nequi, nu, today),
    )
  }

  @Test
  fun everyFailingFieldIsReportedAtOnce() {
    val broken =
        valid.copy(
            cuentaDestinoId = nequi.id,
            montoOrigen = Money(0, Moneda.COP),
            montoDestino = Money(5_00, Moneda.COP),
            fecha = LocalDate(2026, 7, 1),
            descripcion = "x".repeat(TRANSFERENCIA_DESCRIPCION_MAX + 1),
        )

    assertEquals(
        TransferenciaErrors(
            sameCuenta = true,
            montoOrigen = TransferenciaMontoError.NOT_POSITIVE,
            montoDestino = TransferenciaMontoError.DIFFERS_FROM_ORIGEN,
            fechaFuture = true,
            descripcionTooLong = true,
        ),
        TransferenciaRules.validate(broken, nequi, nequi, today),
    )
  }

  @Test
  fun accountsThatAreNotTheTransfersSidesAreACallerBug() {
    assertFailsWith<IllegalArgumentException> {
      TransferenciaRules.validate(valid, nu, nequi, today)
    }
  }

  private fun cuenta(id: String, tipo: TipoCuenta, moneda: Moneda) =
      Cuenta(
          id = id,
          nombre = id,
          tipo = tipo,
          saldoInicial = Money(0, moneda),
          diaCorte = null,
          archivada = false,
          createdAt = createdAt,
      )
}
