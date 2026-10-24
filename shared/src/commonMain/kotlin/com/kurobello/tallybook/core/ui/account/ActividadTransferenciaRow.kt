package com.kurobello.tallybook.core.ui.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.model.Actividad
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Moneda
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.activity_missing_account
import tallybook.shared.generated.resources.transfer_edit_title
import tallybook.shared.generated.resources.transfer_with_moneda

/**
 * A transfer in History or Search: the amount is the viewed currency's side, and accounts outside
 * [primary] carry their currency code so same-named accounts stay apart.
 */
@Composable
@Suppress("LongParameterList")
internal fun ActividadTransferenciaRow(
    actividad: Actividad.DeTransferencia,
    origen: Cuenta?,
    destino: Cuenta?,
    primary: Moneda?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
  TransferenciaRowContent(
      origen = actividadCuentaLabel(origen, primary),
      destino = actividadCuentaLabel(destino, primary),
      amount = actividad.monto.format(),
      modifier = modifier.fillMaxWidth(),
      supporting = actividad.transferencia.descripcion?.takeUnless { it.isBlank() },
      onClick = onClick,
      onClickLabel = onClick?.let { stringResource(Res.string.transfer_edit_title) },
  )
}

/**
 * A missing [cuenta] gets a localized fallback rather than its stored ID; while [primary] is
 * unknown no code is added.
 */
@Composable
internal fun actividadCuentaLabel(cuenta: Cuenta?, primary: Moneda?): String =
    when {
      cuenta == null -> stringResource(Res.string.activity_missing_account)
      primary == null || cuenta.moneda == primary -> cuenta.nombre
      else -> stringResource(Res.string.transfer_with_moneda, cuenta.nombre, cuenta.moneda.name)
    }
