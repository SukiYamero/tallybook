package com.kurobello.tallybook.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class TransferenciaRulesTest {
  private val today = LocalDate(2026, 6, 15)
  private val nequi = cuenta("nequi", TipoCuenta.BANCO, Moneda.COP)
  private val nu = cuenta("nu", TipoCuenta.CREDITO, Moneda.COP)
  private val dolares = cuenta("dolares", TipoCuenta.EFECTIVO, Moneda.USD)
  private val cop = Money(50_000_00, Moneda.COP)

  @Test
  fun sameCurrencyTransferWithEqualAmountsTodayIsValid() {
    val errors = validate()

    assertEquals(TransferenciaErrors(), errors)
    assertTrue(errors.isValid)
  }

  @Test
  fun missingAccountsAndAmountsAreReportedPerField() {
    assertEquals(
        TransferenciaErrors(
            cuentaOrigen = TransferenciaCuentaError.MISSING,
            cuentaDestino = TransferenciaCuentaError.MISSING,
            montoOrigen = TransferenciaMontoError.MISSING,
            montoDestino = TransferenciaMontoError.MISSING,
        ),
        validate(origen = null, destino = null, montoOrigen = null, montoDestino = null),
    )
  }

  @Test
  fun payCardFromAnotherCurrencyStartsWithOnlyTheOriginAmountMissing() {
    val deuda = Money(395_000_00, Moneda.COP)

    assertEquals(
        TransferenciaErrors(montoOrigen = TransferenciaMontoError.MISSING),
        validate(origen = dolares, destino = nu, montoOrigen = null, montoDestino = deuda),
    )
  }

  @Test
  fun transferToTheSameAccountIsRejected() {
    assertEquals(
        TransferenciaCuentaError.SAME_AS_ORIGEN,
        validate(origen = nequi, destino = nequi).cuentaDestino,
    )
  }

  @Test
  fun zeroOrNegativeAmountsAreRejectedPerSide() {
    assertEquals(
        TransferenciaMontoError.NOT_POSITIVE,
        validate(montoOrigen = Money(0, Moneda.COP)).montoOrigen,
    )
    assertEquals(
        TransferenciaMontoError.NOT_POSITIVE,
        validate(montoOrigen = Money(-1, Moneda.COP)).montoOrigen,
    )
    assertEquals(
        TransferenciaErrors(montoDestino = TransferenciaMontoError.NOT_POSITIVE),
        validate(destino = dolares, montoDestino = Money(0, Moneda.USD)),
    )
  }

  @Test
  fun sameCurrencyAmountsMustMatch() {
    assertEquals(
        TransferenciaErrors(montoDestino = TransferenciaMontoError.DIFFERS_FROM_ORIGEN),
        validate(montoDestino = Money(49_000_00, Moneda.COP)),
    )
  }

  @Test
  fun invalidOriginAmountIsNotAlsoReportedAsAMismatch() {
    assertEquals(
        TransferenciaErrors(montoOrigen = TransferenciaMontoError.NOT_POSITIVE),
        validate(montoOrigen = Money(0, Moneda.COP)),
    )
    assertEquals(
        TransferenciaErrors(montoOrigen = TransferenciaMontoError.WRONG_MONEDA),
        validate(montoOrigen = Money(50_000_00, Moneda.USD)),
    )
  }

  @Test
  fun amountsMustBeInTheirAccountsCurrency() {
    assertEquals(
        TransferenciaMontoError.WRONG_MONEDA,
        validate(montoOrigen = Money(50_000_00, Moneda.USD)).montoOrigen,
    )
    assertEquals(
        TransferenciaErrors(montoDestino = TransferenciaMontoError.WRONG_MONEDA),
        validate(destino = dolares, montoDestino = Money(12_00, Moneda.COP)),
    )
  }

  @Test
  fun crossCurrencyTransferKeepsItsOwnReceivedAmount() {
    val errors =
        validate(
            origen = dolares,
            destino = nequi,
            montoOrigen = Money(100_00, Moneda.USD),
            montoDestino = Money(395_000_00, Moneda.COP),
        )

    assertTrue(errors.isValid)
  }

  @Test
  fun futureFechaIsRejected() {
    assertEquals(
        TransferenciaErrors(fechaFuture = true),
        validate(fecha = LocalDate(2026, 6, 16)),
    )
    assertTrue(validate(fecha = LocalDate(2026, 6, 14)).isValid)
  }

  @Test
  fun descripcionIsLimitedTo180Characters() {
    assertEquals(180, TRANSFERENCIA_DESCRIPCION_MAX)
    assertTrue(validate(descripcion = "x".repeat(TRANSFERENCIA_DESCRIPCION_MAX)).isValid)
    assertEquals(
        TransferenciaErrors(descripcionTooLong = true),
        validate(descripcion = "x".repeat(TRANSFERENCIA_DESCRIPCION_MAX + 1)),
    )
  }

  @Test
  fun everyFailingFieldIsReportedAtOnce() {
    val errors =
        validate(
            destino = nequi,
            montoOrigen = Money(5_00, Moneda.COP),
            montoDestino = Money(0, Moneda.COP),
            fecha = LocalDate(2026, 7, 1),
            descripcion = "x".repeat(TRANSFERENCIA_DESCRIPCION_MAX + 1),
        )

    assertEquals(
        TransferenciaErrors(
            cuentaDestino = TransferenciaCuentaError.SAME_AS_ORIGEN,
            montoDestino = TransferenciaMontoError.NOT_POSITIVE,
            fechaFuture = true,
            descripcionTooLong = true,
        ),
        errors,
    )
  }

  private fun validate(
      origen: Cuenta? = nequi,
      destino: Cuenta? = nu,
      montoOrigen: Money? = cop,
      montoDestino: Money? = cop,
      fecha: LocalDate = today,
      descripcion: String? = null,
  ): TransferenciaErrors =
      TransferenciaRules.validate(
          origen = origen,
          destino = destino,
          montoOrigen = montoOrigen,
          montoDestino = montoDestino,
          fecha = fecha,
          descripcion = descripcion,
          today = today,
      )

  private fun cuenta(id: String, tipo: TipoCuenta, moneda: Moneda) =
      Cuenta(
          id = id,
          nombre = id,
          tipo = tipo,
          saldoInicial = Money(0, moneda),
          diaCorte = null,
          archivada = false,
          createdAt = Instant.parse("2026-06-15T12:00:00Z"),
      )
}
