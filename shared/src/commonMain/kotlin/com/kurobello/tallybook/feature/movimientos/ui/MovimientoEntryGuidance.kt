package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.Composable
import com.kurobello.tallybook.core.model.TipoMovimiento
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.movimiento_amount_expense_prompt
import tallybook.shared.generated.resources.movimiento_amount_income_prompt

@Composable
internal fun movimientoAmountPrompt(tipo: TipoMovimiento): String =
    stringResource(
        if (tipo == TipoMovimiento.GASTO) Res.string.movimiento_amount_expense_prompt
        else Res.string.movimiento_amount_income_prompt
    )
