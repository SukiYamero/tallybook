package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.ParsedMovimientoPhrase
import com.kurobello.tallybook.core.model.PhraseReviewFlag
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

class MovimientoPhraseParserTest {

  private val parser = MovimientoPhraseParser()

  // A Thursday.
  private val today = LocalDate(2026, 10, 15)

  private fun parse(
      text: String,
      moneda: Moneda = Moneda.COP,
      locale: AppLocale = AppLocale.ES_AR,
      on: LocalDate = today,
  ): ParsedMovimientoPhrase = parser.parse(text, moneda, locale, on)

  private fun minor(text: String, moneda: Moneda = Moneda.COP): Long? =
      parse(text, moneda).monto?.minorUnits

  @Test
  fun detectsExpenseAndIncomeVerbsInEveryLanguage() {
    val cases =
        mapOf(
            "Gasté 5 en cine" to TipoMovimiento.GASTO,
            "gaste 5 en cine" to TipoMovimiento.GASTO,
            "Pagué 5 de luz" to TipoMovimiento.GASTO,
            "Compré 5 de pan" to TipoMovimiento.GASTO,
            "Me cobraron 5 de comisión" to TipoMovimiento.GASTO,
            "I spent 5 on coffee" to TipoMovimiento.GASTO,
            "I paid 5 for parking" to TipoMovimiento.GASTO,
            "I bought 5 stamps" to TipoMovimiento.GASTO,
            "Gastei 5 no cinema" to TipoMovimiento.GASTO,
            "Paguei 5 de luz" to TipoMovimiento.GASTO,
            "Comprei 5 de pão" to TipoMovimiento.GASTO,
            "Recibí 5 de sueldo" to TipoMovimiento.INGRESO,
            "Me pagaron 5 de sueldo" to TipoMovimiento.INGRESO,
            "Cobré 5 de alquiler" to TipoMovimiento.INGRESO,
            "Vendí 5 de ropa" to TipoMovimiento.INGRESO,
            "Me depositaron 5 de sueldo" to TipoMovimiento.INGRESO,
            "Gané 5 en la rifa" to TipoMovimiento.INGRESO,
            "I received 5 from Ana" to TipoMovimiento.INGRESO,
            "I got paid 5 today" to TipoMovimiento.INGRESO,
            "I earned 5 tutoring" to TipoMovimiento.INGRESO,
            "I sold 5 books" to TipoMovimiento.INGRESO,
            "Recebi 5 de salário" to TipoMovimiento.INGRESO,
            "Ganhei 5 na rifa" to TipoMovimiento.INGRESO,
            "Me pagaram 5 de salário" to TipoMovimiento.INGRESO,
            "Vendi 5 de roupa" to TipoMovimiento.INGRESO,
        )

    cases.forEach { (text, expected) -> assertEquals(expected, parse(text).tipo, text) }
  }

  @Test
  fun meCobraronIsAnExpenseWhileCobreIsIncome() {
    assertEquals(TipoMovimiento.GASTO, parse("Me cobraron 5000 de envío").tipo)
    assertEquals("de envío", parse("Me cobraron 5000 de envío").subject)
    assertEquals(TipoMovimiento.INGRESO, parse("Cobré 5000 de envío").tipo)
  }

  @Test
  fun leavesTipoNullWithoutVerbAndKeepsTheFirstVerbOtherwise() {
    assertNull(parse("Cine 20000").tipo)
    assertEquals(TipoMovimiento.GASTO, parse("Compré con lo que me pagaron 20000").tipo)
  }

  @Test
  fun readsDigitAmountForms() {
    val cases =
        listOf(
            AmountCase("20000", Moneda.COP, 2_000_000),
            AmountCase("20.000", Moneda.COP, 2_000_000),
            AmountCase("20,000", Moneda.COP, 2_000_000),
            AmountCase("1.234.567", Moneda.COP, 123_456_700),
            AmountCase("12.50", Moneda.COP, 1_250),
            AmountCase("12,50", Moneda.COP, 1_250),
            AmountCase("1.234,56", Moneda.COP, 123_456),
            AmountCase("\$20.000", Moneda.COP, 2_000_000),
            AmountCase("\$ 20.000", Moneda.COP, 2_000_000),
            AmountCase("20000 pesos", Moneda.COP, 2_000_000),
            AmountCase("20,000", Moneda.USD, 2_000_000),
            AmountCase("12.50", Moneda.USD, 1_250),
            AmountCase("12.5", Moneda.USD, 1_250),
            AmountCase("20 dollars", Moneda.USD, 2_000),
            AmountCase("R$ 30", Moneda.BRL, 3_000),
            AmountCase("R\$30,5", Moneda.BRL, 3_050),
            AmountCase("S/ 25", Moneda.PEN, 2_500),
            AmountCase("20.000", Moneda.CLP, 20_000),
            AmountCase("20000", Moneda.CLP, 20_000),
        )

    cases.forEach { case ->
      assertEquals(case.minorUnits, minor(case.text, case.moneda), "${case.text} ${case.moneda}")
    }
  }

