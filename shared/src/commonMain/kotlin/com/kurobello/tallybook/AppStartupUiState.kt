package com.kurobello.tallybook

sealed interface AppStartupUiState {
  data object Initializing : AppStartupUiState

  data object Ready : AppStartupUiState

  data object Error : AppStartupUiState
}
