package com.kurobello.tallybook.feature.accounts.viewmodel

import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaDraft
import com.kurobello.tallybook.core.model.CuentaNombreError
import com.kurobello.tallybook.core.model.CuentaRules
import com.kurobello.tallybook.core.model.DiaCorteError
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class AccountDraftStateTest {

  @Test
  fun aNewDraftTrimsItsNameAndReadsABlankOpeningBalanceAsZero() {
    val draft = AccountDraftState.forNew("  Nequi ", TipoCuenta.BANCO, Moneda.COP)

    assertEquals("Nequi", draft.nombre)
    assertNull(draft.visibleNombreError)
    assertEquals(
        CuentaDraft("Nequi", TipoCuenta.BANCO, Money(0, Moneda.COP), diaCorte = null),
        draft.toDraft(),
    )
  }

  @Test
  fun theNameIsCappedAtTheRuleLimitWithoutSplittingASurrogatePair() {
    val long = "a".repeat(CuentaRules.NOMBRE_MAX - 1) + "💳"
    val draft = AccountDraftState.forNew("", TipoCuenta.BANCO, Moneda.COP).onNombre(long)

    assertEquals("a".repeat(CuentaRules.NOMBRE_MAX - 1), draft.nombre)
    assertTrue(draft.nombreEdited)
  }

  @Test
  fun aBlankNameIsOnlyShownOnceEditedAndBlocksTheDraft() {
    val pristine = AccountDraftState.forNew("", TipoCuenta.BANCO, Moneda.COP)
    assertNull(pristine.visibleNombreError)
    assertEquals(CuentaNombreError.BLANK, pristine.nombreError)
    assertNull(pristine.toDraft())

    val edited = pristine.onNombre("   ")
    assertEquals(CuentaNombreError.BLANK, edited.visibleNombreError)
    assertFalse(edited.isValid)
  }

  @Test
  fun theOpeningBalanceUsesTheCurrencySeparatorsAndRejectsSignsAndExtraDecimals() {
    val cop = AccountDraftState.forNew("Nequi", TipoCuenta.BANCO, Moneda.COP)

    assertEquals(Money(150_050, Moneda.COP), cop.onSaldo("1500,50").toDraft()?.saldoInicial)
    assertEquals(AccountSaldoError.MALFORMED, cop.onSaldo("1.500").saldoError)
    assertEquals(AccountSaldoError.SIGNED, cop.onSaldo("-20").saldoError)
    assertEquals(AccountSaldoError.TOO_MANY_DECIMALS, cop.onSaldo("1,505").saldoError)
    assertEquals(AccountSaldoError.OVERFLOW, cop.onSaldo("9".repeat(25)).saldoError)
    assertEquals(Money(0, Moneda.COP), cop.onSaldo("0").toDraft()?.saldoInicial)

    val usd = cop.onMoneda(Moneda.USD).onSaldo("12.5")
    assertEquals(Money(1_250, Moneda.USD), usd.toDraft()?.saldoInicial)
  }

  @Test
  fun changingTheCurrencyKeepsTheTypedAmountInTheNewSeparators() {
    val cop = AccountDraftState.forNew("Nequi", TipoCuenta.BANCO, Moneda.COP).onSaldo("1500,50")

    assertEquals("1500.50", cop.onMoneda(Moneda.USD).saldo)
    assertEquals("1500,50", cop.onMoneda(Moneda.USD).onMoneda(Moneda.BRL).saldo)
    assertEquals(AccountSaldoError.TOO_MANY_DECIMALS, cop.onMoneda(Moneda.CLP).saldoError)
  }

  @Test
  fun aCreditCardsPositiveOpeningDebtBecomesANegativeSaldoInicialAndBack() {
    val draft =
        AccountDraftState.forNew("NU", TipoCuenta.CREDITO, Moneda.COP)
            .onSaldo("300000")
            .onDiaCorte("15")
    val written = draft.toDraft()

    assertEquals(Money(-30_000_000, Moneda.COP), written?.saldoInicial)
    assertEquals(15, written?.diaCorte)

    val stored =
        Cuenta(
            id = "nu",
            nombre = "NU",
            tipo = TipoCuenta.CREDITO,
            saldoInicial = Money(-30_000_000, Moneda.COP),
            diaCorte = 15,
            archivada = false,
            createdAt = CREATED_AT,
        )
    val reopened = AccountDraftState.forEdit(stored)
    assertEquals("300000,00", reopened.saldo)
    assertEquals("15", reopened.diaCorte)
    assertEquals(written, reopened.toDraft())
  }

  @Test
  fun aBankBalanceIsStoredAsTypedWithoutNegation() {
    val written = AccountDraftState.forNew("Nequi", TipoCuenta.BANCO, Moneda.COP).onSaldo("80")

    assertEquals(Money(8_000, Moneda.COP), written.toDraft()?.saldoInicial)
  }

  @Test
  fun theStatementDayIsOnlyKeptForCreditCardsAndMustBeADayOfTheMonth() {
    val credit = AccountDraftState.forNew("NU", TipoCuenta.CREDITO, Moneda.COP)

    assertEquals("31", credit.onDiaCorte("3x1").diaCorte)
    assertEquals("12", credit.onDiaCorte("123").diaCorte)
    assertEquals(DiaCorteError.OUT_OF_RANGE, credit.onDiaCorte("0").diaCorteError)
    assertEquals(DiaCorteError.OUT_OF_RANGE, credit.onDiaCorte("32").diaCorteError)
    assertNull(credit.onDiaCorte("").toDraft()?.diaCorte)

    val bank = credit.onDiaCorte("15").onTipo(TipoCuenta.BANCO)
    assertNull(bank.diaCorteError)
    assertNull(bank.toDraft()?.diaCorte)
    assertEquals(15, bank.onTipo(TipoCuenta.CREDITO).toDraft()?.diaCorte)
  }

  @Test
  fun aDraftIsUnchangedWhileItsNormalizedValuesMatchTheBaseline() {
    val baseline =
        AccountDraftState.forEdit(
            Cuenta(
                id = "nequi",
                nombre = "Nequi",
                tipo = TipoCuenta.BANCO,
                saldoInicial = Money(150_000, Moneda.COP),
                diaCorte = null,
                archivada = false,
                createdAt = CREATED_AT,
            )
        )

    assertTrue(baseline.onNombre("Nequi  ").sameContentAs(baseline))
    assertTrue(baseline.onSaldo("1500").sameContentAs(baseline))
    assertTrue(baseline.onDiaCorte("9").sameContentAs(baseline))
    assertFalse(baseline.onNombre("Daviplata").sameContentAs(baseline))
    assertFalse(baseline.onSaldo("1501").sameContentAs(baseline))
    assertFalse(baseline.onSaldo("15x").sameContentAs(baseline))
    assertFalse(baseline.onTipo(TipoCuenta.EFECTIVO).sameContentAs(baseline))
    assertFalse(baseline.onMoneda(Moneda.USD).sameContentAs(baseline))
  }

  @Test
  fun aZeroOpeningBalanceReopensEmpty() {
    val stored =
        Cuenta(
            id = Cuenta.defaultCashId(Moneda.COP),
            nombre = "Efectivo",
            tipo = TipoCuenta.EFECTIVO,
            saldoInicial = Money(0, Moneda.COP),
            diaCorte = null,
            archivada = false,
            createdAt = CREATED_AT,
        )

    assertEquals("", AccountDraftState.forEdit(stored).saldo)
  }
}

private val CREATED_AT = Instant.parse("2026-01-01T00:00:00Z")
