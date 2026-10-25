package com.kurobello.tallybook.feature.history.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.savedstate.SavedState
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import com.kurobello.tallybook.core.model.Moneda
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerializationException

internal const val HISTORY_SAVED_INPUT_KEY = "history-input"

internal data class HistoryRestoredInput(val selection: HistorySelection, val moneda: Moneda?)

internal fun restoreHistoryInput(handle: SavedStateHandle, today: LocalDate): HistorySelection =
    restoreHistoryCheckpoint(handle, today).selection

internal fun restoreHistoryCheckpoint(
    handle: SavedStateHandle,
    today: LocalDate,
): HistoryRestoredInput {
  val checkpoint = handle.get<Any>(HISTORY_SAVED_INPUT_KEY) as? SavedState
  val input = checkpoint?.let {
    try {
      decodeFromSavedState(HistorySavedInput.serializer(), it)
    } catch (_: SerializationException) {
      null
    } catch (_: IllegalArgumentException) {
      null
    }
  }
  return HistoryRestoredInput(HistorySelection.restore(input, today), input?.restoredMoneda())
}

internal fun historySavedInput(selection: HistorySelection, moneda: Moneda?): HistorySavedInput =
    selection.savedInput().copy(moneda = moneda?.name)

internal fun installHistoryCheckpoint(
    handle: SavedStateHandle,
    selection: () -> HistorySelection,
    moneda: () -> Moneda?,
) {
  handle.setSavedStateProvider(HISTORY_SAVED_INPUT_KEY) {
    encodeToSavedState(HistorySavedInput.serializer(), historySavedInput(selection(), moneda()))
  }
}
