package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.ArrowDownLeft
import com.composables.icons.lucide.ArrowUpRight
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.component.TallybookSegmentOption
import com.kurobello.tallybook.core.ui.component.TallybookSegmentTone
import com.kurobello.tallybook.core.ui.component.TallybookSegmentedControl
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
  val expense = stringResource(Res.string.expense)
  val income = stringResource(Res.string.income)
  val options =
      remember(expense, income) {
        listOf(
            TallybookSegmentOption(
                value = TipoMovimiento.GASTO,
                label = expense,
                icon = Lucide.ArrowUpRight,
                tone = TallybookSegmentTone.Expense,
            ),
            TallybookSegmentOption(
                value = TipoMovimiento.INGRESO,
                label = income,
                icon = Lucide.ArrowDownLeft,
                tone = TallybookSegmentTone.Income,
            ),
        )
      }
  TallybookSegmentedControl(
      options = options,
      selected = selected,
      onSelect = onSelected,
      modifier = modifier.fillMaxWidth(),
      enabled = enabled,
  )
}
