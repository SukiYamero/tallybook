package com.kurobello.tallybook.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

@Composable
internal fun rememberHomeRouteState(viewModel: HomeViewModel): State<HomeUiState> {
  val owner = LocalLifecycleOwner.current
  return key(viewModel, owner) {
    produceState(initialValue = viewModel.uiState.value.revalidating()) {
      owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        try {
          viewModel.onResume()
          viewModel.uiState.collect { value = it }
        } finally {
          value = value.revalidating()
        }
      }
    }
  }
}
