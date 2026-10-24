package com.kurobello.tallybook.core.ui.account

import com.kurobello.tallybook.core.model.TipoCuenta
import org.jetbrains.compose.resources.StringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.account_type_bank
import tallybook.shared.generated.resources.account_type_cash
import tallybook.shared.generated.resources.account_type_credit_card

private val labels: Map<TipoCuenta, StringResource> =
    mapOf(
        TipoCuenta.EFECTIVO to Res.string.account_type_cash,
        TipoCuenta.BANCO to Res.string.account_type_bank,
        TipoCuenta.CREDITO to Res.string.account_type_credit_card,
    )

internal val TipoCuenta.labelResource: StringResource
  get() = labels.getValue(this)
