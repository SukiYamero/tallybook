package com.kurobello.tallybook.feature.movimientos.ui

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
import com.kurobello.tallybook.feature.movimientos.viewmodel.ManageMovimientoActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.ManageMovimientoUiState
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFeedback
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFeedbackKind
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

@Composable
internal fun MovimientoFeedbackRoute(
    uiState: ManageMovimientoUiState,
    actions: ManageMovimientoActions,
    modals: PrimaryModalCoordinator,
    bottomBarClearance: Dp,
) {
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val lifecycleState by lifecycle.currentStateFlow.collectAsState()
  val viewport = LocalTallybookViewportOverlayState.current
  val canDeliver = {
    lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && viewport?.isScrimVisible != true
  }
  val isUncovered =
      uiState.session == null && !modals.isContentCovered && viewport?.isScrimVisible != true
  MovimientoFeedbackHost(
      feedbackQueue = uiState.feedbackQueue,
      isActive = lifecycleState.isAtLeast(Lifecycle.State.RESUMED) && isUncovered,
      bottomBarClearance = bottomBarClearance,
      onAction = { token, revision ->
        modals.dispatchFeedback(canDeliver()) { actions.onFeedbackAction(token, revision) }
      },
      onDismiss = { token, revision, _ ->
        modals.dispatchFeedback(canDeliver()) { actions.onFeedbackDismiss(token, revision) }
      },
  )
}

@Composable
private fun MovimientoFeedbackHost(
    feedbackQueue: List<MovimientoFeedback>,
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
private fun MovimientoFeedback.toSnackbarMessage(): TallybookSnackbarMessage {
  val message =
      if (isRestoring) stringResource(Res.string.movimiento_restoring)
      else
          stringResource(
              when (kind) {
                MovimientoFeedbackKind.Saved -> Res.string.movimiento_updated
                MovimientoFeedbackKind.Deleted -> Res.string.movimiento_deleted
                MovimientoFeedbackKind.Missing -> Res.string.movimiento_missing
                MovimientoFeedbackKind.Restored -> Res.string.movimiento_restored
                MovimientoFeedbackKind.RestoreFailed -> Res.string.movimiento_restore_error
                MovimientoFeedbackKind.RestoreConflict -> Res.string.movimiento_restore_conflict
              }
          )
  val actionLabel =
      when (kind) {
        MovimientoFeedbackKind.Deleted -> stringResource(Res.string.movimiento_undo)
        MovimientoFeedbackKind.RestoreFailed -> stringResource(Res.string.retry)
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

private fun MovimientoFeedbackKind.snackbarStatus(): TallybookSnackbarStatus =
    when (this) {
      MovimientoFeedbackKind.Saved,
      MovimientoFeedbackKind.Deleted,
      MovimientoFeedbackKind.Restored -> TallybookSnackbarStatus.Success
      MovimientoFeedbackKind.Missing,
      MovimientoFeedbackKind.RestoreConflict -> TallybookSnackbarStatus.Warning
      MovimientoFeedbackKind.RestoreFailed -> TallybookSnackbarStatus.Error
    }
