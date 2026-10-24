package com.kurobello.tallybook.feature.accounts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CivilToday
import com.kurobello.tallybook.core.data.CuentaRepository
import com.kurobello.tallybook.core.data.SaldoRepository
import com.kurobello.tallybook.core.model.CreditEstimate
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaArchive
import com.kurobello.tallybook.core.model.CuentaDelete
import com.kurobello.tallybook.core.model.CuentaDraft
import com.kurobello.tallybook.core.model.CuentaSaldo
import com.kurobello.tallybook.core.model.CuentaWrite
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.fold
import com.kurobello.tallybook.core.model.map
import com.kurobello.tallybook.core.model.primaryMoneda
import com.kurobello.tallybook.core.text.matchesSearchText
import com.kurobello.tallybook.core.text.normalizeSearchText
import com.kurobello.tallybook.core.ui.account.CuentaBalance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
internal class AccountsViewModel(
    private val cuentas: CuentaRepository,
    private val saldos: SaldoRepository,
    private val preferences: AppPreferencesRepository,
    private val today: CivilToday,
    scope: CoroutineScope? = null,
) : ViewModel(), AccountsActions {

  private val coroutineScope = scope ?: viewModelScope
  private val retryRequests = MutableStateFlow(0L)
  // The cycle estimate depends on the day, which a resumed screen must re-read.
  private val currentDay = MutableStateFlow(today.date())
  private var source: AccountsSource = AccountsSource.Loading
  private val effectChannel = Channel<AccountsEffect>(Channel.BUFFERED)

  val uiState: StateFlow<AccountsUiState>
    field = MutableStateFlow(AccountsUiState())

  val effects: Flow<AccountsEffect> = effectChannel.receiveAsFlow()

  init {
    coroutineScope.launch {
      retryRequests
          .flatMapLatest { observeSource().onStart { emit(AccountsSource.Loading) } }
          .collect { loaded ->
            source = loaded
            uiState.update { it.withSource(loaded) }
          }
    }
  }

  fun onResume() {
    currentDay.value = today.date()
  }

  /** The shell's modal admission calls this; only one sheet opens at a time. */
  fun openSheet(request: AccountSheetRequest): Boolean {
    val loaded = source as? AccountsSource.Ready
    val sheet =
        if (loaded == null || uiState.value.sheet != null) null
        else
            when (request) {
              is AccountSheetRequest.New ->
                  AccountSheet.forNew(request.nombre, TipoCuenta.BANCO, loaded.primary)
              is AccountSheetRequest.Open ->
                  loaded.find(request.id)?.let { AccountSheet.forEdit(it, loaded.credit[it.id]) }
            }
    if (sheet != null) uiState.update { it.copy(sheet = sheet) }
    return sheet != null
  }

  override fun onSearchChanged(query: String) {
    uiState.update { it.copy(query = query, content = source.content(query)) }
  }

  override fun onRetry() {
    if (source is AccountsSource.Failed) retryRequests.update { it + 1 }
  }

  override fun onToggleArchived() {
    uiState.update { it.copy(archivedExpanded = !it.archivedExpanded) }
  }

  override fun onCreate() {
    openSheet(AccountSheetRequest.New(uiState.value.query))
  }

  override fun onOpenCuenta(id: String) {
    openSheet(AccountSheetRequest.Open(id))
  }

  override fun onDraftChanged(transform: AccountDraftState.() -> AccountDraftState) = updateSheet {
    if (it.isEditable) it.copy(draft = it.draft.transform(), error = null) else it
  }

  override fun onSave() {
    val sheet = uiState.value.sheet?.takeIf { it.canSave } ?: return
    val draft = sheet.draft.toDraft() ?: return
    if (sheet.editing != null && !sheet.hasUnsavedChanges) {
      updateSheet { it.copy(isClosing = true) }
    } else {
      write(sheet.editing, draft)
    }
  }

  private fun write(editing: Cuenta?, draft: CuentaDraft) {
    updateSheet { it.copy(pending = true, error = null) }
    coroutineScope.launch {
      val written =
          if (editing == null) cuentas.create(draft) else cuentas.update(editing.id, draft)
      val outcome = (written as? DataOutcome.Success)?.data
      updateSheet {
        when (outcome) {
          null -> it.copy(pending = false, error = AccountSheetError.SaveFailed)
          is CuentaWrite.Saved -> it.copy(pending = false, isClosing = true)
          is CuentaWrite.Duplicate ->
              it.copy(pending = false, error = AccountSheetError.Duplicate(outcome.existing))
          CuentaWrite.CurrencyLocked ->
              it.copy(pending = false, error = AccountSheetError.CurrencyLocked)
          CuentaWrite.TypeLocked -> it.copy(pending = false, error = AccountSheetError.TypeLocked)
        }
      }
      if (outcome is CuentaWrite.Saved) {
        feedback(if (editing == null) AccountsFeedback.Created else AccountsFeedback.Updated)
      }
    }
  }

  override fun onRequestArchive(archived: Boolean) {
    val cuenta = uiState.value.sheet?.takeIf { it.isEditable }?.editing ?: return
    if (cuenta.archivada == archived) return
    if (archived) {
      updateSheet { it.copy(confirmation = AccountConfirmation.Archive, error = null) }
    } else {
      writeArchive(cuenta, archived = false)
    }
  }

  override fun onConfirmArchive() {
    val cuenta = uiState.value.sheet?.takeIf { it.confirmsArchive && !it.pending }?.editing
    if (cuenta != null) writeArchive(cuenta, archived = true)
  }

  override fun onRequestDelete() = updateSheet {
    if (it.isEditable && it.editing != null) {
      it.copy(confirmation = AccountConfirmation.Delete, error = null)
    } else {
      it
    }
  }

  override fun onConfirmDelete() {
    val cuenta =
        uiState.value.sheet
            ?.takeIf { it.confirmation == AccountConfirmation.Delete && !it.pending }
            ?.editing ?: return
    updateSheet { it.copy(pending = true) }
    coroutineScope.launch {
      val deleted = (cuentas.delete(cuenta.id) as? DataOutcome.Success)?.data
      updateSheet {
        val done = it.copy(pending = false, confirmation = null)
        when (deleted) {
          null -> done.copy(error = AccountSheetError.DeleteFailed)
          CuentaDelete.Deleted -> done.copy(isClosing = true)
          CuentaDelete.LastActive -> done.copy(error = AccountSheetError.DeleteLastActive)
          is CuentaDelete.InUse ->
              done.copy(confirmation = AccountConfirmation.DeleteInUse(deleted.count))
        }
      }
      if (deleted == CuentaDelete.Deleted) feedback(AccountsFeedback.Deleted)
    }
  }

  override fun onCancelConfirmation() = updateSheet {
    if (it.pending || it.confirmation == null) it else it.copy(confirmation = null)
  }

  override fun onDismissSheet(): Boolean {
    val sheet = uiState.value.sheet?.takeIf { it.isEditable } ?: return false
    return if (sheet.hasUnsavedChanges) {
      updateSheet { it.copy(confirmation = AccountConfirmation.Discard) }
      false
    } else {
      updateSheet { it.copy(isClosing = true) }
      true
    }
  }

  override fun onConfirmDiscard() = updateSheet {
    if (it.confirmation == AccountConfirmation.Discard) {
      it.copy(confirmation = null, isClosing = true)
    } else {
      it
    }
  }

  override fun onContinueEditing() = updateSheet {
    if (it.confirmation == AccountConfirmation.Discard) it.copy(confirmation = null, exit = null)
    else it
  }

  override fun onSheetHidden() {
    val sheet = uiState.value.sheet?.takeIf { it.isClosing } ?: return
    uiState.update { it.copy(sheet = null) }
    sheet.exit?.let(effectChannel::trySend)
  }

  override fun onTransfer() {
    val sheet = uiState.value.sheet?.takeIf { it.isEditable && it.canTransfer } ?: return
    leaveFor(AccountsEffect.Transfer(origenId = sheet.editing?.id, destinoId = null, null))
  }

  override fun onPayCard() {
    val sheet = uiState.value.sheet?.takeIf { it.isEditable && it.canPayCard } ?: return
    val deuda = sheet.credit?.deuda?.takeIf { it.minorUnits > 0 }
    leaveFor(AccountsEffect.Transfer(origenId = null, destinoId = sheet.editing?.id, deuda))
  }

  override fun onViewMovements() {
    val cuenta = uiState.value.sheet?.takeIf { it.isEditable }?.editing ?: return
    leaveFor(AccountsEffect.ViewMovements(cuenta.id, cuenta.moneda))
  }

  private fun leaveFor(exit: AccountsEffect) = updateSheet {
    if (it.hasUnsavedChanges) it.copy(confirmation = AccountConfirmation.Discard, exit = exit)
    else it.copy(isClosing = true, exit = exit)
  }

  private fun writeArchive(cuenta: Cuenta, archived: Boolean) {
    updateSheet { it.copy(pending = true, error = null) }
    coroutineScope.launch {
      val written = (cuentas.setArchivada(cuenta.id, archived) as? DataOutcome.Success)?.data
      updateSheet {
        val done = it.copy(pending = false, confirmation = null)
        when (written) {
          null -> done.copy(error = AccountSheetError.ArchiveFailed)
          CuentaArchive.Updated -> done.copy(isClosing = true)
          CuentaArchive.LastActive -> done.copy(error = AccountSheetError.ArchiveLastActive)
        }
      }
      if (written == CuentaArchive.Updated) {
        feedback(
            if (archived) AccountsFeedback.Archived(cuenta.nombre)
            else AccountsFeedback.Unarchived(cuenta.nombre)
        )
      }
    }
  }

  private fun updateSheet(transform: (AccountSheet) -> AccountSheet) {
    uiState.update { state -> state.sheet?.let { state.copy(sheet = transform(it)) } ?: state }
  }

  private fun feedback(feedback: AccountsFeedback) {
    effectChannel.trySend(AccountsEffect.Feedback(feedback))
  }

  private fun observeSource(): Flow<AccountsSource> {
    val primary = preferences.preferences.map { it.primaryMoneda() }.distinctUntilChanged()
    val estimates = currentDay.flatMapLatest { day: LocalDate ->
      saldos.observeCreditEstimates(day)
    }
    return combine(primary, saldos.observeSaldos(), saldos.observeTotales(), estimates) {
        moneda,
        balances,
        totals,
        credit ->
      moneda
          .zip(balances) { m, b -> m to b }
          .zip(totals.zip(credit) { t, c -> t to c }) { (m, b), (t, c) ->
            AccountsSource.Ready(m, b, t, c)
          }
          .fold(onSuccess = { it }, onFailure = { AccountsSource.Failed })
    }
  }
}

