package com.kurobello.tallybook.feature.statistics.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.savedstate.SavedState
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.StatisticsScope
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

internal const val STATISTICS_SAVED_INPUT_KEY = "statistics-input"

/** Unknown or missing values restore their default: the current month, primary and Categorias. */
@Serializable
internal data class StatisticsSavedInput(
    val version: Int,
    val scope: String,
    val anchor: String,
    val moneda: String? = null,
    val dimension: String? = null,
)

internal data class StatisticsRestoredInput(
    val selection: StatisticsSelection,
    val moneda: Moneda?,
    val dimension: StatisticsDimension,
)

internal fun statisticsSavedInput(
    selection: StatisticsSelection,
    moneda: Moneda?,
    dimension: StatisticsDimension,
): StatisticsSavedInput =
    StatisticsSavedInput(
        STATISTICS_SAVED_VERSION,
        selection.scope.name,
        selection.anchor.toString(),
        moneda?.name,
        dimension.name,
    )

internal fun restoreStatisticsInput(
    handle: SavedStateHandle,
    today: LocalDate,
): StatisticsRestoredInput {
  val input =
      (handle.get<Any>(STATISTICS_SAVED_INPUT_KEY) as? SavedState)
          ?.let {
            try {
              decodeFromSavedState(StatisticsSavedInput.serializer(), it)
            } catch (_: SerializationException) {
              null
            } catch (_: IllegalArgumentException) {
              null
            }
          }
          ?.takeIf { it.version == STATISTICS_SAVED_VERSION }
  val scope = StatisticsScope.entries.firstOrNull { it.name == input?.scope }
  val anchor = input?.anchor?.let { parseDateOrNull(it) }?.takeIf { it <= today }
  return StatisticsRestoredInput(
      selection =
          if (scope != null && anchor != null) StatisticsSelection(scope, anchor)
          else StatisticsSelection.initial(today),
      moneda = Moneda.entries.firstOrNull { it.name == input?.moneda },
      dimension =
          StatisticsDimension.entries.firstOrNull { it.name == input?.dimension }
              ?: StatisticsDimension.Categorias,
  )
}

internal fun installStatisticsCheckpoint(
    handle: SavedStateHandle,
    input: () -> StatisticsSavedInput,
) {
  handle.setSavedStateProvider(STATISTICS_SAVED_INPUT_KEY) {
    encodeToSavedState(StatisticsSavedInput.serializer(), input())
  }
}

private fun parseDateOrNull(text: String): LocalDate? =
    try {
      LocalDate.parse(text)
    } catch (_: IllegalArgumentException) {
      null
    }

private const val STATISTICS_SAVED_VERSION = 1
