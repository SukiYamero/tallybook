package com.kurobello.tallybook.core.ui.account

import com.kurobello.tallybook.core.model.MedioPago
import org.jetbrains.compose.resources.StringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.payment_method_cash
import tallybook.shared.generated.resources.payment_method_credit
import tallybook.shared.generated.resources.payment_method_debit
import tallybook.shared.generated.resources.payment_method_transfer

private val labels: Map<MedioPago, StringResource> =
    mapOf(
        MedioPago.EFECTIVO to Res.string.payment_method_cash,
        MedioPago.DEBITO to Res.string.payment_method_debit,
        MedioPago.TRANSFERENCIA to Res.string.payment_method_transfer,
        MedioPago.CREDITO to Res.string.payment_method_credit,
    )

internal val MedioPago.labelResource: StringResource
  get() = labels.getValue(this)
