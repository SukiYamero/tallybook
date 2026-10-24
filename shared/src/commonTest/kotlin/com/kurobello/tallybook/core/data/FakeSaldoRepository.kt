package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.CreditEstimate
import com.kurobello.tallybook.core.model.CuentaSaldo
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.creditCycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate

/**
 * Balances that tests set directly in [saldos]; totals and deudas derive from them like the real
 * repository. Cycle spending per card id comes from [cycleSpending] (zero when absent), since it
 * depends on movimientos this fake does not hold. [failure] makes every flow emit that error.
 */
internal class FakeSaldoRepository(initial: List<CuentaSaldo> = emptyList()) : SaldoRepository {

  val saldos = MutableStateFlow(initial)
  val cycleSpending = MutableStateFlow<Map<String, Money>>(emptyMap())
  val failure = MutableStateFlow<DataError?>(null)

  override fun observeSaldos(): Flow<DataOutcome<List<CuentaSaldo>>> = outcomes { it }

  override fun observeTotales(): Flow<DataOutcome<Map<Moneda, Money>>> = outcomes {
    it.totalsByMoneda()
  }

  override fun observeCreditEstimates(
      today: LocalDate
  ): Flow<DataOutcome<Map<String, CreditEstimate>>> =
      combine(saldos, cycleSpending, failure) { current, spending, error ->
        error?.let { DataOutcome.Failure(it) }
            ?: DataOutcome.Success(
                current
                    .filter { it.cuenta.tipo == TipoCuenta.CREDITO }
                    .associate { (cuenta, saldo) ->
                      cuenta.id to
                          CreditEstimate(
                              deuda = CreditEstimate.deudaOf(saldo),
                              cycleSpending = spending[cuenta.id] ?: Money(0, cuenta.moneda),
                              cycle = creditCycle(today, cuenta.diaCorte),
                          )
                    }
            )
      }

  private fun <T> outcomes(transform: (List<CuentaSaldo>) -> T): Flow<DataOutcome<T>> =
      combine(saldos, failure) { current, error ->
        error?.let { DataOutcome.Failure(it) } ?: DataOutcome.Success(transform(current))
      }
}