  @Test
  fun readsMultipliersAndSlang() {
    val cases =
        listOf(
            AmountCase("20k", Moneda.COP, 2_000_000),
            AmountCase("20K", Moneda.COP, 2_000_000),
            AmountCase("20 mil", Moneda.COP, 2_000_000),
            AmountCase("1,5 millones", Moneda.COP, 150_000_000),
            AmountCase("2 millones", Moneda.COP, 200_000_000),
            AmountCase("1.5M", Moneda.USD, 150_000_000),
            AmountCase("1 million", Moneda.USD, 100_000_000),
            AmountCase("1,5 milhões", Moneda.BRL, 150_000_000),
            AmountCase("20 lucas", Moneda.COP, 2_000_000),
            AmountCase("2 palos", Moneda.COP, 200_000_000),
            AmountCase("5 mangos", Moneda.COP, 500),
            AmountCase("1,5 millones", Moneda.CLP, 1_500_000),
            AmountCase("20 lucas", Moneda.CLP, 20_000),
            AmountCase("2.5k", Moneda.CLP, 2_500),
        )

    cases.forEach { case ->
      assertEquals(case.minorUnits, minor(case.text, case.moneda), "${case.text} ${case.moneda}")
    }
  }

  @Test
  fun readsNumberWords() {
    val cases =
        listOf(
            AmountCase("veinte mil", Moneda.COP, 2_000_000),
            AmountCase("cien", Moneda.COP, 10_000),
            AmountCase("quinientos", Moneda.COP, 50_000),
            AmountCase("treinta y cinco", Moneda.COP, 3_500),
            AmountCase("cien mil", Moneda.COP, 10_000_000),
            AmountCase("dos millones quinientos mil", Moneda.COP, 250_000_000),
            AmountCase("mil", Moneda.COP, 100_000),
            AmountCase("un millón", Moneda.COP, 100_000_000),
            AmountCase("twenty", Moneda.USD, 2_000),
            AmountCase("two hundred and five", Moneda.USD, 20_500),
            AmountCase("one thousand five hundred", Moneda.USD, 150_000),
            AmountCase("vinte reais", Moneda.BRL, 2_000),
            AmountCase("vinte e cinco", Moneda.BRL, 2_500),
            AmountCase("quinhentos", Moneda.BRL, 50_000),
            AmountCase("veinte lucas", Moneda.CLP, 20_000),
            AmountCase("veinte mil", Moneda.CLP, 20_000),
        )

    cases.forEach { case ->
      assertEquals(case.minorUnits, minor(case.text, case.moneda), "${case.text} ${case.moneda}")
    }
  }

  @Test
  fun aConnectorJoinsAValueToTheLargerOneBeforeIt() {
    val cases =
        listOf(
            AmountCase("mil e quinhentos", Moneda.BRL, 150_000),
            AmountCase("dois mil e quinhentos", Moneda.BRL, 250_000),
            AmountCase("cento e vinte", Moneda.BRL, 12_000),
            AmountCase("cento e vinte e cinco", Moneda.BRL, 12_500),
            AmountCase("mil y quinientos", Moneda.COP, 150_000),
            AmountCase("dos mil y quinientos", Moneda.COP, 250_000),
            AmountCase("ciento veinte", Moneda.COP, 12_000),
            AmountCase("ciento y veinte", Moneda.COP, 12_000),
            AmountCase("dos mil veinte y cinco", Moneda.COP, 202_500),
            AmountCase("one hundred and twenty", Moneda.USD, 12_000),
            AmountCase("one thousand and five", Moneda.USD, 100_500),
            AmountCase("dos millones y quinientos mil", Moneda.COP, 250_000_000),
        )

    cases.forEach { case ->
      assertEquals(case.minorUnits, minor(case.text, case.moneda), "${case.text} ${case.moneda}")
    }
    val sentence = parse("Gastei mil e quinhentos", moneda = Moneda.BRL, locale = AppLocale.PT_BR)
    assertEquals(150_000, sentence.monto?.minorUnits)
    assertEquals(emptySet(), sentence.reviewFlags)
    assertEquals("", sentence.subject)
  }

