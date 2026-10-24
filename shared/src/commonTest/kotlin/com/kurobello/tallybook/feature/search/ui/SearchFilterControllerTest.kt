package com.kurobello.tallybook.feature.search.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import com.kurobello.tallybook.core.ui.movimiento.AddMovimientoSheetSession
import com.kurobello.tallybook.core.ui.navigation.PrimaryModalCoordinator
import com.kurobello.tallybook.feature.search.viewmodel.SearchFilterSession
import com.kurobello.tallybook.feature.search.viewmodel.SearchFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class SearchFilterControllerTest {
  @Test
  fun filterSessionCoverageIsObservableUntilTheSheetIsHidden() = runTest {
    val vm = SearchFixture(this).create()
    backgroundScope.launch { vm.uiState.collect {} }
    runCurrent()
    val collected = mutableStateOf(vm.uiState.value)
    val coordinator = PrimaryModalCoordinator(AddMovimientoSheetSession()) { true }
    val controller = SearchFilterController(vm, collected)
    coordinator.attach(controller)
    val coverage = mutableListOf<Boolean>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      snapshotFlow { coordinator.isContentCovered }.collect { coverage += it }
    }
    val publish = {
      collected.value = vm.uiState.value
      Snapshot.sendApplyNotifications()
      runCurrent()
    }
    assertTrue(coordinator.tryOpenDestinationModal(controller))
    assertTrue(coordinator.isContentCovered)
    publish()
    assertEquals(listOf(false, true), coverage)
    val id = assertIs<SearchFilterSession.Editing>(vm.uiState.value.filterSession).sessionId
    vm.onCancelFilters(id)
    publish()
    assertEquals(listOf(false, true), coverage)
    vm.onFiltersHidden(id)
    publish()
    assertEquals(listOf(false, true, false), coverage)
  }
}
