package com.kurobello.tallybook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.LocalDataBootstrapper
import com.kurobello.tallybook.core.model.DataOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class AppStartupViewModel(
    private val bootstrapper: LocalDataBootstrapper,
    scope: CoroutineScope? = null,
) : ViewModel(), AppStartupActions {

  private val coroutineScope = scope ?: viewModelScope
  private val initializationMutex = Mutex()

  val uiState: StateFlow<AppStartupUiState>
    field = MutableStateFlow<AppStartupUiState>(AppStartupUiState.Initializing)

  init {
    initialize()
  }

  override fun onRetry() {
    initialize()
  }

  private fun initialize() {
    if (!initializationMutex.tryLock()) return
    uiState.value = AppStartupUiState.Initializing
    coroutineScope.launch {
      try {
        uiState.value =
            when (bootstrapper.initialize()) {
              is DataOutcome.Success -> AppStartupUiState.Ready
              is DataOutcome.Failure -> AppStartupUiState.Error
            }
      } finally {
        initializationMutex.unlock()
      }
    }
  }
}
