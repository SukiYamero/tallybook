package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.ArrowDownLeft
import com.composables.icons.lucide.ArrowUpRight
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.component.TallybookSegmentOption
import com.kurobello.tallybook.core.ui.component.TallybookSegmentTone
import com.kurobello.tallybook.core.ui.component.TallybookSegmentedControl
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.expense
import tallybook.shared.generated.resources.income
import tallybook.shared.generated.resources.movement_type

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
  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    TallybookSectionHeading(stringResource(Res.string.movement_type))
    TallybookSegmentedControl(
        options = options,
        selected = selected,
        onSelect = onSelected,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        labelStyle = TallybookTheme.typography.controlValue,
    )
  }
}
