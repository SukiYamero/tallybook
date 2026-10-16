package com.kurobello.tallybook.feature.tags.viewmodel

import androidx.compose.runtime.Stable

// The screen, its tag sheet and the sheet's dialog share one owner of the open sheet.
@Suppress("TooManyFunctions")
@Stable
internal interface TagsActions {
  fun onQueryChanged(query: String)

  fun onRetry()

  fun onOpenSheet(request: TagSheetRequest): Boolean

  fun onNombreChanged(value: String)

  fun onDescripcionChanged(value: String)

  fun onSaveSheet()

  fun onViewMovimientos()

  fun onOpenDuplicate()

  fun onRequestDelete()

  fun onConfirmDelete()

  fun onCancelDelete()

  fun onRequestCloseSheet()

  fun onSheetHidden()
}

internal sealed interface TagsEffect {
  data class OpenSearch(val id: String) : TagsEffect

  data class Feedback(val feedback: TagsFeedback) : TagsEffect
}

internal enum class TagsFeedback {
  CREATED,
  UPDATED,
  DELETED,
}
