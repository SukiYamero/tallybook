package com.kurobello.tallybook.feature.transfers.viewmodel

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDate

/** What the form's fields report; the sheet around them adds [TransferActions]. */
@Stable
internal interface TransferFormActions {
  fun onOrigenSelected(id: String)

  fun onDestinoSelected(id: String)

  fun onMontoOrigenChanged(value: String)

  fun onMontoDestinoChanged(value: String)

  fun onFechaChanged(value: LocalDate)

  fun onDescripcionChanged(value: String)
}

@Stable
internal interface TransferActions : TransferFormActions {
  fun onSubmit()

  fun onRequestDelete()

  fun onConfirmDelete()

  fun onCancelDelete()

  /** Returns whether the sheet may hide now; unsaved changes ask first. */
  fun onRequestDismiss(): Boolean

  fun onContinueEditing()

  fun onDiscardChanges()

  fun onBack()

  fun onRetryLoad()

  fun onSheetHidden(origin: Long)
}
