package com.kurobello.tallybook.feature.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow

internal class HomeViewModel : ViewModel(), HomeActions {

  val uiState: StateFlow<HomeUiState>
    field = MutableStateFlow(HomeUiState())

  private val effectChannel = Channel<HomeEffect>(Channel.BUFFERED)
  val effects: Flow<HomeEffect> = effectChannel.receiveAsFlow()

  override fun onNavigateNext() {
    effectChannel.trySend(HomeEffect.NavigateNext).getOrThrow()
  }
}
