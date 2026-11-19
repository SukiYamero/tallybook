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
import com.kurobello.tallybook.core.model.BudgetNotice
import com.kurobello.tallybook.core.model.BudgetState
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
import tallybook.shared.generated.resources.budget_notice_left
import tallybook.shared.generated.resources.budget_notice_merged
import tallybook.shared.generated.resources.budget_notice_over
import tallybook.shared.generated.resources.budget_notice_pace
import tallybook.shared.generated.resources.budget_notice_view
import tallybook.shared.generated.resources.movimiento_deleted
import tallybook.shared.generated.resources.movimiento_missing
import tallybook.shared.generated.resources.movimiento_notification_close
import tallybook.shared.generated.resources.movimiento_restore_conflict
import tallybook.shared.generated.resources.movimiento_restore_error
import tallybook.shared.generated.resources.movimiento_restore_missing_cuenta
import tallybook.shared.generated.resources.movimiento_restored
import tallybook.shared.generated.resources.movimiento_restoring
import tallybook.shared.generated.resources.movimiento_undo
import tallybook.shared.generated.resources.movimiento_updated
import tallybook.shared.generated.resources.recurrencia_confirmed
import tallybook.shared.generated.resources.recurrencia_deleted
import tallybook.shared.generated.resources.recurrencia_skipped
import tallybook.shared.generated.resources.recurrencia_undo_conflict
import tallybook.shared.generated.resources.recurrencia_undo_error
import tallybook.shared.generated.resources.recurrencia_undoing
import tallybook.shared.generated.resources.recurrencia_undone
import tallybook.shared.generated.resources.recurrencia_updated
import tallybook.shared.generated.resources.retry
import tallybook.shared.generated.resources.transferencia_deleted
import tallybook.shared.generated.resources.transferencia_missing
import tallybook.shared.generated.resources.transferencia_restore_conflict
import tallybook.shared.generated.resources.transferencia_restore_error
import tallybook.shared.generated.resources.transferencia_restore_missing_cuenta
import tallybook.shared.generated.resources.transferencia_restored
import tallybook.shared.generated.resources.transferencia_restoring
import tallybook.shared.generated.resources.transferencia_updated

