package com.kurobello.tallybook.feature.search.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.savedstate.savedState
import androidx.savedstate.serialization.encodeToSavedState
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.SearchTipo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SearchSavedStateTest {
  @Test
  fun `typed checkpoint restores raw query and applied filters`() {
    val today = LocalDate(2026, 10, 6)
    val filters = SearchFilters.initial(today).copy(preset = SearchDatePreset.ThisYear)
    val checkpoint =
        encodeToSavedState(SearchSavedInput.serializer(), filters.savedInput(" Café %_ "))
    val restored =
        restoreSearchInput(SavedStateHandle(mapOf(SEARCH_SAVED_INPUT_KEY to checkpoint)), today)
    assertEquals(" Café %_ ", restored.rawQuery)
    assertEquals(filters, restored.filters)
  }

  @Test
  fun `typed checkpoint restores tag filters`() {
    val today = LocalDate(2026, 10, 6)
    val filters = SearchFilters.initial(today).copy(etiquetaIds = setOf("t_empresa"))
    val checkpoint = encodeToSavedState(SearchSavedInput.serializer(), filters.savedInput(""))
    val restored =
        restoreSearchInput(SavedStateHandle(mapOf(SEARCH_SAVED_INPUT_KEY to checkpoint)), today)
    assertEquals(filters, restored.filters)
  }

  @Test
  fun `checkpoint saved before tags existed restores its filters without tags`() {
    val today = LocalDate(2026, 10, 6)
    val checkpoint =
        encodeToSavedState(
            PreTagsSearchSavedInput.serializer(),
            PreTagsSearchSavedInput(
                1,
                "cafe",
                "this-year",
                "2026-10-06",
                "2026-10-06",
                null,
                listOf("food"),
            ),
        )
    val restored =
        restoreSearchInput(SavedStateHandle(mapOf(SEARCH_SAVED_INPUT_KEY to checkpoint)), today)
    assertEquals("cafe", restored.rawQuery)
    assertEquals(
        SearchFilters.initial(today)
            .copy(preset = SearchDatePreset.ThisYear, categoryIds = setOf("food")),
        restored.filters,
    )
  }

  @Test
  fun `typed checkpoint restores account medio transfer type and currency selections`() {
    val today = LocalDate(2026, 10, 6)
    val filters =
        SearchFilters.initial(today)
            .copy(
                tipo = SearchTipo.TRANSFERENCIA,
                cuentaIds = setOf("nequi", "gone"),
                medios = setOf(MedioPago.CREDITO, MedioPago.DEBITO),
            )
    val checkpoint =
        encodeToSavedState(SearchSavedInput.serializer(), filters.savedInput("", Moneda.USD))
    val restored =
        restoreSearchInput(SavedStateHandle(mapOf(SEARCH_SAVED_INPUT_KEY to checkpoint)), today)
    assertEquals(filters, restored.filters)
    assertEquals(Moneda.USD, restored.moneda)
  }

  @Test
  fun `checkpoint saved while the type was a TipoMovimiento keeps its type`() {
    val today = LocalDate(2026, 10, 6)
    for ((token, tipo) in listOf("gasto" to SearchTipo.GASTO, "ingreso" to SearchTipo.INGRESO)) {
      val checkpoint =
          encodeToSavedState(
              PreTagsSearchSavedInput.serializer(),
              PreTagsSearchSavedInput(1, "", "all", "2026-10-06", "2026-10-06", token, emptyList()),
          )
      val restored =
          restoreSearchInput(SavedStateHandle(mapOf(SEARCH_SAVED_INPUT_KEY to checkpoint)), today)
      assertEquals(SearchFilters.initial(today).copy(tipo = tipo), restored.filters)
      assertNull(restored.moneda)
    }
  }

  @Test
  fun `wrong or undecodable containers recover fresh input`() {
    val today = LocalDate(2026, 10, 6)
    for (payload in listOf("invalid", 7, savedState())) {
      val restored =
          restoreSearchInput(SavedStateHandle(mapOf(SEARCH_SAVED_INPUT_KEY to payload)), today)
      assertEquals("", restored.rawQuery)
      assertEquals(SearchFilters.initial(today), restored.filters)
    }
  }
}

@Serializable
private data class PreTagsSearchSavedInput(
    val version: Int,
    val rawQuery: String,
    val preset: String,
    val customStart: String,
    val customEnd: String,
    val tipo: String?,
    val categoryIds: List<String>,
)
