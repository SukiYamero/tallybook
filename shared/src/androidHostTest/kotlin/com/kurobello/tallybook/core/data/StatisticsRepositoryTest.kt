package com.kurobello.tallybook.core.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryStatisticsRequest
import com.kurobello.tallybook.core.model.CategoryStatisticsSnapshot
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.DayGasto
import com.kurobello.tallybook.core.model.DimensionTotal
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MonthTotals
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.SearchTipo
import com.kurobello.tallybook.core.model.StatisticsKey
import com.kurobello.tallybook.core.model.StatisticsRequest
import com.kurobello.tallybook.core.model.StatisticsScope
import com.kurobello.tallybook.core.model.StatisticsSnapshot
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.Transferencia
import com.kurobello.tallybook.core.model.statisticsRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.daysUntil
import kotlinx.datetime.yearMonth

class StatisticsRepositoryTest {

  @Test
  fun `the snapshot counts live gastos once per currency and adds categorias up to the total`() =
      runTest {
        withRepository { database, repository ->
          database.seedSeptember()

          val snapshot = success(repository.observe(request()).first())

          assertEquals(summary(ingresos = 10_000, gastos = 4_200), snapshot.current)
          assertEquals(summary(ingresos = 0, gastos = 700), snapshot.previous)
          assertEquals(
              listOf(
                  total(StatisticsKey.Categoria(FOOD), 3_500, 300),
                  total(StatisticsKey.Categoria(MISSING), 700, 0),
                  total(StatisticsKey.Categoria(FUN), 0, 400),
              ),
              snapshot.categorias,
          )
          assertEquals(
              snapshot.current.gastos,
              snapshot.categorias.sumMoney(DimensionTotal::current),
          )
          assertEquals(
              listOf(
                  total(StatisticsKey.Etiqueta(TRIP), 1_000, 0),
                  total(StatisticsKey.Etiqueta(WORK), 1_000, 0),
              ),
              snapshot.etiquetas,
          )
          assertEquals(
              listOf(
                  total(StatisticsKey.Cuenta(NEQUI), 2_500, 0),
                  total(StatisticsKey.Cuenta(COP_CASH), 1_700, 700),
              ),
              snapshot.cuentas,
          )
          assertEquals(
              listOf(
                  total(StatisticsKey.Medio(MedioPago.DEBITO), 2_500, 0),
                  total(StatisticsKey.Medio(MedioPago.EFECTIVO), 1_700, 700),
              ),
              snapshot.medios,
          )
          assertTrue(snapshot.hasAnyMovimiento)

          val usd = success(repository.observe(request(Moneda.USD)).first())
          assertEquals(summary(ingresos = 0, gastos = 50_000, Moneda.USD), usd.current)
          assertEquals(
              listOf(total(StatisticsKey.Categoria(FOOD), 50_000, 0, Moneda.USD)),
              usd.categorias,
          )
          assertEquals(
              false,
              success(repository.observe(request(Moneda.ARS)).first()).hasAnyMovimiento,
          )
        }
      }

  @Test
  fun `months and days without movimientos are zero filled`() = runTest {
    withRepository { database, repository ->
      database.seedSeptember()

      val snapshot = success(repository.observe(request()).first())

      assertEquals(
          (4..7).map { MonthTotals(YearMonth(2026, it), summary(0, 0)) } +
              MonthTotals(YearMonth(2026, 8), summary(ingresos = 0, gastos = 700)) +
              MonthTotals(YearMonth(2026, 9), summary(ingresos = 10_000, gastos = 4_200)),
          snapshot.months,
      )
      assertEquals(30, snapshot.daily.size)
      assertEquals(
          (1..30).map { day ->
            val gasto =
                when (day) {
                  5 -> 3_500L
                  10 -> 700L
                  else -> 0L
                }
            DayGasto(LocalDate(2026, 9, day), cop(gasto))
          },
          snapshot.daily,
      )
    }
  }

  @Test
  fun `an empty comparison stretch reads as zero and no comparison as null`() = runTest {
    withRepository { database, repository ->
      database.seedSeptember()
      val january = MovimientoDateRange(LocalDate(2026, 1, 1), LocalDate(2026, 1, 31))

      val empty = success(repository.observe(request().copy(comparison = january)).first())
      assertEquals(summary(0, 0), empty.previous)
      assertTrue(empty.categorias.all { it.previous == cop(0) })
      assertEquals(
          listOf(FOOD, MISSING),
          empty.categorias.map { (it.key as StatisticsKey.Categoria).id },
      )

      val none = success(repository.observe(request().copy(comparison = null)).first())
      assertEquals(null, none.previous)
      assertTrue(
          (none.categorias + none.etiquetas + none.cuentas + none.medios).all {
            it.previous == null
          }
      )
    }
  }

