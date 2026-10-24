package com.kurobello.tallybook.feature.transfers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetLayout
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookButtonDefaults
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialog
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialogCopy
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButtonDefaults
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.core.ui.component.TallybookProgressIndicator
import com.kurobello.tallybook.core.ui.component.TallybookSheetForm
import com.kurobello.tallybook.core.ui.component.TallybookSheetFormLayout
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferActions
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferError
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferPhase
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferSession
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferUiState
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.cancel
import tallybook.shared.generated.resources.continue_editing
import tallybook.shared.generated.resources.discard_changes_confirm
import tallybook.shared.generated.resources.discard_changes_message
import tallybook.shared.generated.resources.discard_changes_title
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.movimiento_close
import tallybook.shared.generated.resources.movimiento_delete
import tallybook.shared.generated.resources.movimiento_save_changes
import tallybook.shared.generated.resources.retry
import tallybook.shared.generated.resources.transfer_create
import tallybook.shared.generated.resources.transfer_delete
import tallybook.shared.generated.resources.transfer_delete_error
import tallybook.shared.generated.resources.transfer_delete_message
import tallybook.shared.generated.resources.transfer_delete_title
import tallybook.shared.generated.resources.transfer_edit_title
import tallybook.shared.generated.resources.transfer_missing_edit
import tallybook.shared.generated.resources.transfer_new_title
import tallybook.shared.generated.resources.transfer_read_error
import tallybook.shared.generated.resources.transfer_save_error
import tallybook.shared.generated.resources.transferencia_missing

private val TRANSFER_CONTENT_DETENTS = listOf("loading", "load-failed", "missing", "edit")

/**
 * The shell's transfer sheet; each session gets fresh composition so no draft leaks into the next.
 */
@Composable
internal fun TransferSheetRoute(uiState: TransferUiState, actions: TransferActions) {
  val session = uiState.session ?: return
  key(session.token) {
    CompositionLocalProvider(LocalSaveableStateRegistry provides null) {
      TransferSheet(session, actions)
    }
  }
}

@Composable
internal fun TransferSheet(session: TransferSession, actions: TransferActions) {
  val origin = session.token
  val title =
      stringResource(
          if (session.isNew) Res.string.transfer_new_title else Res.string.transfer_edit_title
      )
  TallybookModalBottomSheet(
      visible = session.isVisible,
      paneTitle = title,
      callbacks =
          ModalBottomSheetCallbacks(
              onDismiss = { actions.onRequestDismiss() },
              onDismissRequest = actions::onRequestDismiss,
              onHidden = { actions.onSheetHidden(origin) },
              onBack = actions::onBack,
          ),
      layout =
          ModalBottomSheetLayout(
              contentDetentIdentifiers = TRANSFER_CONTENT_DETENTS,
              activeContentDetentIdentifier = session.contentDetent(),
          ),
  ) {
    if (session.form != null) TransferEditContent(session, actions, title)
    else TransferReadContent(session, actions, title)
  }
  TransferConfirmations(session, actions)
}

private fun TransferSession.contentDetent(): String =
    when {
      form != null -> "edit"
      phase == TransferPhase.LoadFailed -> "load-failed"
      phase == TransferPhase.Missing -> "missing"
      else -> "loading"
    }

@Composable
private fun TransferEditContent(session: TransferSession, actions: TransferActions, title: String) {
  val form = requireNotNull(session.form)
  TallybookSheetForm(
      header = { TransferHeader(session, actions, title) },
      footer = { TransferFooter(session, actions) },
      layout =
          TallybookSheetFormLayout(
              minimumBodyHeight = TallybookTheme.dimensions.editorSheetFormMinimumHeight
          ),
  ) {
    TransferForm(
        form = form,
        actions = actions,
        enabled = session.canEdit,
        revealFocusedInput = { revealWhenFocused() },
    )
    TransferErrorText(session.error)
  }
}

