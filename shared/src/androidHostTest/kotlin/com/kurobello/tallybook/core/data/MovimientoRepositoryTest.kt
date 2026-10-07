package com.kurobello.tallybook.core.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.MovimientoRestoreResult
import com.kurobello.tallybook.core.model.MovimientoUpdateResult
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class MovimientoRepositoryTest {

  @Test
  fun `insert rejects invalid movimiento input without writing`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    val repository =
        MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
    val valid = movimiento(id = "valid")
    val invalid =
        listOf(
            valid.copy(monto = Money(0, Moneda.COP)),
            valid.copy(id = ""),
            valid.copy(categoriaId = ""),
            valid.copy(descripcion = "a".repeat(MOVIMIENTO_DESCRIPTION_MAX_LENGTH + 1)),
        )

    invalid.forEach { movimiento ->
      assertIs<DataError.InvalidInput>(
          assertIs<DataOutcome.Failure>(repository.insert(movimiento)).error
      )
    }
    assertEquals(DataOutcome.Success(emptyList()), repository.getAll())
    driver.close()
  }

  @Test
  fun `insert and getAll round-trip a domain movimiento`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    val repository =
        MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
    val movimiento =
        Movimiento(
            id = "mov1",
            tipo = TipoMovimiento.GASTO,
            monto = Money(1_050, Moneda.USD),
            fecha = LocalDate(2026, 9, 3),
            categoriaId = "cat_comida",
            descripcion = "almuerzo",
            createdAt = Instant.parse("2026-09-03T12:00:00Z"),
        )

    assertEquals(DataOutcome.Success(Unit), repository.insert(movimiento))
    assertEquals(DataOutcome.Success(listOf(movimiento)), repository.getAll())

    driver.close()
  }

  @Test
  fun `insert maps a duplicate id to a local error`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    val repository =
        MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
    val movimiento =
        Movimiento(
            id = "duplicado",
            tipo = TipoMovimiento.INGRESO,
            monto = Money(25_000, Moneda.COP),
            fecha = LocalDate(2026, 9, 4),
            categoriaId = "cat_sueldo",
            descripcion = null,
            createdAt = Instant.parse("2026-09-04T12:00:00Z"),
        )

    assertIs<DataOutcome.Success<Unit>>(repository.insert(movimiento))
    val failure = assertIs<DataOutcome.Failure>(repository.insert(movimiento))

    assertIs<DataError.Local>(failure.error)
    assertTrue(failure.error.cause != null)
    driver.close()
  }

  @Test
  fun `delete removes the movimiento`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    val repository =
        MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
    val movimiento =
        Movimiento(
            id = "mov1",
            tipo = TipoMovimiento.GASTO,
            monto = Money(500, Moneda.MXN),
            fecha = LocalDate(2026, 9, 5),
            categoriaId = "cat_transporte",
            descripcion = null,
            createdAt = Instant.parse("2026-09-05T12:00:00Z"),
        )

    repository.insert(movimiento)

    assertEquals(DataOutcome.Success(Unit), repository.delete(movimiento.id))
    assertEquals(DataOutcome.Success(emptyList()), repository.getAll())
    driver.close()
  }

  @Test
  fun `getById distinguishes absent rows invalid ids and unreadable rows`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val database = TallybookDatabase(driver)
      val repository = MovimientoRepositoryImpl(database, StandardTestDispatcher(testScheduler))
      val row = movimiento("read", monto = Money(Long.MAX_VALUE, Moneda.CLP))
      assertEquals(DataOutcome.Success(Unit), repository.insert(row))
      assertEquals(DataOutcome.Success(row), repository.getById(row.id))
      assertEquals(DataOutcome.Success(null), repository.getById("absent"))
      for (id in listOf("", "  ")) assertInvalid(repository.getById(id))
      insertUnreadable(database, "unreadable")
      assertLocal(repository.getById("unreadable"))
    }
  }

  @Test
  fun `update replaces editable values while preserving identity currency creation and row count`() =
      runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
          TallybookDatabase.Schema.create(driver)
          val repository =
              MovimientoRepositoryImpl(
                  TallybookDatabase(driver),
                  StandardTestDispatcher(testScheduler),
              )
          val original = movimiento("edit", monto = Money(1_050, Moneda.USD))
          val sibling = original.copy(id = "sibling")
          repository.insert(original)
          repository.insert(sibling)
          val changes =
              MovimientoChanges(
                  TipoMovimiento.INGRESO,
                  Money(Long.MAX_VALUE, Moneda.USD),
                  LocalDate(2025, 12, 31),
                  "missing-legacy-category",
                  "updated",
              )
          assertEquals(
              DataOutcome.Success(MovimientoUpdateResult.Updated),
              repository.update(original.id, changes),
          )
          val expected =
              original.copy(
                  tipo = TipoMovimiento.INGRESO,
                  monto = Money(Long.MAX_VALUE, Moneda.USD),
                  fecha = LocalDate(2025, 12, 31),
                  categoriaId = "missing-legacy-category",
                  descripcion = "updated",
              )
          assertEquals(DataOutcome.Success(expected), repository.getById(original.id))
          assertEquals(DataOutcome.Success(listOf(sibling, expected)), repository.getAll())
          assertEquals(
              DataOutcome.Success(MovimientoUpdateResult.NotFound),
              repository.update("absent", changes),
          )
          assertEquals(DataOutcome.Success(listOf(sibling, expected)), repository.getAll())
          assertEquals(
              DataOutcome.Success(MovimientoUpdateResult.Updated),
              repository.update(original.id, changes.copy(descripcion = null)),
          )
          assertEquals(
              DataOutcome.Success(expected.copy(descripcion = null)),
              repository.getById(original.id),
          )
        }
      }

  @Test
  fun `update rejects invalid values without replacing the stored row`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val repository =
          MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
      val original = movimiento("valid")
      repository.insert(original)
      val valid = original.changes()
      for (changes in
          listOf(
              valid.copy(monto = Money(0, Moneda.COP)),
              valid.copy(monto = Money(-1, Moneda.COP)),
              valid.copy(monto = Money(100, Moneda.USD)),
              valid.copy(categoriaId = " "),
              valid.copy(descripcion = "a".repeat(MOVIMIENTO_DESCRIPTION_MAX_LENGTH + 1)),
          )) {
        assertInvalid(repository.update(original.id, changes))
        assertEquals(DataOutcome.Success(original), repository.getById(original.id))
      }
      for (id in listOf("", " ")) assertInvalid(repository.update(id, valid))
      assertEquals(DataOutcome.Success(listOf(original)), repository.getAll())
    }
  }

  @Test
  fun `capture deletes the latest committed row and repeated calls cannot capture it again`() =
      runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
          TallybookDatabase.Schema.create(driver)
          val repository =
              MovimientoRepositoryImpl(
                  TallybookDatabase(driver),
                  StandardTestDispatcher(testScheduler),
              )
          val original = movimiento("delete")
          repository.insert(original)
          val latest = original.copy(monto = Money(750, Moneda.COP), descripcion = "latest")
          repository.update(original.id, latest.changes())
          val captures =
              listOf(
                      async { repository.deleteAndCapture(original.id) },
                      async { repository.deleteAndCapture(original.id) },
                  )
                  .awaitAll()
          assertEquals(1, captures.count { it == DataOutcome.Success(latest) })
          assertEquals(1, captures.count { it == DataOutcome.Success(null) })
          assertEquals(DataOutcome.Success(emptyList()), repository.getAll())
          for (id in listOf("", " ")) assertInvalid(repository.deleteAndCapture(id))
        }
      }

  @Test
  fun `capture preserves an unreadable row and old delete still removes it idempotently`() =
      runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
          TallybookDatabase.Schema.create(driver)
          val database = TallybookDatabase(driver)
          val repository = MovimientoRepositoryImpl(database, StandardTestDispatcher(testScheduler))
          insertUnreadable(database, "corrupt")
          assertLocal(repository.deleteAndCapture("corrupt"))
          assertEquals(
              "corrupt",
              database.movimientoQueries.selectMovimientoById("corrupt").executeAsOne().id,
          )
          assertEquals(DataOutcome.Success(Unit), repository.delete("corrupt"))
          insertUnreadable(database, "")
          assertEquals(DataOutcome.Success(Unit), repository.delete(""))
          for (id in listOf("corrupt", "", " ", "absent")) assertEquals(
              DataOutcome.Success(Unit),
              repository.delete(id),
          )
          assertEquals(DataOutcome.Success(emptyList()), repository.getAll())
        }
      }

  @Test
  fun `delete and restore reproduce every stored field and stable tied order`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val repository =
          MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
      val first =
          movimiento(
              "a",
              monto = Money(Long.MAX_VALUE, Moneda.CLP),
              descripcion = "a".repeat(MOVIMIENTO_DESCRIPTION_MAX_LENGTH),
          )
      val second = first.copy(id = "b", descripcion = null)
      repository.insert(first)
      repository.insert(second)
      assertEquals(DataOutcome.Success(first), repository.deleteAndCapture(first.id))
      assertEquals(
          DataOutcome.Success(MovimientoRestoreResult.Restored),
          repository.restoreDeleted(first),
      )
      assertEquals(DataOutcome.Success(listOf(second, first)), repository.getAll())
      assertEquals(
          DataOutcome.Success(MovimientoRestoreResult.AlreadyPresent),
          repository.restoreDeleted(first),
      )
      val conflict = first.copy(monto = Money(100, Moneda.CLP))
      assertEquals(
          DataOutcome.Success(MovimientoRestoreResult.Conflict),
          repository.restoreDeleted(conflict),
      )
      assertEquals(DataOutcome.Success(listOf(second, first)), repository.getAll())
    }
  }

  @Test
  fun `restore rejects invalid snapshots without writing`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val repository =
          MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
      val valid = movimiento("restore")
      for (row in
          listOf(
              valid.copy(id = " "),
              valid.copy(categoriaId = " "),
              valid.copy(monto = Money(0, Moneda.COP)),
              valid.copy(descripcion = "a".repeat(MOVIMIENTO_DESCRIPTION_MAX_LENGTH + 1)),
          )) assertInvalid(repository.restoreDeleted(row))
      assertEquals(DataOutcome.Success(emptyList()), repository.getAll())
    }
  }

  @Test
  fun `mutation write failures roll back without yielding a false capture or restore`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val repository =
          MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
      val row = movimiento("blocked")
      repository.insert(row)
      driver.execute(
          null,
          "CREATE TRIGGER block_delete BEFORE DELETE ON movimiento BEGIN SELECT RAISE(ABORT, 'blocked'); END",
          0,
      )
      assertLocal(repository.deleteAndCapture(row.id))
      assertEquals(DataOutcome.Success(row), repository.getById(row.id))
      driver.execute(null, "DROP TRIGGER block_delete", 0)
      assertEquals(DataOutcome.Success(row), repository.deleteAndCapture(row.id))
      driver.execute(
          null,
          "CREATE TRIGGER block_insert BEFORE INSERT ON movimiento BEGIN SELECT RAISE(ABORT, 'blocked'); END",
          0,
      )
      assertLocal(repository.restoreDeleted(row))
      assertEquals(DataOutcome.Success(null), repository.getById(row.id))
      driver.execute(null, "DROP TRIGGER block_insert", 0)
      assertEquals(
          DataOutcome.Success(MovimientoRestoreResult.Restored),
          repository.restoreDeleted(row),
      )
    }
  }

  @Test
  fun `new reads and mutations wrap unavailable storage`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    try {
      TallybookDatabase.Schema.create(driver)
      val repository =
          MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
      driver.close()
      val row = movimiento("closed")
      assertLocal(repository.getById(row.id))
      assertLocal(repository.update(row.id, row.changes()))
      assertLocal(repository.deleteAndCapture(row.id))
      assertLocal(repository.restoreDeleted(row))
    } finally {
      driver.close()
    }
  }

  private fun assertInvalid(outcome: DataOutcome<*>) {
    assertIs<DataError.InvalidInput>(assertIs<DataOutcome.Failure>(outcome).error)
  }

  private fun assertLocal(outcome: DataOutcome<*>) {
    assertIs<DataError.Local>(assertIs<DataOutcome.Failure>(outcome).error)
  }

  private fun Movimiento.changes() = MovimientoChanges(tipo, monto, fecha, categoriaId, descripcion)

  private fun insertUnreadable(database: TallybookDatabase, id: String) {
    database.movimientoQueries.insertMovimiento(
        id,
        "INVALID",
        500,
        "COP",
        "2026-09-05",
        "cat_comida",
        null,
        "invalid-instant",
    )
  }

  @Test
  fun `restore treats an unreadable existing id as conflict without modifying it`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val database = TallybookDatabase(driver)
      val repository = MovimientoRepositoryImpl(database, StandardTestDispatcher(testScheduler))
      insertUnreadable(database, "occupied")
      val raw = database.movimientoQueries.selectMovimientoById("occupied").executeAsOne()
      assertEquals(
          DataOutcome.Success(MovimientoRestoreResult.Conflict),
          repository.restoreDeleted(movimiento("occupied")),
      )
      assertEquals(raw, database.movimientoQueries.selectMovimientoById("occupied").executeAsOne())
    }
  }

  private fun movimiento(
      id: String,
      tipo: TipoMovimiento = TipoMovimiento.GASTO,
      monto: Money = Money(500, Moneda.COP),
      categoriaId: String = "cat_comida",
      descripcion: String? = null,
  ) =
      Movimiento(
          id = id,
          tipo = tipo,
          monto = monto,
          fecha = LocalDate(2026, 9, 5),
          categoriaId = categoriaId,
          descripcion = descripcion,
          createdAt = Instant.parse("2026-09-05T12:00:00Z"),
      )
}
