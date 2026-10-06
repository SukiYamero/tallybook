package com.kurobello.tallybook.feature.history.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.savedstate.SavedState
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerializationException

internal const val HISTORY_SAVED_INPUT_KEY = "history-input"

internal fun restoreHistoryInput(handle: SavedStateHandle, today: LocalDate): HistorySelection {
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
  return HistorySelection.restore(input, today)
}

internal fun installHistoryCheckpoint(handle: SavedStateHandle, selection: () -> HistorySelection) {
  handle.setSavedStateProvider(HISTORY_SAVED_INPUT_KEY) {
    encodeToSavedState(HistorySavedInput.serializer(), selection().savedInput())
  }
}
