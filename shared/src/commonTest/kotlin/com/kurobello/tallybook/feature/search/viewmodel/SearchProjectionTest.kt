package com.kurobello.tallybook.feature.search.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.text.normalizeSearchText
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
  fun usesDescriptionFirstAndLocalizedFallbackWhilePreservingDomainObjectsAndOrder() = runTest {
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

  private fun row(
      id: String,
      description: String?,
      categoriaIds: List<String> = listOf("missing"),
  ) =
      Movimiento(
          id,
          TipoMovimiento.GASTO,
          Money(Long.MAX_VALUE, Moneda.USD),
          LocalDate(2026, 10, 6),
          categoriaIds,
          description,
          Instant.parse("2026-10-06T12:00:00Z"),
      )
}