  @Test
  fun aConnectorNeverJoinsTwoIndependentNumbers() {
    listOf("veinte y veinte", "veinte y diez", "cien y cien", "mil e mil").forEach { text ->
      val result = parse("Gasté $text")
      assertEquals(setOf(PhraseReviewFlag.AMBIGUOUS_AMOUNT), result.reviewFlags, text)
    }
    assertNull(minor("cinco y cinco"))
  }

  @Test
  fun aLoneSpelledUnitBelowTenIsNotAnAmountWithoutAMarker() {
    assertNull(parse("Compré dos pasajes").monto)
    assertEquals("dos pasajes", parse("Compré dos pasajes").subject)
    assertNull(parse("I bought two tickets").monto)
    assertNull(parse("Compré un café").monto)
    assertNull(parse("I bought one coffee").monto)
    assertEquals("un café", parse("Compré un café").subject)
  }

  @Test
  fun aSpelledUnitStaysAnAmountWithAScaleOrMarker() {
    val cases =
        listOf(
            AmountCase("dos mil", Moneda.COP, 200_000),
            AmountCase("cinco dólares", Moneda.USD, 500),
            AmountCase("5 dólares", Moneda.USD, 500),
            AmountCase("one dollar", Moneda.USD, 100),
            AmountCase("\$ cinco", Moneda.USD, 500),
            AmountCase("cinco lucas", Moneda.COP, 500_000),
            AmountCase("veinte", Moneda.COP, 2_000),
            AmountCase("diez", Moneda.COP, 1_000),
        )

    cases.forEach { case ->
      assertEquals(case.minorUnits, minor(case.text, case.moneda), "${case.text} ${case.moneda}")
    }
  }

  @Test
  fun spelledNumbersThatOverflowAreNotAmounts() {
    listOf(
            "dos millones millones millones millones",
            "veinte mil millones millones millones",
            "mil millones millones millones millones",
        )
        .forEach { text ->
          assertNull(parse("Gasté $text", moneda = Moneda.CLP).monto, text)
          assertNull(minor(text, Moneda.CLP), text)
        }
  }

  @Test
  fun aSingleSeparatorGroupsOnlyAfterAValidLeadingGroup() {
    val cases =
        listOf(
            AmountCase("100.000", Moneda.COP, 10_000_000),
            AmountCase("999.999", Moneda.COP, 99_999_900),
            AmountCase("1.000", Moneda.COP, 100_000),
            AmountCase("1234,5", Moneda.COP, 123_450),
            AmountCase("0,50", Moneda.COP, 50),
            AmountCase("0.5", Moneda.USD, 50),
            AmountCase("0,5", Moneda.COP, 50),
        )

    cases.forEach { case ->
      assertEquals(case.minorUnits, minor(case.text, case.moneda), "${case.text} ${case.moneda}")
    }
  }

  @Test
  fun zeroLeadingThreeDigitsAreDecimalAndExceedEveryMonedaScale() {
    val cases =
        listOf(
            AmountCase("0,500", Moneda.COP, null),
            AmountCase("0.500", Moneda.USD, null),
            AmountCase("0.500", Moneda.CLP, null),
            AmountCase("0.500.000", Moneda.COP, null),
        )

    cases.forEach { case ->
      assertNull(minor(case.text, case.moneda), "${case.text} ${case.moneda}")
    }
  }

  @Test
  fun aLeadingGroupLongerThanThreeDigitsMakesTheSeparatorDecimal() {
    val cases =
        listOf(
            AmountCase("1234.567", Moneda.COP, null),
            AmountCase("1234.567", Moneda.USD, null),
            AmountCase("1234,567", Moneda.COP, null),
            AmountCase("1234.567.890", Moneda.COP, null),
            AmountCase("1234.5,6", Moneda.COP, null),
        )

    cases.forEach { case ->
      assertNull(minor(case.text, case.moneda), "${case.text} ${case.moneda}")
    }
  }

