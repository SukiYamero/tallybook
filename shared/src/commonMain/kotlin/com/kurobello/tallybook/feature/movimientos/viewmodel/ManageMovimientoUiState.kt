package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Movimiento

internal enum class MovimientoManagementPhase {
  Loading,
  LoadFailed,
  Missing,
  Detail,
  Edit,
  Closing,
}

internal enum class MovimientoDiscardTarget {
  Detail,
  Close,
}

internal enum class MovimientoManagementError {
  DeleteFailed,
  MissingOnSave,
}

internal data class MovimientoManagementSession(
    val token: Long,
    val movimientoId: String,
    val phase: MovimientoManagementPhase = MovimientoManagementPhase.Loading,
    val movimiento: Movimiento? = null,
    val categorias: List<Categoria> = emptyList(),
    val isCategoriesLoading: Boolean = true,
    val isCategoriesFailed: Boolean = false,
    val edit: EditMovimientoDraftState? = null,
    val isSubmitting: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false,
    val discardTarget: MovimientoDiscardTarget? = null,
    val error: MovimientoManagementError? = null,
) {
  val form: EditMovimientoUiState?
    get() = edit?.let {
      EditMovimientoUiState(
          original = it.original,
          draft = it.draft,
          categorias = categorias,
          maxSelectableDate = it.maxSelectableDate,
          isLoading = isCategoriesLoading,
          isSubmitting = isSubmitting,
          submitAttempted = it.submitAttempted,
          error = if (isCategoriesFailed) MovimientoFormError.CATEGORIES_UNAVAILABLE else it.error,
          validationDate = it.validationDate,
      )
    }

  val hasUnsavedChanges: Boolean
    get() = edit != null && edit.draft != edit.original.toDraft()

  val isVisible: Boolean
    get() = phase != MovimientoManagementPhase.Closing
}

internal data class ManageMovimientoUiState(
    val session: MovimientoManagementSession? = null,
    val feedbackQueue: List<MovimientoFeedback> = emptyList(),
) {
  val feedback: MovimientoFeedback?
    get() = feedbackQueue.firstOrNull()

  val isRestoring: Boolean
    get() = feedback?.isRestoring == true
}
