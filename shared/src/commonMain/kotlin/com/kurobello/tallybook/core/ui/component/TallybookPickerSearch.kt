package com.kurobello.tallybook.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged

/**
 * The query of a searchable picker sheet plus the height its content had before searching. Results
 * that shrink while searching keep that height, so the sheet does not jump under the keyboard.
 */
@Stable
internal class PickerSearchState(initialQuery: String) {
  var query by mutableStateOf(initialQuery)
    private set

  private var shownHeightPx by mutableIntStateOf(0)
  var searchMinimumHeightPx by mutableIntStateOf(0)
    private set

  val isSearching: Boolean
    get() = query.isNotBlank()

  fun onQueryChange(value: String) {
    if (value.isNotBlank() && query.isBlank()) searchMinimumHeightPx = shownHeightPx
    query = value
    if (value.isBlank()) searchMinimumHeightPx = 0
  }

  internal fun reset() {
    query = ""
    shownHeightPx = 0
    searchMinimumHeightPx = 0
  }

  internal fun onShownHeightChanged(height: Int) {
    if (!isSearching) shownHeightPx = height
  }

  companion object {
    val Saver: Saver<PickerSearchState, String> =
        Saver(save = { it.query }, restore = { PickerSearchState(it) })
  }
}

/** Every time the sheet opens it starts without a query. */
@Composable
internal fun rememberPickerSearchState(visible: Boolean): PickerSearchState {
  val state = rememberSaveable(saver = PickerSearchState.Saver) { PickerSearchState("") }
  LaunchedEffect(visible) { if (visible) state.reset() }
  return state
}

/** Applied to the picker content: records its resting height and holds it while searching. */
internal fun Modifier.pickerSearchHeight(state: PickerSearchState): Modifier =
    layout { measurable, constraints ->
          val minimum = if (state.isSearching) state.searchMinimumHeightPx else 0
          val minHeight = minimum.coerceIn(constraints.minHeight, constraints.maxHeight)
          val placeable = measurable.measure(constraints.copy(minHeight = minHeight))
          layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
        .onSizeChanged { size -> state.onShownHeightChanged(size.height) }
