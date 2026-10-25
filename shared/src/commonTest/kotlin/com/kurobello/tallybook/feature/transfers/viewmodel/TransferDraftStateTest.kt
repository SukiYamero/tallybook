package com.kurobello.tallybook.feature.transfers.viewmodel

import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.Transferencia
import com.kurobello.tallybook.core.model.TransferenciaCuentaError
import com.kurobello.tallybook.core.text.AmountInputError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class TransferDraftStateTest {

  @Test
  fun sameCurrencyShowsOneAmountThatMovesTheSameMoneyOnBothSides() {
    val state = newState(origenId = NEQUI.id, destinoId = EFECTIVO.id).withMontoOrigen("15000")

    assertFalse(state.isCrossCurrency)
    val transferencia = assertNotNull(state.submitted().toTransferencia(ID, CREATED_AT))
    assertEquals(Money(1_500_000, Moneda.COP), transferencia.montoOrigen)
    assertEquals(Money(1_500_000, Moneda.COP), transferencia.montoDestino)
  }

  @Test
  fun crossCurrencyAsksForTheReceivedAmountInTheDestinationCurrency() {
    val state =
        newState(origenId = DOLARES.id, destinoId = NEQUI.id).withMontoOrigen("100.50").submitted()

    assertTrue(state.isCrossCurrency)
    assertEquals(AmountInputError.EMPTY, state.visibleErrors.montoDestino)
    assertNull(state.toTransferencia(ID, CREATED_AT))

    val received = state.withMontoDestino("400000")
    val transferencia = assertNotNull(received.toTransferencia(ID, CREATED_AT))
    assertEquals(Money(10_050, Moneda.USD), transferencia.montoOrigen)
    assertEquals(Money(40_000_000, Moneda.COP), transferencia.montoDestino)
  }

  @Test
  fun eachSideReadsItsAmountWithItsOwnCurrencyScale() {
    val state =
        newState(origenId = NEQUI.id, destinoId = PESOS_CHILE.id)
            .withMontoOrigen("1000")
            .withMontoDestino("1500,5")
            .submitted()

    assertEquals(AmountInputError.TOO_MANY_DECIMALS, state.visibleErrors.montoDestino)
    assertNull(state.visibleErrors.montoOrigen)
  }

  @Test
  fun everyInvalidFieldIsReportedAtOnceOnlyAfterSubmitting() {
    val pristine =
        newState(origenId = NEQUI.id, destinoId = NEQUI.id)
            .withMontoOrigen("0")
            .withFecha(TODAY.plusDays(1))

    assertEquals(TransferFieldErrors(), pristine.visibleErrors)
    val submitted = pristine.submitted()
    assertEquals(
        TransferFieldErrors(
            destino = TransferenciaCuentaError.SAME_AS_ORIGEN,
            montoOrigen = AmountInputError.NOT_POSITIVE,
            fechaFuture = true,
        ),
        submitted.visibleErrors,
    )
    assertNull(submitted.toTransferencia(ID, CREATED_AT))
  }

  @Test
  fun theFirstInvalidFieldFollowsTheFormOrderOnlyAfterSubmitting() {
    val empty = newState(origenId = null, destinoId = null)
    assertNull(empty.firstInvalidField)
    assertEquals(TransferField.Origen, empty.submitted().firstInvalidField)

    val withAccounts = newState(origenId = NEQUI.id, destinoId = EFECTIVO.id).submitted()
    assertEquals(TransferField.MontoOrigen, withAccounts.firstInvalidField)

    val cross =
        newState(origenId = DOLARES.id, destinoId = NEQUI.id).withMontoOrigen("10").submitted()
    assertEquals(TransferField.MontoDestino, cross.firstInvalidField)

    val valid = withAccounts.withMontoOrigen("150")
    assertNull(valid.firstInvalidField)
  }

  @Test
  fun aMissingAccountIsReportedOnItsSide() {
    val state = newState(origenId = NEQUI.id, destinoId = null).withMontoOrigen("10").submitted()

    assertEquals(TransferenciaCuentaError.MISSING, state.visibleErrors.destino)
    assertNull(state.visibleErrors.origen)
  }

  @Test
  fun choosingAnOriginInAnotherCurrencyKeepsTheReceivedAmountAndEmptiesTheSentOne() {
    val state =
        newState(origenId = NEQUI.id, destinoId = EFECTIVO.id)
            .withMontoOrigen("20000")
            .withOrigen(DOLARES.id)

    assertTrue(state.isCrossCurrency)
    assertEquals("", state.draft.montoOrigen)
    assertEquals("20000", state.draft.montoDestino)
  }

  @Test
  fun switchingBackToTheSameCurrencyDropsTheReceivedFieldAndKeepsTheUnchangedSide() {
    val cross =
        newState(origenId = DOLARES.id, destinoId = NEQUI.id)
            .withMontoOrigen("100")
            .withMontoDestino("400000")

    val sameByOrigen = cross.withOrigen(EFECTIVO.id)
    assertFalse(sameByOrigen.isCrossCurrency)
    assertEquals("400000", sameByOrigen.draft.montoOrigen)
    assertEquals(
        Money(40_000_000, Moneda.COP),
        sameByOrigen.submitted().toTransferencia(ID, CREATED_AT)?.montoDestino,
    )

    val crossToDollars = cross.withDestino(AHORRO_USD.id)
    assertFalse(crossToDollars.isCrossCurrency)
    assertEquals("100", crossToDollars.draft.montoDestino)
  }

  @Test
  fun aValidSingleAmountMovesToTheNewCurrencySeparator() {
    val byOrigen = newState(origenId = null, destinoId = null).withMontoOrigen("12,50")
    val inDollars = byOrigen.withOrigen(DOLARES.id)
    assertEquals("12.50", inDollars.draft.montoOrigen)
    assertEquals("12.50", inDollars.draft.montoDestino)
    assertEquals(
        Money(1_250, Moneda.USD),
        inDollars
            .withDestino(AHORRO_USD.id)
            .submitted()
            .toTransferencia(ID, CREATED_AT)
            ?.montoOrigen,
    )

    val byDestino =
        newState(origenId = null, destinoId = DOLARES.id)
            .withMontoOrigen("7.25")
            .withDestino(NEQUI.id)
    assertEquals("7,25", byDestino.draft.montoOrigen)
    assertEquals("7,25", byDestino.draft.montoDestino)
  }

  @Test
  fun anInvalidSingleAmountStaysAsTypedWhenTheCurrencyChanges() {
    val state =
        newState(origenId = null, destinoId = null).withMontoOrigen("12,5,0").withOrigen(DOLARES.id)

    assertEquals("12,5,0", state.draft.montoOrigen)
  }

  @Test
  fun payCardFromTheSameCurrencyPrefillsTheOnlyAmountWithTheDebt() {
    val state =
        TransferDraftState.forNew(
            CUENTAS,
            Moneda.COP,
            TransferEntry.New(origenId = NEQUI.id, destinoId = NU.id, prefill = DEBT),
            TODAY,
        )

    assertFalse(state.isCrossCurrency)
    assertEquals("3500,00", state.draft.montoOrigen)
    assertEquals(DEBT, state.submitted().toTransferencia(ID, CREATED_AT)?.montoDestino)
  }

  @Test
  fun payCardFromAnotherCurrencyLeavesTheSentAmountEmpty() {
    val state =
        TransferDraftState.forNew(
            CUENTAS,
            Moneda.COP,
            TransferEntry.New(origenId = DOLARES.id, destinoId = NU.id, prefill = DEBT),
            TODAY,
        )

    assertTrue(state.isCrossCurrency)
    assertEquals("", state.draft.montoOrigen)
    assertEquals("3500,00", state.draft.montoDestino)
    assertFalse(state.hasUnsavedChanges)
  }

  @Test
  fun archivedAccountsAreNotOfferedForANewTransfer() {
    val state = newState(origenId = VIEJA.id, destinoId = null)

    assertNull(state.draft.origenId)
    assertFalse(VIEJA in state.origenOptions)
    assertFalse(VIEJA in state.destinoOptions)
  }

  @Test
  fun anEditKeepsItsArchivedSideOnThatSideOnly() {
    val stored = transferencia(origen = VIEJA, destino = NEQUI)
    val state = TransferDraftState.forEdit(CUENTAS, Moneda.COP, stored, TODAY)

    assertTrue(VIEJA in state.origenOptions)
    assertFalse(VIEJA in state.destinoOptions)
    assertEquals(stored, state.submitted().toTransferencia(stored.id, stored.createdAt))
  }

  @Test
  fun anOpenedTransferWithoutEditsHasNoUnsavedChanges() {
    val stored = transferencia(origen = DOLARES, destino = NEQUI)
    val state = TransferDraftState.forEdit(CUENTAS, Moneda.COP, stored, TODAY)

    assertFalse(state.hasUnsavedChanges)
    assertTrue(state.withDescripcion("Arriendo").hasUnsavedChanges)
    assertFalse(state.withDescripcion("  ").hasUnsavedChanges)
    assertTrue(state.withMontoDestino("400001").hasUnsavedChanges)
  }

  @Test
  fun theDescriptionIsCappedAndBlankBecomesNull() {
    val state =
        newState(origenId = NEQUI.id, destinoId = EFECTIVO.id)
            .withMontoOrigen("10")
            .withDescripcion("x".repeat(200))

    assertEquals(180, state.draft.descripcion.length)
    assertNull(
        state.withDescripcion("   ").submitted().toTransferencia(ID, CREATED_AT)?.descripcion
    )
  }

  @Test
  fun optionsShowTheCurrencyOnlyForAccountsOutsideThePrimaryOne() {
    val state = newState(origenId = NEQUI.id, destinoId = null)

    assertFalse(state.showsMoneda(NEQUI))
    assertTrue(state.showsMoneda(DOLARES))
  }

  private fun newState(origenId: String?, destinoId: String?) =
      TransferDraftState.forNew(
          CUENTAS,
          Moneda.COP,
          TransferEntry.New(origenId, destinoId, prefill = null),
          TODAY,
      )

  private fun TransferDraftState.submitted() = copy(submitAttempted = true)

  private fun LocalDate.plusDays(days: Int) = LocalDate.fromEpochDays(toEpochDays() + days)
}

