package com.kurobello.tallybook.feature.search.viewmodel

import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.SearchTipo
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
              tipo = SearchTipo.GASTO,
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
  fun accountsMediosTransferTypeAndCurrencyRoundTripWithStableOrder() {
    val withAccounts =
        filters.copy(
            tipo = SearchTipo.TRANSFERENCIA,
            cuentaIds = setOf("z-nu", "a-nequi"),
            medios = setOf(MedioPago.CREDITO, MedioPago.EFECTIVO),
        )
    val input = withAccounts.savedInput(raw, Moneda.USD)
    assertEquals("transferencia", input.tipo)
    assertEquals(listOf("a-nequi", "z-nu"), input.cuentaIds)
    assertEquals(listOf("EFECTIVO", "CREDITO"), input.medios)
    assertEquals("USD", input.moneda)
    val restored =
        SearchSavedInput.restore(
            Json.decodeFromString<SearchSavedInput>(Json.encodeToString(input)),
            today,
        )
    assertEquals(withAccounts, restored.filters)
    assertEquals(Moneda.USD, restored.moneda)
  }

  @Test
  fun previousTipoTokensRestoreTheSameSearchTypeWithoutCurrency() {
    val legacy =
        Json.decodeFromString<SearchSavedInput>(
            """{"version":1,"rawQuery":"","preset":"all","customStart":"2028-03-01",""" +
                """"customEnd":"2028-03-01","tipo":"ingreso","categoryIds":["a"]}"""
        )
    val restored = SearchSavedInput.restore(legacy, today)
    assertEquals(
        SearchFilters.initial(today).copy(tipo = SearchTipo.INGRESO, categoryIds = setOf("a")),
        restored.filters,
    )
    assertEquals(null, restored.moneda)
  }

  @Test
  fun unknownMediosAndCurrencyAreDroppedAndBlankAccountsCleaned() {
    val restored =
        SearchSavedInput.restore(
            filters
                .savedInput(raw)
                .copy(
                    cuentaIds = listOf(" ", "nequi"),
                    medios = listOf("CHEQUE", "DEBITO"),
                    moneda = "XYZ",
                ),
            today,
        )
    assertEquals(setOf("nequi"), restored.filters.cuentaIds)
    assertEquals(setOf(MedioPago.DEBITO), restored.filters.medios)
    assertEquals(null, restored.moneda)
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
