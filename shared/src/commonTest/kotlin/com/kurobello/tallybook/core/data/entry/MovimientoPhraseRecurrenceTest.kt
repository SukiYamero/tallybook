package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.ParsedMovimientoPhrase
import com.kurobello.tallybook.core.model.PhraseReviewFlag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

class MovimientoPhraseRecurrenceTest {

  private val parser = MovimientoPhraseParser()

  // A Thursday.
  private val today = LocalDate(2026, 10, 15)

  private fun parse(text: String, locale: AppLocale = AppLocale.ES_AR): ParsedMovimientoPhrase =
      parser.parse(text, Moneda.COP, locale, today)

  @Test
  fun readsTheDictatedRentWithIntervalAmountAndMedio() {
    val result = parse("arriendo 500000 cada 30 días con efectivo")

    assertEquals(Frecuencia.MENSUAL, result.frecuencia)
    assertEquals(50_000_000, result.monto?.minorUnits)
    assertEquals(MedioPago.EFECTIVO, result.medio)
    assertEquals("arriendo", result.subject)
    assertEquals(emptySet(), result.reviewFlags)
    assertNull(result.fecha)
  }

  @Test
  fun leavesSentencesWithoutRecurrenceUnchanged() {
    val result = parse("gasté 15000 en cine con mis amigos")

    assertNull(result.frecuencia)
    assertEquals(1_500_000, result.monto?.minorUnits)
    assertEquals("en cine con mis amigos", result.subject)
    assertEquals(emptySet(), result.reviewFlags)
  }

  @Test
  fun mapsAdjectivesInEveryLanguageAndKeepsThemInTheSubject() {
    val cases =
        listOf(
            RecurrenceCase("netflix 20k mensual", Frecuencia.MENSUAL, "netflix mensual", 2_000_000),
            RecurrenceCase("compra semanal 30000", Frecuencia.SEMANAL, "compra semanal", 3_000_000),
            RecurrenceCase("seguro anual 900000", Frecuencia.ANUAL, "seguro anual", 90_000_000),
            RecurrenceCase(
                "spotify monthly 15000",
                Frecuencia.MENSUAL,
                "spotify monthly",
                1_500_000,
            ),
            RecurrenceCase(
                "academia mensalmente 100",
                Frecuencia.MENSUAL,
                "academia mensalmente",
                10_000,
            ),
            RecurrenceCase("domain yearly 50", Frecuencia.ANUAL, "domain yearly", 5_000),
            RecurrenceCase("laundry weekly 40", Frecuencia.SEMANAL, "laundry weekly", 4_000),
        )

    cases.forEach { it.assertParsed() }
  }

  @Test
  fun mapsIntervalsByTheirDuration() {
    val cases =
        listOf(
            RecurrenceCase("gym every month 50 dollars", Frecuencia.MENSUAL, "gym", 5_000),
            RecurrenceCase("gym cada 7 días 5000", Frecuencia.SEMANAL, "gym", 500_000),
            RecurrenceCase("gym cada semana 5000", Frecuencia.SEMANAL, "gym", 500_000),
            RecurrenceCase("luz cada treinta días 5000", Frecuencia.MENSUAL, "luz", 500_000),
            RecurrenceCase("luz cada 31 días 5000", Frecuencia.MENSUAL, "luz", 500_000),
            RecurrenceCase("luz cada 4 semanas 5000", Frecuencia.MENSUAL, "luz", 500_000),
            RecurrenceCase("luz todos los meses 5000", Frecuencia.MENSUAL, "luz", 500_000),
            RecurrenceCase("seguro cada año 5000", Frecuencia.ANUAL, "seguro", 500_000),
            RecurrenceCase("seguro cada 12 meses 5000", Frecuencia.ANUAL, "seguro", 500_000),
            RecurrenceCase("seguro cada 365 días 5000", Frecuencia.ANUAL, "seguro", 500_000),
            RecurrenceCase("rent each month 900", Frecuencia.MENSUAL, "rent", 90_000),
            RecurrenceCase("gym every seven days 50", Frecuencia.SEMANAL, "gym", 5_000),
            RecurrenceCase("gym a cada sete dias 50", Frecuencia.SEMANAL, "gym", 5_000),
            RecurrenceCase("aluguel todo mês 2000", Frecuencia.MENSUAL, "aluguel", 200_000),
            RecurrenceCase("feira toda semana 80", Frecuencia.SEMANAL, "feira", 8_000),
            RecurrenceCase("aluguel todos os meses 2000", Frecuencia.MENSUAL, "aluguel", 200_000),
        )

    cases.forEach { it.assertParsed() }
  }

