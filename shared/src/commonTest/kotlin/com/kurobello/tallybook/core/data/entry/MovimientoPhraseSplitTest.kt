package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.PhraseReviewFlag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

class MovimientoPhraseSplitTest {

  /** [parts] pairs each expected part text with the minor units it must parse to on its own. */
  private class SplitCase(val text: String, val parts: List<Pair<String, Long>>, val moneda: Moneda)

  private fun case(text: String, vararg parts: Pair<String, Long>, moneda: Moneda = Moneda.COP) =
      SplitCase(text, parts.toList(), moneda)

  private val parser = MovimientoPhraseParser()

  private val today = LocalDate(2026, 10, 15)

  private fun split(text: String, moneda: Moneda = Moneda.COP): List<String>? =
      parser.split(text, moneda, AppLocale.ES_AR, today)

  private fun assertSplits(case: SplitCase) =
      assertEquals(case.parts.map { it.first }, split(case.text, case.moneda), case.text)

  private val contextCase =
      case(
          "Compré 2 hamburguesas por 20mil y 3 gaseosas fueron 30mil, más lo cargué con la " +
              "tarjeta de crédito",
          "Compré 2 hamburguesas por 20mil" to 2_000_000L,
          "3 gaseosas fueron 30mil" to 3_000_000L,
      )

  private val languageCases =
      listOf(
          case(
              "Gasté 20 mil en cine y 40 mil en comida",
              "Gasté 20 mil en cine" to 2_000_000L,
              "40 mil en comida" to 4_000_000L,
          ),
          case(
              "spent 20 on lunch and 15 on a taxi",
              "spent 20 on lunch" to 2_000L,
              "15 on a taxi" to 1_500L,
              moneda = Moneda.USD,
          ),
          case(
              "gastei 20 no almoço e 15 no uber",
              "gastei 20 no almoço" to 2_000L,
              "15 no uber" to 1_500L,
              moneda = Moneda.BRL,
          ),
      )

  private val cutCases =
      listOf(
          case(
              "20 mil en cine y palomitas y 40 mil en comida",
              "20 mil en cine y palomitas" to 2_000_000L,
              "40 mil en comida" to 4_000_000L,
          ),
          case(
              "cine 20 mil y comida y bebida 40 mil",
              "cine 20 mil" to 2_000_000L,
              "comida y bebida 40 mil" to 4_000_000L,
          ),
          case(
              "20 mil en cine y 40 mil en comida y bebida",
              "20 mil en cine" to 2_000_000L,
              "40 mil en comida y bebida" to 4_000_000L,
          ),
          case(
              "20 mil en cine y 40 mil en comida y bebida y lo pagué con débito",
              "20 mil en cine" to 2_000_000L,
              "40 mil en comida y bebida" to 4_000_000L,
          ),
      )

  // Only stop words or a tipo verb may sit between the cut and the next amount; a medio may not.
  private val fillerCases =
      listOf(
          case(
              "20 mil en cine y palomitas y pagué 40 mil",
              "20 mil en cine y palomitas" to 2_000_000L,
              "pagué 40 mil" to 4_000_000L,
          ),
          case(
              "20 mil en cine y palomitas y con débito 40 mil",
              "20 mil en cine" to 2_000_000L,
              "palomitas y con débito 40 mil" to 4_000_000L,
          ),
          case(
              "20 mil en cine y con débito y 40 mil en comida",
              "20 mil en cine y con débito" to 2_000_000L,
              "40 mil en comida" to 4_000_000L,
          ),
      )

  private val connectorRunCases =
      listOf(
          case(
              "Gasté 20 mil en cine y también 40 mil en comida",
              "Gasté 20 mil en cine" to 2_000_000L,
              "40 mil en comida" to 4_000_000L,
          ),
          case(
              "spent 20 on lunch and also 15 on a taxi",
              "spent 20 on lunch" to 2_000L,
              "15 on a taxi" to 1_500L,
              moneda = Moneda.USD,
          ),
          case(
              "cine 20 mil, y comida 40 mil",
              "cine 20 mil" to 2_000_000L,
              "comida 40 mil" to 4_000_000L,
          ),
          case(
              "netflix 20mil y spotify 30mil, y también lo pagué con débito",
              "netflix 20mil" to 2_000_000L,
              "spotify 30mil" to 3_000_000L,
          ),
      )

  private val quantityEdgeCase =
      case(
          "taxi 15, propina 20 y almuerzo 18",
          "taxi 15" to 1_500L,
          "propina 20" to 2_000L,
          "almuerzo 18" to 1_800L,
          moneda = Moneda.USD,
      )

  private val decimalCommaCase =
      case("pagué 1.500,50 y 2.000", "pagué 1.500,50" to 150_050L, "2.000" to 200_000L)

  private val cadenceCase =
      case(
          "netflix 20 mil y spotify 10 mil cada mes",
          "netflix 20 mil" to 2_000_000L,
          "spotify 10 mil cada mes" to 1_000_000L,
      )

  private val positiveCases =
      listOf(contextCase, quantityEdgeCase, decimalCommaCase, cadenceCase) +
          languageCases +
          cutCases +
          fillerCases +
          connectorRunCases

  @Test fun theContextSentenceSplitsIntoTwoPartsAndASharedTail() = assertSplits(contextCase)

  @Test
  fun sentencesThatAreOneMovimientoTodayDoNotSplit() =
      listOf(
              "cine y comida 60 mil",
              "pan y leche 5 mil",
              "treinta y cinco mil en cine",
              "2 hamburguesas y 3 gaseosas por 50 mil",
              "20 mil por 2 entradas de 10 mil",
              "2 hamburguesas y 3 gaseosas",
          )
          .forEach { assertNull(split(it), it) }

  @Test fun splitsInEveryLanguage() = languageCases.forEach(::assertSplits)

  @Test fun cutsAtTheConnectorThatIntroducesTheNextAmount() = cutCases.forEach(::assertSplits)

  @Test fun onlyStopWordsOrTipoVerbsMayFollowTheCut() = fillerCases.forEach(::assertSplits)

  @Test fun adjacentConnectorsCutAsOne() = connectorRunCases.forEach(::assertSplits)

  @Test fun aNumberBeforeAConnectorIsAnAmountNotAQuantity() = assertSplits(quantityEdgeCase)

  @Test fun decimalCommaIsNotAConnector() = assertSplits(decimalCommaCase)

  @Test fun aCadenceInsideThePartStaysInIt() = assertSplits(cadenceCase)

  @Test
  fun moreThanTenPartsDoNotSplit() =
      assertNull(split((1..11).joinToString(" y ") { "$it mil en algo" }))

  // Guards the contract the ViewModel relies on: parse(part).monto is the part's amount.
  @Test
  fun everyPartParsesToItsOwnAmount() = positiveCases.forEach { case ->
    case.parts.forEach { (part, minorUnits) ->
      val parsed = parser.parse(part, case.moneda, AppLocale.ES_AR, today)
      assertEquals(minorUnits, parsed.monto?.minorUnits, part)
      assertFalse(PhraseReviewFlag.AMBIGUOUS_AMOUNT in parsed.reviewFlags, part)
    }
  }
}
