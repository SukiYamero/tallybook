package com.kurobello.tallybook.core.data

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.kurobello.tallybook.core.database.Cuenta as DatabaseCuenta
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.CreditEstimate
import com.kurobello.tallybook.core.model.CuentaSaldo
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.creditCycle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

internal class SaldoRepositoryImpl(
    private val database: TallybookDatabase,
    private val dispatcher: CoroutineDispatcher,
) : SaldoRepository {

  override fun observeSaldos(): Flow<DataOutcome<List<CuentaSaldo>>> = saldos().asLocalDataOutcome()

  override fun observeTotales(): Flow<DataOutcome<Map<Moneda, Money>>> =
      saldos().map { it.totalsByMoneda() }.asLocalDataOutcome()

  // Every card's cycle depends on its own diaCorte, so the cycle sums are one read per card; the
  // transaction keeps them and the balances on the same database state.
  override fun observeCreditEstimates(
      today: LocalDate
  ): Flow<DataOutcome<Map<String, CreditEstimate>>> =
      saldosQuery()
          .asFlow()
          .map { query ->
            withContext(dispatcher) {
              database.transactionWithResult {
                query
                    .executeAsList()
                    .filter { it.cuenta.tipo == TipoCuenta.CREDITO }
                    .associate { it.cuenta.id to estimate(it, today) }
              }
            }
          }
          .asLocalDataOutcome()

  private fun saldos(): Flow<List<CuentaSaldo>> = saldosQuery().asFlow().mapToList(dispatcher)

  private fun saldosQuery(): Query<CuentaSaldo> =
      database.saldoQueries.selectCuentaSaldos {
          id,
          nombre,
          tipo,
          moneda,
          saldoInicialMinor,
          diaCorte,
          archivada,
          createdAt,
          updatedAt,
          deletedAt,
          ingresosMinor,
          gastosMinor,
          entradasMinor,
          salidasMinor ->
        val cuenta =
            DatabaseCuenta(
                    id,
                    nombre,
                    tipo,
                    moneda,
                    saldoInicialMinor,
                    diaCorte,
                    archivada,
                    createdAt,
                    updatedAt,
                    deletedAt,
                )
                .toDomain()
        val units = { minorUnits: Long -> Money(minorUnits, cuenta.moneda) }
        CuentaSaldo(
            cuenta = cuenta,
            saldo =
                cuenta.saldoInicial + units(ingresosMinor) - units(gastosMinor) +
                    units(entradasMinor) - units(salidasMinor),
        )
      }

  private fun estimate(saldo: CuentaSaldo, today: LocalDate): CreditEstimate {
    val cycle = creditCycle(today, saldo.cuenta.diaCorte)
    val sums =
        database.saldoQueries
            .sumCicloByCuenta(saldo.cuenta.id, cycle.start.toString(), cycle.end.toString())
            .executeAsOne()
    val moneda = saldo.cuenta.moneda
    return CreditEstimate(
        deuda = CreditEstimate.deudaOf(saldo.saldo),
        cycleSpending = Money(sums.gastos_minor, moneda) - Money(sums.ingresos_minor, moneda),
        cycle = cycle,
    )
  }
}

internal fun List<CuentaSaldo>.totalsByMoneda(): Map<Moneda, Money> =
    groupBy { it.saldo.moneda }
        .mapValues { (moneda, saldos) ->
          saldos.fold(Money(0, moneda)) { total, cuentaSaldo -> total + cuentaSaldo.saldo }
        }
