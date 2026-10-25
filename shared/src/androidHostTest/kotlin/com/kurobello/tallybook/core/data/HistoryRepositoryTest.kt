package com.kurobello.tallybook.core.data

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.MovimientoQueries
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.database.TransferenciaQueries
import com.kurobello.tallybook.core.model.Actividad
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.map
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryRepositoryTest {
  @Test
  fun `range includes both endpoints excludes adjacent dates and currency and keeps stable ties`() =
      runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
          createSchemaWithReferences(driver, listOf("food"))
          val repository =
              HistoryReads(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
          val rows =
              listOf(
                  movimiento("before", "2026-08-31"),
                  movimiento("lower", "2026-09-01"),
                  movimiento("a", "2026-09-15"),
                  movimiento("c", "2026-09-15"),
                  movimiento("b", "2026-09-15", createdAt = "2026-09-15T11:00:00Z"),
                  movimiento("upper", "2026-09-30"),
                  movimiento("after", "2026-10-01"),
                  movimiento("usd", "2026-09-15", Moneda.USD),
              )
          for (row in rows) assertEquals(DataOutcome.Success(Unit), repository.insert(row))

          val expected = listOf(rows[5], rows[3], rows[2], rows[4], rows[1])
          assertEquals(
              DataOutcome.Success(expected),
              repository.observeRange(Moneda.COP, september).first(),
          )
          assertEquals(
              DataOutcome.Success(listOf(rows[7])),
              repository.observeRange(Moneda.USD, september).first(),
          )
        }
      }

  @Test
  fun `one day and empty ranges preserve their exact boundaries`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      createSchemaWithReferences(driver, listOf("food"))
      val repository =
          HistoryReads(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
      val row = movimiento("selected", "2026-09-15")
      assertEquals(DataOutcome.Success(Unit), repository.insert(row))

      val day = LocalDate(2026, 9, 15)
      assertEquals(
          DataOutcome.Success(listOf(row)),
          repository.observeRange(Moneda.COP, MovimientoDateRange(day, day)).first(),
      )
      assertEquals(
          DataOutcome.Success(emptyList()),
          repository
              .observeRange(
                  Moneda.COP,
                  MovimientoDateRange(LocalDate(2026, 9, 16), LocalDate(2026, 9, 17)),
              )
              .first(),
      )
    }
  }

  @Test
  fun `cross year exotic intervals fail as invalid input before storage access`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      createSchemaWithReferences(driver, listOf("food"))
      val real = TallybookDatabase(driver)
      val database =
          object : TallybookDatabase by real {
            override val movimientoQueries: MovimientoQueries
              get() = error("unsupported interval must not access storage")

            override val transferenciaQueries: TransferenciaQueries
              get() = error("unsupported interval must not access storage")
          }
      val repository = HistoryReads(database, StandardTestDispatcher(testScheduler))
      val intervals =
          listOf(
              MovimientoDateRange(LocalDate(-2, 1, 1), LocalDate(-1, 1, 1)),
              MovimientoDateRange(LocalDate(-1, 12, 31), LocalDate(0, 1, 1)),
              MovimientoDateRange(LocalDate(9999, 12, 31), LocalDate(10000, 1, 1)),
              MovimientoDateRange(LocalDate(10000, 12, 31), LocalDate(10001, 1, 1)),
          )
      for (range in intervals) {
        assertIs<DataError.InvalidInput>(
            assertIs<DataOutcome.Failure>(repository.observeRange(Moneda.COP, range).first()).error
        )
      }
    }
  }

  @Test
  fun `same year signed and extended periods and ordinary cross year intervals remain supported`() =
      runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
          createSchemaWithReferences(driver, listOf("food"))
          val repository =
              HistoryReads(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
          for (year in listOf(-1, 0, 9999, 10000)) {
            val row = movimiento("year-$year", LocalDate(year, 2, 15).toString())
            assertEquals(DataOutcome.Success(Unit), repository.insert(row))
            val period = MovimientoDateRange(LocalDate(year, 2, 1), LocalDate(year, 2, 28))
            assertEquals(
                DataOutcome.Success(listOf(row)),
                repository.observeRange(Moneda.COP, period).first(),
            )
          }
          assertEquals(
              DataOutcome.Success(Unit),
              repository.insert(movimiento("last", "2025-12-31")),
          )
          assertEquals(
              DataOutcome.Success(Unit),
              repository.insert(movimiento("first", "2026-01-01")),
          )
          val period = MovimientoDateRange(LocalDate(2025, 12, 31), LocalDate(2026, 1, 1))
          val rows =
              assertIs<DataOutcome.Success<List<Movimiento>>>(
                      repository.observeRange(Moneda.COP, period).first()
                  )
                  .data
          assertEquals(listOf("first", "last"), rows.map { it.id })
        }
      }

  @Test
  fun `years are persisted distinct numeric descending and currency specific`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      createSchemaWithReferences(driver, listOf("food"))
      val repository =
          HistoryReads(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
      assertEquals(DataOutcome.Success(emptyList()), repository.observeYears(Moneda.COP).first())
      for ((index, date) in
          listOf(
                  "2026-09-15",
                  "2024-02-29",
                  "2026-06-01",
                  "0000-01-01",
                  "-0001-01-01",
                  "-0002-01-01",
                  "+10000-01-01",
              )
              .withIndex()) {
        assertEquals(DataOutcome.Success(Unit), repository.insert(movimiento("cop-$index", date)))
      }
      assertEquals(
          DataOutcome.Success(Unit),
          repository.insert(movimiento("usd", "2025-01-01", Moneda.USD)),
      )

      assertEquals(
          DataOutcome.Success(listOf(10000, 2026, 2024, 0, -1, -2)),
          repository.observeYears(Moneda.COP).first(),
      )
      assertEquals(DataOutcome.Success(listOf(2025)), repository.observeYears(Moneda.USD).first())
    }
  }

  @Test
  fun `range and years skip tombstoned movimientos`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      createSchemaWithReferences(driver, listOf("food"))
      val repository =
          HistoryReads(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
      val live = movimiento("live", "2026-09-15")
      for (row in listOf(live, movimiento("gone", "2026-09-16"), movimiento("old", "2024-01-01"))) {
        repository.insert(row)
      }
      driver.tombstone("movimiento", "gone")
      driver.tombstone("movimiento", "old")

      assertEquals(
          DataOutcome.Success(listOf(live)),
          repository.observeRange(Moneda.COP, september).first(),
      )
      assertEquals(DataOutcome.Success(listOf(2026)), repository.observeYears(Moneda.COP).first())
    }
  }

  @Test
  fun `active range and year collectors emit after insert and delete and unregister on cancellation`() =
      runTest {
        val actualDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val driver = TrackingDriver(actualDriver)
        try {
          createSchemaWithReferences(driver, listOf("food"))
          val repository =
              HistoryReads(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
          val rangeEmissions = mutableListOf<DataOutcome<List<Movimiento>>>()
          val yearEmissions = mutableListOf<DataOutcome<List<Int>>>()
          val rangeJob = backgroundScope.launch {
            repository.observeRange(Moneda.COP, september).collect(rangeEmissions::add)
          }
          val yearsJob = backgroundScope.launch {
            repository.observeYears(Moneda.COP).collect(yearEmissions::add)
          }
          runCurrent()
          assertEquals(
              listOf<DataOutcome<List<Movimiento>>>(DataOutcome.Success(emptyList())),
              rangeEmissions,
          )
          assertEquals(
              listOf<DataOutcome<List<Int>>>(DataOutcome.Success(emptyList())),
              yearEmissions,
          )
          assertEquals(3, driver.listeners)

          val first = movimiento("first", "2026-09-15")
          assertEquals(DataOutcome.Success(Unit), repository.insert(first))
          runCurrent()
          assertEquals(
              listOf<DataOutcome<List<Movimiento>>>(
                  DataOutcome.Success(emptyList()),
                  DataOutcome.Success(listOf(first)),
              ),
              rangeEmissions,
          )
          assertEquals(
              listOf<DataOutcome<List<Int>>>(
                  DataOutcome.Success(emptyList()),
                  DataOutcome.Success(listOf(2026)),
              ),
              yearEmissions,
          )

          assertEquals(
              DataOutcome.Success(Unit),
              repository.insert(movimiento("past", "2024-02-29")),
          )
          runCurrent()
          assertEquals(DataOutcome.Success(listOf(first)), rangeEmissions.last())
          assertEquals(DataOutcome.Success(listOf(2026, 2024)), yearEmissions.last())

          assertEquals(DataOutcome.Success(Unit), repository.delete(first.id))
          runCurrent()
          assertEquals(DataOutcome.Success(emptyList()), rangeEmissions.last())
          assertEquals(DataOutcome.Success(listOf(2024)), yearEmissions.last())
          assertEquals(DataOutcome.Success(Unit), repository.delete("past"))
          runCurrent()
          assertEquals(DataOutcome.Success(emptyList()), yearEmissions.last())

          rangeJob.cancelAndJoin()
          yearsJob.cancelAndJoin()
          assertEquals(0, driver.listeners)
          val counts = rangeEmissions.size to yearEmissions.size
          assertEquals(DataOutcome.Success(Unit), repository.insert(first.copy(id = "again")))
          runCurrent()
          assertEquals(counts, rangeEmissions.size to yearEmissions.size)
          assertTrue(rangeEmissions.all { it is DataOutcome.Success })
          assertTrue(yearEmissions.all { it is DataOutcome.Success })
        } finally {
          driver.close()
        }
      }

  @Test
  fun `construction errors become local outcomes and the same cold flow can be resubscribed`() =
      runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
          createSchemaWithReferences(driver, listOf("food"))
          val real = TallybookDatabase(driver)
          var unavailable = true
          val database =
              object : TallybookDatabase by real {
                override val movimientoQueries: MovimientoQueries
                  get() = if (unavailable) error("storage unavailable") else real.movimientoQueries

                override val transferenciaQueries: TransferenciaQueries
                  get() =
                      if (unavailable) error("storage unavailable") else real.transferenciaQueries
              }
          val repository = HistoryReads(database, StandardTestDispatcher(testScheduler))
          val range = repository.observeRange(Moneda.COP, september)
          val years = repository.observeYears(Moneda.COP)
          assertIs<DataError.Local>(assertIs<DataOutcome.Failure>(range.first()).error)
          assertIs<DataError.Local>(assertIs<DataOutcome.Failure>(years.first()).error)

          unavailable = false
          assertEquals(DataOutcome.Success(emptyList()), range.first())
          assertEquals(DataOutcome.Success(emptyList()), years.first())
        }
      }

  @Test
  fun `construction cancellation is rethrown rather than mapped to local failure`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      createSchemaWithReferences(driver, listOf("food"))
      val real = TallybookDatabase(driver)
      val database =
          object : TallybookDatabase by real {
            override val movimientoQueries: MovimientoQueries
              get() = throw CancellationException("cancelled")

            override val transferenciaQueries: TransferenciaQueries
              get() = throw CancellationException("cancelled")
          }
      val repository = HistoryReads(database, StandardTestDispatcher(testScheduler))
      assertFailsWith<CancellationException> {
        repository.observeRange(Moneda.COP, september).first()
      }
      assertFailsWith<CancellationException> { repository.observeYears(Moneda.COP).first() }
    }
  }

  @Test
  fun `closed storage emits local failure for both history reads`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    createSchemaWithReferences(driver, listOf("food"))
    val repository = HistoryReads(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
    driver.close()

    assertIs<DataError.Local>(
        assertIs<DataOutcome.Failure>(repository.observeRange(Moneda.COP, september).first()).error
    )
    assertIs<DataError.Local>(
        assertIs<DataOutcome.Failure>(repository.observeYears(Moneda.COP).first()).error
    )
  }

  @Test
  fun `invalid selected rows fail the entire read without partial success`() = runTest {
    val corruptRows =
        listOf(
            Triple(0L, "GASTO", "2026-09-15"),
            Triple(-1L, "GASTO", "2026-09-15"),
            Triple(100L, "unknown", "2026-09-15"),
            Triple(100L, "GASTO", "2026-09-31"),
        )
    for ((amount, tipo, date) in corruptRows) {
      JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
        createSchemaWithReferences(driver, listOf("food"))
        val database = TallybookDatabase(driver)
        val repository = HistoryReads(database, StandardTestDispatcher(testScheduler))
        repository.insert(movimiento("valid", "2026-09-01"))
        database.movimientoQueries.insertMovimiento(
            "invalid",
            tipo,
            amount,
            date,
            null,
            null,
            "2026-09-15T12:00:00Z",
            "food",
            "cuenta_efectivo_cop",
            "EFECTIVO",
            ChangeStamp.SEED.value,
        )
        val wholeYear = MovimientoDateRange(LocalDate(2026, 1, 1), LocalDate(2026, 12, 31))
        val failure =
            assertIs<DataOutcome.Failure>(repository.observeRange(Moneda.COP, wholeYear).first())
        assertIs<IllegalArgumentException>(assertIs<DataError.Local>(failure.error).cause)
        assertIs<DataOutcome.Failure>(repository.getAll())
      }
    }
  }

  @Test
  fun `malformed year metadata fails without inventing a zero or ignoring the invalid row`() =
      runTest {
        for (date in
            listOf("not-a-date", "2026x-09-15", "2026", "02026-09-15", "+1000000000-01-01")) {
          JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            createSchemaWithReferences(driver, listOf("food"))
            val database = TallybookDatabase(driver)
            val repository = HistoryReads(database, StandardTestDispatcher(testScheduler))
            repository.insert(movimiento("valid", "2025-01-01"))
            database.movimientoQueries.insertMovimiento(
                "invalid",
                "GASTO",
                100,
                date,
                null,
                null,
                "2026-09-15T12:00:00Z",
                "food",
                "cuenta_efectivo_cop",
                "EFECTIVO",
                ChangeStamp.SEED.value,
            )

            assertIs<DataError.Local>(
                assertIs<DataOutcome.Failure>(repository.observeYears(Moneda.COP).first()).error
            )
            assertEquals(
                DataOutcome.Success(emptyList()),
                repository.observeYears(Moneda.USD).first(),
            )
          }
        }
      }

  @Test
  fun `year metadata validates only the canonical year and does not deserialize movimiento fields`() =
      runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
          createSchemaWithReferences(driver, listOf("food"))
          val database = TallybookDatabase(driver)
          database.movimientoQueries.insertMovimiento(
              "invalid-row",
              "unknown",
              -1,
              "2026-99-99",
              null,
              null,
              "not-an-instant",
              "food",
              "cuenta_efectivo_cop",
              "EFECTIVO",
              ChangeStamp.SEED.value,
          )
          val repository = HistoryReads(database, StandardTestDispatcher(testScheduler))

          assertEquals(
              DataOutcome.Success(listOf(2026)),
              repository.observeYears(Moneda.COP).first(),
          )
          assertIs<DataOutcome.Failure>(repository.getAll())
        }
      }

  @Test
  fun `active range mapping failure unregisters its listener and retry reads recovered storage`() =
      runTest {
        val actualDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val driver = TrackingDriver(actualDriver)
        try {
          createSchemaWithReferences(driver, listOf("food"))
          val database = TallybookDatabase(driver)
          val repository = HistoryReads(database, StandardTestDispatcher(testScheduler))
          val flow = repository.observeRange(Moneda.COP, september)
          val emissions = mutableListOf<DataOutcome<List<Movimiento>>>()
          val job = backgroundScope.launch { flow.collect(emissions::add) }
          runCurrent()
          assertEquals(2, driver.listeners)

          database.movimientoQueries.insertMovimiento(
              "invalid",
              "GASTO",
              0,
              "2026-09-15",
              null,
              null,
              "2026-09-15T12:00:00Z",
              "food",
              "cuenta_efectivo_cop",
              "EFECTIVO",
              ChangeStamp.SEED.value,
          )
          runCurrent()
          assertEquals(2, emissions.size)
          assertEquals(DataOutcome.Success(emptyList()), emissions.first())
          assertIs<DataError.Local>(assertIs<DataOutcome.Failure>(emissions.last()).error)
          assertTrue(job.isCompleted)
          assertEquals(0, driver.listeners)

          database.movimientoQueries.tombstoneMovimiento(ChangeStamp.SEED.value, "invalid")
          assertEquals(DataOutcome.Success(emptyList()), flow.first())
          runCurrent()
          assertEquals(0, driver.listeners)
        } finally {
          driver.close()
        }
      }

  @Test
  fun `ten thousand selected month rows remain complete with indexed plans and cancellable reads`() =
      runTest {
        val actualDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val driver = TrackingDriver(actualDriver)
        try {
          createSchemaWithReferences(driver, listOf("food"))
          val database = TallybookDatabase(driver)
          database.transaction {
            repeat(10000) { index ->
              val day = (index % 30 + 1).toString().padStart(2, '0')
              database.movimientoQueries.insertMovimiento(
                  "cop-${index.toString().padStart(5, '0')}",
                  "GASTO",
                  100,
                  "2026-09-$day",
                  "Lunch",
                  null,
                  "2026-09-15T12:00:00Z",
                  "food",
                  "cuenta_efectivo_cop",
                  "EFECTIVO",
                  ChangeStamp.SEED.value,
              )
            }
            repeat(4000) { index ->
              database.movimientoQueries.insertMovimiento(
                  "usd-$index",
                  "GASTO",
                  100,
                  "2026-09-15",
                  null,
                  null,
                  "2026-09-15T12:00:00Z",
                  "food",
                  "cuenta_efectivo_usd",
                  "EFECTIVO",
                  ChangeStamp.SEED.value,
              )
              database.movimientoQueries.insertMovimiento(
                  "outside-$index",
                  "GASTO",
                  100,
                  if (index % 2 == 0) "2026-08-31" else "2026-10-01",
                  null,
                  null,
                  "2026-09-15T12:00:00Z",
                  "food",
                  "cuenta_efectivo_cop",
                  "EFECTIVO",
                  ChangeStamp.SEED.value,
              )
            }
          }
          val repository = HistoryReads(database, StandardTestDispatcher(testScheduler))
          val started = TimeSource.Monotonic.markNow()
          val rows =
              assertIs<DataOutcome.Success<List<Movimiento>>>(
                      repository.observeRange(Moneda.COP, september).first()
                  )
                  .data
          val elapsed = started.elapsedNow()
          val rangeRead = driver.reads.last { it.sql.trimStart().startsWith(MOVIMIENTO_ROWS) }
          assertEquals(10000, rows.size)
          assertEquals(10000, rows.map { it.id }.toSet().size)
          assertEquals("cop-09989", rows.first().id)
          assertEquals("cop-00000", rows.last().id)
          assertTrue(
              rows.all {
                it.monto.moneda == Moneda.COP &&
                    it.fecha >= september.start &&
                    it.fecha <= september.end
              }
          )

          val rangePlan = driver.plan(rangeRead)
          assertTrue(rangePlan.any { "movimiento_recent" in it }, "$rangePlan")
          assertTrue(rangePlan.none { "TEMP B-TREE FOR ORDER BY" in it }, "$rangePlan")
          val yearStarted = TimeSource.Monotonic.markNow()
          assertEquals(
              DataOutcome.Success(listOf(2026)),
              repository.observeYears(Moneda.COP).first(),
          )
          val yearElapsed = yearStarted.elapsedNow()
          val yearPlan = driver.plan(driver.reads.last())
          // Tombstones and other currencies stay out of the years read: live movimientos are
          // reached
          // through one of their partial indexes, never a scan of the whole table.
          assertTrue(
              yearPlan.any { it.matches(Regex("(SCAN|SEARCH) movimiento USING .*INDEX.*")) },
              "$yearPlan",
          )
          assertTrue(yearPlan.none { it == "SCAN movimiento" }, "$yearPlan")
          println(
              "History repository fixture: selected=10000 excludedCurrency=4000 excludedDate=4000 read=$elapsed years=$yearElapsed rangePlan=$rangePlan yearPlan=$yearPlan"
          )

          val emissions = mutableListOf<DataOutcome<List<Movimiento>>>()
          lateinit var collector: Job
          driver.onRow = { visited -> if (visited == 10) collector.cancel() }
          collector = backgroundScope.launch {
            repository.observeRange(Moneda.COP, september).collect(emissions::add)
          }
          runCurrent()
          assertTrue(collector.isCancelled)
          assertEquals(emptyList(), emissions)
          assertEquals(0, driver.listeners)
        } finally {
          driver.close()
        }
      }

  private class TrackingDriver(private val delegate: SqlDriver) : SqlDriver by delegate {
    var listeners = 0
      private set

    val reads = mutableListOf<Read>()

    var onRow: ((visited: Int) -> Unit)? = null
    private var visitedRows = 0

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> {
      if (sql.trimStart().startsWith("SELECT")) reads += Read(sql, parameters, binders)
      visitedRows = 0
      return delegate.executeQuery(
          identifier,
          sql,
          { cursor ->
            mapper(
                object : SqlCursor by cursor {
                  override fun next(): QueryResult<Boolean> {
                    val next = cursor.next()
                    if (next.value) {
                      visitedRows += 1
                      onRow?.invoke(visitedRows)
                    }
                    return next
                  }
                }
            )
          },
          parameters,
          binders,
      )
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
      listeners += 1
    }

    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {
      delegate.removeListener(*queryKeys, listener = listener)
      listeners -= 1
    }
  }

  private data class Read(
      val sql: String,
      val parameters: Int,
      val binders: (SqlPreparedStatement.() -> Unit)?,
  )

  private fun movimiento(
      id: String,
      fecha: String,
      moneda: Moneda = Moneda.COP,
      createdAt: String = "2026-09-15T12:00:00Z",
  ): Movimiento =
      Movimiento(
          id = id,
          tipo = TipoMovimiento.GASTO,
          monto = Money(100, moneda),
          fecha = LocalDate.parse(fecha),
          categoriaId = "food",
          titulo = "Lunch",
          descripcion = null,
          createdAt = Instant.parse(createdAt),
          cuentaId = Cuenta.defaultCashId(moneda),
          medio = MedioPago.EFECTIVO,
      )

  private companion object {
    val september = MovimientoDateRange(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30))
    const val MOVIMIENTO_ROWS = "SELECT movimiento_con_etiquetas.id"
  }
}

/**
 * History's reads through [ActividadRepositoryImpl], narrowed to movimientos because no fixture
 * here stores a transfer, with the writes the tests need.
 */
private class HistoryReads(database: TallybookDatabase, dispatcher: CoroutineDispatcher) {
  private val writer = MovimientoRepositoryImpl(database, dispatcher, TestChangeClock())
  private val reader = ActividadRepositoryImpl(database, dispatcher)

  suspend fun insert(movimiento: Movimiento): DataOutcome<Unit> = writer.insert(movimiento)

  suspend fun delete(id: String): DataOutcome<Unit> = writer.deleteAndCapture(id).map {}

  suspend fun getAll(): DataOutcome<List<Movimiento>> = writer.getAll()

  fun observeRange(
      moneda: Moneda,
      range: MovimientoDateRange,
  ): Flow<DataOutcome<List<Movimiento>>> =
      reader.observeActividadRange(moneda, range).map { outcome ->
        outcome.map { rows -> rows.map { assertIs<Actividad.DeMovimiento>(it).movimiento } }
      }

  fun observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>> = reader.observeYears(moneda)
}
