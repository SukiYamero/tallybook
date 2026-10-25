package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import com.kurobello.tallybook.core.ui.movimiento.AddMovimientoSheetSession
import com.kurobello.tallybook.feature.movimientos.viewmodel.ManageMovimientoFixture
import com.kurobello.tallybook.feature.movimientos.viewmodel.managementMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ManageMovimientoControllerTest {
  @Test
  fun managementCoverageIsObservableUntilTheSheetIsHidden() = runTest {
    val fixture = ManageMovimientoFixture(backgroundScope)
    fixture.repository.current = managementMovimiento()
    val collected = mutableStateOf(fixture.viewModel.uiState.value)
    val coordinator = PrimaryModalCoordinator(AddMovimientoSheetSession()) { false }
    coordinator.attachManagement(
        ManageMovimientoController(fixture.viewModel, collected, fixture.feedback)
    )
    val coverage = mutableListOf<Boolean>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      snapshotFlow { coordinator.isContentCovered }.collect { coverage += it }
    }
    val publish = {
      collected.value = fixture.viewModel.uiState.value
      Snapshot.sendApplyNotifications()
      runCurrent()
    }
    assertTrue(coordinator.tryOpenManagement(managementMovimiento().id))
    publish()
    assertEquals(listOf(false, true), coverage)
    val token = assertNotNull(fixture.viewModel.uiState.value.session).token
    assertTrue(fixture.viewModel.onRequestDismiss())
    publish()
    assertEquals(listOf(false, true), coverage)
    fixture.viewModel.onSheetHidden(token)
    publish()
    assertEquals(listOf(false, true, false), coverage)
  }
}