  @Test
  fun mapsPerLeadsWithoutCountToTheirUnit() {
    val cases =
        listOf(
            RecurrenceCase("internet 80000 por mes", Frecuencia.MENSUAL, "internet", 8_000_000),
            RecurrenceCase("internet 80000 al mes", Frecuencia.MENSUAL, "internet", 8_000_000),
            RecurrenceCase("clases 50000 a la semana", Frecuencia.SEMANAL, "clases", 5_000_000),
            RecurrenceCase("seguro 900000 al año", Frecuencia.ANUAL, "seguro", 90_000_000),
            RecurrenceCase("gym 50 dollars a month", Frecuencia.MENSUAL, "gym", 5_000),
            RecurrenceCase("gym 50 per week", Frecuencia.SEMANAL, "gym", 5_000),
            RecurrenceCase("internet 100 ao mês", Frecuencia.MENSUAL, "internet", 10_000),
            RecurrenceCase("seguro 100 por ano", Frecuencia.ANUAL, "seguro", 10_000),
        )

    cases.forEach { it.assertParsed() }
  }

  @Test
  fun weekdaysRecurWeeklyAnchoredOnTheirMostRecentOccurrence() {
    val monday = LocalDate(2026, 10, 12)
    val cases =
        mapOf(
            "clases todos los lunes 20000" to monday,
            "clases cada lunes 20000" to monday,
            "clases los lunes 20000" to monday,
            "piano every monday 20" to monday,
            "piano mondays 20" to monday,
            "piano on mondays 20" to monday,
            "aula toda segunda 20" to monday,
            "aula toda segunda-feira 20" to monday,
            "aula todas as segundas-feiras 20" to monday,
            "feria todos los sábados 20000" to LocalDate(2026, 10, 10),
            "feria cada jueves 20000" to today,
        )

    cases.forEach { (text, anchor) ->
      val result = parse(text)
      assertEquals(Frecuencia.SEMANAL, result.frecuencia, text)
      assertEquals(anchor, result.fecha, text)
      assertEquals(1, result.subject.split(' ').size, text)
      assertEquals(emptySet(), result.reviewFlags, text)
    }
  }

  @Test
  fun daysOfEveryMonthRecurMonthlyAnchoredOnTheirMostRecentOccurrence() {
    val cases =
        mapOf(
            "arriendo el 5 de cada mes 500000" to LocalDate(2026, 10, 5),
            "arriendo el día 20 de cada mes 500000" to LocalDate(2026, 9, 20),
            "rent on the 1st of every month 500" to LocalDate(2026, 10, 1),
            "arriendo todos los 15 500000" to today,
            "aluguel todo dia 31 2000" to LocalDate(2026, 8, 31),
        )

    cases.forEach { (text, anchor) ->
      val result = parse(text)
      assertEquals(Frecuencia.MENSUAL, result.frecuencia, text)
      assertEquals(anchor, result.fecha, text)
      assertEquals(1, result.subject.split(' ').size, text)
    }
  }

  @Test
  fun anExplicitDateWinsOverTheRecurrenceAnchor() {
    val result = parse("clases todos los lunes 20000 ayer")

    assertEquals(Frecuencia.SEMANAL, result.frecuencia)
    assertEquals(LocalDate(2026, 10, 14), result.fecha)
  }

  @Test
  fun unsupportedCadencesAreConsumedAndFlagged() {
    val cases =
        mapOf(
            "gym cada 15 días 30000" to "gym",
            "gym cada dos semanas 30000" to "gym",
            "gym every 2 weeks 30000" to "gym",
            "seguro cada 3 meses 30000" to "seguro",
            "almuerzo todos los días 30000" to "almuerzo",
            "almuerzo cada día 30000" to "almuerzo",
            "almuerzo todo dia 30000" to "almuerzo",
            "almuerzo todos os dias 30000" to "almuerzo",
            "lunch every day 30000" to "lunch",
            "nómina quincenal 30000" to "nómina quincenal",
            "salário quinzenal 30000" to "salário quinzenal",
            "agua bimestral 30000" to "agua bimestral",
            "predial trimestral 30000" to "predial trimestral",
            "tax quarterly 30000" to "tax quarterly",
            "seguro semestral 30000" to "seguro semestral",
            "rent biweekly 30000" to "rent biweekly",
            "café diariamente 30000" to "café diariamente",
            "coffee daily 30000" to "coffee daily",
        )

    cases.forEach { (text, subject) ->
      val result = parse(text)
      assertNull(result.frecuencia, text)
      assertEquals(setOf(PhraseReviewFlag.UNSUPPORTED_RECURRENCE), result.reviewFlags, text)
      assertEquals(3_000_000, result.monto?.minorUnits, text)
      assertEquals(subject, result.subject, text)
    }
  }