@Composable
internal fun ManagementFeedbackRoute(
    state: ManagementFeedbackState,
    queue: ManagementFeedbackQueue,
    modals: PrimaryModalCoordinator,
    bottomBarClearance: Dp,
    onOpenBudgets: () -> Unit,
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
        modals.dispatchFeedback(canDeliver()) {
          if (queue.onBudgetAction(token, revision)) onOpenBudgets()
          else queue.onFeedbackAction(token, revision)
        }
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

/** What the host shows for one feedback, before any string is resolved. */
internal data class ManagementFeedbackPresentation(
    val message: StringResource?,
    val budget: BudgetNotice?,
    val action: StringResource?,
    val duration: TallybookSnackbarDuration,
    val status: TallybookSnackbarStatus,
)

/**
 * A standalone budget notice is informational, so it stays Short even though it offers "Ver"; a
 * line merged into a save keeps that save's Long window.
 */
internal fun ManagementFeedback.presentation(): ManagementFeedbackPresentation {
  val message =
      if (isRestoring) managementFeedbackRestoring.getValue(subject)
      else managementFeedbackMessages[subject]?.getValue(kind)
  val action =
      if (opensBudgets) Res.string.budget_notice_view
      else managementFeedbackActions[kind]?.takeIf { undo != null }
  val isLong = undo != null || (budget != null && message != null)
  return ManagementFeedbackPresentation(
      message = message,
      budget = budget.takeUnless { isRestoring },
      action = action,
      duration = if (isLong) TallybookSnackbarDuration.Long else TallybookSnackbarDuration.Short,
      status = managementFeedbackStatuses.getValue(kind),
  )
}

@Composable
private fun ManagementFeedback.toSnackbarMessage(): TallybookSnackbarMessage {
  val presentation = presentation()
  val base = presentation.message?.let { stringResource(it) }
  val notice = presentation.budget?.let { budgetNoticeText(it) }
  val message =
      if (base != null && notice != null)
          stringResource(Res.string.budget_notice_merged, base, notice)
      else base ?: notice.orEmpty()
  return TallybookSnackbarMessage(
      token,
      revision,
      message,
      presentation.duration,
      presentation.action?.let { stringResource(it) },
      isRestoring,
      status = presentation.status,
  )
}

/** A PACE notice without its day, or a FINE one, still reads truthfully as what remains. */
@Composable
private fun budgetNoticeText(notice: BudgetNotice): String {
  val paceDay = notice.paceDay
  return when {
    notice.state == BudgetState.OVER ->
        stringResource(Res.string.budget_notice_over, notice.categoriaNombre)
    notice.state == BudgetState.PACE && paceDay != null ->
        stringResource(Res.string.budget_notice_pace, notice.categoriaNombre, paceDay)
    else ->
        stringResource(
            Res.string.budget_notice_left,
            notice.categoriaNombre,
            notice.amount.format(),
        )
  }
}

internal val managementFeedbackActions: Map<ManagementFeedbackKind, StringResource> =
    mapOf(
        ManagementFeedbackKind.Deleted to Res.string.movimiento_undo,
        ManagementFeedbackKind.Confirmed to Res.string.movimiento_undo,
        ManagementFeedbackKind.Skipped to Res.string.movimiento_undo,
        ManagementFeedbackKind.RestoreFailed to Res.string.retry,
    )

internal val managementFeedbackStatuses: Map<ManagementFeedbackKind, TallybookSnackbarStatus> =
    mapOf(
        ManagementFeedbackKind.Saved to TallybookSnackbarStatus.Success,
        ManagementFeedbackKind.Deleted to TallybookSnackbarStatus.Success,
        ManagementFeedbackKind.Confirmed to TallybookSnackbarStatus.Success,
        ManagementFeedbackKind.Skipped to TallybookSnackbarStatus.Success,
        ManagementFeedbackKind.Restored to TallybookSnackbarStatus.Success,
        ManagementFeedbackKind.Missing to TallybookSnackbarStatus.Warning,
        ManagementFeedbackKind.RestoreConflict to TallybookSnackbarStatus.Warning,
        ManagementFeedbackKind.RestoreMissingCuenta to TallybookSnackbarStatus.Warning,
        ManagementFeedbackKind.RestoreFailed to TallybookSnackbarStatus.Error,
        ManagementFeedbackKind.BudgetNotice to TallybookSnackbarStatus.Info,
    )

private val movimientoMessages: Map<ManagementFeedbackKind, StringResource> =
    mapOf(
        ManagementFeedbackKind.Saved to Res.string.movimiento_updated,
        ManagementFeedbackKind.Deleted to Res.string.movimiento_deleted,
        ManagementFeedbackKind.Missing to Res.string.movimiento_missing,
        ManagementFeedbackKind.Restored to Res.string.movimiento_restored,
        ManagementFeedbackKind.RestoreFailed to Res.string.movimiento_restore_error,
        ManagementFeedbackKind.RestoreConflict to Res.string.movimiento_restore_conflict,
        ManagementFeedbackKind.RestoreMissingCuenta to Res.string.movimiento_restore_missing_cuenta,
    )

/** Only the kinds a recurrencia produces: it has no Missing state and no cuenta to lose on Undo. */
private val recurrenciaMessages: Map<ManagementFeedbackKind, StringResource> =
    mapOf(
        ManagementFeedbackKind.Saved to Res.string.recurrencia_updated,
        ManagementFeedbackKind.Deleted to Res.string.recurrencia_deleted,
        ManagementFeedbackKind.Confirmed to Res.string.recurrencia_confirmed,
        ManagementFeedbackKind.Skipped to Res.string.recurrencia_skipped,
        ManagementFeedbackKind.Restored to Res.string.recurrencia_undone,
        ManagementFeedbackKind.RestoreFailed to Res.string.recurrencia_undo_error,
        ManagementFeedbackKind.RestoreConflict to Res.string.recurrencia_undo_conflict,
    )

/** A budget notice has no message of its own, so [ManagementSubject.Presupuesto] has no entry. */
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
                ManagementFeedbackKind.RestoreMissingCuenta to
                    Res.string.transferencia_restore_missing_cuenta,
            ),
        ManagementSubject.Recurrencia to recurrenciaMessages,
    )

/** A budget notice carries no snapshot, so it is never restored. */
internal val managementFeedbackRestoring: Map<ManagementSubject, StringResource> =
    mapOf(
        ManagementSubject.Movimiento to Res.string.movimiento_restoring,
        ManagementSubject.Transferencia to Res.string.transferencia_restoring,
        ManagementSubject.Recurrencia to Res.string.recurrencia_undoing,
    )
