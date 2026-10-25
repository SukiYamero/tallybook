package com.kurobello.tallybook.feature.history.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.savedstate.serialization.encodeToSavedState
import com.kurobello.tallybook.core.model.Moneda
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class HistorySavedStateHostTest {
  private val today = LocalDate(2026, 10, 5)

  @Test
  fun `checkpoint restores the selected currency with the selection`() {
    val selection = HistorySelection(HistoryScope.Month, LocalDate(2026, 9, 3))
    val restored =
        restore(
            encodeToSavedState(
                HistorySavedInput.serializer(),
                historySavedInput(selection, Moneda.COP),
            )
        )
    assertEquals(HistoryRestoredInput(selection, Moneda.COP), restored)
  }

  @Test
  fun `checkpoint saved before the currency selector restores the primary currency`() {
    val restored =
        restore(
            encodeToSavedState(
                PreCurrencyHistorySavedInput.serializer(),
                PreCurrencyHistorySavedInput(1, "Week", "2026-09-03"),
            )
        )
    assertEquals(HistorySelection(HistoryScope.Week, LocalDate(2026, 9, 3)), restored.selection)
    assertNull(restored.moneda)
  }

  @Test
  fun `an unknown currency or version restores the primary currency`() {
    for (input in
        listOf(
            HistorySavedInput(1, "Day", "2026-09-03", "XYZ"),
            HistorySavedInput(2, "Day", "2026-09-03", "COP"),
        )) {
      assertNull(restore(encodeToSavedState(HistorySavedInput.serializer(), input)).moneda)
    }
  }

  private fun restore(checkpoint: Any) =
      restoreHistoryCheckpoint(
          SavedStateHandle(mapOf(HISTORY_SAVED_INPUT_KEY to checkpoint)),
          today,
      )
}

@Serializable
private data class PreCurrencyHistorySavedInput(
    val version: Int,
    val scope: String,
    val anchor: String,
)
