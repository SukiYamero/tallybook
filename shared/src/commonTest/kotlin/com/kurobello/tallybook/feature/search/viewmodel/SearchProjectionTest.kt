package com.kurobello.tallybook.feature.search.viewmodel

import com.kurobello.tallybook.core.model.Actividad
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.text.normalizeSearchText
import com.kurobello.tallybook.feature.search.ui.SearchCurrencyYears
import com.kurobello.tallybook.feature.search.ui.SearchRequest
import com.kurobello.tallybook.feature.search.ui.SearchResultRowUi
import com.kurobello.tallybook.feature.search.ui.SearchResultsUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class SearchProjectionTest {
  private suspend fun matchRows(
      candidates: List<SearchCandidate>,
      normalizedQuery: String,
  ): List<SearchResultRowUi> =
      matchSearchCandidates(candidates, normalizedQuery).map { assertIs<SearchResultRowUi>(it) }

  @Test
  fun matchesLiteralFoldedFieldsWithoutSearchingIdsOrSplittingWords() = runTest {
    val rows = listOf(row("hidden-id", "Café %_ [x] 😀"), row("second", "niño"))
    val candidates =
        buildSearchCandidates(
            rows.map(Actividad::DeMovimiento),
            emptyList(),
            emptyList(),
            "Missing category",
        )
    for (query in listOf("cafe", "%_", "[x]", "😀", "MISSING")) {
      val result = matchRows(candidates, normalizeSearchText(query))
      assertEquals(listOf("hidden-id"), result.take(1).map { it.id })
    }
    for (query in listOf("hidden-id", "cafe 😀", "nino")) {
      assertEquals(emptyList(), matchRows(candidates, normalizeSearchText(query)))
    }
    assertEquals(listOf("second"), matchRows(candidates, "niño").map { it.id })
  }

  @Test
  fun usesTituloFirstAndLocalizedFallbackWhilePreservingDomainObjectsAndOrder() = runTest {
    val rows = listOf(row("first", "  "), row("second", null), row("third", "Lunch"))
    val candidates =
        buildSearchCandidates(
            rows.map(Actividad::DeMovimiento),
            emptyList(),
            emptyList(),
            "Sin categoría",
        )
    val matched = matchRows(candidates, "")
    assertEquals(listOf("first", "second", "third"), matched.map { it.id })
    assertEquals(listOf("Sin categoría", "Sin categoría", "Lunch"), matched.map { it.title })
    assertSame(rows.first(), matched.first().movimiento)
    assertEquals(Money(Long.MAX_VALUE, Moneda.USD), matched.first().movimiento.monto)
  }

  @Test
  fun blankCatalogLabelsUseTheSameFallbackForDisplayAndMatching() = runTest {
    val category = Categoria("missing", " ", null, CategoryIcon.WALLET, CategoryTint.BLUE)
    val candidates =
        buildSearchCandidates(
            listOf(Actividad.DeMovimiento(row("one", null))),
            listOf(category),
            emptyList(),
            "Unknown",
        )
    assertEquals("Unknown", matchRows(candidates, "unknown").single().categoryLabel)
  }

  @Test
  fun matchesTheCategoryNameShownByTheRow() = runTest {
    val food = Categoria("food", "Comida", null, CategoryIcon.WALLET, CategoryTint.BLUE)
    val gift = Categoria("gift", "Regalo", null, CategoryIcon.WALLET, CategoryTint.BLUE)
    val rows = listOf(row("dinner", "Cena", "gift"), row("single", "Cine"))
    val candidates =
        buildSearchCandidates(
            rows.map(Actividad::DeMovimiento),
            listOf(food, gift),
            emptyList(),
            "Unknown",
        )

    val byGift = matchRows(candidates, "regalo")
    assertEquals(listOf("dinner"), byGift.map { it.id })
    assertEquals("Regalo", byGift.single().categoryLabel)
    assertSame(gift, byGift.single().categoria)
    assertEquals(emptyList(), matchRows(candidates, "comida"))
    assertEquals(emptyList(), matchRows(candidates, "gift"))
  }

  @Test
  fun matchesTituloAndDescripcionWhileTheTitleStaysTheTitulo() = runTest {
    val rows = listOf(row("dinner", "Cena", descripcion = "Cumpleaños de Ana"), row("cine", "Cine"))
    val candidates =
        buildSearchCandidates(
            rows.map(Actividad::DeMovimiento),
            emptyList(),
            emptyList(),
            "Unknown",
        )

    val byDescription = matchRows(candidates, normalizeSearchText("CUMPLEAÑOS"))
    assertEquals(listOf("dinner"), byDescription.map { it.id })
    assertEquals("Cena", byDescription.single().title)
    assertEquals(listOf("cine"), matchRows(candidates, "cine").map { it.id })
  }

  @Test
  fun queryMatchingOnlyATagNameReturnsTheTaggedRow() = runTest {
    val empresa = Etiqueta("t_empresa", "Empresa Ñandú", Instant.parse("2026-01-01T00:00:00Z"))
    val viaje = Etiqueta("t_viaje", "Viaje Chile", Instant.parse("2026-01-01T00:00:00Z"))
    val rows =
        listOf(
            row("tagged", "Cena", etiquetaIds = listOf("t_viaje", "t_empresa")),
            row("untagged", "Cine"),
            row("deleted-tag", "Taxi", etiquetaIds = listOf("t_deleted")),
        )
    val candidates =
        buildSearchCandidates(
            rows.map(Actividad::DeMovimiento),
            emptyList(),
            listOf(empresa, viaje),
            "Unknown",
        )

    for (query in listOf("ÑANDU", "chile")) {
      val matched = matchRows(candidates, normalizeSearchText(query))
      assertEquals(listOf("tagged"), matched.map { it.id })
      assertEquals("Cena", matched.single().title)
    }
    assertEquals(emptyList(), matchRows(candidates, normalizeSearchText("nandu")))
    assertEquals(emptyList(), matchRows(candidates, "t_deleted"))
    assertEquals(emptyList(), matchRows(candidates, "t_empresa"))
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
            matchRows(
                buildSearchCandidates(
                    rows.map(Actividad::DeMovimiento),
                    emptyList(),
                    emptyList(),
                    "Unknown",
                ),
                "",
            ),
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
      categoriaId: String = "missing",
      descripcion: String? = null,
      fecha: LocalDate = LocalDate(2026, 10, 6),
      etiquetaIds: List<String> = emptyList(),
  ) =
      Movimiento(
          id,
          TipoMovimiento.GASTO,
          Money(Long.MAX_VALUE, Moneda.USD),
          fecha,
          categoriaId,
          titulo,
          descripcion,
          Instant.parse("2026-10-06T12:00:00Z"),
          Cuenta.defaultCashId(Moneda.USD),
          MedioPago.EFECTIVO,
          etiquetaIds,
      )
}
