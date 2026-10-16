package com.kurobello.tallybook.feature.categories.viewmodel

import androidx.compose.runtime.Stable
import com.kurobello.tallybook.feature.categories.ui.CategoryFormActions

// The screen, its editor sheet and both dialogs share one owner of the open editor session.
@Suppress("TooManyFunctions")
@Stable
internal interface CategoriesActions : CategoryFormActions {
  fun onQueryChanged(query: String)

  fun onClearQuery()

  fun onRetry()

  fun onOpenCategory(id: String)

  fun onOpenEditor(request: CategoryEditorRequest): Boolean

  fun onToggleHidden(id: String)

  fun onSaveEditor()

  fun onRequestCloseEditor()

  fun onConfirmDiscard()

  fun onContinueEditing()

  fun onEditorHidden()

  fun onOpenDuplicate()

  fun onRequestDelete()

  fun onConfirmDelete()

  fun onHideInstead()

  fun onCancelDelete()
}

internal sealed interface CategoriesEffect {
  data class OpenSearch(val id: String) : CategoriesEffect

  /** Scrolls [id]'s row into view; the row's highlight lives in the UI state. */
  data class Reveal(val id: String) : CategoriesEffect

  data class Feedback(val feedback: CategoriesFeedback) : CategoriesEffect
}

internal sealed interface CategoriesFeedback {
  data object Created : CategoriesFeedback

  data object Updated : CategoriesFeedback

  data object Deleted : CategoriesFeedback

  data class Hidden(val nombre: String) : CategoriesFeedback

  data class Shown(val nombre: String) : CategoriesFeedback

  data object VisibilityFailed : CategoriesFeedback
}
