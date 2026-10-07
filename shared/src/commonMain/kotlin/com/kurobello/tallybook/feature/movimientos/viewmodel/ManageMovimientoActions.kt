package com.kurobello.tallybook.feature.movimientos.viewmodel

import androidx.compose.runtime.Stable

@Stable
internal interface ManageMovimientoSheetActions : MovimientoFormActions {
  fun onEdit()

  fun onCancelEdit()

  fun onRequestDelete()

  fun onConfirmDelete()

  fun onCancelDelete()

  fun onRequestDismiss(): Boolean

  fun onBack()

  fun onContinueEditing()

  fun onDiscardChanges()

  fun onSheetHidden(origin: Long)
}

@Stable
internal interface ManageMovimientoActions : ManageMovimientoSheetActions {
  fun open(id: String): Boolean

  fun onFeedbackAction(token: Long, revision: Int)

  fun onFeedbackDismiss(token: Long, revision: Int)
}
