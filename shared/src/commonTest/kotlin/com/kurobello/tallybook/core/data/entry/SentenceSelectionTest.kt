package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.ParsedMovimientoPhrase
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

class SentenceSelectionTest {

  private val matcher = CuentaMatcher()

  private val nequi = cuenta("nequi", "Nequi", TipoCuenta.BANCO, Moneda.COP)
  private val visa = cuenta("visa", "Visa", TipoCuenta.CREDITO, Moneda.COP)
  private val billetera = cuenta("billetera", "Billetera", TipoCuenta.EFECTIVO, Moneda.COP)
  private val efectivoCop = cuenta("ef-cop", "Efectivo", TipoCuenta.EFECTIVO, Moneda.COP)
  private val efectivoUsd = cuenta("ef-usd", "Efectivo", TipoCuenta.EFECTIVO, Moneda.USD)
  private val efectivoPen = cuenta("ef-pen", "Efectivo", TipoCuenta.EFECTIVO, Moneda.PEN)

  private fun cuentaFor(
      text: String,
      cuentas: List<Cuenta>,
      primary: Moneda = Moneda.COP,
  ): SentenceCuenta = resolveSentenceCuenta(matcher.match(text, cuentas), cuentas, primary)

  private fun phrase(medio: MedioPago?, candidates: Set<MedioPago> = emptySet()) =
      ParsedMovimientoPhrase(null, null, null, "", emptySet(), medio, candidates)

  @Test
  fun noAccountNamedResolvesNothing() {
    assertEquals(SentenceCuenta(null, emptyList()), cuentaFor("almuerzo 20 mil", listOf(nequi)))
  }

  @Test
  fun theEarliestAccountWinsAndTheOthersAreOffered() {
    val result = cuentaFor("de visa o nequi", listOf(nequi, visa))

    assertEquals(visa, result.cuenta)
    assertEquals(listOf("nequi"), result.alternativeIds)
  }

  @Test
  fun aNameInSeveralCurrenciesPrefersThePrimaryCurrency() {
    val result = cuentaFor("en efectivo", listOf(efectivoUsd, efectivoCop))

    assertEquals(efectivoCop, result.cuenta)
    assertEquals(listOf("ef-usd"), result.alternativeIds)
  }

  @Test
  fun aNameInSeveralCurrenciesWithoutThePrimaryIsAmbiguous() {
    val result = cuentaFor("en efectivo", listOf(efectivoUsd, efectivoPen, nequi))

    assertNull(result.cuenta)
    assertEquals(listOf("ef-usd", "ef-pen"), result.alternativeIds)
  }

  @Test
  fun withoutAnAccountTheMethodWordsDecide() {
    assertEquals(
        SentenceMedio(MedioPago.CREDITO, emptySet()),
        resolveSentenceMedio(phrase(MedioPago.CREDITO), TipoMovimiento.GASTO, cuentaTipo = null),
    )
    val cards = setOf(MedioPago.DEBITO, MedioPago.CREDITO)
    assertEquals(
        SentenceMedio(null, cards),
        resolveSentenceMedio(phrase(null, cards), TipoMovimiento.GASTO, cuentaTipo = null),
    )
    assertEquals(
        SentenceMedio(null, emptySet()),
        resolveSentenceMedio(phrase(null), TipoMovimiento.GASTO, cuentaTipo = null),
    )
  }

  @Test
  fun anIncomeNeverKeepsDebit() {
    assertEquals(
        SentenceMedio(MedioPago.TRANSFERENCIA, emptySet()),
        resolveSentenceMedio(phrase(MedioPago.DEBITO), TipoMovimiento.INGRESO, cuentaTipo = null),
    )
    assertEquals(
        SentenceMedio(null, setOf(MedioPago.TRANSFERENCIA, MedioPago.CREDITO)),
        resolveSentenceMedio(
            phrase(null, setOf(MedioPago.DEBITO, MedioPago.CREDITO)),
            TipoMovimiento.INGRESO,
            cuentaTipo = null,
        ),
    )
    assertEquals(
        SentenceMedio(MedioPago.TRANSFERENCIA, emptySet()),
        resolveSentenceMedio(
            phrase(null, setOf(MedioPago.DEBITO, MedioPago.TRANSFERENCIA)),
            TipoMovimiento.INGRESO,
            cuentaTipo = null,
        ),
    )
  }

  @Test
  fun anAccountImpliesItsMedio() {
    val cases =
        mapOf(
            TipoCuenta.EFECTIVO to MedioPago.EFECTIVO,
            TipoCuenta.CREDITO to MedioPago.CREDITO,
            TipoCuenta.BANCO to MedioPago.DEBITO,
        )

    cases.forEach { (tipoCuenta, medio) ->
      assertEquals(
          SentenceMedio(medio, emptySet()),
          resolveSentenceMedio(phrase(null), TipoMovimiento.GASTO, tipoCuenta),
          tipoCuenta.name,
      )
    }
    assertEquals(
        SentenceMedio(MedioPago.TRANSFERENCIA, emptySet()),
        resolveSentenceMedio(phrase(null), TipoMovimiento.INGRESO, TipoCuenta.BANCO),
    )
  }

  @Test
  fun aBankAccountWithATransferWordIsATransfer() {
    assertEquals(
        SentenceMedio(MedioPago.TRANSFERENCIA, emptySet()),
        resolveSentenceMedio(
            phrase(MedioPago.TRANSFERENCIA),
            TipoMovimiento.GASTO,
            TipoCuenta.BANCO,
        ),
    )
    assertEquals(
        SentenceMedio(MedioPago.TRANSFERENCIA, emptySet()),
        resolveSentenceMedio(
            phrase(null, setOf(MedioPago.TRANSFERENCIA, MedioPago.EFECTIVO)),
            TipoMovimiento.GASTO,
            TipoCuenta.BANCO,
        ),
    )
  }

  @Test
  fun aContradictingMethodWordIsDroppedAndTheAccountKept() {
    assertEquals(
        SentenceMedio(MedioPago.DEBITO, emptySet()),
        resolveSentenceMedio(phrase(MedioPago.CREDITO), TipoMovimiento.GASTO, TipoCuenta.BANCO),
    )
    assertEquals(
        SentenceMedio(MedioPago.EFECTIVO, emptySet()),
        resolveSentenceMedio(
            phrase(null, setOf(MedioPago.DEBITO, MedioPago.CREDITO)),
            TipoMovimiento.GASTO,
            TipoCuenta.EFECTIVO,
        ),
    )
  }

  @Test
  fun theSentenceFlowReadsTheAccountBeforeTheMedio() {
    val parser = MovimientoPhraseParser()
    val text = "nafta 30 mil con crédito de Billetera"
    val cuentas = listOf(billetera, visa)

    val cuenta = cuentaFor(text, cuentas)
    val parsed =
        parser.parse(
            text,
            cuenta.cuenta?.moneda ?: Moneda.USD,
            AppLocale.ES,
            LocalDate(2026, 10, 15),
        )
    val medio = resolveSentenceMedio(parsed, TipoMovimiento.GASTO, cuenta.cuenta?.tipo)

    assertEquals(billetera, cuenta.cuenta)
    assertEquals(3_000_000, parsed.monto?.minorUnits)
    assertEquals(SentenceMedio(MedioPago.EFECTIVO, emptySet()), medio)
  }
}
