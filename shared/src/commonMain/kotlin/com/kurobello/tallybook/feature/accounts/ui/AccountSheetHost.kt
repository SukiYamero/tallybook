package com.kurobello.tallybook.feature.accounts.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.Archive
import com.composables.icons.lucide.ArchiveRestore
import com.composables.icons.lucide.ArrowLeftRight
import com.composables.icons.lucide.CreditCard
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.ui.component.LastWhileVisible
import com.kurobello.tallybook.core.ui.component.LocalTallybookContentColor
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookButtonDefaults
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialog
import com.kurobello.tallybook.core.ui.component.TallybookConfirmationDialogCopy
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButtonDefaults
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.core.ui.component.TallybookSheetForm
import com.kurobello.tallybook.core.ui.component.TallybookSheetFormLayout
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountConfirmation
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountSheet
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountSheetError
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountsActions
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.account_archive
import tallybook.shared.generated.resources.account_archive_error
import tallybook.shared.generated.resources.account_archive_instead
import tallybook.shared.generated.resources.account_archive_message
import tallybook.shared.generated.resources.account_archive_title
import tallybook.shared.generated.resources.account_archived_marker
import tallybook.shared.generated.resources.account_create
import tallybook.shared.generated.resources.account_delete
import tallybook.shared.generated.resources.account_delete_error
import tallybook.shared.generated.resources.account_delete_message
import tallybook.shared.generated.resources.account_delete_title
import tallybook.shared.generated.resources.account_in_use_message
import tallybook.shared.generated.resources.account_last_active
import tallybook.shared.generated.resources.account_pay_card
import tallybook.shared.generated.resources.account_save
import tallybook.shared.generated.resources.account_save_error
import tallybook.shared.generated.resources.account_transfer
import tallybook.shared.generated.resources.account_unarchive
import tallybook.shared.generated.resources.accounts_edit
import tallybook.shared.generated.resources.accounts_new
import tallybook.shared.generated.resources.cancel
import tallybook.shared.generated.resources.continue_editing
import tallybook.shared.generated.resources.discard_changes_confirm
import tallybook.shared.generated.resources.discard_changes_message
import tallybook.shared.generated.resources.discard_changes_title

/**
 * [canTransfer] is false until the transfer form exists, so the sheet never offers an action that
 * goes nowhere.
 */
@Composable
internal fun AccountSheetHost(
    sheet: AccountSheet?,
    actions: AccountsActions,
    canTransfer: Boolean,
) {
  if (sheet == null) return
  val title =
      stringResource(
          if (sheet.editing == null) Res.string.accounts_new else Res.string.accounts_edit
      )
  TallybookModalBottomSheet(
      visible = !sheet.isClosing,
      paneTitle = title,
      callbacks =
          ModalBottomSheetCallbacks(
              onDismiss = { actions.onDismissSheet() },
              onDismissRequest = {
                actions.onDismissSheet()
                false
              },
              onHidden = actions::onSheetHidden,
          ),
  ) {
    key(sheet.editing?.id) { AccountSheetBody(sheet, title, actions, canTransfer) }
  }
  AccountSheetDialogs(sheet, actions)
}

@Composable
private fun AccountSheetBody(
    sheet: AccountSheet,
    title: String,
    actions: AccountsActions,
    canTransfer: Boolean,
) {
  val enabled = sheet.isEditable
  TallybookSheetForm(
      header = { AccountSheetHeader(sheet, title, enabled, actions) },
      footer = { AccountSheetFooter(sheet, enabled, actions) },
      layout =
          TallybookSheetFormLayout(
              minimumBodyHeight = TallybookTheme.dimensions.editorSheetFormMinimumHeight
          ),
  ) {
    AccountForm(
        state = sheet.draft,
        onDraftChanged = actions::onDraftChanged,
        enabled = enabled,
        errors = sheet.fieldErrors,
        revealFocusedInput = { revealWhenFocused() },
    )
    sheet.error?.generalMessage?.let { AccountSheetErrorText(stringResource(it)) }
    if (canTransfer && sheet.canTransfer) AccountTransferActions(sheet, enabled, actions)
  }
}

@Composable
private fun AccountSheetHeader(
    sheet: AccountSheet,
    title: String,
    enabled: Boolean,
    actions: AccountsActions,
) {
  val cuenta = sheet.editing
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      TallybookSheetTitle(title, modifier = Modifier.weight(1f))
      if (cuenta != null) AccountHeaderActions(cuenta, enabled, actions)
    }
    if (cuenta?.archivada == true) {
      TallybookText(
          stringResource(Res.string.account_archived_marker),
          color = TallybookTheme.colors.foreground.secondary,
          style = TallybookTheme.typography.bodySmall,
      )
    }
  }
}

@Composable
private fun AccountHeaderActions(cuenta: Cuenta, enabled: Boolean, actions: AccountsActions) {
  Row(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
    TallybookIconButton(
        onClick = { actions.onRequestArchive(!cuenta.archivada) },
        icon = if (cuenta.archivada) Lucide.ArchiveRestore else Lucide.Archive,
        contentDescription =
            stringResource(
                if (cuenta.archivada) Res.string.account_unarchive else Res.string.account_archive
            ),
        enabled = enabled,
    )
    TallybookIconButton(
        onClick = actions::onRequestDelete,
        icon = Lucide.Trash2,
        contentDescription = stringResource(Res.string.account_delete),
        enabled = enabled,
        colors = TallybookIconButtonDefaults.destructiveColors(),
    )
  }
}