@Composable
private fun TransferHeader(session: TransferSession, actions: TransferActions, title: String) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    TallybookSheetTitle(title, modifier = Modifier.weight(1f))
    if (!session.isNew) {
      TallybookIconButton(
          onClick = actions::onRequestDelete,
          icon = Lucide.Trash2,
          contentDescription = stringResource(Res.string.transfer_delete),
          colors = TallybookIconButtonDefaults.destructiveColors(),
          enabled = session.canDelete,
      )
    }
  }
}

@Composable
private fun TransferFooter(session: TransferSession, actions: TransferActions) {
  val isMissing = session.error == TransferError.MissingOnSave
  val submitLabel =
      stringResource(
          if (session.isNew) Res.string.transfer_create else Res.string.movimiento_save_changes
      )
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    TallybookTextButton(
        onClick = { actions.onRequestDismiss() },
        modifier = Modifier.weight(1f),
        enabled = session.canInteract && !session.hasPendingConfirmation,
    ) {
      TallybookText(
          stringResource(if (isMissing) Res.string.movimiento_close else Res.string.cancel)
      )
    }
    TallybookButton(
        onClick = actions::onSubmit,
        modifier = Modifier.weight(1f),
        enabled = session.canSubmit,
    ) {
      Box(contentAlignment = Alignment.Center) {
        if (session.isSubmitting) {
          TallybookProgressIndicator(
              contentDescription = stringResource(Res.string.loading),
              modifier = Modifier.size(TallybookTheme.dimensions.iconLarge),
          )
        } else {
          TallybookText(submitLabel)
        }
      }
    }
  }
}

@Composable
private fun TransferErrorText(error: TransferError?) {
  val message =
      when (error) {
        TransferError.SaveFailed -> stringResource(Res.string.transfer_save_error)
        TransferError.DeleteFailed -> stringResource(Res.string.transfer_delete_error)
        TransferError.MissingOnSave -> stringResource(Res.string.transfer_missing_edit)
        null -> return
      }
  TallybookText(
      message,
      color = TallybookTheme.colors.status.danger.base,
      modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
  )
}

@Composable
private fun TransferReadContent(session: TransferSession, actions: TransferActions, title: String) {
  val enabled = session.phase != TransferPhase.Closing
  Column(
      Modifier.fillMaxWidth().padding(horizontal = TallybookTheme.dimensions.screenHorizontal),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
  ) {
    TallybookSheetTitle(title)
    when (session.phase) {
      TransferPhase.LoadFailed -> {
        TallybookText(stringResource(Res.string.transfer_read_error))
        TallybookButton(onClick = actions::onRetryLoad, enabled = enabled) {
          TallybookText(stringResource(Res.string.retry))
        }
      }
      TransferPhase.Missing -> TallybookText(stringResource(Res.string.transferencia_missing))
      else -> TallybookProgressIndicator(contentDescription = stringResource(Res.string.loading))
    }
    TallybookTextButton(onClick = { actions.onRequestDismiss() }, enabled = enabled) {
      TallybookText(stringResource(Res.string.movimiento_close))
    }
  }
}

@Composable
private fun TransferConfirmations(session: TransferSession, actions: TransferActions) {
  TallybookConfirmationDialog(
      visible = session.isDeleteConfirmationVisible,
      copy =
          TallybookConfirmationDialogCopy(
              title = stringResource(Res.string.transfer_delete_title),
              message = stringResource(Res.string.transfer_delete_message),
              confirmLabel = stringResource(Res.string.movimiento_delete),
              dismissLabel = stringResource(Res.string.cancel),
          ),
      onConfirm = actions::onConfirmDelete,
      onDismiss = actions::onCancelDelete,
      confirmColors = TallybookButtonDefaults.destructiveColors(),
  )
  TallybookConfirmationDialog(
      visible = session.isDiscardConfirmationVisible,
      copy =
          TallybookConfirmationDialogCopy(
              title = stringResource(Res.string.discard_changes_title),
              message = stringResource(Res.string.discard_changes_message),
              confirmLabel = stringResource(Res.string.discard_changes_confirm),
              dismissLabel = stringResource(Res.string.continue_editing),
          ),
      onConfirm = actions::onDiscardChanges,
      onDismiss = actions::onContinueEditing,
  )
}