  @Test
  fun rejectsInvalidAmounts() {
    val cases =
        listOf(
            AmountCase("12.5051", Moneda.COP, null),
            AmountCase("12,50", Moneda.CLP, null),
            AmountCase("12,00", Moneda.CLP, null),
            AmountCase("1.2.3", Moneda.USD, null),
            AmountCase("0", Moneda.USD, null),
            AmountCase("0,00", Moneda.USD, null),
            AmountCase("99999999999999999999", Moneda.COP, null),
            AmountCase("99999999999999999", Moneda.COP, null),
            AmountCase("9999999999999999 millones", Moneda.CLP, null),
            AmountCase("999999999999999 palos", Moneda.CLP, null),
        )

    cases.forEach { case ->
      assertNull(minor(case.text, case.moneda), "${case.text} ${case.moneda}")
    }
  }

  @Test
  fun acceptsTheLargestAmountThatFitsALong() {
    assertEquals(Long.MAX_VALUE, minor("9223372036854775807", Moneda.CLP))
    assertNull(minor("9223372036854775808", Moneda.CLP))
  }

  @Test
  fun anInvalidCandidateStaysInTheSubject() {
    val result = parse("Cena 12,5051 total 30")

    assertEquals(3_000, result.monto?.minorUnits)
    assertEquals("Cena 12,5051 total", result.subject)
    assertEquals(emptySet(), result.reviewFlags)
  }

  @Test
  fun picksTheLargestCandidateAndFlagsTheAmount() {
    val result = parse("Cena con 2 amigos 50000")

    assertEquals(5_000_000, result.monto?.minorUnits)
    assertEquals(setOf(PhraseReviewFlag.AMBIGUOUS_AMOUNT), result.reviewFlags)
    assertEquals("Cena con 2 amigos", result.subject)
    assertEquals(
        setOf(PhraseReviewFlag.AMBIGUOUS_AMOUNT),
        parse("Compré iPhone 15 por 3.000.000").reviewFlags,
    )
    assertEquals(emptySet(), parse("Cena 50000").reviewFlags)
  }

  @Test
  fun numbersConsumedByADateAreNotAmountCandidates() {
    val result = parse("Gasté 20.000 el 12 de octubre")

    assertEquals(2_000_000, result.monto?.minorUnits)
    assertEquals(LocalDate(2026, 10, 12), result.fecha)
    assertEquals(emptySet(), result.reviewFlags)
  }

  @Test
  fun anExplicitDateWinsOverAWeekdayBesideIt() {
    val cases =
        listOf(
            DateCase("el lunes 12 de octubre", AppLocale.ES, LocalDate(2026, 10, 12)),
            DateCase("el martes 12 de octubre", AppLocale.ES, LocalDate(2026, 10, 12)),
            DateCase("lunes, 12 de octubre", AppLocale.ES, LocalDate(2026, 10, 12)),
            DateCase("lunes el 12 de octubre", AppLocale.ES, LocalDate(2026, 10, 12)),
            DateCase("lunes 12/10", AppLocale.ES, LocalDate(2026, 10, 12)),
            DateCase("na segunda-feira 12 de outubro", AppLocale.PT_BR, LocalDate(2026, 10, 12)),
            DateCase("on monday october 12", AppLocale.EN, LocalDate(2026, 10, 12)),
            DateCase("on monday, october 12th", AppLocale.EN, LocalDate(2026, 10, 12)),
        )

    cases.forEach { case ->
      val result = parse("Gasté 20000 ${case.expression}", locale = case.locale)
      assertEquals(case.expected, result.fecha, case.expression)
      assertEquals(2_000_000, result.monto?.minorUnits, case.expression)
      assertEquals(emptySet(), result.reviewFlags, case.expression)
      assertEquals("", result.subject, case.expression)
    }
    assertNull(parse("Gasté el lunes 12 de octubre").monto)
  }

  @Test
  fun aWeekdayStaysAloneWhenNoExplicitDateFollows() {
    val result = parse("Pagué el lunes 12 pesos")

    assertEquals(LocalDate(2026, 10, 12), result.fecha)
    assertEquals(1_200, result.monto?.minorUnits)
  }

