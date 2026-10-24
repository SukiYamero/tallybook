package com.kurobello.tallybook.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
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
    assertNull(TransferenciaRules.validate(valid, nequi, nu, today))
  }

  @Test
  fun transferToTheSameAccountIsRejected() {
    val same = valid.copy(cuentaDestinoId = nequi.id)

    assertEquals(
        TransferenciaError.SAME_CUENTA,
        TransferenciaRules.validate(same, nequi, nequi, today),
    )
  }

  @Test
  fun zeroOrNegativeAmountsAreRejectedPerSide() {
    val cases =
        listOf(
            valid.copy(montoOrigen = Money(0, Moneda.COP)) to
                TransferenciaError.MONTO_ORIGEN_NOT_POSITIVE,
            valid.copy(montoOrigen = Money(-1, Moneda.COP)) to
                TransferenciaError.MONTO_ORIGEN_NOT_POSITIVE,
            valid.copy(
                cuentaDestinoId = dolares.id,
                montoDestino = Money(0, Moneda.USD),
            ) to TransferenciaError.MONTO_DESTINO_NOT_POSITIVE,
        )

    for ((transferencia, expected) in cases) {
      val destino = if (transferencia.cuentaDestinoId == dolares.id) dolares else nu
      assertEquals(
          expected,
          TransferenciaRules.validate(transferencia, nequi, destino, today),
          "$transferencia",
      )
    }
  }

  @Test
  fun sameCurrencyAmountsMustMatch() {
    val differing = valid.copy(montoDestino = Money(49_000_00, Moneda.COP))

    assertEquals(
        TransferenciaError.MONTOS_DIFFER,
        TransferenciaRules.validate(differing, nequi, nu, today),
    )
  }

  @Test
  fun amountsMustBeInTheirAccountsCurrency() {
    val wrongOrigen = valid.copy(montoOrigen = Money(50_000_00, Moneda.USD))
    val wrongDestino =
        valid.copy(cuentaDestinoId = dolares.id, montoDestino = Money(12_00, Moneda.COP))

    assertEquals(
        TransferenciaError.MONTO_ORIGEN_MONEDA,
        TransferenciaRules.validate(wrongOrigen, nequi, nu, today),
    )
    assertEquals(
        TransferenciaError.MONTO_DESTINO_MONEDA,
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

    assertNull(TransferenciaRules.validate(crossCurrency, dolares, nequi, today))
  }

  @Test
  fun futureFechaIsRejected() {
    assertEquals(
        TransferenciaError.FECHA_FUTURE,
        TransferenciaRules.validate(valid.copy(fecha = LocalDate(2026, 6, 16)), nequi, nu, today),
    )
    assertNull(
        TransferenciaRules.validate(valid.copy(fecha = LocalDate(2026, 6, 14)), nequi, nu, today)
    )
  }

  @Test
  fun descripcionIsLimitedTo180Characters() {
    val longest = valid.copy(descripcion = "x".repeat(TRANSFERENCIA_DESCRIPCION_MAX))
    val tooLong = valid.copy(descripcion = "x".repeat(TRANSFERENCIA_DESCRIPCION_MAX + 1))

    assertEquals(180, TRANSFERENCIA_DESCRIPCION_MAX)
    assertNull(TransferenciaRules.validate(longest, nequi, nu, today))
    assertEquals(
        TransferenciaError.DESCRIPCION_TOO_LONG,
        TransferenciaRules.validate(tooLong, nequi, nu, today),
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
