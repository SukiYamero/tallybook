package com.kurobello.tallybook.core.data

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.Actividad
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Direccion
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import com.kurobello.tallybook.core.model.SearchTipo
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.Transferencia
import java.io.File
import java.util.Properties
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ActividadRepositoryTest {

  @Test
  fun `a range interleaves movimientos and transfers with each currency's side`() = runTest {
    withRepository { _, database, repository ->
      database.insertLegacy(movimiento("gasto", "2026-09-15", createdAt = "2026-09-15T10:00:00Z"))
      database.insertLegacy(movimiento("usd-gasto", "2026-09-15", moneda = Moneda.USD))
      database.insertLegacy(movimiento("before", "2026-08-31"))
      val interna = transferencia("interna", NEQUI, NU, "2026-09-15", 500, 500)
      val cruzada =
          transferencia("cruzada", USD_CASH, NEQUI, "2026-09-20", 10, 40_000)
              .copy(createdAt = Instant.parse("2026-09-20T09:00:00Z"))
      val usdOnly = transferencia("usd-only", USD_CASH, USD_BANK, "2026-09-16", 7, 7)
      val borrada = transferencia("borrada", NEQUI, NU, "2026-09-17", 9, 9)
      for (row in listOf(interna, cruzada, usdOnly, borrada)) {
        database.insertTransferenciaRow(row, ChangeStamp.SEED)
      }
      database.transferenciaQueries.tombstoneTransferencia(TOMBSTONE, borrada.id)

      val cop = success(repository.observeActividadRange(Moneda.COP, september).first())
      assertEquals(listOf("cruzada", "interna", "gasto"), cop.map { it.id })
      assertEquals(
          Actividad.DeTransferencia(cruzada, Money(40_000, Moneda.COP), Direccion.ENTRADA),
          cop[0],
      )
      assertEquals(
          Actividad.DeTransferencia(interna, Money(500, Moneda.COP), Direccion.INTERNA),
          cop[1],
      )

      val usd = success(repository.observeActividadRange(Moneda.USD, september).first())
      assertEquals(listOf("cruzada", "usd-only", "usd-gasto"), usd.map { it.id })
      assertEquals(
          Actividad.DeTransferencia(cruzada, Money(10, Moneda.USD), Direccion.SALIDA),
          usd[0],
      )
    }
  }

  @Test
  fun `years include those that only have a transfer side in the currency`() = runTest {
    withRepository { _, database, repository ->
      database.insertLegacy(movimiento("gasto", "2026-09-15"))
      database.insertTransferenciaRow(
          transferencia("old", USD_CASH, NEQUI, "2023-04-01", 10, 40_000),
          ChangeStamp.SEED,
      )
      database.insertTransferenciaRow(
          transferencia("gone", NEQUI, NU, "2021-04-01", 10, 10),
          ChangeStamp.SEED,
      )
      database.transferenciaQueries.tombstoneTransferencia(TOMBSTONE, "gone")

      assertEquals(
          DataOutcome.Success(listOf(2026, 2023)),
          repository.observeYears(Moneda.COP).first(),
      )
      assertEquals(DataOutcome.Success(listOf(2023)), repository.observeYears(Moneda.USD).first())
      val snapshot = filtered(repository, MovimientoReadCriteria(Moneda.USD))
      assertEquals(listOf(2023), snapshot.persistedYears)
    }
  }

  @Test
  fun `transfers appear only for an empty or Transfer type without category tag or medio`() =
      runTest {
        withRepository { _, database, repository ->
          database.insertLegacy(movimiento("gasto", "2026-09-15"))
          database.insertLegacy(movimiento("ingreso", "2026-09-14", tipo = TipoMovimiento.INGRESO))
          database.insertTransferenciaRow(
              transferencia("move", NEQUI, NU, "2026-09-16", 5, 5),
              ChangeStamp.SEED,
          )

          assertEquals(
              listOf("move", "gasto", "ingreso"),
              ids(repository, MovimientoReadCriteria(Moneda.COP)),
          )
          assertEquals(
              listOf("move"),
              ids(repository, MovimientoReadCriteria(Moneda.COP, tipo = SearchTipo.TRANSFERENCIA)),
          )
          assertEquals(
              listOf("gasto"),
              ids(repository, MovimientoReadCriteria(Moneda.COP, tipo = SearchTipo.GASTO)),
          )
          for (criteria in
              listOf(
                  MovimientoReadCriteria(Moneda.COP, categoryIds = setOf("food")),
                  MovimientoReadCriteria(Moneda.COP, etiquetaIds = setOf("trip")),
                  MovimientoReadCriteria(Moneda.COP, medios = setOf(MedioPago.EFECTIVO)),
              )) {
            assertTrue("move" !in ids(repository, criteria))
          }
          assertEquals(
              emptyList(),
              ids(
                  repository,
                  MovimientoReadCriteria(
                      Moneda.COP,
                      tipo = SearchTipo.TRANSFERENCIA,
                      medios = setOf(MedioPago.EFECTIVO),
                  ),
              ),
          )
        }
      }

  @Test
  fun `cuenta and medio sets compose OR within and AND across dimensions`() = runTest {
    withRepository { _, database, repository ->
      database.insertLegacy(movimiento("cash", "2026-09-15"))
      database.insertLegacy(
          movimiento("debit", "2026-09-14", cuentaId = NEQUI, medio = MedioPago.DEBITO)
      )
      database.insertLegacy(
          movimiento("wire", "2026-09-13", cuentaId = NEQUI, medio = MedioPago.TRANSFERENCIA)
      )
      database.insertLegacy(
          movimiento("card", "2026-09-12", cuentaId = NU, medio = MedioPago.CREDITO)
      )
      database.insertTransferenciaRow(
          transferencia("to-nu", NEQUI, NU, "2026-09-16", 5, 5),
          ChangeStamp.SEED,
      )
      database.insertTransferenciaRow(
          transferencia("from-usd", USD_CASH, COP_CASH, "2026-09-17", 1, 4_000),
          ChangeStamp.SEED,
      )

      assertEquals(
          listOf("to-nu", "debit", "wire"),
          ids(repository, MovimientoReadCriteria(Moneda.COP, cuentaIds = setOf(NEQUI))),
      )
      assertEquals(
          listOf("from-usd", "to-nu", "cash", "card"),
          ids(repository, MovimientoReadCriteria(Moneda.COP, cuentaIds = setOf(COP_CASH, NU))),
      )
      assertEquals(
          listOf("debit", "card"),
          ids(
              repository,
              MovimientoReadCriteria(
                  Moneda.COP,
                  medios = setOf(MedioPago.DEBITO, MedioPago.CREDITO),
              ),
          ),
      )
      assertEquals(
          listOf("wire"),
          ids(
              repository,
              MovimientoReadCriteria(
                  Moneda.COP,
                  cuentaIds = setOf(NEQUI, COP_CASH),
                  medios = setOf(MedioPago.TRANSFERENCIA),
              ),
          ),
      )
      assertEquals(
          listOf("from-usd"),
          ids(repository, MovimientoReadCriteria(Moneda.USD, cuentaIds = setOf(COP_CASH))),
      )
      assertEquals(
          emptyList(),
          ids(repository, MovimientoReadCriteria(Moneda.COP, cuentaIds = setOf("missing"))),
      )
      assertIs<DataError.InvalidInput>(
          assertIs<DataOutcome.Failure>(
                  repository
                      .observeFiltered(MovimientoReadCriteria(Moneda.COP, cuentaIds = setOf(" ")))
                      .first()
              )
              .error
      )
    }
  }

  @Test
  fun `transfer writes re-emit every activity read and cancellation releases its listeners`() =
      runTest {
        val driver = TrackingDriver(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY))
        try {
          val database = database(driver)
          val repository = ActividadRepositoryImpl(database, StandardTestDispatcher(testScheduler))
          val ranges = mutableListOf<DataOutcome<List<Actividad>>>()
          val searches = mutableListOf<DataOutcome<MovimientoSearchSnapshot>>()
          val years = mutableListOf<DataOutcome<List<Int>>>()
          val jobs =
              listOf(
                  backgroundScope.launch {
                    repository.observeActividadRange(Moneda.COP, september).collect(ranges::add)
                  },
                  backgroundScope.launch {
                    repository.observeFiltered(MovimientoReadCriteria(Moneda.COP)).collect {
                      searches += it
                    }
                  },
                  backgroundScope.launch {
                    repository.observeYears(Moneda.COP).collect(years::add)
                  },
              )
          runCurrent()
          assertTrue("transferencia" in driver.listenerKeys)
          assertTrue("movimiento" in driver.listenerKeys)

          val move = transferencia("move", NEQUI, NU, "2026-09-16", 5, 5)
          database.insertTransferenciaRow(move, ChangeStamp.SEED)
          runCurrent()
          assertEquals(listOf("move"), success(ranges.last()).map { it.id })
          assertEquals(listOf("move"), success(searches.last()).actividades.map { it.id })
          assertEquals(DataOutcome.Success(listOf(2026)), years.last())

          database.transferenciaQueries.tombstoneTransferencia(TOMBSTONE, move.id)
          runCurrent()
          assertEquals(emptyList(), success(ranges.last()))
          assertEquals(MovimientoSearchSnapshot(emptyList(), emptyList()), success(searches.last()))
          assertEquals(DataOutcome.Success(emptyList()), years.last())

          jobs.forEach { it.cancelAndJoin() }
          assertEquals(0, driver.listeners)
        } finally {
          driver.close()
        }
      }

  @Test
  fun `a read a newer write superseded never publishes its stale rows`() = runBlocking {
    val file = File.createTempFile("tallybook-actividad", ".db")
    val driver =
        GatedDriver(
            JdbcSqliteDriver(
                "jdbc:sqlite:${file.absolutePath}",
                Properties().apply { put("journal_mode", "WAL") },
            )
        )
    val pool = Executors.newFixedThreadPool(3).asCoroutineDispatcher()
    try {
      val database = database(driver)
      val repository = ActividadRepositoryImpl(database, pool)
      val emissions = Channel<List<String>>(Channel.UNLIMITED)
      val collector =
          launch(pool) {
            repository.observeActividadRange(Moneda.COP, september).collect {
              emissions.send(success(it).map(Actividad::id))
            }
          }
      assertTrue(driver.firstRowRead.await(5, TimeUnit.SECONDS))

      database.insertLegacy(movimiento("late", "2026-09-15"))
      // Released once the newer change cancelled the blocked read, or after a bound when nothing
      // cancels it, so the stale snapshot reaches the assertion instead of a timeout.
      withTimeoutOrNull(2_000) { while (!collector.hasCancelledDescendant()) delay(5) }
      driver.release.countDown()

      assertEquals(listOf("late"), withTimeout(5_000) { emissions.receive() })
      collector.cancelAndJoin()
    } finally {
      driver.release.countDown()
      pool.close()
      driver.close()
      file.delete()
    }
  }

  @Test
  fun `rows of both kinds and years come from one transaction`() = runTest {
    val driver = TrackingDriver(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY))
    try {
      val database = database(driver)
      val repository = ActividadRepositoryImpl(database, StandardTestDispatcher(testScheduler))
      database.insertLegacy(movimiento("gasto", "2026-09-15"))
      database.insertTransferenciaRow(
          transferencia("move", NEQUI, NU, "2026-09-16", 5, 5),
          ChangeStamp.SEED,
      )
      driver.readTransactions.clear()
      filtered(repository, MovimientoReadCriteria(Moneda.COP, september))
      assertEquals(3, driver.readTransactions.size)
      assertTrue(driver.readTransactions.first() != null)
      assertTrue(driver.readTransactions.all { it === driver.readTransactions.first() })

      driver.readTransactions.clear()
      success(repository.observeActividadRange(Moneda.COP, september).first())
      assertEquals(2, driver.readTransactions.size)
      assertTrue(driver.readTransactions.first() != null)
      assertTrue(driver.readTransactions.last() === driver.readTransactions.first())
    } finally {
      driver.close()
    }
  }

  @Test
  fun `invalid cross year ranges never access storage`() = runTest {
    withRepository { _, _, repository ->
      val unsupported = MovimientoDateRange(LocalDate(-1, 12, 1), LocalDate(0, 1, 31))
      assertIs<DataError.InvalidInput>(
          assertIs<DataOutcome.Failure>(
                  repository.observeActividadRange(Moneda.COP, unsupported).first()
              )
              .error
      )
    }
  }

  @Test
  fun `activity plans scan the recent indexes without sorting`() = runTest {
    val driver = TrackingDriver(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY))
    try {
      val database = database(driver)
      val repository = ActividadRepositoryImpl(database, StandardTestDispatcher(testScheduler))
      for (index in 0..11) {
        database.insertLegacy(
            movimiento(
                "row-$index",
                if (index % 2 == 0) "2026-09-15" else "2024-01-01",
                cuentaId = if (index % 3 == 0) NEQUI else COP_CASH,
                medio = if (index % 3 == 0) MedioPago.DEBITO else MedioPago.EFECTIVO,
            )
        )
        database.insertTransferenciaRow(
            transferencia(
                "move-$index",
                if (index % 2 == 0) NEQUI else USD_CASH,
                NU,
                if (index % 2 == 0) "2026-09-15" else "2024-01-01",
                5,
                5,
            ),
            ChangeStamp.SEED,
        )
      }
      val reads =
          listOf<Pair<String, suspend () -> Any>>(
              "range" to
                  {
                    success(repository.observeActividadRange(Moneda.COP, september).first())
                  },
              "all" to { filtered(repository, MovimientoReadCriteria(Moneda.COP)) },
              "cuenta" to
                  {
                    filtered(
                        repository,
                        MovimientoReadCriteria(Moneda.COP, cuentaIds = setOf(NEQUI)),
                    )
                  },
              "cuenta+medio+category" to
                  {
                    filtered(
                        repository,
                        MovimientoReadCriteria(
                            Moneda.COP,
                            september,
                            categoryIds = setOf("food"),
                            cuentaIds = setOf(NEQUI),
                            medios = setOf(MedioPago.DEBITO),
                        ),
                    )
                  },
          )
      var transferPlans = 0
      for ((name, read) in reads) {
        driver.reads.clear()
        read()
        val rowReads = driver.reads.filter { it.isRowRead }
        assertTrue(rowReads.isNotEmpty())
        transferPlans += rowReads.count { it.isTransferencia }
        for (rowRead in rowReads) {
          val plan = driver.plan(rowRead)
          val index = if (rowRead.isTransferencia) "transferencia_recent" else "movimiento_recent"
          assertTrue(plan.any { index in it }, "$name: $plan")
          assertTrue(plan.none { "TEMP B-TREE FOR ORDER BY" in it }, "$name: $plan")
          println(
              "Activity plan $name ${if (rowRead.isTransferencia) "transfers" else "movimientos"}: $plan"
          )
        }
      }
      assertEquals(3, transferPlans)
    } finally {
      driver.close()
    }
  }

  private suspend fun TestScope.withRepository(
      block: suspend (SqlDriver, TallybookDatabase, ActividadRepository) -> Unit
  ) {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    try {
      val database = database(driver)
      block(
          driver,
          database,
          ActividadRepositoryImpl(database, StandardTestDispatcher(testScheduler)),
      )
    } finally {
      driver.close()
    }
  }

  private fun database(driver: SqlDriver): TallybookDatabase =
      createSchemaWithReferences(driver, listOf("food"), listOf("trip")).apply {
        transaction {
          insertCuentaRow(cuenta(NEQUI, TipoCuenta.BANCO, Moneda.COP), ChangeStamp.SEED)
          insertCuentaRow(cuenta(NU, TipoCuenta.CREDITO, Moneda.COP), ChangeStamp.SEED)
          insertCuentaRow(cuenta(USD_BANK, TipoCuenta.BANCO, Moneda.USD), ChangeStamp.SEED)
        }
      }

  private suspend fun ids(repository: ActividadRepository, criteria: MovimientoReadCriteria) =
      filtered(repository, criteria).actividades.map { it.id }

  private suspend fun filtered(repository: ActividadRepository, criteria: MovimientoReadCriteria) =
      success(repository.observeFiltered(criteria).first())

  private fun <T> success(outcome: DataOutcome<T>): T =
      assertIs<DataOutcome.Success<T>>(outcome).data

  private class TrackingDriver(private val delegate: SqlDriver) : SqlDriver by delegate {
    var listeners = 0
      private set

    val listenerKeys = mutableSetOf<String>()
    val readTransactions = mutableListOf<Any?>()
    val reads = mutableListOf<Read>()

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> {
      if (sql.trimStart().startsWith("SELECT")) {
        readTransactions += currentTransaction()
        reads += Read(sql, parameters, binders)
      }
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

    override fun addListener(vararg queryKeys: String, listener: Query.Listener) {
      delegate.addListener(*queryKeys, listener = listener)
      listenerKeys += queryKeys
      listeners += 1
    }

    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {
      delegate.removeListener(*queryKeys, listener = listener)
      listeners -= 1
    }
  }

  private fun Job.hasCancelledDescendant(): Boolean = children.any {
    it.isCancelled || it.hasCancelledDescendant()
  }

  /**
   * Holds the first movimiento row read open after it ran, inside its read transaction, until
   * [release]; WAL lets a write commit meanwhile without touching that read's snapshot.
   */
  private class GatedDriver(private val delegate: SqlDriver) : SqlDriver by delegate {
    val firstRowRead = CountDownLatch(1)
    val release = CountDownLatch(1)
    private val gated = AtomicBoolean(false)

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> {
      val result = delegate.executeQuery(identifier, sql, mapper, parameters, binders)
      if (
          sql.trimStart().startsWith("SELECT movimiento_con_etiquetas.id") &&
              gated.compareAndSet(false, true)
      ) {
        firstRowRead.countDown()
        release.await()
      }
      return result
    }
  }

  private data class Read(
      val sql: String,
      val parameters: Int,
      val binders: (SqlPreparedStatement.() -> Unit)?,
  ) {
    val isTransferencia: Boolean
      get() = sql.trimStart().startsWith("SELECT transferencia_con_monedas.id")

    val isRowRead: Boolean
      get() = isTransferencia || sql.trimStart().startsWith("SELECT movimiento_con_etiquetas.id")
  }

  private fun cuenta(id: String, tipo: TipoCuenta, moneda: Moneda) =
      Cuenta(id, id, tipo, Money(0, moneda), null, false, Instant.parse("2026-01-02T00:00:00Z"))

  @Suppress("LongParameterList")
  private fun movimiento(
      id: String,
      fecha: String,
      moneda: Moneda = Moneda.COP,
      tipo: TipoMovimiento = TipoMovimiento.GASTO,
      createdAt: String = "2026-09-15T12:00:00Z",
      cuentaId: String = Cuenta.defaultCashId(moneda),
      medio: MedioPago = MedioPago.EFECTIVO,
  ) =
      Movimiento(
          id,
          tipo,
          Money(100, moneda),
          LocalDate.parse(fecha),
          "food",
          "Lunch",
          null,
          Instant.parse(createdAt),
          cuentaId,
          medio,
          if (id == "gasto") listOf("trip") else emptyList(),
      )

  @Suppress("LongParameterList")
  private fun transferencia(
      id: String,
      origen: String,
      destino: String,
      fecha: String,
      montoOrigen: Long,
      montoDestino: Long,
  ) =
      Transferencia(
          id = id,
          cuentaOrigenId = origen,
          cuentaDestinoId = destino,
          montoOrigen = Money(montoOrigen, monedaOf(origen)),
          montoDestino = Money(montoDestino, monedaOf(destino)),
          fecha = LocalDate.parse(fecha),
          descripcion = null,
          createdAt = Instant.parse("2026-09-15T12:00:00Z"),
      )

  private fun monedaOf(cuentaId: String) =
      if (cuentaId == USD_CASH || cuentaId == USD_BANK) Moneda.USD else Moneda.COP

  private companion object {
    val COP_CASH = Cuenta.defaultCashId(Moneda.COP)
    val USD_CASH = Cuenta.defaultCashId(Moneda.USD)
    const val NEQUI = "nequi"
    const val NU = "nu"
    const val USD_BANK = "usd-bank"
    val TOMBSTONE = ChangeStamp.of(epochMillis = 1, counter = 0, node = "00000000000000aa").value
    val september = MovimientoDateRange(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30))
  }
}
