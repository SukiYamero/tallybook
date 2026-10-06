package com.kurobello.tallybook.feature.search.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.savedstate.savedState
import androidx.savedstate.serialization.encodeToSavedState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
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
