package com.kurobello.tallybook.feature.transfers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CuentaRepository
import com.kurobello.tallybook.core.data.TransferenciaRepository
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Transferencia
import com.kurobello.tallybook.core.model.primaryMoneda
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal data class RecentTransferencia(
    val transferencia: Transferencia,
    val origen: Cuenta,
    val destino: Cuenta,
)

/** [primary] decides which accounts show their currency code. */
internal data class RecentTransferenciasUiState(
    val primary: Moneda = Moneda.COP,
    val items: List<RecentTransferencia> = emptyList(),
)

/**
 * The newest transfers listed under the accounts, each reopening its sheet. A read failure leaves
 * the section out rather than covering the accounts it follows.
 */
internal class RecentTransferenciasViewModel(
    transferencias: TransferenciaRepository,
    cuentas: CuentaRepository,
    preferences: AppPreferencesRepository,
    scope: CoroutineScope? = null,
) : ViewModel() {

  val uiState: StateFlow<RecentTransferenciasUiState>
    field = MutableStateFlow(RecentTransferenciasUiState())

  init {
    (scope ?: viewModelScope).launch {
      combine(
              transferencias.observeRecent(RECENT_TRANSFERENCIAS_LIMIT),
              cuentas.observeAll(),
              preferences.preferences,
          ) { recent, live, prefs ->
            val rows = (recent as? DataOutcome.Success)?.data
            val byId = (live as? DataOutcome.Success)?.data?.associateBy(Cuenta::id)
            val primary = (prefs.primaryMoneda() as? DataOutcome.Success)?.data
            if (rows == null || byId == null || primary == null) RecentTransferenciasUiState()
            else
                RecentTransferenciasUiState(
                    primary = primary,
                    items =
                        rows.mapNotNull { row ->
                          val origen = byId[row.cuentaOrigenId]
                          val destino = byId[row.cuentaDestinoId]
                          if (origen == null || destino == null) null
                          else RecentTransferencia(row, origen, destino)
                        },
                )
          }
          .collect { uiState.value = it }
    }
  }
}

internal const val RECENT_TRANSFERENCIAS_LIMIT = 5