  @Test
  fun `a write while observing emits a new coherent snapshot`() = runTest {
    withRepository { database, repository ->
      database.seedSeptember()
      val emissions = Channel<StatisticsSnapshot>(Channel.UNLIMITED)
      val collector = launch {
        repository.observe(request()).collect { emissions.send(success(it)) }
      }
      assertEquals(cop(4_200), emissions.receive().current.gastos)

      database.insertLegacy(gasto("late", 800, "2026-09-20", FUN))

      val updated = emissions.receive()
      assertEquals(cop(5_000), updated.current.gastos)
      assertEquals(updated.current.gastos, updated.categorias.sumMoney(DimensionTotal::current))
      assertEquals(
          listOf(
              total(StatisticsKey.Categoria(FOOD), 3_500, 300),
              total(StatisticsKey.Categoria(FUN), 800, 400),
              total(StatisticsKey.Categoria(MISSING), 700, 0),
          ),
          updated.categorias,
      )
      assertEquals(cop(800), updated.daily.single { it.fecha == LocalDate(2026, 9, 20) }.gasto)
      collector.cancelAndJoin()
    }
  }

  @Test
  fun `category detail rolls children into the root and counts what Search lists`() = runTest {
    withRepository { database, repository ->
      database.seedSeptember()

      val detail = success(repository.observeCategory(categoryRequest(FOOD)).first())

      assertEquals(cop(3_500), detail.current)
      assertEquals(cop(300), detail.previous)
      assertEquals(
          listOf(
              total(StatisticsKey.Categoria(FOOD_OUT), 2_500, 0),
              total(StatisticsKey.Categoria(FOOD), 1_000, 300),
          ),
          detail.subcategorias,
      )
      assertEquals(
          (4..7).map { YearMonth(2026, it) to cop(0) } +
              (YearMonth(2026, 8) to cop(300)) +
              (YearMonth(2026, 9) to cop(3_500)),
          detail.months,
      )
      assertEquals(listOf("dinner", "lunch"), detail.largest.map(Movimiento::id))
      assertEquals(listOf(TRIP, WORK), detail.largest.last().etiquetaIds)

      val search = ActividadRepositoryImpl(database, StandardTestDispatcher(testScheduler))
      for (rootId in listOf(FOOD, MISSING, FUN)) {
        val count = success(repository.observeCategory(categoryRequest(rootId)).first()).count
        val rows =
            success(
                    search
                        .observeFiltered(
                            MovimientoReadCriteria(
                                Moneda.COP,
                                september,
                                SearchTipo.GASTO,
                                categoryIds = setOf(rootId),
                            )
                        )
                        .first()
                )
                .actividades
        assertEquals(rows.size.toLong(), count, rootId)
      }
      assertEquals(2L, detail.count)
    }
  }

  @Test
  fun `the largest gastos break ties by newer fecha then id`() = runTest {
    withRepository { database, repository ->
      for ((id, monto, fecha) in
          listOf(
              Triple("a", 500L, "2026-09-03"),
              Triple("c", 500L, "2026-09-07"),
              Triple("b", 500L, "2026-09-07"),
              Triple("d", 900L, "2026-09-01"),
              Triple("e", 100L, "2026-09-09"),
          )) {
        database.insertLegacy(gasto(id, monto, fecha, FOOD))
      }

      val detail: CategoryStatisticsSnapshot =
          success(repository.observeCategory(categoryRequest(FOOD)).first())

      assertEquals(listOf("d", "b", "c"), detail.largest.map(Movimiento::id))
      assertEquals(5L, detail.count)
    }
  }

