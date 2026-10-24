package com.kurobello.tallybook.feature.accounts.viewmodel

import androidx.compose.runtime.Stable
import com.kurobello.tallybook.core.model.Money

// The screen, its account sheet and the sheet's dialogs share one owner of the open sheet.
@Suppress("TooManyFunctions")
@Stable
internal interface AccountsActions {
  fun onSearchChanged(query: String)

  fun onRetry()

  fun onToggleArchived()

  /** Opens a new account named after the current query. */
  fun onCreate()

  fun onOpenCuenta(id: String)

  fun onDraftChanged(transform: AccountDraftState.() -> AccountDraftState)

  fun onSave()

  /** Archiving asks first; unarchiving writes at once. */
  fun onRequestArchive(archived: Boolean)

  fun onConfirmArchive()

  fun onRequestDelete()

  fun onConfirmDelete()

  fun onCancelConfirmation()

  /** Returns whether the sheet starts closing; unsaved changes ask before discarding instead. */
  fun onDismissSheet(): Boolean

  fun onConfirmDiscard()

  fun onContinueEditing()

  fun onSheetHidden()

  fun onTransfer()

  fun onPayCard()
}

internal sealed interface AccountsEffect {
  /** Either side may be unset; [prefill] is the received amount, e.g. a card's estimated debt. */
  data class Transfer(val origenId: String?, val destinoId: String?, val prefill: Money?) :
      AccountsEffect

  data class Feedback(val feedback: AccountsFeedback) : AccountsEffect
}

internal sealed interface AccountsFeedback {
  data object Created : AccountsFeedback

  data object Updated : AccountsFeedback

  data object Deleted : AccountsFeedback

  data class Archived(val nombre: String) : AccountsFeedback

  data class Unarchived(val nombre: String) : AccountsFeedback
}
