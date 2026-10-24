package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.ParsedMovimientoPhrase
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

class MovimientoPhraseMedioTest {

  private val parser = MovimientoPhraseParser()

  // A Thursday.
  private val today = LocalDate(2026, 10, 15)

  private fun parse(
      text: String,
      moneda: Moneda = Moneda.COP,
      reservedSpans: List<IntRange> = emptyList(),
  ): ParsedMovimientoPhrase = parser.parse(text, moneda, AppLocale.ES_AR, today, reservedSpans)

  private fun assertDecided(cases: Map<String, MedioPago>) {
    cases.forEach { (text, medio) ->
      val result = parse(text)
      assertEquals(medio, result.medio, text)
      assertEquals(emptySet(), result.medioCandidates, text)
    }
  }

  private fun assertAmbiguous(cases: Map<String, Set<MedioPago>>) {
    cases.forEach { (text, candidates) ->
      val result = parse(text)
      assertNull(result.medio, text)
      assertEquals(candidates, result.medioCandidates, text)
    }
  }

  private fun assertNoMedio(texts: List<String>) {
    texts.forEach { text ->
      val result = parse(text)
      assertNull(result.medio, text)
      assertEquals(emptySet(), result.medioCandidates, text)
    }
  }

  @Test
  fun detectsSpanishMethodWords() {
    assertDecided(
        mapOf(
            "Almuerzo 20 mil en efectivo" to MedioPago.EFECTIVO,
            "efectivo 5000 taxi" to MedioPago.EFECTIVO,
            "Pagué con débito 30 mil" to MedioPago.DEBITO,
            "mercado con tarjeta de débito" to MedioPago.DEBITO,
            "mercado tarjeta débito" to MedioPago.DEBITO,
            "cena con crédito" to MedioPago.CREDITO,
            "zapatos tarjeta de crédito 200 mil" to MedioPago.CREDITO,
            "tenis con tarjeta crédito" to MedioPago.CREDITO,
            "arriendo por transferencia" to MedioPago.TRANSFERENCIA,
            "Transferencia arriendo 900 mil" to MedioPago.TRANSFERENCIA,
        )
    )
  }

  @Test
  fun detectsRioplatenseMethodWords() {
    assertDecided(
        mapOf(
            "Gasté 5 lucas en cash" to MedioPago.EFECTIVO,
            "nafta con la de débito" to MedioPago.DEBITO,
            "ropa con la de crédito" to MedioPago.CREDITO,
            "expensas con transferencia" to MedioPago.TRANSFERENCIA,
        )
    )
  }

  @Test
  fun detectsEnglishMethodWords() {
    assertDecided(
        mapOf(
            "coffee 5 in cash" to MedioPago.EFECTIVO,
            "I paid cash for lunch" to MedioPago.EFECTIVO,
            "groceries with debit" to MedioPago.DEBITO,
            "shoes with my debit card" to MedioPago.DEBITO,
            "dinner with credit" to MedioPago.CREDITO,
            "hotel by credit card" to MedioPago.CREDITO,
            "rent by transfer" to MedioPago.TRANSFERENCIA,
            "rent bank transfer" to MedioPago.TRANSFERENCIA,
        )
    )
  }

  @Test
  fun detectsBrazilianPortugueseMethodWords() {
    assertDecided(
        mapOf(
            "almoço em dinheiro" to MedioPago.EFECTIVO,
            "padaria dinheiro 20" to MedioPago.EFECTIVO,
            "paguei no dinheiro" to MedioPago.EFECTIVO,
            "feira em espécie" to MedioPago.EFECTIVO,
            "mercado cartão de débito" to MedioPago.DEBITO,
            "roupa no cartão de crédito" to MedioPago.CREDITO,
            "aluguel por pix" to MedioPago.TRANSFERENCIA,
            "pix padaria" to MedioPago.TRANSFERENCIA,
            "paguei no pix" to MedioPago.TRANSFERENCIA,
            "pelo pix 30 reais" to MedioPago.TRANSFERENCIA,
            "via pix" to MedioPago.TRANSFERENCIA,
            "por transferência" to MedioPago.TRANSFERENCIA,
        )
    )
  }

  @Test
  fun aGenericCardOffersDebitAndCredit() {
    val cards = setOf(MedioPago.DEBITO, MedioPago.CREDITO)
    assertAmbiguous(
        mapOf(
            "mercado con tarjeta" to cards,
            "Pagué con la tarjeta 40 mil" to cards,
            "nafta con mi tarjeta" to cards,
            "groceries with card" to cards,
            "paid by card" to cards,
            "with my card" to cards,
            "paguei no cartão" to cards,
            "com cartão 30 reais" to cards,
        )
    )
  }

