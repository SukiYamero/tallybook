package com.kurobello.tallybook.feature.accounts.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.CuentaRepository
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaWrite
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoCuenta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [draft] is null while the selector shows its accounts; otherwise the creation pane is open.
 * [duplicate] is the live account the last save collided with.
 */
internal data class QuickCuentaUiState(
    val draft: AccountDraftState? = null,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
    val duplicate: Cuenta? = null,
) {
  val canSave: Boolean
    get() = draft?.isValid == true && duplicate == null && !saving

  /** Only an active account of the preset type can stand in for the one being created. */
  val canUseDuplicate: Boolean
    get() = duplicate?.let { !it.archivada && it.tipo == draft?.tipo } == true && !saving
}

internal sealed interface QuickCuentaEffect {
  val cuenta: Cuenta

  data class Created(override val cuenta: Cuenta) : QuickCuentaEffect

  data class Used(override val cuenta: Cuenta) : QuickCuentaEffect
}

@Stable
internal interface QuickCuentaActions {
  /** The medio presets [tipo], which the pane never lets the user change. */
  fun onStart(tipo: TipoCuenta, moneda: Moneda, nombre: String = "")

  fun onDraftChanged(transform: AccountDraftState.() -> AccountDraftState)

  fun onSave()

  fun onUseDuplicate()

  /** Leaves the creation pane; a write still in flight completes but is no longer reported. */
  fun onClose()
}

/** The in-place account creation offered by a movimiento's selector when no account fits. */
internal class QuickCuentaViewModel(
    private val cuentaRepository: CuentaRepository,
    scope: CoroutineScope? = null,
) : ViewModel(), QuickCuentaActions {
  private val coroutineScope = scope ?: viewModelScope
  // Bumped whenever the pane opens or closes, so a write that outlives its pane is ignored.
  private var generation = 0L

  val uiState: StateFlow<QuickCuentaUiState>
    field = MutableStateFlow(QuickCuentaUiState())

  private val effectChannel = Channel<QuickCuentaEffect>(Channel.BUFFERED)
  val effects: Flow<QuickCuentaEffect> = effectChannel.receiveAsFlow()

  override fun onStart(tipo: TipoCuenta, moneda: Moneda, nombre: String) {
    generation++
    uiState.value = QuickCuentaUiState(draft = AccountDraftState.forNew(nombre, tipo, moneda))
  }

  override fun onDraftChanged(transform: AccountDraftState.() -> AccountDraftState) =
      uiState.update { state ->
        val draft = state.draft?.takeUnless { state.saving } ?: return@update state
        state.copy(draft = draft.transform().copy(tipo = draft.tipo), duplicate = null)
      }

  override fun onSave() {
    val state = uiState.value.takeIf { it.canSave } ?: return
    val draft = state.draft?.toDraft() ?: return
    val token = generation
    uiState.value = state.copy(saving = true, saveFailed = false)
    coroutineScope.launch {
      val written = cuentaRepository.create(draft)
      if (token != generation) return@launch
      when (val result = (written as? DataOutcome.Success)?.data) {
        is CuentaWrite.Saved -> finish(QuickCuentaEffect.Created(result.cuenta))
        is CuentaWrite.Duplicate ->
            uiState.update { it.copy(saving = false, duplicate = result.existing) }
        CuentaWrite.CurrencyLocked,
        CuentaWrite.TypeLocked,
        null -> uiState.update { it.copy(saving = false, saveFailed = true) }
      }
    }
  }

  override fun onUseDuplicate() {
    val state = uiState.value.takeIf { it.canUseDuplicate } ?: return
    finish(QuickCuentaEffect.Used(requireNotNull(state.duplicate)))
  }

  override fun onClose() {
    generation++
    uiState.value = QuickCuentaUiState()
  }

  private fun finish(effect: QuickCuentaEffect) {
    onClose()
    effectChannel.trySend(effect)
  }
}
