package com.kurobello.tallybook.feature.categories.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.ui.component.LastWhileVisible
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookButtonDefaults
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialog
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialogCopy
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesActions
import com.kurobello.tallybook.feature.categories.viewmodel.CategoryDeletion
import com.kurobello.tallybook.feature.categories.viewmodel.CategoryEditor
import com.kurobello.tallybook.feature.categories.viewmodel.CategoryEditorError
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.cancel
import tallybook.shared.generated.resources.category_create_action
import tallybook.shared.generated.resources.category_delete
import tallybook.shared.generated.resources.category_delete_error
import tallybook.shared.generated.resources.category_delete_message
import tallybook.shared.generated.resources.category_delete_title
import tallybook.shared.generated.resources.category_edit_title
import tallybook.shared.generated.resources.category_hide
import tallybook.shared.generated.resources.category_hide_instead
import tallybook.shared.generated.resources.category_in_use_message
import tallybook.shared.generated.resources.category_new_title
import tallybook.shared.generated.resources.category_open
import tallybook.shared.generated.resources.category_save_changes
import tallybook.shared.generated.resources.category_save_error
import tallybook.shared.generated.resources.category_show
import tallybook.shared.generated.resources.category_visibility_error
import tallybook.shared.generated.resources.continue_editing
import tallybook.shared.generated.resources.discard_changes_confirm
import tallybook.shared.generated.resources.discard_changes_message
import tallybook.shared.generated.resources.discard_changes_title

@Composable
internal fun CategoryEditorSheet(editor: CategoryEditor?, actions: CategoriesActions) {
  if (editor == null) return
  val title =
      stringResource(
          if (editor.editing == null) Res.string.category_new_title
          else Res.string.category_edit_title
      )
  TallybookModalBottomSheet(
      visible = !editor.isClosing,
      paneTitle = title,
      callbacks =
          ModalBottomSheetCallbacks(
              onDismiss = actions::onRequestCloseEditor,
              onDismissRequest = {
                actions.onRequestCloseEditor()
                false
              },
              onHidden = actions::onEditorHidden,
          ),
  ) {
    CategoryEditorBody(editor, title, actions)
  }
  editor.editing?.let { CategoryDeletionDialogs(it, editor.deletion, actions) }
  TallybookConfirmationDialog(
      visible = editor.confirmingDiscard,
      copy =
          TallybookConfirmationDialogCopy(
              title = stringResource(Res.string.discard_changes_title),
              message = stringResource(Res.string.discard_changes_message),
              confirmLabel = stringResource(Res.string.discard_changes_confirm),
              dismissLabel = stringResource(Res.string.continue_editing),
          ),
      onConfirm = actions::onConfirmDiscard,
      onDismiss = actions::onContinueEditing,
  )
}

@Composable
private fun CategoryEditorBody(editor: CategoryEditor, title: String, actions: CategoriesActions) {
  val enabled = !editor.isBusy && !editor.isClosing
  val duplicate = editor.draft.duplicate
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = TallybookTheme.dimensions.screenHorizontal),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
  ) {
    TallybookSheetTitle(title)
    CategoryForm(
        state = editor.draft,
        actions = actions,
        mode = CategoryFormMode.FULL,
        duplicateAction =
            duplicate?.let {
              CategoryDuplicateAction(
                  stringResource(Res.string.category_open, it.nombre),
                  actions::onOpenDuplicate,
              )
            },
    )
    editor.error?.let { CategoryEditorErrorText(it) }
    editor.editing?.let { CategoryManagementActions(it, enabled, actions) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      TallybookTextButton(
          onClick = actions::onRequestCloseEditor,
          modifier = Modifier.weight(1f),
          enabled = enabled,
      ) {
        TallybookText(stringResource(Res.string.cancel))
      }
      TallybookButton(
          onClick = actions::onSaveEditor,
          modifier = Modifier.weight(1f),
          enabled = enabled && editor.draft.canSave,
      ) {
        TallybookText(
            stringResource(
                if (editor.editing == null) Res.string.category_create_action
                else Res.string.category_save_changes
            )
        )
      }
    }
  }
}

@Composable
private fun CategoryEditorErrorText(error: CategoryEditorError) {
  TallybookText(
      stringResource(
          when (error) {
            CategoryEditorError.SAVE -> Res.string.category_save_error
            CategoryEditorError.VISIBILITY -> Res.string.category_visibility_error
            CategoryEditorError.DELETE -> Res.string.category_delete_error
          }
      ),
      color = TallybookTheme.colors.status.danger.base,
      modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
  )
}

@Composable
private fun CategoryManagementActions(
    categoria: Categoria,
    enabled: Boolean,
    actions: CategoriesActions,
) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    TallybookTextButton(
        onClick = { actions.onToggleHidden(categoria.id) },
        modifier = Modifier.weight(1f),
        enabled = enabled,
    ) {
      TallybookText(
          stringResource(
              if (categoria.oculta) Res.string.category_show else Res.string.category_hide
          )
      )
    }
    TallybookButton(
        onClick = actions::onRequestDelete,
        modifier = Modifier.weight(1f),
        enabled = enabled,
        colors = TallybookButtonDefaults.destructiveColors(),
    ) {
      TallybookText(stringResource(Res.string.category_delete))
    }
  }
}

@Composable
private fun CategoryDeletionDialogs(
    categoria: Categoria,
    deletion: CategoryDeletion?,
    actions: CategoriesActions,
) {
  val title = stringResource(Res.string.category_delete_title, categoria.nombre)
  val cancel = stringResource(Res.string.cancel)
  TallybookConfirmationDialog(
      visible = deletion is CategoryDeletion.Confirm,
      copy =
          TallybookConfirmationDialogCopy(
              title = title,
              message = stringResource(Res.string.category_delete_message),
              confirmLabel = stringResource(Res.string.category_delete),
              dismissLabel = cancel,
          ),
      onConfirm = actions::onConfirmDelete,
      onDismiss = actions::onCancelDelete,
      confirmColors = TallybookButtonDefaults.destructiveColors(),
  )
  val inUse = deletion as? CategoryDeletion.InUse
  val count = remember { LastWhileVisible(0L) }.resolve(inUse != null, inUse?.count ?: 0L)
  TallybookConfirmationDialog(
      visible = inUse != null,
      copy =
          TallybookConfirmationDialogCopy(
              title = title,
              message =
                  pluralStringResource(
                      Res.plurals.category_in_use_message,
                      count.toPluralQuantity(),
                      count,
                  ),
              confirmLabel = stringResource(Res.string.category_hide_instead),
              dismissLabel = cancel,
          ),
      onConfirm = actions::onHideInstead,
      onDismiss = actions::onCancelDelete,
  )
}