private inline fun <A, B, R> DataOutcome<A>.zip(
    other: DataOutcome<B>,
    transform: (A, B) -> R,
): DataOutcome<R> =
    when (this) {
      is DataOutcome.Failure -> this
      is DataOutcome.Success -> other.map { transform(data, it) }
    }

private sealed interface AccountsSource {
  data object Loading : AccountsSource

  data object Failed : AccountsSource

  data class Ready(
      val primary: Moneda,
      val saldos: List<CuentaSaldo>,
      val totales: Map<Moneda, Money>,
      val credit: Map<String, CreditEstimate>,
  ) : AccountsSource {
    fun find(id: String): Cuenta? = saldos.firstOrNull { it.cuenta.id == id }?.cuenta

    fun row(saldo: CuentaSaldo): CuentaBalance =
        CuentaBalance(
            cuenta = saldo.cuenta,
            saldo = saldo.saldo,
            credit = credit[saldo.cuenta.id],
            showsMoneda = saldo.cuenta.moneda != primary,
        )

    val orderedTotals: List<Money>
      get() =
          totales.values.sortedWith(
              compareBy<Money> { it.moneda != primary }.thenBy { it.moneda.ordinal }
          )
  }
}

private val nameOrder: Comparator<CuentaBalance> =
    compareBy<CuentaBalance> { normalizeSearchText(it.cuenta.nombre) }.thenBy { it.cuenta.nombre }

