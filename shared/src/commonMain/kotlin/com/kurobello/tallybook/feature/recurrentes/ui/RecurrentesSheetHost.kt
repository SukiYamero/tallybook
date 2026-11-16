package com.kurobello.tallybook.feature.recurrentes.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetLayout
import com.kurobello.tallybook.core.ui.component.TallybookButtonDefaults
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialog
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialogCopy
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.feature.recurrentes.viewmodel.OccurrenceSheet
import com.kurobello.tallybook.feature.recurrentes.viewmodel.RecurrenciaSheet
import com.kurobello.tallybook.feature.recurrentes.viewmodel.RecurrentesActions
import com.kurobello.tallybook.feature.recurrentes.viewmodel.RecurrentesUiState
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.cancel
import tallybook.shared.generated.resources.recurrencia_confirm_title
import tallybook.shared.generated.resources.recurrencia_delete
import tallybook.shared.generated.resources.recurrencia_delete_message
import tallybook.shared.generated.resources.recurrencia_delete_title
import tallybook.shared.generated.resources.recurrencia_edit

private const val OCCURRENCE_DETENT = "occurrence"
private const val RECURRENCIA_DETENT = "recurrencia"
private val SHEET_DETENTS = listOf(OCCURRENCE_DETENT, RECURRENCIA_DETENT)

/**
 * One sheet for whatever is open, so moving from an occurrence to its recurrencia swaps the content
 * in place instead of stacking a second modal.
 */
@Composable
internal fun RecurrentesSheetHost(uiState: RecurrentesUiState, actions: RecurrentesActions) {
  val sheet = uiState.sheet ?: return
  val title =
      stringResource(
          when (sheet) {
            is OccurrenceSheet -> Res.string.recurrencia_confirm_title
            is RecurrenciaSheet -> Res.string.recurrencia_edit
          }
      )
  TallybookModalBottomSheet(
      visible = !sheet.isClosing,
      paneTitle = title,
      callbacks =
          ModalBottomSheetCallbacks(
              onDismiss = actions::onRequestCloseSheet,
              onDismissRequest = {
                actions.onRequestCloseSheet()
                false
              },
              onHidden = actions::onSheetHidden,
          ),
      layout =
          ModalBottomSheetLayout(
              contentDetentIdentifiers = SHEET_DETENTS,
              activeContentDetentIdentifier =
                  if (sheet is OccurrenceSheet) OCCURRENCE_DETENT else RECURRENCIA_DETENT,
          ),
  ) {
    key(sheet::class, sheet.recurrencia.id) {
      when (sheet) {
        is OccurrenceSheet ->
            OccurrenceSheetContent(
                sheet = sheet,
                categoria = uiState.ready?.categoriasById?.get(sheet.recurrencia.categoriaId),
                actions = actions,
            )
        is RecurrenciaSheet ->
            RecurrenciaSheetContent(
                sheet = sheet,
                uiState = uiState,
                title = title,
                actions = actions,
            )
      }
    }
  }
  if (sheet is RecurrenciaSheet) RecurrenciaDeleteDialog(sheet, actions)
}

@Composable
private fun RecurrenciaDeleteDialog(sheet: RecurrenciaSheet, actions: RecurrentesActions) {
  TallybookConfirmationDialog(
      visible = sheet.isDeleteConfirmationVisible,
      copy =
          TallybookConfirmationDialogCopy(
              title = stringResource(Res.string.recurrencia_delete_title),
              message = stringResource(Res.string.recurrencia_delete_message),
              confirmLabel = stringResource(Res.string.recurrencia_delete),
              dismissLabel = stringResource(Res.string.cancel),
          ),
      onConfirm = actions::onConfirmDelete,
      onDismiss = actions::onCancelDelete,
      confirmColors = TallybookButtonDefaults.destructiveColors(),
  )
}
