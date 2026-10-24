package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.entry.SentenceCuenta
import com.kurobello.tallybook.core.data.entry.SentenceMedio
import com.kurobello.tallybook.core.data.entry.SentenceSelection
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals

class CuentaSelectionStateTest {

  private val cuentas = listOf(COP_CASH, NEQUI, NU, OLD_BANK, USD_CASH, USD_CARD)
  private val defaults =
      defaultsFor(
          COP_CASH.selection(MedioPago.EFECTIVO),
          MedioPago.EFECTIVO to COP_CASH,
          MedioPago.DEBITO to NEQUI,
          MedioPago.TRANSFERENCIA to NEQUI,
          MedioPago.CREDITO to NU,
      )

  @Test
  fun compatibleCuentasAreActiveAndMatchTheMedioType() {
    assertEquals(listOf(NEQUI), compatibleCuentas(cuentas, MedioPago.DEBITO))
    assertEquals(listOf(NU, USD_CARD), compatibleCuentas(cuentas, MedioPago.CREDITO))
    assertEquals(listOf(USD_CARD), compatibleCuentas(cuentas, MedioPago.CREDITO, Moneda.USD))
  }

  @Test
  fun withoutAMedioEveryActiveCuentaIsCompatible() {
    assertEquals(
        listOf(COP_CASH, NEQUI, NU, USD_CASH, USD_CARD),
        compatibleCuentas(cuentas, medio = null),
    )
  }

  @Test
  fun medioOptionsFollowTheTipo() {
    assertEquals(MedioPago.entries, medioOptions(TipoMovimiento.GASTO))
    assertEquals(
        listOf(MedioPago.EFECTIVO, MedioPago.TRANSFERENCIA, MedioPago.CREDITO),
        medioOptions(TipoMovimiento.INGRESO),
    )
  }

  @Test
  fun preselectLayersDefaultOwnershipOverTheResolvedSelection() {
    assertEquals(
        CuentaSelectionState(MedioPago.DEBITO, NEQUI.id, SelectionOwner.DEFAULT),
        preselect(NEQUI.selection(MedioPago.DEBITO)),
    )
  }

  @Test
  fun theBaseSelectionAdaptsTheDefaultMedioToTheTipo() {
    val bankDefaults = defaultsFor(NEQUI.selection(MedioPago.DEBITO))

    assertEquals(
        CuentaSelectionState(MedioPago.TRANSFERENCIA, NEQUI.id, SelectionOwner.DEFAULT),
        baseSelection(bankDefaults, TipoMovimiento.INGRESO),
    )
    assertEquals(CuentaSelectionState.NONE, baseSelection(null, TipoMovimiento.GASTO))
  }

  @Test
  fun switchingToIngresoTurnsDebitoIntoTransferenciaOnTheSameCuenta() {
    val debit = CuentaSelectionState(MedioPago.DEBITO, NEQUI.id, SelectionOwner.USER)

    assertEquals(
        CuentaSelectionState(MedioPago.TRANSFERENCIA, NEQUI.id, SelectionOwner.USER),
        debit.forTipo(TipoMovimiento.INGRESO),
    )
    assertEquals(
        CuentaSelectionState(MedioPago.TRANSFERENCIA, NEQUI.id, SelectionOwner.USER),
        debit.forTipo(TipoMovimiento.INGRESO).forTipo(TipoMovimiento.GASTO),
    )
  }

  @Test
  fun aMedioKeepsACompatibleCuenta() {
    val debit = CuentaSelectionState(MedioPago.DEBITO, NEQUI.id, SelectionOwner.DEFAULT)

    assertEquals(
        CuentaSelectionState(MedioPago.TRANSFERENCIA, NEQUI.id, SelectionOwner.USER),
        debit.withMedio(MedioPago.TRANSFERENCIA, cuentas, defaults),
    )
  }

  @Test
  fun aMedioIncompatibleWithTheCuentaTakesTheRepositoryDefaultForIt() {
    val cash = baseSelection(defaults, TipoMovimiento.GASTO)

    assertEquals(
        CuentaSelectionState(MedioPago.CREDITO, NU.id, SelectionOwner.USER),
        cash.withMedio(MedioPago.CREDITO, cuentas, defaults),
    )
  }

  @Test
  fun aMedioWithoutACompatibleCuentaLeavesTheCuentaEmpty() {
    val cash = baseSelection(defaults, TipoMovimiento.GASTO)
    val noCards = defaultsFor(COP_CASH.selection(MedioPago.EFECTIVO))

    assertEquals(
        CuentaSelectionState(MedioPago.CREDITO, null, SelectionOwner.USER),
        cash.withMedio(MedioPago.CREDITO, cuentas, noCards),
    )
  }

  @Test
  fun anArchivedCuentaIsNotKeptWhenTheMedioChanges() {
    val archived = CuentaSelectionState(MedioPago.DEBITO, OLD_BANK.id, SelectionOwner.DEFAULT)

    assertEquals(
        CuentaSelectionState(MedioPago.TRANSFERENCIA, NEQUI.id, SelectionOwner.USER),
        archived.withMedio(MedioPago.TRANSFERENCIA, cuentas, defaults),
    )
  }

