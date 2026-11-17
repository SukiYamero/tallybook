package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.Moneda
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

class MovimientoPhraseSplitTest {

  private val parser = MovimientoPhraseParser()

  private val today = LocalDate(2026, 10, 15)

  private fun split(text: String, moneda: Moneda = Moneda.COP): List<String>? =
      parser.split(text, moneda, AppLocale.ES_AR, today)

  @Test
  fun theContextSentenceSplitsIntoTwoPartsAndASharedTail() =
      assertEquals(
          listOf("Compré 2 hamburguesas por 20mil", "3 gaseosas fueron 30mil"),
          split(
              "Compré 2 hamburguesas por 20mil y 3 gaseosas fueron 30mil, más lo cargué con la " +
                  "tarjeta de crédito"
          ),
      )

  @Test
  fun sentencesThatAreOneMovimientoTodayDoNotSplit() =
      listOf(
              "cine y comida 60 mil",
              "pan y leche 5 mil",
              "treinta y cinco mil en cine",
              "2 hamburguesas y 3 gaseosas por 50 mil",
              "20 mil por 2 entradas de 10 mil",
          )
          .forEach { assertNull(split(it), it) }

  @Test
  fun splitsInEveryLanguage() {
    assertEquals(
        listOf("Gasté 20 mil en cine", "40 mil en comida"),
        split("Gasté 20 mil en cine y 40 mil en comida"),
    )
    assertEquals(
        listOf("spent 20 on lunch", "15 on a taxi"),
        split("spent 20 on lunch and 15 on a taxi", Moneda.USD),
    )
    assertEquals(
        listOf("gastei 20 no almoço", "15 no uber"),
        split("gastei 20 no almoço e 15 no uber", Moneda.BRL),
    )
  }

  @Test
  fun cutsAtTheConnectorThatIntroducesTheNextAmount() {
    assertEquals(
        listOf("20 mil en cine y palomitas", "40 mil en comida"),
        split("20 mil en cine y palomitas y 40 mil en comida"),
    )
    assertEquals(
        listOf("cine 20 mil", "comida y bebida 40 mil"),
        split("cine 20 mil y comida y bebida 40 mil"),
    )
    assertEquals(
        listOf("20 mil en cine", "40 mil en comida y bebida"),
        split("20 mil en cine y 40 mil en comida y bebida"),
    )
  }

  @Test
  fun decimalCommaIsNotAConnector() =
      assertEquals(listOf("pagué 1.500,50", "2.000"), split("pagué 1.500,50 y 2.000"))

  @Test
  fun aCadenceInsideThePartStaysInIt() =
      assertEquals(
          listOf("netflix 20 mil", "spotify 10 mil cada mes"),
          split("netflix 20 mil y spotify 10 mil cada mes"),
      )

  @Test
  fun moreThanTenPartsDoNotSplit() =
      assertNull(split((1..11).joinToString(" y ") { "$it mil en algo" }))

  @Test
  fun everyPartParsesToItsOwnAmount() {
    // Guards the contract the ViewModel relies on: parse(part).monto is the part's amount.
    val parts = split("Gasté 20 mil en cine y 40 mil en comida")!!
    assertEquals(
        listOf(2_000_000L, 4_000_000L),
        parts.map { parser.parse(it, Moneda.COP, AppLocale.ES_AR, today).monto?.minorUnits },
    )
  }
}