private val TODAY = LocalDate(2026, 10, 16)
private val CREATED_AT = Instant.parse("2026-10-16T12:00:00Z")
private const val ID = "transfer-new"
private val DEBT = Money(350_000, Moneda.COP)

private fun cuenta(id: String, tipo: TipoCuenta, moneda: Moneda, archivada: Boolean = false) =
    Cuenta(
        id,
        id.replaceFirstChar(Char::titlecase),
        tipo,
        Money(0, moneda),
        null,
        archivada,
        CREATED_AT,
    )

private val EFECTIVO = cuenta("efectivo", TipoCuenta.EFECTIVO, Moneda.COP)
private val NEQUI = cuenta("nequi", TipoCuenta.BANCO, Moneda.COP)
private val NU = cuenta("nu", TipoCuenta.CREDITO, Moneda.COP)
private val DOLARES = cuenta("dolares", TipoCuenta.BANCO, Moneda.USD)
private val AHORRO_USD = cuenta("ahorro-usd", TipoCuenta.BANCO, Moneda.USD)
private val PESOS_CHILE = cuenta("pesos-chile", TipoCuenta.BANCO, Moneda.CLP)
private val VIEJA = cuenta("vieja", TipoCuenta.BANCO, Moneda.COP, archivada = true)
private val CUENTAS = listOf(EFECTIVO, NEQUI, NU, DOLARES, AHORRO_USD, PESOS_CHILE, VIEJA)

private fun transferencia(origen: Cuenta, destino: Cuenta) =
    Transferencia(
        id = "stored",
        cuentaOrigenId = origen.id,
        cuentaDestinoId = destino.id,
        montoOrigen = Money(10_000, origen.moneda),
        montoDestino =
            if (origen.moneda == destino.moneda) Money(10_000, destino.moneda)
            else Money(400_000, destino.moneda),
        fecha = LocalDate(2026, 10, 1),
        descripcion = null,
        createdAt = Instant.parse("2026-10-01T09:00:00Z"),
    )
