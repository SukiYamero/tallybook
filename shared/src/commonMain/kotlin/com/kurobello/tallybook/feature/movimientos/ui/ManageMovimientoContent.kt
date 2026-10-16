package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.ManageMovimientoSheetActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoManagementError
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoManagementPhase
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoManagementSession
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.continue_editing
import tallybook.shared.generated.resources.discard_changes_confirm
import tallybook.shared.generated.resources.discard_changes_message
import tallybook.shared.generated.resources.discard_changes_title
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.movimiento_cancel
import tallybook.shared.generated.resources.movimiento_close
import tallybook.shared.generated.resources.movimiento_delete
import tallybook.shared.generated.resources.movimiento_delete_error
import tallybook.shared.generated.resources.movimiento_delete_message
import tallybook.shared.generated.resources.movimiento_delete_title
import tallybook.shared.generated.resources.movimiento_edit_title
import tallybook.shared.generated.resources.movimiento_missing
import tallybook.shared.generated.resources.movimiento_missing_edit
import tallybook.shared.generated.resources.movimiento_read_error
import tallybook.shared.generated.resources.movimiento_save_changes
import tallybook.shared.generated.resources.retry

private val MANAGEMENT_CONTENT_DETENTS = listOf("loading", "load-failed", "missing", "edit")

@Composable
internal fun ManageMovimientoContent(
    session: MovimientoManagementSession,
    actions: ManageMovimientoSheetActions,
) {
  val origin = session.token
  val title = stringResource(Res.string.movimiento_edit_title)
  val detent = session.contentDetent()
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
              contentDetentIdentifiers = MANAGEMENT_CONTENT_DETENTS,
              activeContentDetentIdentifier = detent,
          ),
  ) {
    ManagementSheetBody(session, actions, title)
  }
  ManagementConfirmations(session, actions)
}

private fun MovimientoManagementSession.contentDetent(): String =
    when {
      edit != null -> "edit"
      phase == MovimientoManagementPhase.LoadFailed -> "load-failed"
      phase == MovimientoManagementPhase.Missing -> "missing"
      else -> "loading"
    }

@Composable
private fun ManagementSheetBody(
    session: MovimientoManagementSession,
    actions: ManageMovimientoSheetActions,
    title: String,
) {
  val interactionEnabled = session.phase != MovimientoManagementPhase.Closing
  if (session.form != null) {
    ManagementEditContent(session, actions, title)
  } else {
    ManagementReadContent(
        session.phase,
        title,
        actions::onRetryLoad,
        { actions.onRequestDismiss() },
        interactionEnabled,
    )
  }
}

@Composable
private fun ManagementEditContent(
    session: MovimientoManagementSession,
    actions: ManageMovimientoSheetActions,
    title: String,
) {
  val interactionEnabled = session.canInteractWithEditor
  val presentation =
      MovimientoFormPresentation(
          title,
          stringResource(Res.string.movimiento_save_changes),
          session.error != MovimientoManagementError.MissingOnSave,
      )
  MovimientoForm(
      uiState = requireNotNull(session.form),
      interactionEnabled = interactionEnabled,
      actions = actions,
      presentation = presentation,
      headerAction = {
        TallybookIconButton(
            onClick = actions::onRequestDelete,
            icon = Lucide.Trash2,
            contentDescription = stringResource(Res.string.movimiento_delete),
            colors = TallybookIconButtonDefaults.destructiveColors(),
            enabled =
                interactionEnabled &&
                    !session.isSubmitting &&
                    session.movimiento != null &&
                    session.error != MovimientoManagementError.MissingOnSave,
        )
      },
      supportingContent = { ManagementEditError(session.error) },
      footerContent = { ManagementEditFooter(session, actions, presentation) },
  )
}

private val MovimientoManagementSession.canInteractWithEditor: Boolean
  get() =
      phase == MovimientoManagementPhase.Edit &&
          !isDeleteConfirmationVisible &&
          discardTarget == null

@Composable
private fun ManagementEditError(error: MovimientoManagementError?) {
  val message =
      when (error) {
        MovimientoManagementError.MissingOnSave ->
            stringResource(Res.string.movimiento_missing_edit)
        MovimientoManagementError.DeleteFailed -> stringResource(Res.string.movimiento_delete_error)
        null -> return
      }
  TallybookText(
      message,
      color = TallybookTheme.colors.status.danger.base,
      modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
  )
}

@Composable
private fun ManagementEditFooter(
    session: MovimientoManagementSession,
    actions: ManageMovimientoSheetActions,
    presentation: MovimientoFormPresentation,
) {
  val isMissing = session.error == MovimientoManagementError.MissingOnSave
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    TallybookTextButton(
        onClick = { if (isMissing) actions.onRequestDismiss() else actions.onCancelEdit() },
        modifier = Modifier.weight(1f),
        enabled = session.canInteractWithEditor && !session.isSubmitting,
    ) {
      TallybookText(
          stringResource(
              if (isMissing) Res.string.movimiento_close else Res.string.movimiento_cancel
          )
      )
    }
    MovimientoFormSubmit(
        uiState = requireNotNull(session.form),
        onSubmit = actions::onSubmit,
        presentation = presentation,
        interactionEnabled = session.canInteractWithEditor,
        modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun ManagementReadContent(
    phase: MovimientoManagementPhase,
    title: String,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    interactionEnabled: Boolean,
) {
  Column(
      Modifier.fillMaxWidth().padding(horizontal = TallybookTheme.dimensions.screenHorizontal),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
  ) {
    TallybookSheetTitle(title)
    when (phase) {
      MovimientoManagementPhase.Loading ->
          TallybookProgressIndicator(contentDescription = stringResource(Res.string.loading))
      MovimientoManagementPhase.LoadFailed -> {
        TallybookText(stringResource(Res.string.movimiento_read_error))
        TallybookButton(onClick = onRetry, enabled = interactionEnabled) {
          TallybookText(stringResource(Res.string.retry))
        }
      }
      else -> TallybookText(stringResource(Res.string.movimiento_missing))
    }
    TallybookTextButton(onClick = onClose, enabled = interactionEnabled) {
      TallybookText(stringResource(Res.string.movimiento_close))
    }
  }
}

@Composable
private fun ManagementConfirmations(
    session: MovimientoManagementSession,
    actions: ManageMovimientoSheetActions,
) {
  TallybookConfirmationDialog(
      visible = session.isDeleteConfirmationVisible,
      copy =
          TallybookConfirmationDialogCopy(
              title = stringResource(Res.string.movimiento_delete_title),
              message = stringResource(Res.string.movimiento_delete_message),
              confirmLabel = stringResource(Res.string.movimiento_delete),
              dismissLabel = stringResource(Res.string.movimiento_cancel),
          ),
      onConfirm = actions::onConfirmDelete,
      onDismiss = actions::onCancelDelete,
      confirmColors = TallybookButtonDefaults.destructiveColors(),
  )
  TallybookConfirmationDialog(
      visible = session.discardTarget != null,
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