  @Test
  fun `each emission reads in one transaction`() = runTest {
    val driver = TransactionTrackingDriver(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY))
    try {
      val database = database(driver)
      database.seedSeptember()
      val repository = StatisticsRepositoryImpl(database, StandardTestDispatcher(testScheduler))

      for (read in
          listOf<suspend () -> Any>(
              { success(repository.observe(request()).first()) },
              { success(repository.observeCategory(categoryRequest(FOOD)).first()) },
          )) {
        driver.readTransactions.clear()
        read()
        assertTrue(driver.readTransactions.size > 1)
        assertTrue(driver.readTransactions.first() != null)
        assertTrue(driver.readTransactions.all { it === driver.readTransactions.first() })
      }
    } finally {
      driver.close()
    }
  }

  @Test
  fun `invalid requests never access storage`() = runTest {
    withRepository { _, repository ->
      val longDaily = MovimientoDateRange(LocalDate(2025, 1, 1), LocalDate(2026, 1, 2))
      val midMonthTrend = MovimientoDateRange(LocalDate(2026, 4, 2), LocalDate(2026, 9, 30))
      for (outcome in
          listOf(
              repository.observe(request().copy(daily = longDaily)).first(),
              repository.observe(request().copy(trend = midMonthTrend)).first(),
              repository.observeCategory(categoryRequest(" ")).first(),
              repository.observeCategory(categoryRequest(FOOD).copy(trend = midMonthTrend)).first(),
          )) {
        assertIs<DataError.InvalidInput>(assertIs<DataOutcome.Failure>(outcome).error)
      }
    }
  }

  @Test
  fun `every request the ranges build passes validation`() = runTest {
    withRepository { database, repository ->
      database.seedSeptember()
      val leapYearEnd = LocalDate(2028, 12, 31)
      for ((scope, anchor, today) in
          listOf(
              Triple(StatisticsScope.Month, LocalDate(2026, 9, 30), LocalDate(2026, 10, 24)),
              Triple(StatisticsScope.Month, LocalDate(2026, 10, 24), LocalDate(2026, 10, 24)),
              Triple(StatisticsScope.Year, LocalDate(2026, 9, 30), LocalDate(2026, 10, 24)),
              Triple(StatisticsScope.Year, LocalDate(2028, 2, 29), leapYearEnd),
              Triple(StatisticsScope.Year, LocalDate(2027, 6, 1), leapYearEnd),
          )) {
        val request = statisticsRequest(scope, anchor, Moneda.COP, today)

        val snapshot = success(repository.observe(request).first())

        assertEquals(request.daily.start.daysUntil(request.daily.end) + 1, snapshot.daily.size)
        assertEquals(
            (request.trend.start.yearMonth..request.trend.end.yearMonth).count(),
            snapshot.months.size,
        )
        assertIs<DataOutcome.Success<CategoryStatisticsSnapshot>>(
            repository
                .observeCategory(
                    CategoryStatisticsRequest(
                        Moneda.COP,
                        FOOD,
                        request.period,
                        request.comparison,
                        request.trend,
                    )
                )
                .first()
        )
      }
    }
  }

  private suspend fun TestScope.withRepository(
      block: suspend (TallybookDatabase, StatisticsRepository) -> Unit
  ) {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    try {
      val database = database(driver)
      block(database, StatisticsRepositoryImpl(database, StandardTestDispatcher(testScheduler)))
    } finally {
      driver.close()
    }
  }

  private fun database(driver: SqlDriver): TallybookDatabase =
      createSchemaWithReferences(driver, listOf(FOOD, FUN, INCOME), listOf(TRIP, WORK)).apply {
        transaction {
          categoriaQueries.insertCategoria(
              FOOD_OUT,
              FOOD_OUT,
              FOOD,
              CategoryIcon.TAG.storageKey,
              CategoryTint.NEUTRAL.storageKey,
              ChangeStamp.SEED.value,
          )
          insertCuentaRow(
              Cuenta(
                  NEQUI,
                  NEQUI,
                  TipoCuenta.BANCO,
                  cop(0),
                  null,
                  false,
                  Instant.parse("2026-01-02T00:00:00Z"),
                  color = TipoCuenta.BANCO.defaultColor,
              ),
              ChangeStamp.SEED,
          )
        }
      }

  /**
   * September has a two-tag gasto, a child-category gasto, a gasto whose categoria row does not
   * exist, an ingreso, a tombstoned gasto, a transfer and a USD gasto; August has gasto in a
   * categoria September lacks.
   */
  private fun TallybookDatabase.seedSeptember() {
    insertLegacy(gasto("lunch", 1_000, "2026-09-05", FOOD, etiquetaIds = listOf(TRIP, WORK)))
    insertLegacy(
        gasto("dinner", 2_500, "2026-09-05", FOOD_OUT, cuentaId = NEQUI, medio = MedioPago.DEBITO)
    )
    insertLegacy(gasto(MISSING_GASTO, 700, "2026-09-10", MISSING))
    insertLegacy(gasto("salary", 10_000, "2026-09-01", INCOME, tipo = TipoMovimiento.INGRESO))
    insertLegacy(gasto("deleted", 9_999, "2026-09-12", FOOD))
    movimientoQueries.tombstoneMovimiento(TOMBSTONE, "deleted")
    insertTransferenciaRow(
        Transferencia(
            id = "move",
            cuentaOrigenId = COP_CASH,
            cuentaDestinoId = NEQUI,
            montoOrigen = cop(5_000),
            montoDestino = cop(5_000),
            fecha = LocalDate(2026, 9, 15),
            descripcion = null,
            createdAt = Instant.parse("2026-09-15T12:00:00Z"),
        ),
        ChangeStamp.SEED,
    )
    insertLegacy(gasto("usd", 50_000, "2026-09-05", FOOD, moneda = Moneda.USD))
    insertLegacy(gasto("august-fun", 400, "2026-08-20", FUN))
    insertLegacy(gasto("august-food", 300, "2026-08-21", FOOD))
  }

  /** Records the transaction each read runs in. */
  private class TransactionTrackingDriver(private val delegate: SqlDriver) : SqlDriver by delegate {
    val readTransactions = mutableListOf<Any?>()

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> {
      if (sql.trimStart().startsWith("SELECT")) readTransactions += currentTransaction()
      return delegate.executeQuery(identifier, sql, mapper, parameters, binders)
    }
  }

  private fun <T> success(outcome: DataOutcome<T>): T =
      assertIs<DataOutcome.Success<T>>(outcome).data

  private fun request(moneda: Moneda = Moneda.COP) =
      StatisticsRequest(
          moneda = moneda,
          period = september,
          comparison = august,
          trend = trend,
          daily = september,
      )

  private fun categoryRequest(rootId: String) =
      CategoryStatisticsRequest(
          moneda = Moneda.COP,
          rootId = rootId,
          period = september,
          comparison = august,
          trend = trend,
      )

  @Suppress("LongParameterList")
  private fun gasto(
      id: String,
      monto: Long,
      fecha: String,
      categoriaId: String,
      moneda: Moneda = Moneda.COP,
      tipo: TipoMovimiento = TipoMovimiento.GASTO,
      cuentaId: String = Cuenta.defaultCashId(moneda),
      medio: MedioPago = MedioPago.EFECTIVO,
      etiquetaIds: List<String> = emptyList(),
  ) =
      Movimiento(
          id = id,
          tipo = tipo,
          monto = Money(monto, moneda),
          fecha = LocalDate.parse(fecha),
          categoriaId = categoriaId,
          titulo = id,
          descripcion = null,
          createdAt = Instant.parse("${fecha}T12:00:00Z"),
          cuentaId = cuentaId,
          medio = medio,
          etiquetaIds = etiquetaIds,
      )

  private fun cop(minor: Long) = Money(minor, Moneda.COP)

  private fun summary(ingresos: Long, gastos: Long, moneda: Moneda = Moneda.COP) =
      MovimientoSummary(Money(ingresos, moneda), Money(gastos, moneda))

  private fun total(
      key: StatisticsKey,
      current: Long,
      previous: Long,
      moneda: Moneda = Moneda.COP,
  ) = DimensionTotal(key, Money(current, moneda), Money(previous, moneda))

  private fun List<DimensionTotal>.sumMoney(selector: (DimensionTotal) -> Money): Money =
      map(selector).reduce(Money::plus)

  private companion object {
    const val FOOD = "food"
    const val FOOD_OUT = "food-out"
    const val FUN = "fun"
    const val INCOME = "income"
    const val MISSING = "gone-categoria"
    const val MISSING_GASTO = "orphan"
    const val TRIP = "trip"
    const val WORK = "work"
    const val NEQUI = "nequi"
    val COP_CASH = Cuenta.defaultCashId(Moneda.COP)
    val TOMBSTONE = ChangeStamp.of(epochMillis = 1, counter = 0, node = "00000000000000aa").value
    val september = MovimientoDateRange(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30))
    val august = MovimientoDateRange(LocalDate(2026, 8, 1), LocalDate(2026, 8, 31))
    val trend = MovimientoDateRange(LocalDate(2026, 4, 1), LocalDate(2026, 9, 30))
  }
}
