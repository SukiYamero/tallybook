package com.kurobello.tallybook.feature.search.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.savedstate.SavedState
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerializationException

internal const val SEARCH_SAVED_INPUT_KEY = "search-input"

internal fun restoreSearchInput(handle: SavedStateHandle, today: LocalDate): SearchRestoredInput {
  val checkpoint = handle.get<Any>(SEARCH_SAVED_INPUT_KEY) as? SavedState
  val input = checkpoint?.let {
    try {
      decodeFromSavedState(SearchSavedInput.serializer(), it)
    } catch (_: SerializationException) {
      null
    } catch (_: IllegalArgumentException) {
      null
    }
  }
  return SearchSavedInput.restore(input, today)
}

internal fun installSearchCheckpoint(handle: SavedStateHandle, input: () -> SearchSavedInput) {
  handle.setSavedStateProvider(SEARCH_SAVED_INPUT_KEY) {
    encodeToSavedState(SearchSavedInput.serializer(), input())
  }
}
