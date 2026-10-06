package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

@Composable
internal fun AddMovimientoSheetSessionLifecycle(sheetSession: AddMovimientoSheetSession) {
  DisposableEffect(sheetSession) { onDispose { sheetSession.dispose() } }
}

internal class AddMovimientoSheetSession {
  var isVisible by mutableStateOf(false)
    private set

  var hasBeenOpened by mutableStateOf(false)
    private set

  private var nextSession by mutableIntStateOf(0)
  private var activeSession by mutableIntStateOf(0)
  private var owner: AddMovimientoViewModelStoreOwner? = null

  val viewModelKey: String
    get() = "add-movimiento-$activeSession"

  val viewModelStoreOwner: ViewModelStoreOwner
    get() = requireNotNull(owner)

  fun open() {
    if (hasBeenOpened) return
    activeSession = nextSession
    owner = AddMovimientoViewModelStoreOwner()
    hasBeenOpened = true
    isVisible = true
  }

  fun dismiss() {
    if (!isVisible) return
    isVisible = false
  }

  val token: Int
    get() = activeSession

  fun onSheetHidden(origin: Int) {
    if (origin != activeSession || isVisible || !hasBeenOpened) return
    release()
    nextSession += 1
  }

  fun dispose() {
    release()
  }

  private fun release() {
    owner?.viewModelStore?.clear()
    owner = null
    isVisible = false
    hasBeenOpened = false
  }
}

private class AddMovimientoViewModelStoreOwner : ViewModelStoreOwner {
  override val viewModelStore = ViewModelStore()
}
