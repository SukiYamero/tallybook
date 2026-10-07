package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Composable
@Suppress("LongParameterList")
internal fun TallybookSnackbarHost(
    message: TallybookSnackbarMessage?,
    isActive: Boolean,
    closeLabel: String,
    onAction: (Long, Int) -> Unit,
    onDismiss: (Long, Int, TallybookSnackbarDismissReason) -> Unit,
    modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()
  val latestDismiss by rememberUpdatedState(onDismiss)
  val timer =
      remember(scope) {
        TallybookSnackbarTimer(scope) { token, revision ->
          latestDismiss(token, revision, TallybookSnackbarDismissReason.Timeout)
        }
      }
  val timeout = message?.let {
    tallybookSnackbarTimeoutMillis(it.duration, LocalAccessibilityManager.current)
  }
  SideEffect { timer.update(message, isActive, timeout) }
  DisposableEffect(timer) { onDispose { timer.dispose() } }

  if (message != null && isActive) {
    TallybookSnackbar(
        message = message.message,
        modifier = modifier,
        actionContent =
            if (message.isBusy || message.actionLabel != null) {
              { SnackbarAction(message, onAction) }
            } else null,
        dismissContent = { SnackbarClose(message, closeLabel, onDismiss) },
    )
  }
}

@Composable
private fun SnackbarAction(message: TallybookSnackbarMessage, onAction: (Long, Int) -> Unit) {
  if (message.isBusy) {
    Box(
        modifier =
            Modifier.sizeIn(
                minWidth = TallybookTheme.dimensions.minimumTouchTarget,
                minHeight = TallybookTheme.dimensions.minimumTouchTarget,
            ),
        contentAlignment = Alignment.Center,
    ) {
      TallybookProgressIndicator(contentDescription = message.message)
    }
  } else {
    message.actionLabel?.let { label ->
      TallybookTextButton(onClick = { onAction(message.token, message.revision) }) {
        TallybookText(label)
      }
    }
  }
}

@Composable
private fun SnackbarClose(
    message: TallybookSnackbarMessage,
    closeLabel: String,
    onDismiss: (Long, Int, TallybookSnackbarDismissReason) -> Unit,
) {
  TallybookIconButton(
      onClick = {
        onDismiss(message.token, message.revision, TallybookSnackbarDismissReason.Close)
      },
      icon = Lucide.X,
      contentDescription = closeLabel,
      modifier =
          if (message.isBusy) {
            Modifier.clearAndSetSemantics {
              contentDescription = closeLabel
              role = Role.Button
              disabled()
            }
          } else Modifier,
      enabled = !message.isBusy,
  )
}