  @Test
  fun resolvesRelativeDaysAndWeekdays() {
    val cases =
        mapOf(
            "hoy" to LocalDate(2026, 10, 15),
            "today" to LocalDate(2026, 10, 15),
            "hoje" to LocalDate(2026, 10, 15),
            "ayer" to LocalDate(2026, 10, 14),
            "yesterday" to LocalDate(2026, 10, 14),
            "ontem" to LocalDate(2026, 10, 14),
            "anteayer" to LocalDate(2026, 10, 13),
            "antier" to LocalDate(2026, 10, 13),
            "anteontem" to LocalDate(2026, 10, 13),
            "day before yesterday" to LocalDate(2026, 10, 13),
            "el lunes" to LocalDate(2026, 10, 12),
            "lunes" to LocalDate(2026, 10, 12),
            "on monday" to LocalDate(2026, 10, 12),
            "na segunda-feira" to LocalDate(2026, 10, 12),
            "na segunda" to LocalDate(2026, 10, 12),
            "el jueves" to LocalDate(2026, 10, 15),
            "thursday" to LocalDate(2026, 10, 15),
            "el viernes" to LocalDate(2026, 10, 9),
            "sábado" to LocalDate(2026, 10, 10),
            "domingo" to LocalDate(2026, 10, 11),
            "quarta" to LocalDate(2026, 10, 14),
        )

    cases.forEach { (expression, expected) ->
      val result = parse("Compré pan 5000 $expression")
      assertEquals(expected, result.fecha, expression)
      assertEquals("pan", result.subject, expression)
    }
  }

  @Test
  fun resolvesExplicitDatesToTheirMostRecentPastOccurrence() {
    val cases =
        listOf(
            DateCase("el 12", AppLocale.ES, LocalDate(2026, 10, 12)),
            DateCase("el 15", AppLocale.ES, LocalDate(2026, 10, 15)),
            DateCase("el 20", AppLocale.ES, LocalDate(2026, 9, 20)),
            DateCase("el día 20", AppLocale.ES, LocalDate(2026, 9, 20)),
            DateCase("el 31", AppLocale.ES, LocalDate(2026, 8, 31)),
            DateCase("on the 12th", AppLocale.EN, LocalDate(2026, 10, 12)),
            DateCase("12 de octubre", AppLocale.ES, LocalDate(2026, 10, 12)),
            DateCase("el 12 de octubre", AppLocale.ES, LocalDate(2026, 10, 12)),
            DateCase("20 de octubre", AppLocale.ES, LocalDate(2025, 10, 20)),
            DateCase("12 de outubro", AppLocale.PT_BR, LocalDate(2026, 10, 12)),
            DateCase("12th of october", AppLocale.EN, LocalDate(2026, 10, 12)),
            DateCase("october 12", AppLocale.EN, LocalDate(2026, 10, 12)),
            DateCase("1 de enero", AppLocale.ES, LocalDate(2026, 1, 1)),
            DateCase("29 de febrero", AppLocale.ES, LocalDate(2024, 2, 29)),
            DateCase("12/10", AppLocale.ES, LocalDate(2026, 10, 12)),
            DateCase("12/10", AppLocale.ES_AR, LocalDate(2026, 10, 12)),
            DateCase("12/10", AppLocale.PT_BR, LocalDate(2026, 10, 12)),
            DateCase("10/12", AppLocale.EN, LocalDate(2026, 10, 12)),
            DateCase("10/12", AppLocale.ES, LocalDate(2025, 12, 10)),
            DateCase("el 25/12/2025", AppLocale.ES, LocalDate(2025, 12, 25)),
            DateCase("25/12/25", AppLocale.ES, LocalDate(2025, 12, 25)),
            DateCase("12/25/2025", AppLocale.EN, LocalDate(2025, 12, 25)),
        )

    cases.forEach { case ->
      val result = parse("Gasté 20000 ${case.expression}", locale = case.locale)
      assertEquals(case.expected, result.fecha, "${case.expression} ${case.locale}")
      assertEquals(2_000_000, result.monto?.minorUnits, case.expression)
      assertEquals("", result.subject, case.expression)
    }
  }