  @Test
  fun aRestrictedMonedaRejectsADefaultInAnotherCurrency() {
    val usdCash = CuentaSelectionState(MedioPago.EFECTIVO, USD_CASH.id, SelectionOwner.USER)

    assertEquals(
        CuentaSelectionState(MedioPago.CREDITO, null, SelectionOwner.USER),
        usdCash.withMedio(MedioPago.CREDITO, cuentas, defaults, moneda = Moneda.USD),
    )
  }

  @Test
  fun aCuentaKeepsACompatibleMedioOrTakesItsTypeDefault() {
    val transfer = CuentaSelectionState(MedioPago.TRANSFERENCIA, NEQUI.id, SelectionOwner.DEFAULT)
    val cash = baseSelection(defaults, TipoMovimiento.GASTO)

    assertEquals(
        CuentaSelectionState(MedioPago.TRANSFERENCIA, NEQUI.id, SelectionOwner.USER),
        transfer.withCuenta(NEQUI, TipoMovimiento.GASTO),
    )
    assertEquals(
        CuentaSelectionState(MedioPago.CREDITO, NU.id, SelectionOwner.USER),
        cash.withCuenta(NU, TipoMovimiento.GASTO),
    )
    assertEquals(
        CuentaSelectionState(MedioPago.TRANSFERENCIA, NEQUI.id, SelectionOwner.USER),
        cash.withCuenta(NEQUI, TipoMovimiento.INGRESO),
    )
  }

  @Test
  fun aNamedCuentaBecomesSentenceOwnedWithItsMedio() {
    val sentence = sentence(cuenta = NU, medio = MedioPago.CREDITO)

    assertEquals(
        CuentaSelectionState(MedioPago.CREDITO, NU.id, SelectionOwner.SENTENCE),
        baseSelection(defaults, TipoMovimiento.GASTO)
            .withSentence(sentence, cuentas, defaults, TipoMovimiento.GASTO),
    )
  }

  @Test
  fun aSentenceMedioKeepsTheDefaultCuentaWhenItFits() {
    val bankDefaults = defaultsFor(NEQUI.selection(MedioPago.DEBITO), MedioPago.DEBITO to NEQUI)
    val sentence = sentence(medio = MedioPago.TRANSFERENCIA)

    assertEquals(
        CuentaSelectionState(MedioPago.TRANSFERENCIA, NEQUI.id, SelectionOwner.SENTENCE),
        baseSelection(bankDefaults, TipoMovimiento.GASTO)
            .withSentence(sentence, cuentas, bankDefaults, TipoMovimiento.GASTO),
    )
  }

  @Test
  fun aSentenceMedioReplacesAnIncompatibleDefaultCuenta() {
    val sentence = sentence(medio = MedioPago.CREDITO)

    assertEquals(
        CuentaSelectionState(MedioPago.CREDITO, NU.id, SelectionOwner.SENTENCE),
        baseSelection(defaults, TipoMovimiento.GASTO)
            .withSentence(sentence, cuentas, defaults, TipoMovimiento.GASTO),
    )
  }

  @Test
  fun anAmbiguousOrSilentSentenceKeepsTheDefault() {
    val ambiguous =
        SentenceSelection(
            SentenceCuenta(null, emptyList()),
            SentenceMedio(null, setOf(MedioPago.DEBITO, MedioPago.CREDITO)),
        )
    val earlier = CuentaSelectionState(MedioPago.CREDITO, NU.id, SelectionOwner.SENTENCE)

    assertEquals(
        baseSelection(defaults, TipoMovimiento.GASTO),
        earlier.withSentence(ambiguous, cuentas, defaults, TipoMovimiento.GASTO),
    )
  }

  @Test
  fun aUserChoiceOutlivesEverySentence() {
    val user = CuentaSelectionState(MedioPago.DEBITO, NEQUI.id, SelectionOwner.USER)

    assertEquals(
        user,
        user.withSentence(
            sentence(cuenta = NU, medio = MedioPago.CREDITO),
            cuentas,
            defaults,
            TipoMovimiento.GASTO,
        ),
    )
    assertEquals(user, user.withoutSentence(defaults, TipoMovimiento.GASTO))
  }

  @Test
  fun clearingTheSentenceReturnsOnlyItsOwnSelectionToTheDefault() {
    val sentenceOwned = CuentaSelectionState(MedioPago.CREDITO, NU.id, SelectionOwner.SENTENCE)

    assertEquals(
        baseSelection(defaults, TipoMovimiento.GASTO),
        sentenceOwned.withoutSentence(defaults, TipoMovimiento.GASTO),
    )
  }

  private fun sentence(
      cuenta: com.kurobello.tallybook.core.model.Cuenta? = null,
      medio: MedioPago? = null,
  ) = SentenceSelection(SentenceCuenta(cuenta, emptyList()), SentenceMedio(medio, emptySet()))
}
