package com.kurobello.tallybook.core.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MAX_ETIQUETAS_POR_MOVIMIENTO
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.MOVIMIENTO_TITLE_MAX_LENGTH
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoRestoreResult
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.MovimientoUpdateResult
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class MovimientoRepositoryTest {

  @Test
  fun `insert update and restore each store the one stamp their transaction issued`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val database = seeded(driver)
      val changeClock = TestChangeClock()
      val repository =
          MovimientoRepositoryImpl(database, StandardTestDispatcher(testScheduler), changeClock)
      val original = movimiento(id = "mov", etiquetaIds = listOf("tag_a", "tag_b"))
      val storedStamp = {
        database.movimientoQueries.selectMovimientoById("mov").executeAsOne().updated_at
      }

      repository.insert(original)
      assertEquals(changeClock.issuedStamps.single().value, storedStamp())

      repository.update("mov", original.changes().copy(titulo = "cena"))
      assertEquals(2, changeClock.issuedStamps.size)
      assertEquals(changeClock.issuedStamps.last().value, storedStamp())

      val captured = repository.deleteAndCapture("mov")
      repository.restoreDeleted(
          checkNotNull(assertIs<DataOutcome.Success<Movimiento?>>(captured).data)
      )
      assertEquals(4, changeClock.issuedStamps.size)
      assertEquals(changeClock.issuedStamps.last().value, storedStamp())
    }
  }

  @Test
  fun `delete leaves a tombstone stamped once that every read skips`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val changeClock = TestChangeClock()
      val repository =
          MovimientoRepositoryImpl(
              seeded(driver),
              StandardTestDispatcher(testScheduler),
              changeClock,
          )
      val captured = movimiento("captured", titulo = "cafe", etiquetaIds = listOf("tag_a"))
      val deleted =
          movimiento("deleted", titulo = "taxi", etiquetaIds = listOf("tag_b"), at = LATER)
      repository.insert(captured)
      repository.insert(deleted)

      assertEquals(DataOutcome.Success(captured), repository.deleteAndCapture(captured.id))
      assertEquals(DataOutcome.Success(Unit), repository.delete(deleted.id))

      val (captureStamp, deleteStamp) = changeClock.issuedStamps.drop(2).map { it.value }
      assertEquals(captureStamp to captureStamp, driver.storedStamps("movimiento", captured.id))
      assertEquals(deleteStamp to deleteStamp, driver.storedStamps("movimiento", deleted.id))
      assertEquals(emptyList(), linkRows(driver))
      val september = MovimientoDateRange(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30))
      val noMoney = MovimientoSummary(Money(0, Moneda.COP), Money(0, Moneda.COP))
      assertEquals(DataOutcome.Success(emptyList()), repository.getAll())
      assertEquals(DataOutcome.Success(null), repository.getById(captured.id))
      assertEquals(DataOutcome.Success(emptyList()), repository.observeRecent(5).first())
      assertEquals(
          DataOutcome.Success(emptyList()),
          repository.observeRecent(Moneda.COP, 5).first(),
      )
      assertEquals(
          DataOutcome.Success(emptyList()),
          repository.observeRange(Moneda.COP, september).first(),
      )
      assertEquals(
          DataOutcome.Success(MovimientoSearchSnapshot(emptyList(), emptyList())),
          repository.observeFiltered(MovimientoReadCriteria(Moneda.COP)).first(),
      )
      assertEquals(DataOutcome.Success(emptyList()), repository.observeYears(Moneda.COP).first())
      assertEquals(DataOutcome.Success(noMoney), repository.observeSummary(Moneda.COP).first())
      assertEquals(DataOutcome.Success(emptyList()), repository.observeCategoryHistory(5).first())

      assertEquals(DataOutcome.Success(Unit), repository.delete(deleted.id))
      assertEquals(DataOutcome.Success(null), repository.deleteAndCapture(captured.id))
      assertEquals(4, changeClock.issuedStamps.size)
    }
  }

  @Test
  fun `restore revives the tombstone with a newer stamp and only surviving references`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val changeClock = TestChangeClock()
      val repository =
          MovimientoRepositoryImpl(
              seeded(driver, extraCategoriaIds = listOf("custom")),
              StandardTestDispatcher(testScheduler),
              changeClock,
          )
      val row =
          movimiento(
              "gone",
              categoriaId = "custom",
              etiquetaIds = listOf("tag_c", "tag_a", "tag_b"),
          )
      repository.insert(row)
      assertEquals(DataOutcome.Success(row), repository.deleteAndCapture(row.id))
      val deleteStamp = changeClock.issuedStamps.last()
      driver.tombstone("categoria", "custom")
      driver.tombstone("etiqueta", "tag_a")

      assertEquals(
          DataOutcome.Success(MovimientoRestoreResult.Restored),
          repository.restoreDeleted(row),
      )

      val reviveStamp = changeClock.issuedStamps.last()
      assertEquals(3, changeClock.issuedStamps.size)
      assertTrue(reviveStamp > deleteStamp)
      assertEquals(reviveStamp.value to null, driver.storedStamps("movimiento", row.id))
      val revived =
          row.copy(categoriaId = CATEGORIA_OTROS_ID, etiquetaIds = listOf("tag_c", "tag_b"))
      assertEquals(DataOutcome.Success(listOf(revived)), repository.getAll())
      assertEquals(listOf("gone:tag_b:1", "gone:tag_c:0"), linkRows(driver))
      assertEquals(
          DataOutcome.Success(MovimientoRestoreResult.AlreadyPresent),
          repository.restoreDeleted(row),
      )
      assertEquals(
          DataOutcome.Success(MovimientoRestoreResult.Conflict),
          repository.restoreDeleted(row.copy(titulo = "otro")),
      )
      assertEquals(3, changeClock.issuedStamps.size)
    }
  }

  @Test
  fun `restore of a tombstone with different content is a conflict that keeps the tombstone`() =
      runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
          val changeClock = TestChangeClock()
          val repository =
              MovimientoRepositoryImpl(
                  seeded(driver),
                  StandardTestDispatcher(testScheduler),
                  changeClock,
              )
          val row = movimiento("gone", titulo = "cafe", descripcion = "con leche")
          repository.insert(row)
          assertEquals(DataOutcome.Success(row), repository.deleteAndCapture(row.id))
          val tombstone = driver.storedStamps("movimiento", row.id)

          for (different in
              listOf(
                  row.copy(tipo = TipoMovimiento.INGRESO),
                  row.copy(monto = Money(501, Moneda.COP)),
                  row.copy(monto = Money(500, Moneda.CLP)),
                  row.copy(fecha = LocalDate(2026, 9, 6)),
                  row.copy(categoriaId = "cat_cine"),
                  row.copy(titulo = null),
                  row.copy(descripcion = "sola"),
                  row.copy(createdAt = LATER),
              )) {
            assertEquals(
                DataOutcome.Success(MovimientoRestoreResult.Conflict),
                repository.restoreDeleted(different),
            )
          }

          assertEquals(tombstone, driver.storedStamps("movimiento", row.id))
          assertEquals(DataOutcome.Success(null), repository.getById(row.id))
          assertEquals(2, changeClock.issuedStamps.size)
          assertEquals(
              DataOutcome.Success(MovimientoRestoreResult.Restored),
              repository.restoreDeleted(row.copy(etiquetaIds = listOf("tag_a"))),
          )
          assertEquals(
              DataOutcome.Success(row.copy(etiquetaIds = listOf("tag_a"))),
              repository.getById(row.id),
          )
        }
      }

  @Test
  fun `an insert reusing a tombstoned id fails and leaves the tombstone untouched`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val changeClock = TestChangeClock()
      val repository =
          MovimientoRepositoryImpl(
              seeded(driver),
              StandardTestDispatcher(testScheduler),
              changeClock,
          )
      val row = movimiento("reused", etiquetaIds = listOf("tag_a"))
      repository.insert(row)
      repository.delete(row.id)
      val tombstone = driver.storedStamps("movimiento", row.id)
      val replacement =
          row.copy(monto = Money(900, Moneda.COP), etiquetaIds = listOf("tag_b"), createdAt = LATER)

      assertLocal(repository.insert(replacement))

      assertEquals(tombstone, driver.storedStamps("movimiento", row.id))
      assertEquals(DataOutcome.Success(emptyList()), repository.getAll())
      assertEquals(emptyList(), linkRows(driver))
      assertEquals(
          DataOutcome.Success(MovimientoRestoreResult.Restored),
          repository.restoreDeleted(row),
      )
      assertEquals(DataOutcome.Success(listOf(row)), repository.getAll())
    }
  }

  @Test
  fun `update of a tombstone is not found and leaves it untouched`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val changeClock = TestChangeClock()
      val repository =
          MovimientoRepositoryImpl(
              seeded(driver),
              StandardTestDispatcher(testScheduler),
              changeClock,
          )
      val row = movimiento("gone", etiquetaIds = listOf("tag_a"))
      repository.insert(row)
      repository.deleteAndCapture(row.id)
      val tombstone = driver.storedStamps("movimiento", row.id)

      assertEquals(
          DataOutcome.Success(MovimientoUpdateResult.NotFound),
          repository.update(row.id, row.changes().copy(titulo = "cena")),
      )

      assertEquals(tombstone, driver.storedStamps("movimiento", row.id))
      assertEquals(emptyList(), linkRows(driver))
      assertEquals(2, changeClock.issuedStamps.size)
    }
  }

  @Test
  fun `an update that changes nothing writes no stamp and notifies no observer`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val database = seeded(driver)
      val changeClock = TestChangeClock()
      val repository =
          MovimientoRepositoryImpl(database, StandardTestDispatcher(testScheduler), changeClock)
      val row = movimiento("same", titulo = "cafe", etiquetaIds = listOf("tag_b", "tag_a"))
      repository.insert(row)
      val stored = driver.storedStamps("movimiento", row.id)
      var notifications = 0
      database.movimientoQueries.selectAllMovimientos().addListener { notifications++ }

      assertEquals(
          DataOutcome.Success(MovimientoUpdateResult.Updated),
          repository.update(row.id, row.changes()),
      )

      assertEquals(0, notifications)
      assertEquals(stored, driver.storedStamps("movimiento", row.id))
      assertEquals(1, changeClock.issuedStamps.size)
      assertEquals(DataOutcome.Success(row), repository.getById(row.id))
    }
  }

  @Test
  fun `insert rejects invalid movimiento input without writing`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    val repository =
        MovimientoRepositoryImpl(
            seeded(driver),
            StandardTestDispatcher(testScheduler),
            TestChangeClock(),
        )
    val valid = movimiento(id = "valid")
    val invalid =
        listOf(
            valid.copy(monto = Money(0, Moneda.COP)),
            valid.copy(id = ""),
            valid.copy(categoriaId = ""),
            valid.copy(categoriaId = " "),
            valid.copy(categoriaId = "missing"),
            valid.copy(etiquetaIds = listOf("missing")),
            valid.copy(descripcion = "a".repeat(MOVIMIENTO_DESCRIPTION_MAX_LENGTH + 1)),
            valid.copy(titulo = "a".repeat(MOVIMIENTO_TITLE_MAX_LENGTH + 1)),
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
    val repository =
        MovimientoRepositoryImpl(
            seeded(driver),
            StandardTestDispatcher(testScheduler),
            TestChangeClock(),
        )
    val movimiento =
        Movimiento(
            id = "mov1",
            tipo = TipoMovimiento.GASTO,
            monto = Money(1_050, Moneda.USD),
            fecha = LocalDate(2026, 9, 3),
            categoriaId = "cat_comida",
            titulo = "almuerzo",
            descripcion = null,
            createdAt = Instant.parse("2026-09-03T12:00:00Z"),
            etiquetaIds = listOf("tag_b", "tag_a"),
        )

    assertEquals(DataOutcome.Success(Unit), repository.insert(movimiento))
    assertEquals(DataOutcome.Success(listOf(movimiento)), repository.getAll())

    driver.close()
  }

  @Test
  fun `insert maps a duplicate id to a local error`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    val repository =
        MovimientoRepositoryImpl(
            seeded(driver),
            StandardTestDispatcher(testScheduler),
            TestChangeClock(),
        )
    val movimiento =
        Movimiento(
            id = "duplicado",
            tipo = TipoMovimiento.INGRESO,
            monto = Money(25_000, Moneda.COP),
            fecha = LocalDate(2026, 9, 4),
            categoriaId = "cat_sueldo",
            titulo = null,
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
  fun `delete removes the movimiento and its etiqueta links`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    val repository =
        MovimientoRepositoryImpl(
            seeded(driver),
            StandardTestDispatcher(testScheduler),
            TestChangeClock(),
        )
    val movimiento =
        Movimiento(
            id = "mov1",
            tipo = TipoMovimiento.GASTO,
            monto = Money(500, Moneda.MXN),
            fecha = LocalDate(2026, 9, 5),
            categoriaId = "cat_transporte",
            titulo = null,
            descripcion = null,
            createdAt = Instant.parse("2026-09-05T12:00:00Z"),
            etiquetaIds = listOf("tag_a"),
        )

    repository.insert(movimiento)

    assertEquals(DataOutcome.Success(Unit), repository.delete(movimiento.id))
    assertEquals(DataOutcome.Success(emptyList()), repository.getAll())
    assertEquals(emptyList(), linkRows(driver))
    driver.close()
  }

  @Test
  fun `getById distinguishes absent rows invalid ids and unreadable rows`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val database = seeded(driver)
      val repository =
          MovimientoRepositoryImpl(
              database,
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          )
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
          val repository =
              MovimientoRepositoryImpl(
                  seeded(driver),
                  StandardTestDispatcher(testScheduler),
                  TestChangeClock(),
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
                  "cat_sueldo",
                  listOf("tag_c"),
                  "updated",
                  "updated details",
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
                  categoriaId = "cat_sueldo",
                  etiquetaIds = listOf("tag_c"),
                  titulo = "updated",
                  descripcion = "updated details",
              )
          assertEquals(DataOutcome.Success(expected), repository.getById(original.id))
          assertEquals(DataOutcome.Success(listOf(sibling, expected)), repository.getAll())
          assertEquals(
              DataOutcome.Success(MovimientoUpdateResult.NotFound),
              repository.update("absent", changes),
          )
          assertEquals(
              DataOutcome.Success(MovimientoUpdateResult.Updated),
              repository.update(original.id, changes.copy(titulo = null, descripcion = null)),
          )
          assertEquals(
              DataOutcome.Success(expected.copy(titulo = null, descripcion = null)),
              repository.getById(original.id),
          )
        }
      }

  @Test
  fun `update keeps a stored categoria missing from the catalog only while unchanged`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val database = seeded(driver)
      val repository =
          MovimientoRepositoryImpl(
              database,
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          )
      val legacy = movimiento("legacy", categoriaId = "missing-legacy-category")
      insertRaw(database, legacy)

      assertEquals(
          DataOutcome.Success(MovimientoUpdateResult.Updated),
          repository.update(legacy.id, legacy.changes().copy(titulo = "kept")),
      )
      assertEquals(
          DataOutcome.Success(legacy.copy(titulo = "kept")),
          repository.getById(legacy.id),
      )
      assertInvalid(
          repository.update(legacy.id, legacy.changes().copy(categoriaId = "another-missing"))
      )
      assertEquals(
          DataOutcome.Success(legacy.copy(titulo = "kept")),
          repository.getById(legacy.id),
      )
    }
  }

  @Test
  fun `update rejects invalid values without replacing the stored row`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val repository =
          MovimientoRepositoryImpl(
              seeded(driver),
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          )
      val original = movimiento("valid", etiquetaIds = listOf("tag_a"))
      repository.insert(original)
      val valid = original.changes()
      for (changes in
          listOf(
              valid.copy(monto = Money(0, Moneda.COP)),
              valid.copy(monto = Money(-1, Moneda.COP)),
              valid.copy(monto = Money(100, Moneda.USD)),
              valid.copy(categoriaId = " "),
              valid.copy(categoriaId = "missing"),
              valid.copy(etiquetaIds = listOf("tag_a", "missing")),
              valid.copy(descripcion = "a".repeat(MOVIMIENTO_DESCRIPTION_MAX_LENGTH + 1)),
              valid.copy(titulo = "a".repeat(MOVIMIENTO_TITLE_MAX_LENGTH + 1)),
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
          val repository =
              MovimientoRepositoryImpl(
                  seeded(driver),
                  StandardTestDispatcher(testScheduler),
                  TestChangeClock(),
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
          val database = seeded(driver)
          val repository =
              MovimientoRepositoryImpl(
                  database,
                  StandardTestDispatcher(testScheduler),
                  TestChangeClock(),
              )
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
      val repository =
          MovimientoRepositoryImpl(
              seeded(driver),
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          )
      val first =
          movimiento(
              "a",
              monto = Money(Long.MAX_VALUE, Moneda.CLP),
              descripcion = "a".repeat(MOVIMIENTO_DESCRIPTION_MAX_LENGTH),
              etiquetaIds = listOf("tag_b", "tag_a"),
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
  fun `restore skips etiquetas deleted after the capture and keeps the rest`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val database = seeded(driver)
      val repository =
          MovimientoRepositoryImpl(
              database,
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          )
      val row = movimiento("gone", etiquetaIds = listOf("tag_c", "tag_a", "tag_b"))
      repository.insert(row)
      assertEquals(DataOutcome.Success(row), repository.deleteAndCapture(row.id))
      driver.tombstone("etiqueta", "tag_a")

      assertEquals(
          DataOutcome.Success(MovimientoRestoreResult.Restored),
          repository.restoreDeleted(row),
      )

      val restored = row.copy(etiquetaIds = listOf("tag_c", "tag_b"))
      assertEquals(DataOutcome.Success(restored), repository.getById(row.id))
      assertEquals(listOf("gone:tag_b:1", "gone:tag_c:0"), linkRows(driver))
      assertEquals(
          DataOutcome.Success(MovimientoRestoreResult.AlreadyPresent),
          repository.restoreDeleted(row),
      )
    }
  }

  @Test
  fun `restore falls back to Otros when the custom categoria was deleted after the capture`() =
      runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
          val database = seeded(driver, extraCategoriaIds = listOf("custom"))
          val repository =
              MovimientoRepositoryImpl(
                  database,
                  StandardTestDispatcher(testScheduler),
                  TestChangeClock(),
              )
          val row = movimiento("gone", categoriaId = "custom", etiquetaIds = listOf("tag_a"))
          repository.insert(row)
          assertEquals(DataOutcome.Success(row), repository.deleteAndCapture(row.id))
          driver.execute(null, "DELETE FROM categoria WHERE id = 'custom'", 0)

          assertEquals(
              DataOutcome.Success(MovimientoRestoreResult.Restored),
              repository.restoreDeleted(row),
          )

          assertEquals(
              DataOutcome.Success(row.copy(categoriaId = CATEGORIA_OTROS_ID)),
              repository.getById(row.id),
          )
        }
      }

  @Test
  fun `capture rejects nonrestorable legacy rows and preserves their exact stored fields`() =
      runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
          val database = seeded(driver)
          val repository =
              MovimientoRepositoryImpl(
                  database,
                  StandardTestDispatcher(testScheduler),
                  TestChangeClock(),
              )
          val whitespace = movimiento("whitespace-category", categoriaId = " ")
          insertRaw(database, whitespace)
          val oversized =
              movimiento(
                  "legacy-description",
                  descripcion = "x".repeat(MOVIMIENTO_DESCRIPTION_MAX_LENGTH + 1),
              )
          insertRaw(database, oversized)
          for (row in listOf(whitespace, oversized)) {
            val raw = database.movimientoQueries.selectMovimientoById(row.id).executeAsOne()
            assertEquals(DataOutcome.Success(row), repository.getById(row.id))
            assertInvalid(repository.deleteAndCapture(row.id))
            assertEquals(
                raw,
                database.movimientoQueries.selectMovimientoById(row.id).executeAsOne(),
            )
            assertEquals(DataOutcome.Success(row), repository.getById(row.id))
          }
        }
      }

  @Test
  fun `restore rejects invalid snapshots without writing`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val repository =
          MovimientoRepositoryImpl(
              seeded(driver),
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          )
      val valid = movimiento("restore")
      for (row in
          listOf(
              valid.copy(id = " "),
              valid.copy(categoriaId = " "),
              valid.copy(etiquetaIds = listOf("tag_a", "tag_a")),
              valid.copy(monto = Money(0, Moneda.COP)),
              valid.copy(descripcion = "a".repeat(MOVIMIENTO_DESCRIPTION_MAX_LENGTH + 1)),
          )) assertInvalid(repository.restoreDeleted(row))
      assertEquals(DataOutcome.Success(emptyList()), repository.getAll())
    }
  }

  @Test
  fun `mutation write failures roll back without yielding a false capture or restore`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val repository =
          MovimientoRepositoryImpl(
              seeded(driver),
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          )
      val row = movimiento("blocked")
      repository.insert(row)
      driver.execute(
          null,
          "CREATE TRIGGER block_tombstone BEFORE UPDATE ON movimiento BEGIN SELECT RAISE(ABORT, 'blocked'); END",
          0,
      )
      assertLocal(repository.deleteAndCapture(row.id))
      assertEquals(DataOutcome.Success(row), repository.getById(row.id))
      driver.execute(null, "DROP TRIGGER block_tombstone", 0)
      assertEquals(DataOutcome.Success(row), repository.deleteAndCapture(row.id))
      driver.execute(
          null,
          "CREATE TRIGGER block_revive BEFORE UPDATE ON movimiento BEGIN SELECT RAISE(ABORT, 'blocked'); END",
          0,
      )
      assertLocal(repository.restoreDeleted(row))
      assertEquals(DataOutcome.Success(null), repository.getById(row.id))
      driver.execute(null, "DROP TRIGGER block_revive", 0)
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
      val repository =
          MovimientoRepositoryImpl(
              seeded(driver),
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          )
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

  @Test
  fun `category history returns the newest described rows within the limit`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val repository =
          MovimientoRepositoryImpl(
              seeded(driver),
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          )
      val base = Instant.parse("2026-09-05T12:00:00Z")
      val rows =
          listOf(
              movimiento("oldest", categoriaId = "cat_cafe", titulo = "cafe", at = base),
              movimiento("blank", titulo = "   ", at = base + 1.seconds),
              movimiento("empty", titulo = "", at = base + 2.seconds),
              movimiento("none", titulo = null, at = base + 3.seconds),
              movimiento(
                  "tie-a",
                  categoriaId = "cat_cine",
                  titulo = "cine",
                  at = base + 4.seconds,
                  etiquetaIds = listOf("tag_a", "tag_b"),
              ),
              movimiento("tie-b", categoriaId = "cat_taxi", titulo = "uber", at = base + 4.seconds),
              movimiento(
                  "newest",
                  categoriaId = "cat_gasolina",
                  titulo = "terpel",
                  at = base + 5.seconds,
              ),
          )
      rows.forEach { assertEquals(DataOutcome.Success(Unit), repository.insert(it)) }

      val all =
          assertIs<DataOutcome.Success<List<CategoryHistoryRow>>>(
              repository.observeCategoryHistory(10).first()
          )
      val limited =
          assertIs<DataOutcome.Success<List<CategoryHistoryRow>>>(
              repository.observeCategoryHistory(2).first()
          )

      assertEquals(
          listOf("terpel", "uber", "cine", "cafe"),
          all.data.map(CategoryHistoryRow::titulo),
      )
      assertEquals(listOf("cat_gasolina", "cat_taxi"), limited.data.map { it.categoriaId })
      assertEquals(base + 5.seconds, all.data.first().createdAt)
    }
  }

  @Test
  fun `category history rejects a non positive limit and wraps unavailable storage`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    try {
      val repository =
          MovimientoRepositoryImpl(
              seeded(driver),
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          )

      assertInvalid(repository.observeCategoryHistory(0).first())
      assertInvalid(repository.observeCategoryHistory(-1).first())
      driver.close()
      assertLocal(repository.observeCategoryHistory(5).first())
    } finally {
      driver.close()
    }
  }

  @Test
  fun `category history emits again after an insert and after an edit`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val repository =
          MovimientoRepositoryImpl(
              seeded(driver),
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          )
      val emissions = mutableListOf<DataOutcome<List<CategoryHistoryRow>>>()
      val collector = backgroundScope.launch {
        repository.observeCategoryHistory(5).collect(emissions::add)
      }
      testScheduler.runCurrent()
      assertEquals(DataOutcome.Success(emptyList<CategoryHistoryRow>()), emissions.last())

      val row = movimiento("one", categoriaId = "cat_cafe", titulo = "cafe")
      assertEquals(DataOutcome.Success(Unit), repository.insert(row))
      testScheduler.runCurrent()
      assertEquals(
          DataOutcome.Success(listOf(CategoryHistoryRow("cafe", "cat_cafe", row.createdAt))),
          emissions.last(),
      )

      repository.update("one", row.changes().copy(categoriaId = "cat_restaurante"))
      testScheduler.runCurrent()
      assertEquals(
          DataOutcome.Success(listOf(CategoryHistoryRow("cafe", "cat_restaurante", row.createdAt))),
          emissions.last(),
      )
      collector.cancel()
    }
  }

  @Test
  fun `category history keeps the newest rows at the volume bound`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val database = seeded(driver)
      val repository =
          MovimientoRepositoryImpl(
              database,
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          )
      val base = Instant.parse("2026-01-01T00:00:00Z")
      database.transaction {
        repeat(CATEGORY_HISTORY_BOUND + 200) { index ->
          database.movimientoQueries.insertMovimiento(
              "id$index",
              "GASTO",
              100,
              "COP",
              "2026-01-01",
              "row $index",
              null,
              (base + index.seconds).toString(),
              "cat_comida",
              ChangeStamp.SEED.value,
          )
        }
      }

      val rows =
          assertIs<DataOutcome.Success<List<CategoryHistoryRow>>>(
                  repository.observeCategoryHistory(CATEGORY_HISTORY_BOUND).first()
              )
              .data

      assertEquals(CATEGORY_HISTORY_BOUND, rows.size)
      assertEquals("row ${CATEGORY_HISTORY_BOUND + 199}", rows.first().titulo)
      assertEquals("row 200", rows.last().titulo)
    }
  }

  @Test
  fun `zero and the maximum of etiquetas round-trip in display order`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val repository =
          MovimientoRepositoryImpl(
              seeded(driver),
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          )
      val none = movimiento("none")
      val maximum =
          movimiento(
              "maximum",
              etiquetaIds = listOf("tag_f", "tag_b", "tag_e", "tag_a", "tag_d", "tag_c"),
              at = LATER,
          )

      assertEquals(DataOutcome.Success(Unit), repository.insert(none))
      assertEquals(DataOutcome.Success(Unit), repository.insert(maximum))

      assertEquals(DataOutcome.Success(none), repository.getById("none"))
      assertEquals(DataOutcome.Success(maximum), repository.getById("maximum"))
      assertEquals(DataOutcome.Success(listOf(maximum, none)), repository.getAll())
    }
  }

  @Test
  fun `invalid etiqueta lists are rejected on insert update and restore without writing`() =
      runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
          val repository =
              MovimientoRepositoryImpl(
                  seeded(driver),
                  StandardTestDispatcher(testScheduler),
                  TestChangeClock(),
              )
          val original = movimiento("valid", etiquetaIds = listOf("tag_a", "tag_b"))
          repository.insert(original)
          val invalid =
              listOf(
                  listOf(""),
                  listOf("tag_a", " "),
                  listOf("tag_a", "tag_b", "tag_a"),
                  listOf("tag_a\u001Etag_b"),
                  listOf("tag_a", "tag_b\u001Ftag_c"),
                  List(MAX_ETIQUETAS_POR_MOVIMIENTO + 1) { "tag_$it" },
              )

          for (ids in invalid) {
            assertInvalid(
                repository.update(original.id, original.changes().copy(etiquetaIds = ids))
            )
            assertInvalid(
                repository.restoreDeleted(original.copy(id = "restore", etiquetaIds = ids))
            )
            assertInvalid(repository.insert(original.copy(id = "insert", etiquetaIds = ids)))
          }

          assertEquals(DataOutcome.Success(listOf(original)), repository.getAll())
          assertEquals(listOf("valid:tag_a:0", "valid:tag_b:1"), linkRows(driver))
        }
      }

  @Test
  fun `update replaces the categoria and the whole etiqueta list without stale links`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val repository =
          MovimientoRepositoryImpl(
              seeded(driver),
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          )
      val original = movimiento("edit", etiquetaIds = listOf("tag_a", "tag_b", "tag_c"))
      val sibling = movimiento("sibling", etiquetaIds = listOf("tag_a"), at = LATER)
      repository.insert(original)
      repository.insert(sibling)

      val reordered =
          original
              .changes()
              .copy(categoriaId = "cat_cine", etiquetaIds = listOf("tag_d", "tag_c", "tag_a"))
      assertEquals(
          DataOutcome.Success(MovimientoUpdateResult.Updated),
          repository.update(original.id, reordered),
      )

      assertEquals(
          DataOutcome.Success(
              original.copy(
                  categoriaId = "cat_cine",
                  etiquetaIds = listOf("tag_d", "tag_c", "tag_a"),
              )
          ),
          repository.getById(original.id),
      )
      assertEquals(DataOutcome.Success(sibling), repository.getById("sibling"))
      assertEquals(
          listOf("edit:tag_a:2", "edit:tag_c:1", "edit:tag_d:0", "sibling:tag_a:0"),
          linkRows(driver),
      )
      assertEquals(
          DataOutcome.Success(MovimientoUpdateResult.Updated),
          repository.update(original.id, reordered.copy(etiquetaIds = emptyList())),
      )
      assertEquals(listOf("sibling:tag_a:0"), linkRows(driver))
    }
  }

  @Test
  fun `a failing etiqueta write rolls back the movimiento insert and update`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val repository =
          MovimientoRepositoryImpl(
              seeded(driver, etiquetaIds = listOf("bad")),
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          )
      val original = movimiento("kept", etiquetaIds = listOf("tag_a", "tag_b"))
      repository.insert(original)
      driver.execute(
          null,
          "CREATE TRIGGER block_etiqueta BEFORE INSERT ON movimiento_etiqueta " +
              "WHEN new.etiqueta_id = 'bad' BEGIN SELECT RAISE(ABORT, 'blocked'); END",
          0,
      )

      assertLocal(repository.insert(movimiento("new", etiquetaIds = listOf("tag_a", "bad"))))
      assertLocal(
          repository.update(
              original.id,
              original
                  .changes()
                  .copy(
                      monto = Money(900, Moneda.COP),
                      categoriaId = "cat_cine",
                      etiquetaIds = listOf("bad"),
                  ),
          )
      )

      assertEquals(DataOutcome.Success(listOf(original)), repository.getAll())
      assertEquals(listOf("kept:tag_a:0", "kept:tag_b:1"), linkRows(driver))
    }
  }

  @Test
  fun `delete capture and restore carry the etiquetas and detect a different list`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val repository =
          MovimientoRepositoryImpl(
              seeded(driver),
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          )
      val row = movimiento("gone", etiquetaIds = listOf("tag_b", "tag_a", "tag_c"))
      val other = movimiento("other", etiquetaIds = listOf("tag_a"), at = LATER)
      repository.insert(row)
      repository.insert(other)

      assertEquals(DataOutcome.Success(row), repository.deleteAndCapture(row.id))
      assertEquals(listOf("other:tag_a:0"), linkRows(driver))
      assertEquals(
          DataOutcome.Success(MovimientoRestoreResult.Restored),
          repository.restoreDeleted(row),
      )
      assertEquals(DataOutcome.Success(row), repository.getById(row.id))
      assertEquals(
          DataOutcome.Success(MovimientoRestoreResult.AlreadyPresent),
          repository.restoreDeleted(row),
      )
      for (different in
          listOf(
              listOf("tag_b", "tag_a"),
              listOf("tag_b", "tag_a", "tag_d"),
              listOf("tag_a", "tag_b", "tag_c"),
          )) {
        assertEquals(
            DataOutcome.Success(MovimientoRestoreResult.Conflict),
            repository.restoreDeleted(row.copy(etiquetaIds = different)),
        )
      }
      assertEquals(
          DataOutcome.Success(MovimientoRestoreResult.Conflict),
          repository.restoreDeleted(row.copy(categoriaId = "cat_cine")),
      )
      assertEquals(DataOutcome.Success(row), repository.getById(row.id))

      assertEquals(DataOutcome.Success(Unit), repository.delete(row.id))
      assertEquals(listOf("other:tag_a:0"), linkRows(driver))
    }
  }

  @Test
  fun `observed reads emit when only the etiqueta link table changes`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val database = seeded(driver)
      val repository =
          MovimientoRepositoryImpl(
              database,
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          )
      val row = movimiento("one", etiquetaIds = listOf("tag_a"), titulo = "cafe")
      repository.insert(row)
      val september = MovimientoDateRange(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30))
      val recent = mutableListOf<DataOutcome<List<Movimiento>>>()
      val inRange = mutableListOf<DataOutcome<List<Movimiento>>>()
      val filtered = mutableListOf<DataOutcome<MovimientoSearchSnapshot>>()
      backgroundScope.launch { repository.observeRecent(Moneda.COP, 5).collect(recent::add) }
      backgroundScope.launch {
        repository.observeRange(Moneda.COP, september).collect(inRange::add)
      }
      backgroundScope.launch {
        repository.observeFiltered(MovimientoReadCriteria(Moneda.COP)).collect(filtered::add)
      }
      testScheduler.runCurrent()

      database.movimientoQueries.insertMovimientoEtiqueta("one", "tag_b", 1)
      testScheduler.runCurrent()

      val updated = row.copy(etiquetaIds = listOf("tag_a", "tag_b"))
      assertEquals(DataOutcome.Success(listOf(updated)), recent.last())
      assertEquals(DataOutcome.Success(listOf(updated)), inRange.last())
      assertEquals(
          DataOutcome.Success(MovimientoSearchSnapshot(listOf(updated), listOf(2026))),
          filtered.last(),
      )
    }
  }

  private fun seeded(
      driver: SqlDriver,
      extraCategoriaIds: List<String> = emptyList(),
      etiquetaIds: List<String> = emptyList(),
  ): TallybookDatabase =
      createSchemaWithReferences(driver, extraCategoriaIds, ETIQUETA_IDS + etiquetaIds)

  private fun linkRows(driver: SqlDriver): List<String> =
      driver
          .executeQuery(
              null,
              "SELECT movimiento_id || ':' || etiqueta_id || ':' || posicion " +
                  "FROM movimiento_etiqueta ORDER BY 1",
              { cursor ->
                val rows = mutableListOf<String>()
                while (cursor.next().value) rows += requireNotNull(cursor.getString(0))
                QueryResult.Value(rows)
              },
              0,
          )
          .value

  private fun assertInvalid(outcome: DataOutcome<*>) {
    assertIs<DataError.InvalidInput>(assertIs<DataOutcome.Failure>(outcome).error)
  }

  private fun assertLocal(outcome: DataOutcome<*>) {
    assertIs<DataError.Local>(assertIs<DataOutcome.Failure>(outcome).error)
  }

  private fun Movimiento.changes() =
      MovimientoChanges(tipo, monto, fecha, categoriaId, etiquetaIds, titulo, descripcion)

  private fun insertUnreadable(database: TallybookDatabase, id: String) {
    database.movimientoQueries.insertMovimiento(
        id,
        "INVALID",
        500,
        "COP",
        "2026-09-05",
        null,
        null,
        "invalid-instant",
        "cat_comida",
        ChangeStamp.SEED.value,
    )
  }

  private fun insertRaw(database: TallybookDatabase, row: Movimiento) {
    database.movimientoQueries.insertMovimiento(
        row.id,
        row.tipo.name,
        row.monto.minorUnits,
        row.monto.moneda.name,
        row.fecha.toString(),
        row.titulo,
        row.descripcion,
        row.createdAt.toString(),
        row.categoriaId,
        ChangeStamp.SEED.value,
    )
    row.etiquetaIds.forEachIndexed { posicion, etiquetaId ->
      database.movimientoQueries.insertMovimientoEtiqueta(row.id, etiquetaId, posicion.toLong())
    }
  }

  @Test
  fun `restore treats an unreadable existing id as conflict without modifying it`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val database = seeded(driver)
      val repository =
          MovimientoRepositoryImpl(
              database,
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          )
      insertUnreadable(database, "occupied")
      val raw = database.movimientoQueries.selectMovimientoById("occupied").executeAsOne()
      assertEquals(
          DataOutcome.Success(MovimientoRestoreResult.Conflict),
          repository.restoreDeleted(movimiento("occupied")),
      )
      assertEquals(raw, database.movimientoQueries.selectMovimientoById("occupied").executeAsOne())
    }
  }

  @Test
  fun `restore replaces orphaned links left by an interrupted delete`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val database = seeded(driver)
      val repository =
          MovimientoRepositoryImpl(
              database,
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          )
      val row = movimiento("gone", etiquetaIds = listOf("tag_a", "tag_b"))
      database.movimientoQueries.insertMovimientoEtiqueta("gone", "tag_a", 0)
      database.movimientoQueries.insertMovimientoEtiqueta("gone", "stale", 1)

      assertEquals(
          DataOutcome.Success(MovimientoRestoreResult.Restored),
          repository.restoreDeleted(row),
      )

      assertEquals(DataOutcome.Success(row), repository.getById("gone"))
      assertEquals(listOf("gone:tag_a:0", "gone:tag_b:1"), linkRows(driver))
    }
  }

  private fun movimiento(
      id: String,
      tipo: TipoMovimiento = TipoMovimiento.GASTO,
      monto: Money = Money(500, Moneda.COP),
      categoriaId: String = "cat_comida",
      titulo: String? = null,
      descripcion: String? = null,
      at: Instant = Instant.parse("2026-09-05T12:00:00Z"),
      etiquetaIds: List<String> = emptyList(),
  ) =
      Movimiento(
          id = id,
          tipo = tipo,
          monto = monto,
          fecha = LocalDate(2026, 9, 5),
          categoriaId = categoriaId,
          titulo = titulo,
          descripcion = descripcion,
          createdAt = at,
          etiquetaIds = etiquetaIds,
      )
}

private const val CATEGORY_HISTORY_BOUND = 1_000
private val LATER = Instant.parse("2026-09-05T13:00:00Z")
private val ETIQUETA_IDS = listOf("tag_a", "tag_b", "tag_c", "tag_d", "tag_e", "tag_f")
