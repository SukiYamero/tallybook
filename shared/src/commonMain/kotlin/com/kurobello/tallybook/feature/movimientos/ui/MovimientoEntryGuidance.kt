package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.movimiento_amount_expense_prompt
import tallybook.shared.generated.resources.movimiento_amount_income_prompt
import tallybook.shared.generated.resources.movimiento_category_expense_prompt
import tallybook.shared.generated.resources.movimiento_category_hint
import tallybook.shared.generated.resources.movimiento_category_income_prompt

@Composable
internal fun MovimientoCategoryGuidance(tipo: TipoMovimiento) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    TallybookSectionHeading(
        text =
            stringResource(
                if (tipo == TipoMovimiento.GASTO) Res.string.movimiento_category_expense_prompt
                else Res.string.movimiento_category_income_prompt
            ),
    )
    TallybookText(
        text = stringResource(Res.string.movimiento_category_hint),
        style = TallybookTheme.typography.bodySmall,
        color = TallybookTheme.colors.foreground.secondary,
    )
  }
}

@Composable
internal fun movimientoAmountPrompt(tipo: TipoMovimiento): String =
    stringResource(
        if (tipo == TipoMovimiento.GASTO) Res.string.movimiento_amount_expense_prompt
        else Res.string.movimiento_amount_income_prompt
    )
