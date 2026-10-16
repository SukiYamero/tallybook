package com.kurobello.tallybook.feature.categories.viewmodel

import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoriaDraft
import com.kurobello.tallybook.core.model.CategoriaOrigen

internal data class CategoriesUiState(
    val query: String = "",
    val content: CategoriesContent = CategoriesContent.Loading,
    val sheet: CategorySheet? = null,
) {
  val editor: CategoryEditor?
    get() = sheet as? CategoryEditor

  val details: CategoryDetails?
    get() = sheet as? CategoryDetails
}

internal sealed interface CategoriesContent {
  data object Loading : CategoriesContent

  data object Failed : CategoriesContent

  /** No query: the custom block sorted by name, then each predefined family in catalog order. */
  data class Browse(
      val custom: List<CategoryRow>,
      val families: List<List<CategoryRow>>,
  ) : CategoriesContent

  /** Every category matching [query], as one flat block in browsing order. */
  data class Search(val query: String, val matches: List<CategoryRow>) : CategoriesContent
}

/** [count] is the primary moneda's movimientos carrying the categoria, children included. */
internal data class CategoryRow(val categoria: Categoria, val count: Long)

internal val Categoria.isCustom: Boolean
  get() = origen == CategoriaOrigen.USUARIO

internal val Categoria.canChangeVisibility: Boolean
  get() = id != CATEGORIA_OTROS_ID

internal sealed interface CategorySheetRequest {
  data class New(val nombre: String) : CategorySheetRequest

  /** A custom categoria opens its editor; a predefined one its read-only details. */
  data class Open(val id: String) : CategorySheetRequest
}

/**
 * The one open category sheet. Its stored categoria and [count] follow the source, so writes made
 * while it is open show at once. A closing sheet keeps the shell modal-owned until it reports it
 * hid; [opensSearch] then shows the categoria's movimientos in Search.
 */
internal sealed interface CategorySheet {
  val count: Long
  val isClosing: Boolean
  val opensSearch: Boolean
}

/**
 * The create/edit sheet of a custom categoria; [baseline] is what the draft started from, to tell
 * unsaved changes.
 */
internal data class CategoryEditor(
    val editing: Categoria?,
    val draft: CategoryDraftState,
    val baseline: CategoriaDraft,
    override val count: Long = 0L,
    val error: CategoryEditorError? = null,
    val deletion: CategoryDeletion? = null,
    val confirmingDiscard: Boolean = false,
    val visibilityPending: Boolean = false,
    override val isClosing: Boolean = false,
    override val opensSearch: Boolean = false,
) : CategorySheet {
  val hasUnsavedChanges: Boolean
    get() = draft.toDraft() != baseline

  val isBusy: Boolean
    get() = draft.saving || visibilityPending || deletion?.isBusy == true

  companion object {
    fun of(editing: Categoria?, draft: CategoryDraftState, count: Long = 0L): CategoryEditor =
        CategoryEditor(editing = editing, draft = draft, baseline = draft.toDraft(), count = count)
  }
}

/** The read-only sheet of a predefined categoria, whose only write is hiding or showing it. */
internal data class CategoryDetails(
    val categoria: Categoria,
    override val count: Long,
    val visibilityPending: Boolean = false,
    val visibilityFailed: Boolean = false,
    override val isClosing: Boolean = false,
    override val opensSearch: Boolean = false,
) : CategorySheet

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
