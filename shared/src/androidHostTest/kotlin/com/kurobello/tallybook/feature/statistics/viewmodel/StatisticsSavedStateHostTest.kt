package com.kurobello.tallybook.feature.statistics.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.savedstate.serialization.encodeToSavedState
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.StatisticsScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class StatisticsSavedStateHostTest {
  private val today = LocalDate(2026, 10, 24)

  @Test
  fun `a checkpoint restores the selection the currency and the dimension`() = runTest {
    val selection = StatisticsSelection(StatisticsScope.Year, LocalDate(2025, 3, 10))
    val handle =
        handleWith(statisticsSavedInput(selection, Moneda.COP, StatisticsDimension.Cuentas))
    val vm = StatisticsFixture(this).create(handle)
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()

    val state = vm.uiState.value
    assertEquals(selection, state.selection)
    assertEquals(Moneda.COP, state.moneda)
    assertEquals(StatisticsDimension.Cuentas, state.dimension)
    assertEquals(StatisticsScope.Year, state.content.cachedValue?.scope)
  }

  @Test
  fun `an unknown version or value restores the defaults`() {
    for (input in
        listOf(
            StatisticsSavedInput(2, "Year", "2025-03-10", "COP", "Cuentas"),
            StatisticsSavedInput(1, "Week", "2025-03-10", "XYZ", "Semana"),
            StatisticsSavedInput(1, "Month", "2027-01-01"),
        )) {
      assertEquals(
          StatisticsRestoredInput(
              StatisticsSelection.initial(today),
              null,
              StatisticsDimension.Categorias,
          ),
          restoreStatisticsInput(handleWith(input), today),
      )
    }
  }

  private fun handleWith(input: StatisticsSavedInput) =
      SavedStateHandle(
          mapOf(
              STATISTICS_SAVED_INPUT_KEY to
                  encodeToSavedState(StatisticsSavedInput.serializer(), input)
          )
      )
}
