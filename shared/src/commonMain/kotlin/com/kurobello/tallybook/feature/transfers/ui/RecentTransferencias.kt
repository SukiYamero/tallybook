package com.kurobello.tallybook.feature.transfers.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.ui.account.TransferenciaRowContent
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.date.fullCivilDate
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.transfers.viewmodel.RecentTransferencia
import com.kurobello.tallybook.feature.transfers.viewmodel.RecentTransferenciasUiState
import com.kurobello.tallybook.feature.transfers.viewmodel.RecentTransferenciasViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.accounts_recent_transfers
import tallybook.shared.generated.resources.transfer_edit_title
import tallybook.shared.generated.resources.transfer_row_route
import tallybook.shared.generated.resources.transfer_with_moneda

/** The Accounts screen's trailing list items; [onOpen] reopens a transfer in its sheet. */
@Composable
internal fun recentTransferenciasContent(onOpen: (id: String) -> Unit): LazyListScope.() -> Unit {
  val viewModel: RecentTransferenciasViewModel = koinViewModel()
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  return { recentTransferencias(state, onOpen) }
}

/** The newest transfers under the accounts; nothing at all while there are none. */
private fun LazyListScope.recentTransferencias(
    state: RecentTransferenciasUiState,
    onOpen: (id: String) -> Unit,
) {
  if (state.items.isEmpty()) return
  item(key = "recent-transfers") {
    TallybookSectionHeading(
        stringResource(Res.string.accounts_recent_transfers),
        modifier = Modifier.padding(top = TallybookTheme.spacing.space8),
    )
  }
  items(state.items, key = { "transfer-${it.transferencia.id}" }) { item ->
    RecentTransferenciaRow(item, state.primary, onOpen)
  }
}

@Composable
private fun RecentTransferenciaRow(
    item: RecentTransferencia,
    primary: Moneda,
    onOpen: (id: String) -> Unit,
) {
  val transferencia = item.transferencia
  val amount =
      if (transferencia.montoOrigen.moneda == transferencia.montoDestino.moneda) {
        transferencia.montoOrigen.format()
      } else {
        stringResource(
            Res.string.transfer_row_route,
            transferencia.montoOrigen.format(),
            transferencia.montoDestino.format(),
        )
      }
  TransferenciaRowContent(
      origen = cuentaLabel(item.origen, primary),
      destino = cuentaLabel(item.destino, primary),
      amount = amount,
      modifier = Modifier.fillMaxWidth(),
      supporting =
          listOfNotNull(fullCivilDate(transferencia.fecha), transferencia.descripcion)
              .joinToString(SUPPORTING_SEPARATOR),
      onClick = { onOpen(transferencia.id) },
      onClickLabel = stringResource(Res.string.transfer_edit_title),
  )
}

@Composable
private fun cuentaLabel(cuenta: Cuenta, primary: Moneda): String =
    if (cuenta.moneda == primary) cuenta.nombre
    else stringResource(Res.string.transfer_with_moneda, cuenta.nombre, cuenta.moneda.name)

private const val SUPPORTING_SEPARATOR = " · "
