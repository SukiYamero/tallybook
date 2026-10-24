package com.kurobello.tallybook.core.ui.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kurobello.tallybook.core.ui.component.LocalTallybookViewportOverlayState
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarDismissReason
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarDuration
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarHost
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarMessage
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarStatus
import com.kurobello.tallybook.core.ui.navigation.PrimaryModalCoordinator
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.movimiento_deleted
import tallybook.shared.generated.resources.movimiento_missing
import tallybook.shared.generated.resources.movimiento_notification_close
import tallybook.shared.generated.resources.movimiento_restore_conflict
import tallybook.shared.generated.resources.movimiento_restore_error
import tallybook.shared.generated.resources.movimiento_restored
import tallybook.shared.generated.resources.movimiento_restoring
import tallybook.shared.generated.resources.movimiento_undo
import tallybook.shared.generated.resources.movimiento_updated
import tallybook.shared.generated.resources.retry
import tallybook.shared.generated.resources.transferencia_deleted
import tallybook.shared.generated.resources.transferencia_missing
import tallybook.shared.generated.resources.transferencia_restore_conflict
import tallybook.shared.generated.resources.transferencia_restore_error
import tallybook.shared.generated.resources.transferencia_restored
import tallybook.shared.generated.resources.transferencia_restoring
import tallybook.shared.generated.resources.transferencia_updated

@Composable
internal fun ManagementFeedbackRoute(
    state: ManagementFeedbackState,
    queue: ManagementFeedbackQueue,
    modals: PrimaryModalCoordinator,
    bottomBarClearance: Dp,
) {
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val lifecycleState by lifecycle.currentStateFlow.collectAsState()
  val viewport = LocalTallybookViewportOverlayState.current
  val canDeliver = {
    lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && viewport?.isScrimVisible != true
  }
  // The queue's own admission count covers the frame before a recreated controller re-attaches.
  val isUncovered =
      state.openSessions == 0 && !modals.isContentCovered && viewport?.isScrimVisible != true
  ManagementFeedbackHost(
      feedbackQueue = state.feedbackQueue,
      isActive = lifecycleState.isAtLeast(Lifecycle.State.RESUMED) && isUncovered,
      bottomBarClearance = bottomBarClearance,
      onAction = { token, revision ->
        modals.dispatchFeedback(canDeliver()) { queue.onFeedbackAction(token, revision) }
      },
      onDismiss = { token, revision, _ ->
        modals.dispatchFeedback(canDeliver()) { queue.onFeedbackDismiss(token, revision) }
      },
  )
}

@Composable
private fun ManagementFeedbackHost(
    feedbackQueue: List<ManagementFeedback>,
    isActive: Boolean,
    bottomBarClearance: Dp,
    onAction: (Long, Int) -> Unit,
    onDismiss: (Long, Int, TallybookSnackbarDismissReason) -> Boolean,
) {
  val density = LocalDensity.current
  val direction = LocalLayoutDirection.current
  val insets = WindowInsets.safeDrawing
  val start =
      with(density) {
        (if (direction == LayoutDirection.Ltr) insets.getLeft(density, direction)
            else insets.getRight(density, direction))
            .toDp()
      }
  val end =
      with(density) {
        (if (direction == LayoutDirection.Ltr) insets.getRight(density, direction)
            else insets.getLeft(density, direction))
            .toDp()
      }
  val bottom =
      with(density) { maxOf(insets.getBottom(density), WindowInsets.ime.getBottom(density)).toDp() }
  Box(
      Modifier.fillMaxSize()
          .padding(
              start = start + TallybookTheme.dimensions.screenHorizontal,
              end = end + TallybookTheme.dimensions.screenHorizontal,
              bottom = maxOf(bottomBarClearance, bottom) + TallybookTheme.spacing.space16,
          )
  ) {
    TallybookSnackbarHost(
        message = feedbackQueue.firstOrNull()?.toSnackbarMessage(),
        isActive = isActive,
        closeLabel = stringResource(Res.string.movimiento_notification_close),
        onAction = onAction,
        onDismiss = onDismiss,
        hasWaitingMessages = feedbackQueue.size > 1,
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .widthIn(max = TallybookTheme.dimensions.screenContentMaxWidth)
                .fillMaxWidth(),
    )
  }
}

@Composable
private fun ManagementFeedback.toSnackbarMessage(): TallybookSnackbarMessage {
  val message =
      stringResource(
          if (isRestoring) managementFeedbackRestoring.getValue(subject)
          else managementFeedbackMessages.getValue(subject).getValue(kind)
      )
  val actionLabel =
      when {
        !hasAction -> null
        kind == ManagementFeedbackKind.Deleted -> stringResource(Res.string.movimiento_undo)
        kind == ManagementFeedbackKind.RestoreFailed -> stringResource(Res.string.retry)
        else -> null
      }
  return TallybookSnackbarMessage(
      token,
      revision,
      message,
      if (hasAction) TallybookSnackbarDuration.Long else TallybookSnackbarDuration.Short,
      actionLabel,
      isRestoring,
      status = kind.snackbarStatus(),
  )
}

private fun ManagementFeedbackKind.snackbarStatus(): TallybookSnackbarStatus =
    when (this) {
      ManagementFeedbackKind.Saved,
      ManagementFeedbackKind.Deleted,
      ManagementFeedbackKind.Restored -> TallybookSnackbarStatus.Success
      ManagementFeedbackKind.Missing,
      ManagementFeedbackKind.RestoreConflict -> TallybookSnackbarStatus.Warning
      ManagementFeedbackKind.RestoreFailed -> TallybookSnackbarStatus.Error
    }

private val movimientoMessages: Map<ManagementFeedbackKind, StringResource> =
    mapOf(
        ManagementFeedbackKind.Saved to Res.string.movimiento_updated,
        ManagementFeedbackKind.Deleted to Res.string.movimiento_deleted,
        ManagementFeedbackKind.Missing to Res.string.movimiento_missing,
        ManagementFeedbackKind.Restored to Res.string.movimiento_restored,
        ManagementFeedbackKind.RestoreFailed to Res.string.movimiento_restore_error,
        ManagementFeedbackKind.RestoreConflict to Res.string.movimiento_restore_conflict,
    )

internal val managementFeedbackMessages:
    Map<ManagementSubject, Map<ManagementFeedbackKind, StringResource>> =
    mapOf(
        ManagementSubject.Movimiento to movimientoMessages,
        ManagementSubject.Transferencia to
            mapOf(
                ManagementFeedbackKind.Saved to Res.string.transferencia_updated,
                ManagementFeedbackKind.Deleted to Res.string.transferencia_deleted,
                ManagementFeedbackKind.Missing to Res.string.transferencia_missing,
                ManagementFeedbackKind.Restored to Res.string.transferencia_restored,
                ManagementFeedbackKind.RestoreFailed to Res.string.transferencia_restore_error,
                ManagementFeedbackKind.RestoreConflict to Res.string.transferencia_restore_conflict,
            ),
    )

internal val managementFeedbackRestoring: Map<ManagementSubject, StringResource> =
    mapOf(
        ManagementSubject.Movimiento to Res.string.movimiento_restoring,
        ManagementSubject.Transferencia to Res.string.transferencia_restoring,
    )
