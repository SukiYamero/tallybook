package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetLayout
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialog
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialogCopy
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.core.ui.component.TallybookProgressIndicator
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.movimiento.signedMovimientoAmountText
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
import tallybook.shared.generated.resources.movimiento_detail_title
import tallybook.shared.generated.resources.movimiento_edit_title
import tallybook.shared.generated.resources.movimiento_missing
import tallybook.shared.generated.resources.movimiento_missing_edit
import tallybook.shared.generated.resources.movimiento_read_error
import tallybook.shared.generated.resources.movimiento_save_changes
import tallybook.shared.generated.resources.retry
import tallybook.shared.generated.resources.unknown_category

private val MANAGEMENT_CONTENT_DETENTS =
    listOf("loading", "load-failed", "missing", "detail", "edit-compact", "edit-details")

@Composable
internal fun ManageMovimientoContent(
    session: MovimientoManagementSession,
    actions: ManageMovimientoSheetActions,
) {
  val origin = session.token
  val isEditing = session.form != null
  val title =
      stringResource(
          if (isEditing) Res.string.movimiento_edit_title else Res.string.movimiento_detail_title
      )
  var detailsExpanded by
      rememberSaveable(origin, isEditing) {
        mutableStateOf(session.form?.descripcion?.isNotEmpty() == true)
      }
  val detent = session.contentDetent(detailsExpanded)
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
    ManagementSheetBody(session, actions, title, detailsExpanded) { detailsExpanded = it }
  }
  ManagementConfirmations(session, actions)
}

private fun MovimientoManagementSession.contentDetent(detailsExpanded: Boolean): String =
    when {
      edit != null -> if (detailsExpanded) "edit-details" else "edit-compact"
      movimiento != null -> "detail"
      phase == MovimientoManagementPhase.LoadFailed -> "load-failed"
      phase == MovimientoManagementPhase.Missing -> "missing"
      else -> "loading"
    }

@Composable
private fun ManagementSheetBody(
    session: MovimientoManagementSession,
    actions: ManageMovimientoSheetActions,
    title: String,
    detailsExpanded: Boolean,
    onDetailsExpandedChange: (Boolean) -> Unit,
) {
  val interactionEnabled = session.phase != MovimientoManagementPhase.Closing
  when {
    session.form != null ->
        ManagementEditContent(session, actions, title, detailsExpanded, onDetailsExpandedChange)
    session.movimiento != null ->
        MovimientoDetailContent(
            movimiento = session.movimiento,
            categoria = session.categorias.firstOrNull { it.id == session.movimiento.categoriaId },
            isSubmitting = session.isSubmitting,
            interactionEnabled = interactionEnabled,
            error =
                stringResource(Res.string.movimiento_delete_error).takeIf {
                  session.error == MovimientoManagementError.DeleteFailed
                },
            onEdit = actions::onEdit,
            onDelete = actions::onRequestDelete,
        )
    else ->
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
    detailsExpanded: Boolean,
    onDetailsExpandedChange: (Boolean) -> Unit,
) {
  MovimientoForm(
      uiState = requireNotNull(session.form),
      interactionEnabled = session.phase != MovimientoManagementPhase.Closing,
      actions = actions,
      detailsExpanded = detailsExpanded,
      onDetailsExpandedChange = onDetailsExpandedChange,
      presentation =
          MovimientoFormPresentation(
              title,
              stringResource(Res.string.movimiento_save_changes),
              session.error != MovimientoManagementError.MissingOnSave,
          ),
      footerContent = { ManagementEditFooter(session, actions) },
  )
}

@Composable
private fun ManagementEditFooter(
    session: MovimientoManagementSession,
    actions: ManageMovimientoSheetActions,
) {
  val isMissing = session.error == MovimientoManagementError.MissingOnSave
  if (isMissing)
      TallybookText(
          stringResource(Res.string.movimiento_missing_edit),
          color = TallybookTheme.colors.status.danger.base,
      )
  TallybookTextButton(
      onClick = { if (isMissing) actions.onRequestDismiss() else actions.onCancelEdit() },
      modifier = Modifier.fillMaxWidth(),
      enabled = session.phase != MovimientoManagementPhase.Closing && !session.isSubmitting,
  ) {
    TallybookText(
        stringResource(if (isMissing) Res.string.movimiento_close else Res.string.movimiento_cancel)
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
  val movimiento = session.movimiento
  val identity =
      movimiento
          ?.let {
            listOf(
                    signedMovimientoAmountText(it.monto, it.tipo),
                    movimientoDetailDate(it),
                    session.categorias
                        .firstOrNull { categoria -> categoria.id == it.categoriaId }
                        ?.nombre ?: stringResource(Res.string.unknown_category),
                )
                .joinToString(" · ")
          }
          .orEmpty()
  TallybookConfirmationDialog(
      visible = session.isDeleteConfirmationVisible,
      copy =
          TallybookConfirmationDialogCopy(
              title = stringResource(Res.string.movimiento_delete_title),
              message = stringResource(Res.string.movimiento_delete_message, identity),
              confirmLabel = stringResource(Res.string.movimiento_delete),
              dismissLabel = stringResource(Res.string.movimiento_cancel),
          ),
      onConfirm = actions::onConfirmDelete,
      onDismiss = actions::onCancelDelete,
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
