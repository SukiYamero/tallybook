package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.CreditEstimate
import com.kurobello.tallybook.core.model.CuentaSaldo
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Derived money views of the cuentas, recomputed from live rows on every committed write. */
interface SaldoRepository {
  /** Every live account, archived included, with its balance. */
  fun observeSaldos(): Flow<DataOutcome<List<CuentaSaldo>>>

  /** One total per currency that has at least one live account. */
  fun observeTotales(): Flow<DataOutcome<Map<Moneda, Money>>>

  /**
   * Live credit-card accounts by id; the cycle comes from `creditCycle(today, diaCorte)`. [today]
   * belongs to the caller's clock and time zone, so a new day needs a new subscription.
   */
  fun observeCreditEstimates(today: LocalDate): Flow<DataOutcome<Map<String, CreditEstimate>>>
}