private fun AccountsSource.content(query: String): AccountsContent =
    when (this) {
      AccountsSource.Loading -> AccountsContent.Loading
      AccountsSource.Failed -> AccountsContent.Failed
      is AccountsSource.Ready -> {
        val (archived, active) =
            saldos
                .filter { matchesSearchText(query, listOf(it.cuenta.nombre)) }
                .map(::row)
                .sortedWith(nameOrder)
                .partition { it.cuenta.archivada }
        AccountsContent.Ready(
            totals = orderedTotals,
            groups =
                TipoCuenta.entries.mapNotNull { tipo ->
                  active
                      .filter { it.cuenta.tipo == tipo }
                      .takeIf { it.isNotEmpty() }
                      ?.let { AccountGroup(tipo, it) }
                },
            archived = archived,
            query = query.trim(),
        )
      }
    }

private fun AccountsUiState.withSource(loaded: AccountsSource): AccountsUiState =
    copy(content = loaded.content(query), sheet = sheet?.refreshedFrom(loaded))

private fun AccountSheet.refreshedFrom(loaded: AccountsSource): AccountSheet {
  val ready = loaded as? AccountsSource.Ready
  val stored = editing?.let { ready?.find(it.id) }
  return if (ready == null || stored == null) this
  else copy(editing = stored, credit = ready.credit[stored.id])
}
