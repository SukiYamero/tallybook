package com.kurobello.tallybook.feature.categories.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.ui.component.LastWhileVisible
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookButtonDefaults
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialog
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialogCopy
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButtonDefaults
import com.kurobello.tallybook.core.ui.component.TallybookSheetForm
import com.kurobello.tallybook.core.ui.component.TallybookSheetFormLayout
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
import tallybook.shared.generated.resources.category_hide_instead
import tallybook.shared.generated.resources.category_in_use_message
import tallybook.shared.generated.resources.category_open
import tallybook.shared.generated.resources.category_save_changes
import tallybook.shared.generated.resources.category_save_error
import tallybook.shared.generated.resources.category_visibility_error
import tallybook.shared.generated.resources.continue_editing
import tallybook.shared.generated.resources.discard_changes_confirm
import tallybook.shared.generated.resources.discard_changes_message
import tallybook.shared.generated.resources.discard_changes_title

@Composable
internal fun CategoryEditorBody(editor: CategoryEditor, title: String, actions: CategoriesActions) {
  val enabled = !editor.isBusy && !editor.isClosing
  val duplicate = editor.draft.duplicate
  TallybookSheetForm(
      header = { CategoryEditorHeader(editor, title, enabled, actions) },
      footer = { CategoryEditorFooter(editor, enabled, actions) },
      layout =
          TallybookSheetFormLayout(
              minimumBodyHeight = TallybookTheme.dimensions.editorSheetFormMinimumHeight
          ),
  ) {
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
        revealFocusedInput = { revealWhenFocused() },
    )
    editor.error?.let { CategoryEditorErrorText(it) }
    if (editor.editing != null && editor.count > 0) {
      TallybookTextButton(onClick = actions::onViewMovimientos, enabled = enabled) {
        TallybookText(viewMovimientosLabel(editor.count))
      }
    }
  }
}

@Composable
private fun CategoryEditorHeader(
    editor: CategoryEditor,
    title: String,
    enabled: Boolean,
    actions: CategoriesActions,
) {
  val categoria = editor.editing
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      TallybookSheetTitle(title, modifier = Modifier.weight(1f))
      if (categoria != null) CategoryEditorHeaderActions(categoria, enabled, actions)
    }
    if (categoria?.oculta == true) CategoryHiddenStatus()
  }
}

@Composable
private fun CategoryEditorHeaderActions(
    categoria: Categoria,
    enabled: Boolean,
    actions: CategoriesActions,
) {
  Row(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
    CategoryVisibilityButton(categoria, enabled, actions::onToggleHidden)
    TallybookIconButton(
        onClick = actions::onRequestDelete,
        icon = Lucide.Trash2,
        contentDescription = stringResource(Res.string.category_delete),
        enabled = enabled,
        colors = TallybookIconButtonDefaults.destructiveColors(),
    )
  }
}

@Composable
private fun CategoryEditorFooter(
    editor: CategoryEditor,
    enabled: Boolean,
    actions: CategoriesActions,
) {
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
internal fun CategoryEditorDialogs(editor: CategoryEditor, actions: CategoriesActions) {
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