  @Test
  fun resolvesDatesAcrossMonthAndYearBoundaries() {
    val firstOfMarch = LocalDate(2026, 3, 1)
    val secondOfJanuary = LocalDate(2026, 1, 2)
    val cases =
        listOf(
            DatedCase("ayer", firstOfMarch, LocalDate(2026, 2, 28)),
            DatedCase("el 31", firstOfMarch, LocalDate(2026, 1, 31)),
            DatedCase("el 28", firstOfMarch, LocalDate(2026, 2, 28)),
            DatedCase("anteayer", secondOfJanuary, LocalDate(2025, 12, 31)),
            DatedCase("el 31", secondOfJanuary, LocalDate(2025, 12, 31)),
            DatedCase("12 de diciembre", secondOfJanuary, LocalDate(2025, 12, 12)),
            DatedCase("lunes", secondOfJanuary, LocalDate(2025, 12, 29)),
            DatedCase("30/12", secondOfJanuary, LocalDate(2025, 12, 30)),
        )

    cases.forEach { case ->
      assertEquals(
          case.expected,
          parse("Gasté 5 ${case.expression}", on = case.today).fecha,
          case.expression,
      )
    }
  }

  @Test
  fun dropsFutureDatesAndFlagsThem() {
    val expressions =
        listOf(
            "mañana",
            "manana",
            "tomorrow",
            "amanhã",
            "pasado mañana",
            "12/10/2027",
            "25/12/2026",
        )

    expressions.forEach { expression ->
      val result = parse("Gasté 5000 $expression")
      assertNull(result.fecha, expression)
      assertEquals(setOf(PhraseReviewFlag.FUTURE_DATE_IGNORED), result.reviewFlags, expression)
      assertEquals(500_000, result.monto?.minorUnits, expression)
      assertEquals("", result.subject, expression)
    }
  }

  @Test
  fun leavesTextThatOnlyLooksLikeADateAlone() {
    val cases =
        mapOf(
            "Compré pan por la mañana 5000" to "pan por la mañana",
            "Ropa de segunda mano 5000" to "Ropa de segunda mano",
            "Pagué 5000 el 31/04" to "el 31/04",
            "Pagué 5000 el 5 pesos" to "el 5 pesos",
        )

    cases.forEach { (text, subject) ->
      val result = parse(text)
      assertNull(result.fecha, text)
      assertEquals(subject, result.subject, text)
    }
  }

  @Test
  fun englishReadsNumericDatesMonthFirstAndOthersDayFirst() {
    assertEquals(LocalDate(2026, 3, 4), parse("paid 5 3/4", locale = AppLocale.EN).fecha)
    assertEquals(LocalDate(2026, 4, 3), parse("paid 5 3/4", locale = AppLocale.ES).fecha)
    assertNull(parse("paid 5 25/10", locale = AppLocale.EN).fecha)
  }

  @Test
  fun extractsTheSubjectInTheUsersWordsAndOrder() {
    val cases =
        mapOf(
            "Gasté 20.000 en cine ayer" to "en cine",
            "Cambio de aceite 80 mil el lunes" to "Cambio de aceite",
            "Pagué \$50.000 de la luz, el 12 de octubre" to "de la luz",
            "  Almuerzo   con   Ana  \$ 35.000  " to "Almuerzo con Ana",
            "Recebi vinte reais do João ontem" to "do João",
            "I spent 12.50 at McDonald's yesterday!" to "I at McDonald's",
            "Uber 12k hoy" to "Uber",
            "Gasté 20 lucas en Rappi-Turbo" to "en Rappi-Turbo",
            "Netflix" to "Netflix",
        )

    cases.forEach { (text, subject) -> assertEquals(subject, parse(text).subject, text) }
  }

  @Test
  fun understandsMixedLanguagesAndDictationOutput() {
    val result = parse("I got paid veinte mil on friday", moneda = Moneda.COP)

    assertEquals(TipoMovimiento.INGRESO, result.tipo)
    assertEquals(2_000_000, result.monto?.minorUnits)
    assertEquals(LocalDate(2026, 10, 9), result.fecha)
    assertEquals("I", result.subject)
    assertEquals("en Café", parse("Gasté cinco mil en Café ayer").subject)
  }

  @Test
  fun returnsEmptyResultForBlankOrUnrecognizableInput() {
    val empty = ParsedMovimientoPhrase(null, null, null, "", emptySet())

    assertEquals(empty, parse(""))
    assertEquals(empty, parse("   \n\t "))
    assertEquals(empty, parse("?! ..."))
    assertEquals("Hola mundo", parse("Hola mundo").subject)
  }

  private data class AmountCase(val text: String, val moneda: Moneda, val minorUnits: Long?)

  private data class DateCase(
      val expression: String,
      val locale: AppLocale,
      val expected: LocalDate,
  )

  private data class DatedCase(
      val expression: String,
      val today: LocalDate,
      val expected: LocalDate,
  )
}
