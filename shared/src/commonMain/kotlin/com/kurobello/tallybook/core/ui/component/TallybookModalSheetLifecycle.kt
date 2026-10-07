package com.kurobello.tallybook.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.composeunstyled.ModalBottomSheetState
import com.composeunstyled.SheetDetent

@Composable
internal fun ModalBottomSheetBackHandling(
    visible: Boolean,
    callbacks: State<ModalBottomSheetCallbacks>,
) {
  NavigationBackHandler(
      state = rememberNavigationEventState(NavigationEventInfo.None),
      isBackEnabled = visible && callbacks.value.onBack != null,
      onBackCompleted = { callbacks.value.onBack?.invoke() },
  )
}

@Composable
internal fun ModalBottomSheetVisibilityEffect(
    visible: Boolean,
    state: ModalBottomSheetState,
    expandedDetent: SheetDetent,
    callbacks: State<ModalBottomSheetCallbacks>,
) {
  LaunchedEffect(visible, state.currentDetent, state.isIdle) {
    if (!visible && state.currentDetent == SheetDetent.Hidden && state.isIdle) {
      callbacks.value.onHidden()
    } else if (visible && state.currentDetent == expandedDetent && state.isIdle) {
      callbacks.value.onShown()
    }
  }
}
