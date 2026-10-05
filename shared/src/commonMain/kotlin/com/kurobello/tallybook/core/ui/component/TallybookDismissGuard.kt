package com.kurobello.tallybook.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

internal enum class DismissalDecision {
  Dismiss,
  RequestConfirmation,
}

internal fun dismissalDecision(hasUnsavedChanges: Boolean): DismissalDecision =
    if (hasUnsavedChanges) DismissalDecision.RequestConfirmation else DismissalDecision.Dismiss

@Composable
internal fun rememberDismissGuard(
    hasUnsavedChanges: Boolean,
    isDismissEnabled: Boolean,
    onDiscardChanges: () -> Unit,
    onDismiss: () -> Unit,
): DismissGuardState {
  val currentOnDiscardChanges by rememberUpdatedState(onDiscardChanges)
  val currentOnDismiss by rememberUpdatedState(onDismiss)
  val state = remember { DismissGuardState() }
  state.update(
      hasUnsavedChanges = hasUnsavedChanges,
      isDismissEnabled = isDismissEnabled,
      onDiscardChanges = currentOnDiscardChanges,
      onDismiss = currentOnDismiss,
  )
  return state
}

internal class DismissGuardState {
  var isConfirmationVisible by mutableStateOf(false)
    private set

  private var hasUnsavedChanges = false
  private var isDismissEnabled = true
  private var onDiscardChanges: () -> Unit = {}
  private var onDismiss: () -> Unit = {}

  fun update(
      hasUnsavedChanges: Boolean,
      isDismissEnabled: Boolean = true,
      onDiscardChanges: () -> Unit,
      onDismiss: () -> Unit,
  ) {
    this.hasUnsavedChanges = hasUnsavedChanges
    this.isDismissEnabled = isDismissEnabled
    this.onDiscardChanges = onDiscardChanges
    this.onDismiss = onDismiss
  }

  fun requestDismiss(): Boolean {
    if (!isDismissEnabled) return false

    return when (dismissalDecision(hasUnsavedChanges)) {
      DismissalDecision.Dismiss -> {
        onDismiss()
        true
      }
      DismissalDecision.RequestConfirmation -> {
        isConfirmationVisible = true
        false
      }
    }
  }

  fun continueEditing() {
    isConfirmationVisible = false
  }

  fun confirmDiscard() {
    isConfirmationVisible = false
    onDiscardChanges()
    onDismiss()
  }
}
