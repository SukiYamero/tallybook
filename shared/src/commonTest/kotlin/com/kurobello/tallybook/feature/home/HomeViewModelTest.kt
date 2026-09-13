package com.kurobello.tallybook.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class HomeViewModelTest {

  @Test
  fun initialStateExposesTheHomeMessage() {
    val viewModel = HomeViewModel()

    assertEquals(HomeUiState(message = "Tallybook"), viewModel.uiState.value)
  }

  @Test
  fun navigateNextBuffersOneNavigationEffectUntilCollected() = runTest {
    val viewModel = HomeViewModel()

    viewModel.onNavigateNext()

    assertEquals(HomeEffect.NavigateNext, viewModel.effects.first())
  }
}
