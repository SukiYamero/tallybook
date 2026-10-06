package com.kurobello.tallybook.core.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class ReactiveRepositoryTest {

  @Test
  fun `observe recent emits an initial list and another list after insert`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    val repository =
        MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
    val emissions = Channel<DataOutcome<List<Movimiento>>>(Channel.UNLIMITED)
    val collection = launch {
      repository.observeRecent(limit = 10).take(2).collect(emissions::send)
    }

    testScheduler.advanceUntilIdle()
    assertEquals(DataOutcome.Success(emptyList()), emissions.receive())

    assertEquals(DataOutcome.Success(Unit), repository.insert(movimiento("first")))

    assertEquals(DataOutcome.Success(listOf(movimiento("first"))), emissions.receive())
    collection.join()
    driver.close()
  }

  @Test
  fun `scoped recent bounds currency before limit mapping and reacts to inserts and deletes`() =
      runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
          TallybookDatabase.Schema.create(driver)
          val database = TallybookDatabase(driver)
          val repository = MovimientoRepositoryImpl(database, StandardTestDispatcher(testScheduler))
          val flow = repository.observeRecent(Moneda.COP, 2)
          val emissions = mutableListOf<DataOutcome<List<Movimiento>>>()
          val collector = backgroundScope.launch { flow.collect(emissions::add) }
          testScheduler.runCurrent()
          assertEquals(1, emissions.size)
          assertEquals(DataOutcome.Success(emptyList<Movimiento>()), emissions.single())
          val first = movimiento("a", monto = Money(100, Moneda.COP))
          val second = movimiento("b", monto = Money(100, Moneda.COP))
          val third =
              movimiento("c", monto = Money(100, Moneda.COP))
                  .copy(createdAt = first.createdAt + kotlin.time.Duration.parse("1s"))
          for (row in listOf(first, second, third)) assertEquals(
              DataOutcome.Success(Unit),
              repository.insert(row),
          )
          database.movimientoQueries.insertMovimiento(
              "foreign-corrupt",
              "INVALID_TYPE",
              100,
              "USD",
              "2026-09-16",
              "cat_comida",
              null,
              "invalid-instant",
          )
          testScheduler.runCurrent()
          assertEquals(DataOutcome.Success(listOf(third, second)), emissions.last())
          assertIs<DataOutcome.Failure>(repository.observeRecent(10).first())
          assertEquals(DataOutcome.Success(Unit), repository.delete(third.id))
          testScheduler.runCurrent()
          assertEquals(DataOutcome.Success(listOf(second, first)), emissions.last())
          database.movimientoQueries.insertMovimiento(
              "selected-corrupt",
              "GASTO",
              0,
              "COP",
              "2026-09-16",
              "cat_comida",
              null,
              first.createdAt.toString(),
          )
          testScheduler.runCurrent()
          assertIs<DataError.Local>(assertIs<DataOutcome.Failure>(emissions.last()).error)
          assertTrue(collector.isCompleted)
          assertEquals(DataOutcome.Success(Unit), repository.delete("selected-corrupt"))
          assertEquals(DataOutcome.Success(listOf(second, first)), flow.first())
        }
      }

  @Test
  fun `scoped recent rejects invalid limits before reading and wraps closed storage`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    val repository =
        MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
    driver.close()
    for (limit in listOf(0, -1)) {
      assertIs<DataError.InvalidInput>(
          assertIs<DataOutcome.Failure>(repository.observeRecent(Moneda.COP, limit).first()).error
      )
    }
    assertIs<DataError.Local>(
        assertIs<DataOutcome.Failure>(repository.observeRecent(Moneda.COP, 2).first()).error
    )
  }

  @Test
  fun `observe all categorias emits an initial list and another list after seeding`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    val repository =
        CategoriaRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
    val categoria =
        Categoria("cat_comida", "Comida", null, CategoryIcon.UTENSILS, CategoryTint.AMBER)
    val emissions = Channel<DataOutcome<List<Categoria>>>(Channel.UNLIMITED)
    val collection = launch { repository.observeAll().take(2).collect(emissions::send) }

    testScheduler.advanceUntilIdle()
    assertEquals(DataOutcome.Success(emptyList()), emissions.receive())

    assertEquals(DataOutcome.Success(Unit), repository.ensureSeeded(listOf(categoria)))

    assertEquals(DataOutcome.Success(listOf(categoria)), emissions.receive())
    collection.join()
    driver.close()
  }

  @Test
  fun `observe recent rejects a nonpositive limit without starting a query`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    val repository =
        MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))

    val outcome = repository.observeRecent(limit = 0).toList()

    assertIs<DataError.InvalidInput>(assertIs<DataOutcome.Failure>(outcome.single()).error)
    driver.close()
  }

  @Test
  fun `observe summary returns zeroes for a moneda without movimientos`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    val repository =
        MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))

    assertEquals(
        DataOutcome.Success(
            MovimientoSummary(ingresos = Money(0, Moneda.COP), gastos = Money(0, Moneda.COP))
        ),
        repository.observeSummary(Moneda.COP).first(),
    )
    driver.close()
  }

  @Test
  fun `observe summary separates gastos and ingresos in the requested moneda`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    val repository =
        MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
    repository.insert(movimiento("gasto", TipoMovimiento.GASTO, Money(500, Moneda.COP)))
    repository.insert(movimiento("ingreso", TipoMovimiento.INGRESO, Money(2_000, Moneda.COP)))
    repository.insert(movimiento("other", TipoMovimiento.INGRESO, Money(9_999, Moneda.USD)))

    assertEquals(
        DataOutcome.Success(
            MovimientoSummary(ingresos = Money(2_000, Moneda.COP), gastos = Money(500, Moneda.COP))
        ),
        repository.observeSummary(Moneda.COP).first(),
    )
    driver.close()
  }

  @Test
  fun `observe recent maps a closed storage driver to a local error`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    val repository =
        MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
    driver.close()

    assertIs<DataError.Local>(
        assertIs<DataOutcome.Failure>(repository.observeRecent(10).first()).error
    )
  }

  @Test
  fun `cancelling an active recent collector does not emit a local failure`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    val repository =
        MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
    val emissions = Channel<DataOutcome<List<Movimiento>>>(Channel.UNLIMITED)
    val collection = launch { repository.observeRecent(10).collect(emissions::send) }

    testScheduler.advanceUntilIdle()
    assertEquals(DataOutcome.Success(emptyList()), emissions.receive())
    collection.cancelAndJoin()

    assertTrue(emissions.tryReceive().isFailure)
    driver.close()
  }

  private fun movimiento(
      id: String,
      tipo: TipoMovimiento = TipoMovimiento.GASTO,
      monto: Money = Money(1_050, Moneda.USD),
  ) =
      Movimiento(
          id = id,
          tipo = tipo,
          monto = monto,
          fecha = LocalDate(2026, 9, 3),
          categoriaId = "cat_comida",
          descripcion = "almuerzo",
          createdAt = Instant.parse("2026-09-03T12:00:00Z"),
      )
}
