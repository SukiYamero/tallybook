package com.kurobello.tallybook.core.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.CreditEstimate
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaSaldo
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.Transferencia
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class SaldoRepositoryTest {

  @Test
  fun `balance is opening plus ingresos minus gastos over live movimientos`() =
      withDatabase { repository, database, driver ->
        val cash = cuenta("cash", saldoInicial = cop(10_000))
        database.insertCuentaRow(cash, ChangeStamp.SEED)
        database.insertLegacy(movimiento("salary", TipoMovimiento.INGRESO, cop(5_000), "cash"))
        database.insertLegacy(movimiento("lunch", TipoMovimiento.GASTO, cop(2_000), "cash"))
        database.insertLegacy(movimiento("gone", TipoMovimiento.GASTO, cop(999), "cash"))
        driver.tombstone("movimiento", "gone")

        assertEquals(
            DataOutcome.Success(listOf(CuentaSaldo(cash, cop(13_000)))),
            repository.observeSaldos().first(),
        )
      }

  @Test
  fun `transfers move montoOrigen out and montoDestino in each in its own currency`() =
      withDatabase { repository, database, driver ->
        val cash = cuenta("cash", saldoInicial = cop(10_000))
        val nequi = cuenta("nequi", tipo = TipoCuenta.BANCO, minute = 1)
        val wise = cuenta("wise", tipo = TipoCuenta.BANCO, saldoInicial = usd(100), minute = 2)
        listOf(cash, nequi, wise).forEach { database.insertCuentaRow(it, ChangeStamp.SEED) }
        database.insertTransferenciaRow(transferencia("to-nequi", "cash", "nequi", cop(3_000)))
        database.insertTransferenciaRow(
            transferencia("from-wise", "wise", "cash", usd(25), cop(100_000))
        )
        database.insertTransferenciaRow(transferencia("gone", "cash", "nequi", cop(777)))
        driver.tombstone("transferencia", "gone")

        assertEquals(
            DataOutcome.Success(
                listOf(
                    CuentaSaldo(cash, cop(107_000)),
                    CuentaSaldo(nequi, cop(3_000)),
                    CuentaSaldo(wise, usd(75)),
                )
            ),
            repository.observeSaldos().first(),
        )
      }

  @Test
  fun `totals per currency include archived accounts and skip tombstoned ones`() =
      withDatabase { repository, database, driver ->
        val cash = cuenta("cash", saldoInicial = cop(1_000))
        val savings =
            cuenta("savings", tipo = TipoCuenta.BANCO, saldoInicial = cop(500), archivada = true)
        val wise = cuenta("wise", tipo = TipoCuenta.BANCO, saldoInicial = usd(50), minute = 1)
        val closedCop = cuenta("closed-cop", saldoInicial = cop(10_000), minute = 2)
        val closedMxn = cuenta("closed-mxn", saldoInicial = Money(999, Moneda.MXN), minute = 3)
        listOf(cash, savings, wise, closedCop, closedMxn).forEach {
          database.insertCuentaRow(it, ChangeStamp.SEED)
        }
        database.insertLegacy(movimiento("interest", TipoMovimiento.INGRESO, cop(200), "savings"))
        driver.tombstone("cuenta", "closed-cop")
        driver.tombstone("cuenta", "closed-mxn")

        assertEquals(
            DataOutcome.Success(
                listOf(
                    CuentaSaldo(cash, cop(1_000)),
                    CuentaSaldo(savings, cop(700)),
                    CuentaSaldo(wise, usd(50)),
                )
            ),
            repository.observeSaldos().first(),
        )
        assertEquals(
            DataOutcome.Success(mapOf(Moneda.COP to cop(1_700), Moneda.USD to usd(50))),
            repository.observeTotales().first(),
        )
      }

  @Test
  fun `credit estimate reads deuda over all dates and spending inside the cycle`() =
      withDatabase { repository, database, driver ->
        val cash = cuenta("cash", saldoInicial = cop(500_000))
        val nu = cuenta("nu", TipoCuenta.CREDITO, saldoInicial = cop(-150_000), diaCorte = 15)
        val visa = cuenta("visa", TipoCuenta.CREDITO, minute = 1)
        val closed = cuenta("closed", TipoCuenta.CREDITO, saldoInicial = cop(-1), minute = 2)
        listOf(cash, nu, visa, closed).forEach { database.insertCuentaRow(it, ChangeStamp.SEED) }
        driver.tombstone("cuenta", "closed")
        val purchases =
            listOf(
                movimiento("nu-before", TipoMovimiento.GASTO, cop(40_000), "nu", "2026-02-15"),
                movimiento("nu-first-day", TipoMovimiento.GASTO, cop(20_000), "nu", "2026-02-16"),
                movimiento("nu-refund", TipoMovimiento.INGRESO, cop(10_000), "nu", "2026-03-01"),
                movimiento("nu-closing", TipoMovimiento.GASTO, cop(30_000), "nu", "2026-03-15"),
                movimiento("nu-gone", TipoMovimiento.GASTO, cop(9_999), "nu", "2026-03-02"),
                movimiento("visa-before", TipoMovimiento.GASTO, cop(3_000), "visa", "2026-02-28"),
                movimiento("visa-first", TipoMovimiento.GASTO, cop(10_000), "visa", "2026-03-01"),
            )
        purchases.forEach { database.insertLegacy(it) }
        driver.tombstone("movimiento", "nu-gone")
        database.insertTransferenciaRow(
            transferencia("pay-nu", "cash", "nu", cop(100_000), fecha = "2026-03-05")
        )
        database.insertTransferenciaRow(
            transferencia("pay-visa", "cash", "visa", cop(25_000), fecha = "2026-03-05")
        )

        assertEquals(
            DataOutcome.Success(
                mapOf(
                    "nu" to
                        CreditEstimate(
                            deuda = cop(130_000),
                            cycleSpending = cop(40_000),
                            cycle =
                                MovimientoDateRange(LocalDate(2026, 2, 16), LocalDate(2026, 3, 15)),
                        ),
                    "visa" to
                        CreditEstimate(
                            deuda = cop(0),
                            cycleSpending = cop(10_000),
                            cycle =
                                MovimientoDateRange(LocalDate(2026, 3, 1), LocalDate(2026, 3, 31)),
                        ),
                )
            ),
            repository.observeCreditEstimates(LocalDate(2026, 3, 15)).first(),
        )
      }

  @Test
  fun `balances and estimates re-emit once per committed movimiento or transfer`() =
      withDatabase { repository, database, _ ->
        val cash = cuenta("cash", saldoInicial = cop(1_000))
        val card = cuenta("card", TipoCuenta.CREDITO, minute = 1)
        listOf(cash, card).forEach { database.insertCuentaRow(it, ChangeStamp.SEED) }
        val today = LocalDate(2026, 3, 15)
        val saldos = mutableListOf<DataOutcome<List<CuentaSaldo>>>()
        val estimates = mutableListOf<DataOutcome<Map<String, CreditEstimate>>>()
        val collections =
            listOf(
                launch { repository.observeSaldos().collect(saldos::add) },
                launch { repository.observeCreditEstimates(today).collect(estimates::add) },
            )
        testScheduler.advanceUntilIdle()

        database.insertLegacy(
            movimiento("tagged", TipoMovimiento.GASTO, cop(400), "card", "2026-03-10")
                .copy(etiquetaIds = listOf("trip", "food"))
        )
        testScheduler.advanceUntilIdle()
        database.insertTransferenciaRow(transferencia("pay", "cash", "card", cop(300)))
        testScheduler.advanceUntilIdle()

        val march = MovimientoDateRange(LocalDate(2026, 3, 1), LocalDate(2026, 3, 31))
        assertEquals(
            listOf(
                    listOf(CuentaSaldo(cash, cop(1_000)), CuentaSaldo(card, cop(0))),
                    listOf(CuentaSaldo(cash, cop(1_000)), CuentaSaldo(card, cop(-400))),
                    listOf(CuentaSaldo(cash, cop(700)), CuentaSaldo(card, cop(-100))),
                )
                .map<_, DataOutcome<List<CuentaSaldo>>> { DataOutcome.Success(it) },
            saldos,
        )
        assertEquals(
            listOf(
                    CreditEstimate(cop(0), cop(0), march),
                    CreditEstimate(cop(400), cop(400), march),
                    CreditEstimate(cop(100), cop(400), march),
                )
                .map<_, DataOutcome<Map<String, CreditEstimate>>> {
                  DataOutcome.Success(mapOf("card" to it))
                },
            estimates,
        )
        collections.forEach { it.cancel() }
      }

  @Test
  fun `balance aggregates search the account indexes inside one snapshot`() = runTest {
    val driver = TrackingDriver(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY))
    driver.use {
      TallybookDatabase.Schema.create(driver)
      val database = TallybookDatabase(driver)
      listOf(cuenta("cash"), cuenta("card", TipoCuenta.CREDITO, diaCorte = 15, minute = 1))
          .forEach { database.insertCuentaRow(it, ChangeStamp.SEED) }
      val repository = SaldoRepositoryImpl(database, StandardTestDispatcher(testScheduler))

      driver.reads.clear()
      repository.observeSaldos().first()
      val saldoRead = driver.reads.single()
      driver.reads.clear()
      repository.observeCreditEstimates(LocalDate(2026, 3, 15)).first()
      val estimateReads = driver.reads.toList()

      val saldoPlan = driver.plan(saldoRead)
      println("Saldo plan: $saldoPlan")
      listOf(
              "USING INDEX movimiento_by_cuenta",
              "USING INDEX transferencia_by_origen",
              "USING INDEX transferencia_by_destino",
          )
          .forEach { index -> assertTrue(saldoPlan.any { index in it }, "$index in $saldoPlan") }
      assertTrue(
          saldoPlan.none { it.startsWith("SCAN m") || it.startsWith("SCAN t") },
          "$saldoPlan",
      )

      val cycleRead = estimateReads.last()
      val cyclePlan = driver.plan(cycleRead)
      println("Credit cycle plan: $cyclePlan")
      assertTrue(cyclePlan.single().startsWith("SEARCH"), "$cyclePlan")
      assertTrue(estimateReads.size == 2, "$estimateReads")
      val transaction = assertNotNull(estimateReads.first().transaction)
      assertTrue(estimateReads.all { it.transaction === transaction })
    }
  }

  private fun withDatabase(
      block: suspend TestScope.(SaldoRepository, TallybookDatabase, SqlDriver) -> Unit
  ) = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val database = TallybookDatabase(driver)
      block(SaldoRepositoryImpl(database, StandardTestDispatcher(testScheduler)), database, driver)
    }
  }

  private class Read(
      val sql: String,
      val parameters: Int,
      val binders: (SqlPreparedStatement.() -> Unit)?,
      val transaction: Any?,
  ) {
    override fun toString(): String = sql
  }

  private class TrackingDriver(private val delegate: SqlDriver) : SqlDriver by delegate {
    val reads = mutableListOf<Read>()

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> {
      reads += Read(sql, parameters, binders, currentTransaction())
      return delegate.executeQuery(identifier, sql, mapper, parameters, binders)
    }

    fun plan(read: Read): List<String> =
        delegate
            .executeQuery(
                null,
                "EXPLAIN QUERY PLAN ${read.sql}",
                { cursor ->
                  val details = mutableListOf<String>()
                  while (cursor.next().value) details += requireNotNull(cursor.getString(3))
                  QueryResult.Value(details)
                },
                read.parameters,
                read.binders,
            )
            .value
  }
}

