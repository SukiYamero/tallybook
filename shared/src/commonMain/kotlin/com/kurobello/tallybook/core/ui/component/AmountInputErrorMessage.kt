package com.kurobello.tallybook.core.ui.component

import androidx.compose.runtime.Composable
import com.kurobello.tallybook.core.text.AmountInputError
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.amount_malformed
import tallybook.shared.generated.resources.amount_not_positive
import tallybook.shared.generated.resources.amount_overflow
import tallybook.shared.generated.resources.amount_required
import tallybook.shared.generated.resources.amount_too_many_decimals

internal val amountInputErrorMessages: Map<AmountInputError, StringResource> =
    mapOf(
        AmountInputError.EMPTY to Res.string.amount_required,
        AmountInputError.MALFORMED to Res.string.amount_malformed,
        AmountInputError.NOT_POSITIVE to Res.string.amount_not_positive,
        AmountInputError.TOO_MANY_DECIMALS to Res.string.amount_too_many_decimals,
        AmountInputError.OVERFLOW to Res.string.amount_overflow,
    )

@Composable
internal fun AmountInputError.message(): String =
    stringResource(amountInputErrorMessages.getValue(this))
