package com.kurobello.tallybook.feature.categories.viewmodel

import androidx.compose.runtime.Stable
import com.kurobello.tallybook.feature.categories.ui.CategoryFormActions

// The screen, its category sheet and the sheet's dialogs share one owner of the open sheet.
@Suppress("TooManyFunctions")
@Stable
internal interface CategoriesActions : CategoryFormActions {
  fun onQueryChanged(query: String)

  fun onClearQuery()

  fun onRetry()

  fun onOpenSheet(request: CategorySheetRequest): Boolean

  fun onToggleHidden(id: String)

  fun onViewMovimientos()

  fun onSaveEditor()

  fun onRequestCloseSheet()

  fun onConfirmDiscard()

  fun onContinueEditing()

  fun onSheetHidden()

  fun onOpenDuplicate()

  fun onRequestDelete()

  fun onConfirmDelete()

  fun onHideInstead()

  fun onCancelDelete()
}

internal sealed interface CategoriesEffect {
  data class OpenSearch(val id: String) : CategoriesEffect

  data class Feedback(val feedback: CategoriesFeedback) : CategoriesEffect
}

internal sealed interface CategoriesFeedback {
  data object Created : CategoriesFeedback

  data object Updated : CategoriesFeedback

  data object Deleted : CategoriesFeedback

  data class Hidden(val nombre: String) : CategoriesFeedback
}
