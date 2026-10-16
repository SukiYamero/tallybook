package com.kurobello.tallybook.feature.tags.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.kurobello.tallybook.core.model.CategoriaRules
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.ui.component.LastWhileVisible
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookButtonDefaults
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialog
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialogCopy
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessage
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButtonDefaults
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.core.ui.component.TallybookSheetForm
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.component.TallybookTextField
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.tags.viewmodel.TagDeletion
import com.kurobello.tallybook.feature.tags.viewmodel.TagNombreError
import com.kurobello.tallybook.feature.tags.viewmodel.TagSheet
import com.kurobello.tallybook.feature.tags.viewmodel.TagSheetError
import com.kurobello.tallybook.feature.tags.viewmodel.TagsActions
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.cancel
import tallybook.shared.generated.resources.tag_delete
import tallybook.shared.generated.resources.tag_delete_error
import tallybook.shared.generated.resources.tag_delete_message
import tallybook.shared.generated.resources.tag_delete_message_unused
import tallybook.shared.generated.resources.tag_delete_title
import tallybook.shared.generated.resources.tag_duplicate_message
import tallybook.shared.generated.resources.tag_duplicate_open
import tallybook.shared.generated.resources.tag_edit_title
import tallybook.shared.generated.resources.tag_name_blank
import tallybook.shared.generated.resources.tag_name_label
import tallybook.shared.generated.resources.tag_name_placeholder
import tallybook.shared.generated.resources.tag_name_too_long
import tallybook.shared.generated.resources.tag_new_title
import tallybook.shared.generated.resources.tag_save
import tallybook.shared.generated.resources.tag_save_error
import tallybook.shared.generated.resources.tag_view_movimientos_count

/**
 * One sheet for whichever tag is open, so opening a duplicate swaps its content in place instead of
 * stacking a second modal.
 */
@Composable
internal fun TagSheetHost(sheet: TagSheet?, actions: TagsActions) {
  if (sheet == null) return
  val title =
      stringResource(
          if (sheet.editing == null) Res.string.tag_new_title else Res.string.tag_edit_title
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
  ) {
    key(sheet.editing?.id) { TagSheetBody(sheet, title, actions) }
  }
  sheet.editing?.let { TagDeleteDialog(it, sheet.deletion, actions) }
}

@Composable
private fun TagSheetBody(sheet: TagSheet, title: String, actions: TagsActions) {
  val enabled = sheet.isEditable
  TallybookSheetForm(
      header = { TagSheetHeader(sheet, title, enabled, actions) },
      footer = { TagSheetFooter(sheet, enabled, actions) },
  ) {
    val nombreModifier = Modifier.revealWhenFocused()
    Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
      TagNombreField(sheet, enabled, actions, nombreModifier)
      val duplicate = sheet.duplicate
      if (sheet.editing == null && duplicate != null) {
        TallybookTextButton(onClick = actions::onOpenDuplicate, enabled = enabled) {
          TallybookText(stringResource(Res.string.tag_duplicate_open, duplicate.nombre))
        }
      }
    }
    sheet.error?.let { TagSheetErrorText(it) }
    if (sheet.editing != null && sheet.count > 0) {
      TallybookTextButton(onClick = actions::onViewMovimientos, enabled = enabled) {
        TallybookText(
            pluralStringResource(
                Res.plurals.tag_view_movimientos_count,
                sheet.count.toPluralQuantity(),
                sheet.count,
            )
        )
      }
    }
  }
}

@Composable
private fun TagSheetHeader(
    sheet: TagSheet,
    title: String,
    enabled: Boolean,
    actions: TagsActions,
) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    TallybookSheetTitle(title, modifier = Modifier.weight(1f))
    if (sheet.editing != null) {
      TallybookIconButton(
          onClick = actions::onRequestDelete,
          icon = Lucide.Trash2,
          contentDescription = stringResource(Res.string.tag_delete),
          enabled = enabled,
          colors = TallybookIconButtonDefaults.destructiveColors(),
      )
    }
  }
}

@Composable
private fun TagNombreField(
    sheet: TagSheet,
    enabled: Boolean,
    actions: TagsActions,
    modifier: Modifier,
) {
  val focusManager = LocalFocusManager.current
  TallybookTextField(
      value = sheet.nombre,
      onValueChange = actions::onNombreChanged,
      modifier = modifier.fillMaxWidth(),
      label = stringResource(Res.string.tag_name_label),
      placeholder = stringResource(Res.string.tag_name_placeholder),
      message = nombreMessage(sheet),
      enabled = enabled,
      keyboardOptions =
          KeyboardOptions(
              capitalization = KeyboardCapitalization.Sentences,
              imeAction = ImeAction.Done,
          ),
      keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
  )
}

@Composable
private fun nombreMessage(sheet: TagSheet): TallybookFieldMessage {
  val duplicate = sheet.duplicate
  return when {
    sheet.visibleNombreError == TagNombreError.BLANK ->
        TallybookFieldMessage.Error(stringResource(Res.string.tag_name_blank))
    sheet.visibleNombreError == TagNombreError.TOO_LONG ->
        TallybookFieldMessage.Error(
            stringResource(Res.string.tag_name_too_long, CategoriaRules.NOMBRE_MAX)
        )
    duplicate != null ->
        TallybookFieldMessage.Error(
            stringResource(Res.string.tag_duplicate_message, duplicate.nombre)
        )
    else -> TallybookFieldMessage.Supporting("${sheet.nombre.length}/${CategoriaRules.NOMBRE_MAX}")
  }
}

@Composable
private fun TagSheetFooter(sheet: TagSheet, enabled: Boolean, actions: TagsActions) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    TallybookTextButton(
        onClick = actions::onRequestCloseSheet,
        modifier = Modifier.weight(1f),
        enabled = enabled,
    ) {
      TallybookText(stringResource(Res.string.cancel))
    }
    TallybookButton(
        onClick = actions::onSaveSheet,
        modifier = Modifier.weight(1f),
        enabled = sheet.canSave,
    ) {
      TallybookText(stringResource(Res.string.tag_save))
    }
  }
}

@Composable
private fun TagSheetErrorText(error: TagSheetError) {
  TallybookText(
      stringResource(
          when (error) {
            TagSheetError.SAVE -> Res.string.tag_save_error
            TagSheetError.DELETE -> Res.string.tag_delete_error
          }
      ),
      color = TallybookTheme.colors.status.danger.base,
      modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
  )
}

@Composable
private fun TagDeleteDialog(etiqueta: Etiqueta, deletion: TagDeletion?, actions: TagsActions) {
  val confirm = deletion as? TagDeletion.Confirm
  // Keeps the last count while the dialog animates out after the deletion state clears.
  val count = remember { LastWhileVisible(0L) }.resolve(confirm != null, confirm?.count ?: 0L)
  TallybookConfirmationDialog(
      visible = confirm != null,
      copy =
          TallybookConfirmationDialogCopy(
              title = stringResource(Res.string.tag_delete_title, etiqueta.nombre),
              message =
                  if (count == 0L) stringResource(Res.string.tag_delete_message_unused)
                  else
                      pluralStringResource(
                          Res.plurals.tag_delete_message,
                          count.toPluralQuantity(),
                          count,
                      ),
              confirmLabel = stringResource(Res.string.tag_delete),
              dismissLabel = stringResource(Res.string.cancel),
          ),
      onConfirm = actions::onConfirmDelete,
      onDismiss = actions::onCancelDelete,
      confirmColors = TallybookButtonDefaults.destructiveColors(),
  )
}

/** Plural selection takes an Int; counts beyond it all read as "other". */
private fun Long.toPluralQuantity(): Int = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