private fun cop(minorUnits: Long) = Money(minorUnits, Moneda.COP)

private fun usd(minorUnits: Long) = Money(minorUnits, Moneda.USD)

@Suppress("LongParameterList")
private fun cuenta(
    id: String,
    tipo: TipoCuenta = TipoCuenta.EFECTIVO,
    saldoInicial: Money = cop(0),
    diaCorte: Int? = null,
    archivada: Boolean = false,
    minute: Int = 0,
) =
    Cuenta(
        id = id,
        nombre = id,
        tipo = tipo,
        saldoInicial = saldoInicial,
        diaCorte = diaCorte,
        archivada = archivada,
        createdAt = Instant.parse("2026-01-01T08:0$minute:00Z"),
    )

private fun movimiento(
    id: String,
    tipo: TipoMovimiento,
    monto: Money,
    cuentaId: String,
    fecha: String = "2026-03-01",
) =
    Movimiento(
        id = id,
        tipo = tipo,
        monto = monto,
        fecha = LocalDate.parse(fecha),
        categoriaId = "food",
        titulo = null,
        descripcion = null,
        createdAt = Instant.parse("${fecha}T12:00:00Z"),
        cuentaId = cuentaId,
        medio = MedioPago.EFECTIVO,
    )

private fun TallybookDatabase.insertTransferenciaRow(transferencia: Transferencia) =
    insertTransferenciaRow(transferencia, ChangeStamp.SEED)

private fun transferencia(
    id: String,
    origen: String,
    destino: String,
    montoOrigen: Money,
    montoDestino: Money = montoOrigen,
    fecha: String = "2026-03-01",
) =
    Transferencia(
        id = id,
        cuentaOrigenId = origen,
        cuentaDestinoId = destino,
        montoOrigen = montoOrigen,
        montoDestino = montoDestino,
        fecha = LocalDate.parse(fecha),
        descripcion = null,
        createdAt = Instant.parse("${fecha}T12:00:00Z"),
    )