  @Test
  fun theFirstStatedIntervalWinsAndEveryIntervalIsConsumed() {
    val supportedFirst = parse("netflix cada mes cada 15 días 20000")
    assertEquals(Frecuencia.MENSUAL, supportedFirst.frecuencia)
    assertEquals(emptySet(), supportedFirst.reviewFlags)
    assertEquals("netflix", supportedFirst.subject)

    val unsupportedFirst = parse("netflix cada 15 días cada mes 20000")
    assertNull(unsupportedFirst.frecuencia)
    assertEquals(setOf(PhraseReviewFlag.UNSUPPORTED_RECURRENCE), unsupportedFirst.reviewFlags)
    assertEquals("netflix", unsupportedFirst.subject)

    val statedOverAdjective = parse("netflix quincenal cada mes 20000")
    assertEquals(Frecuencia.MENSUAL, statedOverAdjective.frecuencia)
    assertEquals(emptySet(), statedOverAdjective.reviewFlags)
    assertEquals("netflix quincenal", statedOverAdjective.subject)
  }

  @Test
  fun leavesDatePhrasesDurationsAndLookalikesAlone() {
    val cases =
        mapOf(
            "gasté 5000 este mes" to "este mes",
            "gasté 5000 el mes pasado" to "el mes pasado",
            "gasté 5000 el próximo mes" to "el próximo mes",
            "spent 5000 last month" to "last month",
            "spent 5000 next week" to "next week",
            "gasté 5000 esta semana" to "esta semana",
            "gasté 5000 el año pasado" to "el año pasado",
            "gastei 5000 mês que vem" to "mês que vem",
            "gastei 5000 a semana passada" to "a semana passada",
            "rented a car for a month 5000" to "rented a car for a month",
            "rented it a year ago 5000" to "rented it a year ago",
            "curso de 12 meses 5000" to "curso de 12 meses",
            "cada uno 5000" to "cada uno",
            "diario 5000" to "diario",
            "viaje por semana santa 5000" to "viaje por semana santa",
            "pago al mes siguiente 5000" to "pago al mes siguiente",
            "cena por año nuevo 5000" to "cena por año nuevo",
            "gym 2 veces por semana 5000" to "gym 2 veces por semana",
            "gym 3 times a week 5000" to "gym 3 times a week",
            "delivery in a month 5000" to "delivery in a month",
            "delivered within a week 5000" to "delivered within a week",
        )

    cases.forEach { (text, subject) ->
      val result = parse(text)
      assertNull(result.frecuencia, text)
      assertEquals(subject, result.subject, text)
      assertEquals(500_000, result.monto?.minorUnits, text)
    }
  }

  @Test
  fun aBareNumberBeforeEveryMonthIsTheAmountNotTheDay() {
    val cases = mapOf("netflix 15 every month" to 1_500L, "uber 12 each month" to 1_200L)

    cases.forEach { (text, minorUnits) ->
      val result = parse(text)
      assertEquals(Frecuencia.MENSUAL, result.frecuencia, text)
      assertEquals(minorUnits, result.monto?.minorUnits, text)
      assertNull(result.fecha, text)
    }
  }

  @Test
  fun aStatedCadenceWinsOverAnEarlierAdjective() {
    val result = parse("compra semanal cada mes 30000")

    assertEquals(Frecuencia.MENSUAL, result.frecuencia)
    assertEquals("compra semanal", result.subject)
  }

  @Test
  fun aDailyPriceIsNotARecurrence() {
    val result = parse("hotel 200000 por día")

    assertNull(result.frecuencia)
    assertEquals(emptySet(), result.reviewFlags)
    assertEquals(20_000_000, result.monto?.minorUnits)
    assertEquals("hotel por día", result.subject)
  }

  @Test
  fun anOrdinalSegundaBeforeAUnitIsNotMonday() {
    val result = parse("ropa de segunda mano cada semana 5000")

    assertEquals(Frecuencia.SEMANAL, result.frecuencia)
    assertNull(result.fecha)
    assertEquals("ropa de segunda mano", result.subject)
    assertNull(parse("ahorro cada segunda semana 5000").frecuencia)
  }

  @Test
  fun weekdaysWithoutARecurringLeadStayDates() {
    val result = parse("gasté 5000 el lunes")

    assertNull(result.frecuencia)
    assertEquals(LocalDate(2026, 10, 12), result.fecha)
    assertNull(parse("paid 5000 on monday", AppLocale.EN).frecuencia)
  }

  private inner class RecurrenceCase(
      val text: String,
      val frecuencia: Frecuencia,
      val subject: String,
      val minorUnits: Long,
  ) {
    fun assertParsed() {
      val result = parse(text)
      assertEquals(frecuencia, result.frecuencia, text)
      assertEquals(subject, result.subject, text)
      assertEquals(minorUnits, result.monto?.minorUnits, text)
      assertEquals(emptySet(), result.reviewFlags, text)
    }
  }
}
