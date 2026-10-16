package com.kurobello.tallybook.feature.search.viewmodel

import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

class SearchSavedInputTest {
  private val today = LocalDate(2028, 3, 1)
  private val raw = "  Niño 🧾  % café_  "
  private val filters =
      SearchFilters.initial(today)
          .copy(
              preset = SearchDatePreset.Custom,
              customStart = LocalDate(2027, 12, 31),
              tipo = TipoMovimiento.GASTO,
              categoryIds = setOf("z-missing", "a"),
              etiquetaIds = setOf("t_viaje", "t_empresa"),
          )

  @Test
  fun schemaRoundTripPreservesLiteralRawTextAndDeterministicCategoryOrder() {
    val input = filters.savedInput(raw)
    assertEquals(1, input.version)
    assertEquals(listOf("a", "z-missing"), input.categoryIds)
    assertEquals(listOf("t_empresa", "t_viaje"), input.etiquetaIds)
    val encoded = Json.encodeToString(input)
    assertFalse(encoded.contains("session"))
    assertFalse(encoded.contains("results"))
    val restored = SearchSavedInput.restore(Json.decodeFromString<SearchSavedInput>(encoded), today)
    assertEquals(raw, restored.rawQuery)
    assertEquals(filters, restored.filters)
  }

  @Test
  fun absentAndUnknownSchemaRecoverBothInputs() {
    listOf(null, filters.savedInput(raw).copy(version = 99)).forEach { input ->
      val restored = SearchSavedInput.restore(input, today)
      assertEquals("", restored.rawQuery)
      assertEquals(SearchFilters.initial(today), restored.filters)
    }
  }

  @Test
  fun unknownTokensMalformedDatesAndFutureCustomResetOnlyFilters() {
    val input = filters.savedInput(raw)
    listOf(
            input.copy(preset = "unknown"),
            input.copy(tipo = "unknown"),
            input.copy(customStart = "wrong"),
            input.copy(customEnd = "2028-02-30"),
            input.copy(customEnd = "2028-03-02"),
        )
        .forEach { invalid ->
          val restored = SearchSavedInput.restore(invalid, today)
          assertEquals(raw, restored.rawQuery)
          assertEquals(SearchFilters.initial(today), restored.filters)
        }
  }

  @Test
  fun restoreNormalizesReversedDatesAndCleansIdsWithoutCatalogPruning() {
    val restored =
        SearchSavedInput.restore(
            filters
                .savedInput(raw)
                .copy(
                    customStart = today.toString(),
                    customEnd = "1900-01-01",
                    categoryIds = listOf("z-missing", "", "  ", "a", "a"),
                    etiquetaIds = listOf("t_viaje", " ", "t_viaje"),
                ),
            today,
        )
    assertEquals(LocalDate(1900, 1, 1), restored.filters.customStart)
    assertEquals(today, restored.filters.customEnd)
    assertEquals(setOf("a", "z-missing"), restored.filters.categoryIds)
    assertEquals(setOf("t_viaje"), restored.filters.etiquetaIds)
  }

  @Test
  fun inputSavedBeforeTagsExistedRestoresWithoutTagFilters() {
    val legacy =
        JsonObject(Json.encodeToJsonElement(filters.savedInput(raw)).jsonObject - "etiquetaIds")

    val restored =
        SearchSavedInput.restore(Json.decodeFromJsonElement<SearchSavedInput>(legacy), today)

    assertEquals(raw, restored.rawQuery)
    assertEquals(filters.copy(etiquetaIds = emptySet()), restored.filters)
  }

  @Test
  fun restoredRelativePresetRecomputesAgainstCurrentTodayWithoutLosingCustomDates() {
    val restored =
        SearchSavedInput.restore(
            filters.copy(preset = SearchDatePreset.Last7Days).savedInput(raw),
            LocalDate(2028, 3, 2),
        )
    val resolution =
        resolveSearchFilters(restored.filters, LocalDate(2028, 3, 2))
            as SearchFilterResolution.Valid
    assertEquals(LocalDate(2028, 2, 25), resolution.range!!.start)
    assertEquals(LocalDate(2028, 3, 2), resolution.range.end)
    assertEquals(filters.customStart, restored.filters.customStart)
    assertEquals(filters.customEnd, restored.filters.customEnd)
  }
}
