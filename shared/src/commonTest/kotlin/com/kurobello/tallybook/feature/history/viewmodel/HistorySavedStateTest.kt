package com.kurobello.tallybook.feature.history.viewmodel

import androidx.lifecycle.SavedStateHandle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

class HistorySavedStateTest {
  @Test
  fun wrongCheckpointContainerFallsBackToFreshSelection() {
    val today = LocalDate(2026, 10, 5)
    for (payload in listOf("invalid checkpoint", 7)) {
      val handle = SavedStateHandle(mapOf(HISTORY_SAVED_INPUT_KEY to payload))
      assertEquals(HistorySelection.initial(today), restoreHistoryInput(handle, today))
    }
  }
}