@Composable
private fun AccountTransferActions(
    sheet: AccountSheet,
    enabled: Boolean,
    actions: AccountsActions,
) {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    TallybookTextButton(onClick = actions::onTransfer, enabled = enabled) {
      ActionLabel(Lucide.ArrowLeftRight, stringResource(Res.string.account_transfer))
    }
    if (sheet.canPayCard) {
      TallybookTextButton(onClick = actions::onPayCard, enabled = enabled) {
        ActionLabel(Lucide.CreditCard, stringResource(Res.string.account_pay_card))
      }
    }
  }
}

@Composable
private fun ActionLabel(icon: ImageVector, label: String) {
  Image(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier.size(TallybookTheme.dimensions.iconMedium),
      colorFilter = ColorFilter.tint(LocalTallybookContentColor.current),
  )
  TallybookText(label)
}

@Composable
private fun AccountSheetFooter(sheet: AccountSheet, enabled: Boolean, actions: AccountsActions) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    TallybookTextButton(
        onClick = { actions.onDismissSheet() },
        modifier = Modifier.weight(1f),
        enabled = enabled,
    ) {
      TallybookText(stringResource(Res.string.cancel))
    }
    TallybookButton(
        onClick = actions::onSave,
        modifier = Modifier.weight(1f),
        enabled = sheet.canSave,
    ) {
      TallybookText(
          stringResource(
              if (sheet.editing == null) Res.string.account_create else Res.string.account_save
          )
      )
    }
  }
}

@Composable
private fun AccountSheetErrorText(text: String) {
  TallybookText(
      text,
      color = TallybookTheme.colors.status.danger.base,
      modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
  )
}

@Composable
private fun AccountSheetDialogs(sheet: AccountSheet, actions: AccountsActions) {
  sheet.editing?.let { AccountWriteDialogs(it, sheet.confirmation, actions) }
  TallybookConfirmationDialog(
      visible = sheet.confirmation == AccountConfirmation.Discard,
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
private fun AccountWriteDialogs(
    cuenta: Cuenta,
    confirmation: AccountConfirmation?,
    actions: AccountsActions,
) {
  val cancel = stringResource(Res.string.cancel)
  TallybookConfirmationDialog(
      visible = confirmation == AccountConfirmation.Archive,
      copy =
          TallybookConfirmationDialogCopy(
              title = stringResource(Res.string.account_archive_title, cuenta.nombre),
              message = stringResource(Res.string.account_archive_message),
              confirmLabel = stringResource(Res.string.account_archive),
              dismissLabel = cancel,
          ),
      onConfirm = actions::onConfirmArchive,
      onDismiss = actions::onCancelConfirmation,
  )
  val deleteTitle = stringResource(Res.string.account_delete_title, cuenta.nombre)
  TallybookConfirmationDialog(
      visible = confirmation == AccountConfirmation.Delete,
      copy =
          TallybookConfirmationDialogCopy(
              title = deleteTitle,
              message = stringResource(Res.string.account_delete_message),
              confirmLabel = stringResource(Res.string.account_delete),
              dismissLabel = cancel,
          ),
      onConfirm = actions::onConfirmDelete,
      onDismiss = actions::onCancelConfirmation,
      confirmColors = TallybookButtonDefaults.destructiveColors(),
  )
  val inUse = confirmation as? AccountConfirmation.DeleteInUse
  // Keeps the last count while the dialog animates out after the confirmation clears.
  val count = remember { LastWhileVisible(0L) }.resolve(inUse != null, inUse?.count ?: 0L)
  TallybookConfirmationDialog(
      visible = inUse != null,
      copy =
          TallybookConfirmationDialogCopy(
              title = deleteTitle,
              message =
                  pluralStringResource(
                      Res.plurals.account_in_use_message,
                      count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                      count,
                  ),
              confirmLabel = stringResource(Res.string.account_archive_instead),
              dismissLabel = cancel,
          ),
      onConfirm = actions::onConfirmArchive,
      onDismiss = actions::onCancelConfirmation,
  )
}

private val AccountSheet.fieldErrors: AccountFieldErrors
  get() =
      AccountFieldErrors(
          duplicate = (error as? AccountSheetError.Duplicate)?.existing,
          currencyLocked = error == AccountSheetError.CurrencyLocked,
          typeLocked = error == AccountSheetError.TypeLocked,
      )

private val generalMessages: Map<AccountSheetError, StringResource> =
    mapOf(
        AccountSheetError.ArchiveLastActive to Res.string.account_last_active,
        AccountSheetError.DeleteLastActive to Res.string.account_last_active,
        AccountSheetError.SaveFailed to Res.string.account_save_error,
        AccountSheetError.ArchiveFailed to Res.string.account_archive_error,
        AccountSheetError.DeleteFailed to Res.string.account_delete_error,
    )

/** Null for the rejections [AccountForm] shows on their own field. */
private val AccountSheetError.generalMessage: StringResource?
  get() = generalMessages[this]
