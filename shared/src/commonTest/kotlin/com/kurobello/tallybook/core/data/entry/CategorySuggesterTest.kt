package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.data.CategorySeedCatalog
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class CategorySuggesterTest {

  private val now = Instant.parse("2026-10-15T12:00:00Z")
  private val suggester = CategorySuggester(CategoryKeywordDictionary.builtIn)
  private val allIds = CategorySeedCatalog.categories.map { it.id }.toSet() + "cat_otros"

  private fun suggest(
      subject: String,
      history: List<CategoryHistoryEntry> = emptyList(),
      tipo: TipoMovimiento? = null,
      available: Set<String> = allIds,
      excluded: Set<String> = emptySet(),
      limit: Int = 3,
  ) = suggester.index(history, now).suggest(subject, tipo, available, excluded, limit)

  private fun entry(subject: String, categoriaId: String, daysAgo: Int = 1) =
      CategoryHistoryEntry(subject, categoriaId, now - daysAgo.days)

  private fun ids(suggestions: List<CategorySuggestion>) = suggestions.map { it.categoriaId }

  @Test
  fun hotelAndReservationsSuggestLodgingWithOnlyTheFirstHigh() {
    val result = suggest("hotel y reservaciones")

    assertEquals("cat_hospedaje", result.first().categoriaId)
    assertEquals(SuggestionConfidence.HIGH, result.first().confidence)
    assertTrue(result.drop(1).all { it.confidence == SuggestionConfidence.MEDIUM })
  }

  @Test
  fun singleBrandAndWordsResolveToTheirCategory() {
    assertEquals("cat_taxi", suggest("uber").first().categoriaId)
    assertEquals("cat_juegos", suggest("videojuegos").first().categoriaId)
    assertEquals("cat_hospedaje", suggest("hoteles").first().categoriaId)
  }

  @Test
  fun anAmbiguousWordAloneNeverProducesAHighResult() {
    val result = suggest("aceite")

    assertEquals(setOf("cat_supermercado", "cat_transporte"), ids(result).toSet())
    assertTrue(result.all { it.confidence == SuggestionConfidence.MEDIUM })
  }

  @Test
  fun aPhraseOutweighsItsAmbiguousWord() {
    val result = suggest("cambio de aceite")

    assertEquals(
        listOf(CategorySuggestion("cat_transporte", SuggestionConfidence.HIGH)),
        result,
    )
  }

  @Test
  fun learnedHistoryBeatsTheDictionary() {
    val history = List(3) { entry("aceite", "cat_supermercado", daysAgo = it + 1) }

    val result = suggest("aceite", history)

    assertEquals(
        CategorySuggestion("cat_supermercado", SuggestionConfidence.HIGH),
        result.first(),
    )
    assertTrue("cat_transporte" in ids(result))
  }

  @Test
  fun anExactHistoryMatchIgnoresWordOrderAndPlurals() {
    val history = listOf(entry("reservaciones hotel", "cat_vuelos"))

    val result = suggest("Hotel y reservaciones", history)

    assertEquals(CategorySuggestion("cat_vuelos", SuggestionConfidence.HIGH), result.first())
  }

  @Test
  fun aRecentCorrectionBeatsAnOlderMajorityAfterDecay() {
    val history =
        List(4) { entry("zorro", "cat_comida", daysAgo = 300 + it) } +
            entry("zorro", "cat_ocio", daysAgo = 1)

    val result = suggest("zorro", history)

    assertEquals(CategorySuggestion("cat_ocio", SuggestionConfidence.HIGH), result.first())
  }

  @Test
  fun withoutACorrectionTheMajorityWinsWithoutBeingHighWhenSplit() {
    val history =
        List(3) { entry("zorro", "cat_comida", daysAgo = it + 1) } +
            List(2) { entry("zorro", "cat_ocio", daysAgo = it + 1) }

    val result = suggest("zorro", history)

    assertEquals(listOf("cat_comida", "cat_ocio"), ids(result))
    assertTrue(result.all { it.confidence == SuggestionConfidence.MEDIUM })
  }

  @Test
  fun aSingleVeryOldEntryDoesNotDominate() {
    assertEquals(emptyList(), suggest("zorro", listOf(entry("zorro", "cat_comida", 1_500))))
  }

  @Test
  fun learnedWordsGeneralizeToNewSubjectsAtMediumConfidence() {
    val history = List(2) { entry("zorro azul", "cat_mascotas", daysAgo = it + 1) }

    val result = suggest("zorro grande", history)

    assertEquals(listOf(CategorySuggestion("cat_mascotas", SuggestionConfidence.MEDIUM)), result)
  }

  @Test
  fun incomeCategoriesAreBoostedForIngresoAndPenalizedForGasto() {
    assertEquals("cat_regalo", suggest("regalo", tipo = TipoMovimiento.INGRESO).first().categoriaId)

    val expense = ids(suggest("regalo", tipo = TipoMovimiento.GASTO))
    assertEquals(listOf("cat_varios"), expense)

    assertEquals(
        setOf("cat_regalo", "cat_varios"),
        ids(suggest("regalo", tipo = null)).toSet(),
    )
  }

  @Test
  fun excludedAndUnavailableIdsAreNeverReturned() {
    assertFalse("cat_taxi" in ids(suggest("uber", excluded = setOf("cat_taxi"))))
    assertEquals(emptyList(), suggest("uber", available = setOf("cat_comida")))
    assertEquals(
        listOf("cat_transporte"),
        ids(suggest("cambio de aceite", available = setOf("cat_transporte", "cat_comida"))),
    )
  }

  @Test
  fun otrosIsNeverSuggestedNorLearned() {
    val history = List(5) { entry("zorro", "cat_otros", daysAgo = it + 1) }

    assertEquals(emptyList(), suggest("zorro", history))
    assertFalse("cat_otros" in ids(suggest("otros varios", history)))
  }

  @Test
  fun competingStrongMatchesStayMedium() {
    val result = suggest("cine y pizza")

    assertEquals(setOf("cat_cine", "cat_restaurante"), ids(result).toSet())
    assertTrue(result.all { it.confidence == SuggestionConfidence.MEDIUM })
  }

  @Test
  fun limitBoundsTheResultAndNonPositiveLimitReturnsNothing() {
    assertEquals(1, suggest("hotel y reservaciones", limit = 1).size)
    assertEquals(emptyList(), suggest("hotel", limit = 0))
  }

  @Test
  fun subjectsWithoutContentWordsReturnNothing() {
    assertEquals(emptyList(), suggest("de la en"))
    assertEquals(emptyList(), suggest("   "))
  }

  @Test
  fun supportedLanguagesAndRegionalVariantsResolve() {
    val expected =
        mapOf(
            "estacionamiento" to "cat_parqueadero",
            "parqueadero" to "cat_parqueadero",
            "estacionamento" to "cat_parqueadero",
            "parking" to "cat_parqueadero",
            "nafta" to "cat_gasolina",
            "bencina" to "cat_gasolina",
            "gasolina" to "cat_gasolina",
            "colectivo" to "cat_transporte_publico",
            "bus" to "cat_transporte_publico",
            "domicilio" to "cat_domicilios",
            "delivery" to "cat_domicilios",
            "groceries" to "cat_supermercado",
            "padaria" to "cat_cafe",
            "farmacia" to "cat_farmacia",
            "pharmacy" to "cat_farmacia",
            "aluguel" to "cat_arriendo",
            "sueldo" to "cat_sueldo",
            "paycheck" to "cat_sueldo",
        )

    expected.forEach { (subject, categoriaId) ->
      assertEquals(categoriaId, suggest(subject).firstOrNull()?.categoriaId, subject)
    }
  }

  @Test
  fun commonRegionalBrandsResolve() {
    val expected =
        mapOf(
            "Uber" to "cat_taxi",
            "DiDi" to "cat_taxi",
            "Cabify" to "cat_taxi",
            "Rappi" to "cat_domicilios",
            "iFood" to "cat_domicilios",
            "PedidosYa" to "cat_domicilios",
            "Netflix" to "cat_streaming",
            "Spotify" to "cat_streaming",
            "Disney+" to "cat_streaming",
            "Airbnb" to "cat_hospedaje",
            "Booking" to "cat_hospedaje",
            "Terpel" to "cat_gasolina",
            "YPF" to "cat_gasolina",
            "Copec" to "cat_gasolina",
            "Éxito" to "cat_supermercado",
            "Carulla" to "cat_supermercado",
            "Walmart" to "cat_supermercado",
            "OXXO" to "cat_supermercado",
        )

    expected.forEach { (subject, categoriaId) ->
      assertEquals(categoriaId, suggest(subject).firstOrNull()?.categoriaId, subject)
    }
    val marketplace = ids(suggest("Mercado Libre"))
    assertTrue("cat_varios" in marketplace && "cat_supermercado" !in marketplace)
  }

  @Test
  fun phrasesBeatAnotherCategorysBrand() {
    assertEquals("cat_domicilios", suggest("Uber Eats").first().categoriaId)
    assertEquals("cat_domicilios", suggest("didi food").first().categoriaId)
  }
}
