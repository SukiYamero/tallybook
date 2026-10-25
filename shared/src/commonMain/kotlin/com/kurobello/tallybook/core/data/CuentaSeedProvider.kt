package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import kotlin.time.Clock
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.account_type_cash

/** Account names are never translated later, so the cash name is resolved once, at creation. */
class CuentaSeedProvider(
    private val clock: Clock,
    private val stringResolver: suspend (StringResource) -> String = ::getString,
) {
  suspend fun defaultCash(moneda: Moneda): Cuenta =
      Cuenta(
          id = Cuenta.defaultCashId(moneda),
          nombre = stringResolver(Res.string.account_type_cash),
          tipo = TipoCuenta.EFECTIVO,
          saldoInicial = Money(0, moneda),
          diaCorte = null,
          archivada = false,
          createdAt = clock.now(),
      )
}
