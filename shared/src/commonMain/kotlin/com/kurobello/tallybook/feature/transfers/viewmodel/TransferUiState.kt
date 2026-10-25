package com.kurobello.tallybook.feature.transfers.viewmodel

internal enum class TransferPhase {
  Loading,
  LoadFailed,
  Missing,
  Edit,
  Closing,
}

internal enum class TransferError {
  SaveFailed,
  DeleteFailed,
  MissingOnSave,
}

/**
 * One open transfer sheet. [token] tells this session's callbacks from a previous one's; Closing
 * keeps the last [form] rendered and disabled until the sheet reports it hid.
 */
internal data class TransferSession(
    val token: Long,
    val entry: TransferEntry,
    val phase: TransferPhase = TransferPhase.Loading,
    val form: TransferDraftState? = null,
    val isSubmitting: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false,
    val isDiscardConfirmationVisible: Boolean = false,
    val error: TransferError? = null,
) {
  val isNew: Boolean
    get() = entry is TransferEntry.New

  val isVisible: Boolean
    get() = phase != TransferPhase.Closing

  val canInteract: Boolean
    get() = !isSubmitting && phase != TransferPhase.Closing

  val hasPendingConfirmation: Boolean
    get() = isDeleteConfirmationVisible || isDiscardConfirmationVisible

  val canEdit: Boolean
    get() = phase == TransferPhase.Edit && canInteract && !hasPendingConfirmation

  val canSubmit: Boolean
    get() = canEdit && error != TransferError.MissingOnSave

  val canDelete: Boolean
    get() = canSubmit && !isNew
}

internal data class TransferUiState(val session: TransferSession? = null)
