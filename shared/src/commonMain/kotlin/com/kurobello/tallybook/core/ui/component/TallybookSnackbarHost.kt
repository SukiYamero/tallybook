package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.TriangleAlert
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
    hasWaitingMessages: Boolean = false,
) {
  val scope = rememberCoroutineScope()
  val latestDismiss by rememberUpdatedState(onDismiss)
  val timer =
      remember(scope) {
        TallybookSnackbarTimer(scope) { token, revision ->
          latestDismiss(token, revision, TallybookSnackbarDismissReason.Timeout)
        }
      }
  val compacted = timer.shouldCompact(message, hasWaitingMessages)
  val timeout = message?.let {
    tallybookSnackbarTimeoutMillis(it.duration, LocalAccessibilityManager.current, compacted)
  }
  SideEffect { timer.update(message, isActive, timeout, compacted) }
  DisposableEffect(timer) { onDispose { timer.dispose() } }

  if (message != null && isActive) {
    TallybookSnackbar(
        message = message.message,
        modifier = modifier,
        leadingContent = { SnackbarLeading(message) },
        actionContent =
            if (!message.isBusy && message.actionLabel != null) {
              { SnackbarAction(message, onAction) }
            } else null,
        dismissContent =
            if (timeout == null) {
              { SnackbarClose(message, closeLabel, onDismiss) }
            } else null,
    )
  }
}

private const val STATUS_TINT_ALPHA = 0.12f

@Composable
private fun SnackbarLeading(message: TallybookSnackbarMessage) {
  val status = TallybookTheme.colors.status
  val color =
      when (message.status) {
        TallybookSnackbarStatus.Success -> status.success.base
        TallybookSnackbarStatus.Info -> status.info
        TallybookSnackbarStatus.Warning -> status.warning
        TallybookSnackbarStatus.Error -> status.danger.base
      }
  val icon =
      when (message.status) {
        TallybookSnackbarStatus.Success -> Lucide.Check
        TallybookSnackbarStatus.Info -> Lucide.Info
        TallybookSnackbarStatus.Warning -> Lucide.TriangleAlert
        TallybookSnackbarStatus.Error -> Lucide.CircleAlert
      }
  Box(
      modifier =
          Modifier.size(TallybookTheme.spacing.space32)
              .background(
                  if (message.isBusy) Color.Transparent else color.copy(alpha = STATUS_TINT_ALPHA),
                  RoundedCornerShape(TallybookTheme.radii.radius10),
              ),
      contentAlignment = Alignment.Center,
  ) {
    if (message.isBusy) {
      TallybookProgressIndicator(
          contentDescription = message.message,
          modifier = Modifier.size(TallybookTheme.dimensions.iconMedium),
      )
    } else {
      Image(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(TallybookTheme.dimensions.iconMedium),
          colorFilter = ColorFilter.tint(color),
      )
    }
  }
}

@Composable
private fun SnackbarAction(message: TallybookSnackbarMessage, onAction: (Long, Int) -> Unit) {
  message.actionLabel?.let { label ->
    TallybookTextButton(onClick = { onAction(message.token, message.revision) }) {
      TallybookText(label)
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
