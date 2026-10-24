package com.kurobello.tallybook.feature.accounts.viewmodel

import com.kurobello.tallybook.core.model.CreditEstimate
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.ui.account.CuentaBalance

internal data class AccountsUiState(
    val query: String = "",
    val content: AccountsContent = AccountsContent.Loading,
    val archivedExpanded: Boolean = false,
    val sheet: AccountSheet? = null,
) {
  /** A search reaches archived matches without the user expanding the section first. */
  val showsArchived: Boolean
    get() = archivedExpanded || query.isNotBlank()
}

internal sealed interface AccountsContent {
  data object Loading : AccountsContent

  data object Failed : AccountsContent

  /**
   * [totals] has one total per currency with a live account, archived included, primary first and
   * the rest in [Moneda] order; a search never filters them. [groups] holds the active accounts
   * matching [query] by type, [archived] the archived ones.
   */
  data class Ready(
      val totals: List<Money>,
      val groups: List<AccountGroup>,
      val archived: List<CuentaBalance>,
      val query: String = "",
  ) : AccountsContent {
    val isSearchEmpty: Boolean
      get() = query.isNotEmpty() && groups.isEmpty() && archived.isEmpty()
  }
}

internal data class AccountGroup(val tipo: TipoCuenta, val rows: List<CuentaBalance>)

internal sealed interface AccountSheetRequest {
  data class New(val nombre: String) : AccountSheetRequest

  data class Open(val id: String) : AccountSheetRequest
}

/** An inline problem of the open sheet; the first three belong to a field. */
internal sealed interface AccountSheetError {
  data class Duplicate(val existing: Cuenta) : AccountSheetError

  data object CurrencyLocked : AccountSheetError

  data object TypeLocked : AccountSheetError

  data object ArchiveLastActive : AccountSheetError

  data object DeleteLastActive : AccountSheetError

  data object SaveFailed : AccountSheetError

  data object ArchiveFailed : AccountSheetError

  data object DeleteFailed : AccountSheetError
}

internal sealed interface AccountConfirmation {
  data object Archive : AccountConfirmation

  data object Delete : AccountConfirmation

  /** [count] live movimientos and transferencias keep the account; archiving is offered. */
  data class DeleteInUse(val count: Long) : AccountConfirmation

  data object Discard : AccountConfirmation
}

/**
 * The one open account sheet: a new account when [editing] is null. [editing] and [credit] follow
 * the source while open; [baseline] is what [draft] started from, to tell unsaved changes. A
 * closing sheet keeps the shell modal-owned until it reports it hid; [exit] then opens a transfer
 * or Search.
 */
internal data class AccountSheet(
    val editing: Cuenta?,
    val draft: AccountDraftState,
    val baseline: AccountDraftState = draft,
    val credit: CreditEstimate? = null,
    val pending: Boolean = false,
    val error: AccountSheetError? = null,
    val confirmation: AccountConfirmation? = null,
    val isClosing: Boolean = false,
    val exit: AccountsEffect? = null,
) {
  val hasUnsavedChanges: Boolean
    get() = !draft.sameContentAs(baseline)

  val isEditable: Boolean
    get() = !isClosing && !pending && confirmation == null

  /** A field rejection blocks saving because the same values would be rejected again. */
  val canSave: Boolean
    get() =
        isEditable &&
            draft.isValid &&
            error !is AccountSheetError.Duplicate &&
            error != AccountSheetError.CurrencyLocked &&
            error != AccountSheetError.TypeLocked

  /** Archiving is confirmed directly, or offered when deleting found the account in use. */
  val confirmsArchive: Boolean
    get() =
        confirmation == AccountConfirmation.Archive ||
            confirmation is AccountConfirmation.DeleteInUse

  /** An archived account cannot become a side of a new transfer. */
  val canTransfer: Boolean
    get() = editing?.archivada == false

  val canPayCard: Boolean
    get() = canTransfer && editing?.tipo == TipoCuenta.CREDITO

  companion object {
    fun forNew(nombre: String, tipo: TipoCuenta, moneda: Moneda) =
        AccountSheet(editing = null, draft = AccountDraftState.forNew(nombre, tipo, moneda))

    fun forEdit(cuenta: Cuenta, credit: CreditEstimate?) =
        AccountSheet(editing = cuenta, draft = AccountDraftState.forEdit(cuenta), credit = credit)
  }
}
