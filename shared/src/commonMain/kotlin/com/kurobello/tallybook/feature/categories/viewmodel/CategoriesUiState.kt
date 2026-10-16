package com.kurobello.tallybook.feature.categories.viewmodel

import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoriaDraft
import com.kurobello.tallybook.core.model.CategoriaOrigen

internal data class CategoriesUiState(
    val query: String = "",
    val content: CategoriesContent = CategoriesContent.Loading,
    val editor: CategoryEditor? = null,
    val pendingVisibility: Set<String> = emptySet(),
    val highlightedId: String? = null,
)

internal sealed interface CategoriesContent {
  data object Loading : CategoriesContent

  data object Failed : CategoriesContent

  /** Both sections already match the query; [predefined] keeps each family's catalog order. */
  data class Ready(
      val custom: List<CategoryRow>,
      val predefined: List<List<CategoryRow>>,
  ) : CategoriesContent {
    val isEmpty: Boolean
      get() = custom.isEmpty() && predefined.isEmpty()
  }
}

/** [count] is the primary moneda's movimientos carrying the categoria, children included. */
internal data class CategoryRow(val categoria: Categoria, val count: Long) {
  val isCustom: Boolean
    get() = categoria.origen == CategoriaOrigen.USUARIO

  val canChangeVisibility: Boolean
    get() = categoria.id != CATEGORIA_OTROS_ID

  val isChild: Boolean
    get() = categoria.padreId != null
}

internal sealed interface CategoryEditorRequest {
  data class New(val nombre: String) : CategoryEditorRequest

  data class Edit(val id: String) : CategoryEditorRequest
}

/**
 * The open create/edit sheet. [editing] follows the stored row, so Hide/Show reflects writes made
 * while the sheet is open; [baseline] is what the draft started from, to tell unsaved changes. A
 * closing editor keeps the shell modal-owned until the sheet reports it hid.
 */
internal data class CategoryEditor(
    val editing: Categoria?,
    val draft: CategoryDraftState,
    val baseline: CategoriaDraft,
    val error: CategoryEditorError? = null,
    val deletion: CategoryDeletion? = null,
    val confirmingDiscard: Boolean = false,
    val visibilityPending: Boolean = false,
    val isClosing: Boolean = false,
) {
  val hasUnsavedChanges: Boolean
    get() = draft.toDraft() != baseline

  val isBusy: Boolean
    get() = draft.saving || visibilityPending || deletion?.isBusy == true

  companion object {
    fun of(editing: Categoria?, draft: CategoryDraftState): CategoryEditor =
        CategoryEditor(editing = editing, draft = draft, baseline = draft.toDraft())
  }
}

internal enum class CategoryEditorError {
  SAVE,
  VISIBILITY,
  DELETE,
}

internal sealed interface CategoryDeletion {
  val isBusy: Boolean

  /** Counting movimientos in every moneda before telling the user whether deleting is safe. */
  data object Checking : CategoryDeletion {
    override val isBusy: Boolean = true
  }

  data class Confirm(val deleting: Boolean = false) : CategoryDeletion {
    override val isBusy: Boolean
      get() = deleting
  }

  data class InUse(val count: Long, val hiding: Boolean = false) : CategoryDeletion {
    override val isBusy: Boolean
      get() = hiding
  }
}