  @Test
  fun aCardWordNamingItsTypeIsNotAmbiguous() {
    assertDecided(
        mapOf(
            "con tarjeta de crédito" to MedioPago.CREDITO,
            "con la tarjeta de débito" to MedioPago.DEBITO,
            "com cartão de débito" to MedioPago.DEBITO,
            "with a credit card" to MedioPago.CREDITO,
        )
    )
  }

  @Test
  fun twoDifferentMethodWordsDecideNothing() {
    assertAmbiguous(
        mapOf(
            "mitad en efectivo y mitad con débito" to setOf(MedioPago.EFECTIVO, MedioPago.DEBITO),
            "pix e dinheiro" to setOf(MedioPago.TRANSFERENCIA, MedioPago.EFECTIVO),
            "con tarjeta y en efectivo" to
                setOf(MedioPago.EFECTIVO, MedioPago.DEBITO, MedioPago.CREDITO),
        )
    )
    assertDecided(mapOf("efectivo, todo en efectivo" to MedioPago.EFECTIVO))
  }

  @Test
  fun negatedMentionsAreNeverMethodWords() {
    assertNoMedio(
        listOf(
            "no con débito",
            "almuerzo sin tarjeta",
            "not with credit",
            "sem cartão",
            "sin efectivo",
            "not in cash",
            "sem pix",
            "sem cartão de crédito",
            "no con tarjeta de crédito",
        )
    )
    assertEquals("almuerzo", parse("almuerzo sin tarjeta 20 mil").subject)
  }

  @Test
  fun portugueseNoBeforeDebitOrCreditIsNotAMethodWord() {
    assertNoMedio(listOf("Paguei 50 no débito", "Paguei 50 no crédito"))
    assertEquals("no débito", parse("Paguei 50 no débito").subject)
  }

  @Test
  fun aCardThatIsWhatGetsPaidIsNotTheMethod() {
    assertNoMedio(
        listOf(
            "Pagué la tarjeta de crédito",
            "pago tarjeta de crédito 500 mil",
            "pago de la tarjeta de crédito",
            "paid the credit card",
            "credit card bill",
            "fatura do cartão de crédito",
        )
    )
    assertEquals("la tarjeta de crédito", parse("Pagué la tarjeta de crédito 500 mil").subject)
  }

  @Test
  fun categoryPhrasesEmbeddingAMethodWordStayInTheSubject() {
    assertNoMedio(listOf("cash back 20", "petty cash", "transferencia recibida 50 mil"))
    assertEquals("transferencia recibida", parse("transferencia recibida 50 mil").subject)
  }

  @Test
  fun transferVerbsAreNotMethodWords() {
    assertNoMedio(listOf("le transferí 50k a Juan", "I transferred 20 to Ana", "transferi 30"))
  }

  @Test
  fun sentencesWithoutMethodWordsHaveNoMedio() {
    assertNoMedio(listOf("Uber 12k hoy", "Gasté 20.000 en cine ayer", "", "tarjeta de regalo"))
  }

  @Test
  fun methodWordsLeaveTheSubjectNextToAmountAndDate() {
    val result = parse("50 mil en efectivo ayer")

    assertEquals(MedioPago.EFECTIVO, result.medio)
    assertEquals(5_000_000, result.monto?.minorUnits)
    assertEquals(LocalDate(2026, 10, 14), result.fecha)
    assertEquals("", result.subject)
    val subjects =
        mapOf(
            "Almuerzo 20 mil en efectivo ayer" to "Almuerzo",
            "Mercado con tarjeta de crédito 80k" to "Mercado",
            "Gasté 30 mil con la tarjeta en farmacia" to "en farmacia",
            "almoço no pix 30 reais" to "almoço",
            "I spent 12.50 at McDonald's by card" to "I at McDonald's",
        )
    subjects.forEach { (text, subject) -> assertEquals(subject, parse(text).subject, text) }
  }

  @Test
  fun theTipoVerbStaysIndependentOfTheMedio() {
    val result = parse("Recibí 50 mil por transferencia")

    assertEquals(TipoMovimiento.INGRESO, result.tipo)
    assertEquals(MedioPago.TRANSFERENCIA, result.medio)
  }

  @Test
  fun reservedSpansLeaveTheSubjectAndAreNeverAmounts() {
    val text = "Gasolina Nu 360 50k"
    val span = text.indexOf("Nu 360").let { it until it + "Nu 360".length }

    val result = parse(text, reservedSpans = listOf(span))

    assertEquals(5_000_000, result.monto?.minorUnits)
    assertEquals(emptySet(), result.reviewFlags)
    assertEquals("Gasolina", result.subject)
  }
}
