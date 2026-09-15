package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.TipoMovimiento
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.expense
import tallybook.shared.generated.resources.income

@Composable
internal fun MovimientoTypeSelector(
    selected: TipoMovimiento,
    enabled: Boolean,
    onSelected: (TipoMovimiento) -> Unit,
    modifier: Modifier = Modifier,
) {
  Row(
      modifier = modifier.fillMaxWidth().selectableGroup(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    TipoMovimiento.entries.forEach { tipo ->
      FilterChip(
          selected = tipo == selected,
          onClick = { onSelected(tipo) },
          modifier = Modifier.weight(1f),
          enabled = enabled,
          label = { Text(tipo.label()) },
      )
    }
  }
}

@Composable
private fun TipoMovimiento.label(): String =
    when (this) {
      TipoMovimiento.GASTO -> stringResource(Res.string.expense)
      TipoMovimiento.INGRESO -> stringResource(Res.string.income)
    }
