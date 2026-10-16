package com.kurobello.tallybook.core.data.entry

import kotlin.test.Test
import kotlin.test.assertEquals

class SuggestionTextTest {

  @Test
  fun stopWordsOfEverySupportedLanguageAreDropped() {
    val expected =
        mapOf(
            "hotel y reservaciones" to listOf("hotel", "reservacion"),
            "Cambio de aceite" to listOf("cambio", "aceite"),
            "café con leche" to listOf("cafe", "leche"),
            "pão de açúcar" to listOf("pao", "acucar"),
            "the movie at night" to listOf("movie", "night"),
            "almoço com a família" to listOf("almoco", "familia"),
            "de la en un" to emptyList(),
        )

    expected.forEach { (text, tokens) -> assertEquals(tokens, suggestionTokens(text), text) }
  }

  @Test
  fun punctuationAndDigitsSeparateWords() {
    assertEquals(listOf("disney"), suggestionTokens("Disney+"))
    assertEquals(listOf("mercado", "libre"), suggestionTokens("mercado-libre #123"))
    assertEquals(listOf("pañal"), suggestionTokens("PAÑALES"))
    assertEquals(emptyList(), suggestionTokens("   "))
  }

  @Test
  fun regularPluralsReduceToTheirSingular() {
    val expected =
        mapOf(
            "hoteles" to "hotel",
            "reservaciones" to "reservacion",
            "cafés" to "cafe",
            "tickets" to "ticket",
            "restaurantes" to "restaurante",
            "cines" to "cine",
            "muebles" to "mueble",
            "doctores" to "doctor",
            "ciudades" to "ciudad",
            "gastos" to "gasto",
            "bus" to "bus",
            "gas" to "gas",
            "boss" to "boss",
        )

    expected.forEach { (plural, singular) ->
      assertEquals(listOf(singular), suggestionTokens(plural), plural)
    }
  }

  @Test
  fun subjectKeyIgnoresWordOrder() {
    assertEquals(
        subjectKey(suggestionTokens("reservaciones hotel")),
        subjectKey(suggestionTokens("Hotel y reservaciones")),
    )
  }
}
