package com.kurobello.tallybook.feature.search.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.text.normalizeSearchText
import com.kurobello.tallybook.feature.search.ui.SearchCurrencyYears
import com.kurobello.tallybook.feature.search.ui.SearchRequest
import com.kurobello.tallybook.feature.search.ui.SearchResultsUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class SearchProjectionTest {
  @Test
  fun matchesLiteralFoldedFieldsWithoutSearchingIdsOrSplittingWords() = runTest {
    val rows = listOf(row("hidden-id", "Café %_ [x] 😀"), row("second", "niño"))
    val candidates = buildSearchCandidates(rows, emptyList(), "Missing category")
    for (query in listOf("cafe", "%_", "[x]", "😀", "MISSING")) {
      val result = matchSearchCandidates(candidates, normalizeSearchText(query))
      assertEquals(listOf("hidden-id"), result.take(1).map { it.id })
    }
    for (query in listOf("hidden-id", "cafe 😀", "nino")) {
      assertEquals(emptyList(), matchSearchCandidates(candidates, normalizeSearchText(query)))
    }
    assertEquals(listOf("second"), matchSearchCandidates(candidates, "niño").map { it.id })
  }

  @Test
  fun usesTituloFirstAndLocalizedFallbackWhilePreservingDomainObjectsAndOrder() = runTest {
    val rows = listOf(row("first", "  "), row("second", null), row("third", "Lunch"))
    val candidates = buildSearchCandidates(rows, emptyList(), "Sin categoría")
    val matched = matchSearchCandidates(candidates, "")
    assertEquals(listOf("first", "second", "third"), matched.map { it.id })
    assertEquals(listOf("Sin categoría", "Sin categoría", "Lunch"), matched.map { it.title })
    assertSame(rows.first(), matched.first().movimiento)
    assertEquals(Money(Long.MAX_VALUE, Moneda.USD), matched.first().movimiento.monto)
  }

  @Test
  fun blankCatalogLabelsUseTheSameFallbackForDisplayAndMatching() = runTest {
    val category = Categoria("missing", " ", null, CategoryIcon.WALLET, CategoryTint.BLUE)
    val candidates = buildSearchCandidates(listOf(row("one", null)), listOf(category), "Unknown")
    assertEquals("Unknown", matchSearchCandidates(candidates, "unknown").single().categoryLabel)
  }

  @Test
  fun matchesAnyCategoryNameWhileTheRowShowsTheFirstOne() = runTest {
    val food = Categoria("food", "Comida", null, CategoryIcon.WALLET, CategoryTint.BLUE)
    val gift = Categoria("gift", "Regalo", null, CategoryIcon.WALLET, CategoryTint.BLUE)
    val rows = listOf(row("multi", "Cena", listOf("food", "gift")), row("single", "Cine"))
    val candidates = buildSearchCandidates(rows, listOf(food, gift), "Unknown")

    val byGift = matchSearchCandidates(candidates, "regalo")
    assertEquals(listOf("multi"), byGift.map { it.id })
    assertEquals("Comida", byGift.single().categoryLabel)
    assertSame(food, byGift.single().categoria)
    assertEquals(listOf("multi"), matchSearchCandidates(candidates, "comida").map { it.id })
    assertEquals(emptyList(), matchSearchCandidates(candidates, "gift"))
  }

  @Test
  fun matchesTituloAndDescripcionWhileTheTitleStaysTheTitulo() = runTest {
    val rows = listOf(row("dinner", "Cena", descripcion = "Cumpleaños de Ana"), row("cine", "Cine"))
    val candidates = buildSearchCandidates(rows, emptyList(), "Unknown")

    val byDescription = matchSearchCandidates(candidates, normalizeSearchText("CUMPLEAÑOS"))
    assertEquals(listOf("dinner"), byDescription.map { it.id })
    assertEquals("Cena", byDescription.single().title)
    assertEquals(listOf("cine"), matchSearchCandidates(candidates, "cine").map { it.id })
  }

  @Test
  fun resultsGroupByDayNewestFirstWithOnlyTheDaysThatHaveResults() = runTest {
    val october8 = LocalDate(2026, 10, 8)
    val october6 = LocalDate(2026, 10, 6)
    val rows =
        listOf(
            row("late", "Taxi", fecha = october8),
            row("early", "Café", fecha = october8),
            row("older", "Cine", fecha = october6),
        )
    val results =
        SearchResultsUi(
            SearchRequest(MovimientoReadCriteria(Moneda.USD), ""),
            matchSearchCandidates(buildSearchCandidates(rows, emptyList(), "Unknown"), ""),
            SearchCurrencyYears(Moneda.USD, listOf(2026)),
        )

    assertEquals(listOf(october8, october6), results.groups.map { it.fecha })
    assertEquals(
        listOf(listOf("late", "early"), listOf("older")),
        results.groups.map { group -> group.rows.map { it.id } },
    )
    assertEquals(3, results.count)
  }

  private fun row(
      id: String,
      titulo: String?,
      categoriaIds: List<String> = listOf("missing"),
      descripcion: String? = null,
      fecha: LocalDate = LocalDate(2026, 10, 6),
  ) =
      Movimiento(
          id,
          TipoMovimiento.GASTO,
          Money(Long.MAX_VALUE, Moneda.USD),
          fecha,
          categoriaIds,
          titulo,
          descripcion,
          Instant.parse("2026-10-06T12:00:00Z"),
      )
}
