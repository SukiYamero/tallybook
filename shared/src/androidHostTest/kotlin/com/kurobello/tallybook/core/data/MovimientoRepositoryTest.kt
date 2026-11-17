package com.kurobello.tallybook.core.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.Actividad
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaSelection
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MAX_ETIQUETAS_POR_MOVIMIENTO
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.MOVIMIENTO_TITLE_MAX_LENGTH
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.MovimientoUpdateResult
import com.kurobello.tallybook.core.model.RestoreResult
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.map
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
import kotlinx.coroutines.test.TestScope
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
      val database = seeded(driver)
      val repository =
          MovimientoRepositoryImpl(
              database,
              StandardTestDispatcher(testScheduler),
              changeClock,
          )
      val actividades = ActividadRepositoryImpl(database, StandardTestDispatcher(testScheduler))
      val captured = movimiento("captured", titulo = "cafe", etiquetaIds = listOf("tag_a"))
      val deleted =
          movimiento("deleted", titulo = "taxi", etiquetaIds = listOf("tag_b"), at = LATER)
      repository.insert(captured)
      repository.insert(deleted)

      assertEquals(DataOutcome.Success(captured), repository.deleteAndCapture(captured.id))
      assertEquals(DataOutcome.Success(deleted), repository.deleteAndCapture(deleted.id))

      val (captureStamp, deleteStamp) = changeClock.issuedStamps.drop(2).map { it.value }
      assertEquals(captureStamp to captureStamp, driver.storedStamps("movimiento", captured.id))
      assertEquals(deleteStamp to deleteStamp, driver.storedStamps("movimiento", deleted.id))
      assertEquals(emptyList(), linkRows(driver))
      val september = MovimientoDateRange(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30))
      assertEquals(DataOutcome.Success(emptyList()), repository.getAll())
      assertEquals(DataOutcome.Success(null), repository.getById(captured.id))
      assertEquals(
          DataOutcome.Success(emptyList()),
          repository.observeRecent(Moneda.COP, 5).first(),
      )
      assertEquals(
          DataOutcome.Success(emptyList()),
          actividades.observeActividadRange(Moneda.COP, september).first(),
      )
      assertEquals(
          DataOutcome.Success(MovimientoSearchSnapshot(emptyList(), emptyList())),
          actividades.observeFiltered(MovimientoReadCriteria(Moneda.COP)).first(),
      )
      assertEquals(DataOutcome.Success(emptyList()), actividades.observeYears(Moneda.COP).first())
      assertEquals(DataOutcome.Success(emptyList()), repository.observeCategoryHistory(5).first())

      assertEquals(DataOutcome.Success(null), repository.deleteAndCapture(deleted.id))
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
          DataOutcome.Success(RestoreResult.Restored),
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
          DataOutcome.Success(RestoreResult.AlreadyPresent),
          repository.restoreDeleted(row),
      )
      assertEquals(
          DataOutcome.Success(RestoreResult.Conflict),
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
                DataOutcome.Success(RestoreResult.Conflict),
                repository.restoreDeleted(different),
            )
          }

          assertEquals(tombstone, driver.storedStamps("movimiento", row.id))
          assertEquals(DataOutcome.Success(null), repository.getById(row.id))
          assertEquals(2, changeClock.issuedStamps.size)
          assertEquals(
              DataOutcome.Success(RestoreResult.Restored),
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
      repository.deleteAndCapture(row.id)
      val tombstone = driver.storedStamps("movimiento", row.id)
      val replacement =
          row.copy(monto = Money(900, Moneda.COP), etiquetaIds = listOf("tag_b"), createdAt = LATER)

      assertLocal(repository.insert(replacement))

      assertEquals(tombstone, driver.storedStamps("movimiento", row.id))
      assertEquals(DataOutcome.Success(emptyList()), repository.getAll())
      assertEquals(emptyList(), linkRows(driver))
      assertEquals(
          DataOutcome.Success(RestoreResult.Restored),
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
            cuentaId = Cuenta.defaultCashId(Moneda.USD),
            medio = MedioPago.EFECTIVO,
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
            cuentaId = Cuenta.defaultCashId(Moneda.COP),
            medio = MedioPago.EFECTIVO,
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
            cuentaId = Cuenta.defaultCashId(Moneda.MXN),
            medio = MedioPago.EFECTIVO,
            etiquetaIds = listOf("tag_a"),
        )

    repository.insert(movimiento)

    assertIs<DataOutcome.Success<Movimiento?>>(repository.deleteAndCapture(movimiento.id))
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
      val row = movimiento("read", monto = Money(Money.MAX_MINOR_UNITS, Moneda.CLP))
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
                  Money(Money.MAX_MINOR_UNITS, Moneda.USD),
                  LocalDate(2025, 12, 31),
                  "cat_sueldo",
                  listOf("tag_c"),
                  "updated",
                  "updated details",
                  original.cuentaId,
                  original.medio,
              )
          assertEquals(
              DataOutcome.Success(MovimientoUpdateResult.Updated),
              repository.update(original.id, changes),
          )
          val expected =
              original.copy(
                  tipo = TipoMovimiento.INGRESO,
                  monto = Money(Money.MAX_MINOR_UNITS, Moneda.USD),
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
  fun `amounts up to the domain maximum are stored and larger ones rejected`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val repository =
          MovimientoRepositoryImpl(
              seeded(driver),
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          )
      val largest = movimiento("largest").copy(monto = Money(Money.MAX_MINOR_UNITS, Moneda.COP))
      val tooLarge = Money(Money.MAX_MINOR_UNITS + 1, Moneda.COP)

      assertEquals(DataOutcome.Success(Unit), repository.insert(largest))
      assertInvalid(repository.insert(movimiento("too-large").copy(monto = tooLarge)))
      assertInvalid(repository.update(largest.id, largest.changes().copy(monto = tooLarge)))
      repository.deleteAndCapture(largest.id)
      assertInvalid(repository.restoreDeleted(largest.copy(monto = tooLarge)))
      assertEquals(DataOutcome.Success(emptyList()), repository.getAll())
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
  fun `capture preserves an unreadable row`() = runTest {
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
              monto = Money(Money.MAX_MINOR_UNITS, Moneda.CLP),
              descripcion = "a".repeat(MOVIMIENTO_DESCRIPTION_MAX_LENGTH),
              etiquetaIds = listOf("tag_b", "tag_a"),
          )
      val second = first.copy(id = "b", descripcion = null)
      repository.insert(first)
      repository.insert(second)
      assertEquals(DataOutcome.Success(first), repository.deleteAndCapture(first.id))
      assertEquals(
          DataOutcome.Success(RestoreResult.Restored),
          repository.restoreDeleted(first),
      )
      assertEquals(DataOutcome.Success(listOf(second, first)), repository.getAll())
      assertEquals(
          DataOutcome.Success(RestoreResult.AlreadyPresent),
          repository.restoreDeleted(first),
      )
      val conflict = first.copy(monto = Money(100, Moneda.CLP))
      assertEquals(
          DataOutcome.Success(RestoreResult.Conflict),
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
          DataOutcome.Success(RestoreResult.Restored),
          repository.restoreDeleted(row),
      )

      val restored = row.copy(etiquetaIds = listOf("tag_c", "tag_b"))
      assertEquals(DataOutcome.Success(restored), repository.getById(row.id))
      assertEquals(listOf("gone:tag_b:1", "gone:tag_c:0"), linkRows(driver))
      assertEquals(
          DataOutcome.Success(RestoreResult.AlreadyPresent),
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
              DataOutcome.Success(RestoreResult.Restored),
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
          DataOutcome.Success(RestoreResult.Restored),
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
              "2026-01-01",
              "row $index",
              null,
              (base + index.seconds).toString(),
              "cat_comida",
              "cuenta_efectivo_cop",
              "EFECTIVO",
              ChangeStamp.SEED.value,
              null,
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
          DataOutcome.Success(RestoreResult.Restored),
          repository.restoreDeleted(row),
      )
      assertEquals(DataOutcome.Success(row), repository.getById(row.id))
      assertEquals(
          DataOutcome.Success(RestoreResult.AlreadyPresent),
          repository.restoreDeleted(row),
      )
      for (different in
          listOf(
              listOf("tag_b", "tag_a"),
              listOf("tag_b", "tag_a", "tag_d"),
              listOf("tag_a", "tag_b", "tag_c"),
          )) {
        assertEquals(
            DataOutcome.Success(RestoreResult.Conflict),
            repository.restoreDeleted(row.copy(etiquetaIds = different)),
        )
      }
      assertEquals(
          DataOutcome.Success(RestoreResult.Conflict),
          repository.restoreDeleted(row.copy(categoriaId = "cat_cine")),
      )
      assertEquals(DataOutcome.Success(row), repository.getById(row.id))

      assertEquals(DataOutcome.Success(row), repository.deleteAndCapture(row.id))
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
      val inRange = mutableListOf<DataOutcome<List<Actividad>>>()
      val filtered = mutableListOf<DataOutcome<MovimientoSearchSnapshot>>()
      backgroundScope.launch { repository.observeRecent(Moneda.COP, 5).collect(recent::add) }
      val actividades = ActividadRepositoryImpl(database, StandardTestDispatcher(testScheduler))
      backgroundScope.launch {
        actividades.observeActividadRange(Moneda.COP, september).collect(inRange::add)
      }
      backgroundScope.launch {
        actividades.observeFiltered(MovimientoReadCriteria(Moneda.COP)).collect(filtered::add)
      }
      testScheduler.runCurrent()

      database.movimientoQueries.insertMovimientoEtiqueta("one", "tag_b", 1)
      testScheduler.runCurrent()

      val updated = row.copy(etiquetaIds = listOf("tag_a", "tag_b"))
      assertEquals(DataOutcome.Success(listOf(updated)), recent.last())
      assertEquals(DataOutcome.Success(listOf(Actividad.DeMovimiento(updated))), inRange.last())
      assertEquals(
          DataOutcome.Success(
              MovimientoSearchSnapshot(listOf(Actividad.DeMovimiento(updated)), listOf(2026))
          ),
          filtered.last(),
      )
    }
  }

  @Test
  fun `defaultSelection without history picks the primary cash account`() = runTest {
    withSelectionFixture { repository, _, _ ->
      assertEquals(
          DataOutcome.Success(CuentaSelection(COP_CASH, MedioPago.EFECTIVO, Moneda.COP)),
          repository.defaultSelection(Moneda.COP, TipoMovimiento.GASTO),
      )
    }
  }

  @Test
  fun `defaultSelection without a medio ignores the newest movimiento and picks primary cash`() =
      runTest {
        withSelectionFixture { repository, database, _ ->
          database.insertLegacy(selectionRow("newest", NEQUI, MedioPago.DEBITO, at = 2))
          val cash = DataOutcome.Success(CuentaSelection(COP_CASH, MedioPago.EFECTIVO, Moneda.COP))

          assertEquals(cash, repository.defaultSelection(Moneda.COP, TipoMovimiento.GASTO))
          assertEquals(cash, repository.defaultSelection(Moneda.COP, TipoMovimiento.INGRESO))
        }
      }

  @Test
  fun `defaultSelection with a medio skips a last used cuenta archived tombstoned or incompatible`() =
      runTest {
        withSelectionFixture { repository, database, driver ->
          database.insertLegacy(selectionRow("last", BANCOLOMBIA, MedioPago.DEBITO, at = 2))
          suspend fun select() =
              repository.defaultSelection(Moneda.COP, TipoMovimiento.GASTO, MedioPago.DEBITO)
          val nequi = DataOutcome.Success(CuentaSelection(NEQUI, MedioPago.DEBITO, Moneda.COP))
          assertEquals(
              DataOutcome.Success(CuentaSelection(BANCOLOMBIA, MedioPago.DEBITO, Moneda.COP)),
              select(),
          )

          driver.execute(null, "UPDATE cuenta SET archivada = 1 WHERE id = '$BANCOLOMBIA'", 0)
          assertEquals(nequi, select())

          driver.execute(null, "UPDATE cuenta SET archivada = 0 WHERE id = '$BANCOLOMBIA'", 0)
          driver.tombstone("cuenta", BANCOLOMBIA)
          assertEquals(nequi, select())

          driver.execute(
              null,
              "UPDATE cuenta SET deleted_at = NULL, tipo = 'CREDITO' WHERE id = '$BANCOLOMBIA'",
              0,
          )
          assertEquals(nequi, select())
        }
      }

  @Test
  fun `defaultSelection with a medio ignores a tombstoned last movimiento`() = runTest {
    withSelectionFixture { repository, database, driver ->
      database.insertLegacy(selectionRow("kept", BANCOLOMBIA, MedioPago.DEBITO, at = 1))
      database.insertLegacy(selectionRow("deleted", NEQUI, MedioPago.DEBITO, at = 2))
      driver.tombstone("movimiento", "deleted")

      assertEquals(
          DataOutcome.Success(CuentaSelection(BANCOLOMBIA, MedioPago.DEBITO, Moneda.COP)),
          repository.defaultSelection(Moneda.COP, TipoMovimiento.GASTO, MedioPago.DEBITO),
      )
    }
  }

  @Test
  fun `defaultSelection falls back to another cash account when the primary one is unavailable`() =
      runTest {
        withSelectionFixture { repository, database, driver ->
          database.insertCuentaRow(
              selectionCuenta(BILLETERA, TipoCuenta.EFECTIVO, "2026-01-06T00:00:00Z"),
              ChangeStamp.SEED,
          )
          val billetera =
              DataOutcome.Success(CuentaSelection(BILLETERA, MedioPago.EFECTIVO, Moneda.COP))

          driver.execute(null, "UPDATE cuenta SET archivada = 1 WHERE id = '$COP_CASH'", 0)
          assertEquals(billetera, repository.defaultSelection(Moneda.COP, TipoMovimiento.GASTO))

          driver.execute(null, "UPDATE cuenta SET archivada = 0 WHERE id = '$COP_CASH'", 0)
          driver.tombstone("cuenta", COP_CASH)
          assertEquals(billetera, repository.defaultSelection(Moneda.COP, TipoMovimiento.INGRESO))
        }
      }

  @Test
  fun `defaultSelection with a medio ranks its newest cuenta ahead of the primary cash account`() =
      runTest {
        withSelectionFixture { repository, database, _ ->
          database.insertCuentaRow(
              selectionCuenta(BILLETERA, TipoCuenta.EFECTIVO, "2026-01-06T00:00:00Z"),
              ChangeStamp.SEED,
          )
          database.insertLegacy(selectionRow("wallet", BILLETERA, MedioPago.EFECTIVO, at = 1))

          assertEquals(
              DataOutcome.Success(CuentaSelection(BILLETERA, MedioPago.EFECTIVO, Moneda.COP)),
              repository.defaultSelection(Moneda.COP, TipoMovimiento.GASTO, MedioPago.EFECTIVO),
          )
          assertEquals(
              DataOutcome.Success(CuentaSelection(COP_CASH, MedioPago.EFECTIVO, Moneda.COP)),
              repository.defaultSelection(Moneda.COP, TipoMovimiento.GASTO),
          )
        }
      }

  @Test
  fun `defaultSelection without a primary cash account falls back by currency then creation`() =
      runTest {
        withSelectionFixture { repository, _, driver ->
          driver.execute(null, "UPDATE cuenta SET archivada = 1 WHERE id = '$COP_CASH'", 0)
          assertEquals(
              DataOutcome.Success(CuentaSelection(NEQUI, MedioPago.DEBITO, Moneda.COP)),
              repository.defaultSelection(Moneda.COP, TipoMovimiento.GASTO),
          )
          assertEquals(
              DataOutcome.Success(CuentaSelection(USD_CASH, MedioPago.EFECTIVO, Moneda.USD)),
              repository.defaultSelection(Moneda.MXN, TipoMovimiento.GASTO),
          )
        }
      }

  @Test
  fun `defaultSelection with a medio uses the last cuenta used with it and its fallbacks`() =
      runTest {
        withSelectionFixture { repository, database, driver ->
          database.insertLegacy(selectionRow("card", NU, MedioPago.CREDITO, at = 1))
          database.insertLegacy(selectionRow("cash", COP_CASH, MedioPago.EFECTIVO, at = 2))

          assertEquals(
              DataOutcome.Success(CuentaSelection(NU, MedioPago.CREDITO, Moneda.COP)),
              repository.defaultSelection(Moneda.COP, TipoMovimiento.GASTO, MedioPago.CREDITO),
          )
          assertEquals(
              DataOutcome.Success(CuentaSelection(NEQUI, MedioPago.DEBITO, Moneda.COP)),
              repository.defaultSelection(Moneda.COP, TipoMovimiento.GASTO, MedioPago.DEBITO),
          )
          assertEquals(
              DataOutcome.Success(CuentaSelection(COP_CASH, MedioPago.EFECTIVO, Moneda.COP)),
              repository.defaultSelection(Moneda.MXN, TipoMovimiento.INGRESO, MedioPago.EFECTIVO),
          )
          driver.tombstone("cuenta", NU)
          assertEquals(
              DataOutcome.Success(null),
              repository.defaultSelection(Moneda.COP, TipoMovimiento.GASTO, MedioPago.CREDITO),
          )
          assertInvalid(
              repository.defaultSelection(Moneda.COP, TipoMovimiento.INGRESO, MedioPago.DEBITO)
          )
        }
      }

  @Test
  fun `insert and update accept exactly the medio cuenta and tipo combinations of the table`() =
      runTest {
        withSelectionFixture { repository, _, _ ->
          val cuentas =
              mapOf(
                  TipoCuenta.EFECTIVO to COP_CASH,
                  TipoCuenta.BANCO to NEQUI,
                  TipoCuenta.CREDITO to NU,
              )
          repository.insert(movimiento("edited"))
          var next = 0
          for (medio in MedioPago.entries) {
            for ((tipoCuenta, cuentaId) in cuentas) {
              for (tipo in TipoMovimiento.entries) {
                val valid = tipoCuenta in medio.compatibleTipos && medio.allows(tipo)
                val row =
                    movimiento("row-${next++}", tipo = tipo)
                        .copy(cuentaId = cuentaId, medio = medio)
                val inserted = repository.insert(row)
                val updated = repository.update("edited", row.changes())
                if (valid) {
                  assertEquals(DataOutcome.Success(Unit), inserted, "$medio $tipoCuenta $tipo")
                  assertEquals(
                      DataOutcome.Success(MovimientoUpdateResult.Updated),
                      updated,
                      "$medio $tipoCuenta $tipo",
                  )
                } else {
                  assertInvalid(inserted)
                  assertInvalid(updated)
                }
              }
            }
          }
          assertEquals(
              DataOutcome.Success(8),
              repository.getAll().map { rows -> rows.size },
          )
        }
      }

  @Test
  fun `insert rejects a missing tombstoned or archived cuenta and a foreign currency`() = runTest {
    withSelectionFixture { repository, _, driver ->
      val row = movimiento("row").copy(cuentaId = NEQUI, medio = MedioPago.DEBITO)
      driver.execute(null, "UPDATE cuenta SET archivada = 1 WHERE id = '$NU'", 0)
      driver.tombstone("cuenta", COP_CASH)

      for (invalid in
          listOf(
              row.copy(cuentaId = "absent"),
              row.copy(cuentaId = COP_CASH, medio = MedioPago.EFECTIVO),
              row.copy(cuentaId = NU, medio = MedioPago.CREDITO),
              row.copy(monto = Money(500, Moneda.USD)),
              row.copy(cuentaId = USD_CASH, medio = MedioPago.EFECTIVO),
          )) {
        assertInvalid(repository.insert(invalid))
      }
      assertEquals(DataOutcome.Success(emptyList()), repository.getAll())
      assertEquals(DataOutcome.Success(Unit), repository.insert(row))
    }
  }

  @Test
  fun `insertAll writes every movimiento`() = runTest {
    withSelectionFixture { repository, _, _ ->
      val rows = listOf(movimiento("one"), movimiento("two", monto = Money(900, Moneda.COP)))

      assertEquals(DataOutcome.Success(Unit), repository.insertAll(rows))

      val stored = assertIs<DataOutcome.Success<List<Movimiento>>>(repository.getAll()).data
      assertEquals(setOf("one", "two"), stored.map { it.id }.toSet())
    }
  }

  @Test
  fun `insertAll writes nothing when one movimiento is rejected`() = runTest {
    withSelectionFixture { repository, _, _ ->
      val rows = listOf(movimiento("one"), movimiento("two").copy(cuentaId = "absent"))

      assertInvalid(repository.insertAll(rows))

      assertEquals(DataOutcome.Success(emptyList()), repository.getAll())
    }
  }

  @Test
  fun `update keeps an archived cuenta only while unchanged and never changes the currency`() =
      runTest {
        withSelectionFixture { repository, database, driver ->
          val row = movimiento("row").copy(cuentaId = NEQUI, medio = MedioPago.DEBITO)
          repository.insert(row)
          driver.execute(null, "UPDATE cuenta SET archivada = 1 WHERE id = '$NEQUI'", 0)

          val kept = row.changes().copy(titulo = "kept", medio = MedioPago.TRANSFERENCIA)
          assertEquals(
              DataOutcome.Success(MovimientoUpdateResult.Updated),
              repository.update(row.id, kept),
          )
          driver.execute(null, "UPDATE cuenta SET archivada = 1 WHERE id = '$NU'", 0)
          assertInvalid(
              repository.update(row.id, kept.copy(cuentaId = NU, medio = MedioPago.CREDITO))
          )
          for (otherCurrency in
              listOf(
                  kept.copy(cuentaId = USD_CASH, medio = MedioPago.EFECTIVO),
                  kept.copy(
                      cuentaId = USD_CASH,
                      medio = MedioPago.EFECTIVO,
                      monto = Money(500, Moneda.USD),
                  ),
              )) {
            assertInvalid(repository.update(row.id, otherCurrency))
          }
          val expected = row.copy(titulo = "kept", medio = MedioPago.TRANSFERENCIA)
          assertEquals(DataOutcome.Success(expected), repository.getById(row.id))

          val moved = kept.copy(cuentaId = COP_CASH, medio = MedioPago.EFECTIVO)
          assertEquals(
              DataOutcome.Success(MovimientoUpdateResult.Updated),
              repository.update(row.id, moved),
          )
          assertInvalid(
              repository.update(row.id, moved.copy(cuentaId = NEQUI, medio = MedioPago.DEBITO))
          )
          assertEquals(
              COP_CASH,
              database.movimientoQueries.selectMovimientoById(row.id).executeAsOne().cuenta_id,
          )
        }
      }

  @Test
  fun `update rejects a stored medio its cuenta type no longer allows`() = runTest {
    withSelectionFixture { repository, _, driver ->
      val row = movimiento("row").copy(cuentaId = NEQUI, medio = MedioPago.DEBITO)
      repository.insert(row)
      driver.execute(null, "UPDATE cuenta SET tipo = 'EFECTIVO' WHERE id = '$NEQUI'", 0)

      assertInvalid(repository.update(row.id, row.changes().copy(titulo = "kept")))
      assertEquals(DataOutcome.Success(row), repository.getById(row.id))
      assertInvalid(repository.update(row.id, row.changes().copy(medio = MedioPago.TRANSFERENCIA)))
      assertInvalid(repository.update(row.id, row.changes().copy(tipo = TipoMovimiento.INGRESO)))
    }
  }

  @Test
  fun `restore into an archived cuenta revives the row and the cuenta stays archived`() = runTest {
    withSelectionFixture { repository, database, _ ->
      val row = movimiento("row").copy(cuentaId = NEQUI, medio = MedioPago.DEBITO)
      repository.insert(row)
      assertEquals(DataOutcome.Success(row), repository.deleteAndCapture(row.id))
      database.cuentaQueries.updateCuentaArchivada(1L, ChangeStamp.SEED.value, NEQUI)

      assertEquals(DataOutcome.Success(RestoreResult.Restored), repository.restoreDeleted(row))

      assertEquals(DataOutcome.Success(row), repository.getById(row.id))
      assertTrue(database.cuentaQueries.selectCuentaById(NEQUI).executeAsOne().toDomain().archivada)
    }
  }

  @Test
  fun `restore without its cuenta is MissingCuenta and keeps the tombstone`() = runTest {
    withSelectionFixture { repository, _, driver ->
      val row = movimiento("row").copy(cuentaId = NEQUI, medio = MedioPago.DEBITO)
      repository.insert(row)
      repository.deleteAndCapture(row.id)
      val tombstone = driver.storedStamps("movimiento", row.id)
      driver.tombstone("cuenta", NEQUI)

      assertEquals(DataOutcome.Success(RestoreResult.MissingCuenta), repository.restoreDeleted(row))
      assertEquals(
          DataOutcome.Success(RestoreResult.MissingCuenta),
          repository.restoreDeleted(row.copy(id = "never-stored", cuentaId = "absent")),
      )

      assertEquals(tombstone, driver.storedStamps("movimiento", row.id))
      assertEquals(null, driver.storedStamps("movimiento", "never-stored"))
    }
  }

  @Test
  fun `restore after the cuenta changed currency is a conflict that keeps the tombstone`() =
      runTest {
        withSelectionFixture { repository, _, driver ->
          val row = movimiento("row").copy(cuentaId = NEQUI, medio = MedioPago.DEBITO)
          repository.insert(row)
          repository.deleteAndCapture(row.id)
          val tombstone = driver.storedStamps("movimiento", row.id)
          driver.execute(null, "UPDATE cuenta SET moneda = 'USD' WHERE id = '$NEQUI'", 0)

          assertEquals(DataOutcome.Success(RestoreResult.Conflict), repository.restoreDeleted(row))
          assertEquals(tombstone, driver.storedStamps("movimiento", row.id))
        }
      }

  @Test
  fun `restore after the cuenta changed type is a conflict that keeps the tombstone`() = runTest {
    withSelectionFixture { repository, _, driver ->
      val row = movimiento("row").copy(cuentaId = NU, medio = MedioPago.CREDITO)
      repository.insert(row)
      repository.deleteAndCapture(row.id)
      val tombstone = driver.storedStamps("movimiento", row.id)
      driver.execute(null, "UPDATE cuenta SET tipo = 'BANCO' WHERE id = '$NU'", 0)

      assertEquals(DataOutcome.Success(RestoreResult.Conflict), repository.restoreDeleted(row))
      assertEquals(
          DataOutcome.Success(RestoreResult.Conflict),
          repository.restoreDeleted(row.copy(id = "never-stored")),
      )

      assertEquals(tombstone, driver.storedStamps("movimiento", row.id))
      assertEquals(null, driver.storedStamps("movimiento", "never-stored"))
    }
  }

  @Test
  fun `range summary adds live ingresos and gastos of one currency inside the range`() = runTest {
    withSelectionFixture { repository, _, driver ->
      val october = MovimientoDateRange(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31))
      listOf(
              movimiento("first", monto = Money(100, Moneda.COP)).copy(fecha = october.start),
              movimiento("last", monto = Money(200, Moneda.COP)).copy(fecha = october.end),
              movimiento("income", tipo = TipoMovimiento.INGRESO, monto = Money(1_000, Moneda.COP))
                  .copy(fecha = LocalDate(2026, 10, 15)),
              movimiento("before", monto = Money(400, Moneda.COP))
                  .copy(fecha = LocalDate(2026, 9, 30)),
              movimiento("after", monto = Money(800, Moneda.COP))
                  .copy(fecha = LocalDate(2026, 11, 1)),
              movimiento("dollars", monto = Money(1_600, Moneda.USD))
                  .copy(fecha = LocalDate(2026, 10, 15)),
              movimiento("deleted", monto = Money(3_200, Moneda.COP))
                  .copy(fecha = LocalDate(2026, 10, 15)),
          )
          .forEach { repository.insert(it) }
      driver.tombstone("movimiento", "deleted")

      assertEquals(
          DataOutcome.Success(MovimientoSummary(Money(1_000, Moneda.COP), Money(300, Moneda.COP))),
          repository.observeSummary(Moneda.COP, october).first(),
      )
      assertEquals(
          DataOutcome.Success(MovimientoSummary(Money(0, Moneda.MXN), Money(0, Moneda.MXN))),
          repository.observeSummary(Moneda.MXN, october).first(),
      )
      assertInvalid(
          repository
              .observeSummary(
                  Moneda.COP,
                  MovimientoDateRange(LocalDate(2026, 1, 1), LocalDate(10_000, 1, 1)),
              )
              .first()
      )
    }
  }

  /**
   * Cash in COP and USD, the Nequi bank, the NU card and the Bancolombia bank in COP, created in
   * that order.
   */
  private suspend fun TestScope.withSelectionFixture(
      block: suspend (MovimientoRepository, TallybookDatabase, SqlDriver) -> Unit
  ) {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val database = seeded(driver)
      driver.execute(null, "DELETE FROM cuenta", 0)
      listOf(
              cashCuenta(Moneda.COP, Instant.parse("2026-01-01T00:00:00Z")),
              cashCuenta(Moneda.USD, Instant.parse("2026-01-02T00:00:00Z")),
              selectionCuenta(NEQUI, TipoCuenta.BANCO, "2026-01-03T00:00:00Z"),
              selectionCuenta(NU, TipoCuenta.CREDITO, "2026-01-04T00:00:00Z"),
              selectionCuenta(BANCOLOMBIA, TipoCuenta.BANCO, "2026-01-05T00:00:00Z"),
          )
          .forEach { database.insertCuentaRow(it, ChangeStamp.SEED) }
      block(
          MovimientoRepositoryImpl(
              database,
              StandardTestDispatcher(testScheduler),
              TestChangeClock(),
          ),
          database,
          driver,
      )
    }
  }

  private fun selectionCuenta(id: String, tipo: TipoCuenta, createdAt: String) =
      Cuenta(
          id = id,
          nombre = id,
          tipo = tipo,
          saldoInicial = Money(0, Moneda.COP),
          diaCorte = null,
          archivada = false,
          createdAt = Instant.parse(createdAt),
          color = tipo.defaultColor,
      )

  private fun selectionRow(id: String, cuentaId: String, medio: MedioPago, at: Int) =
      movimiento(id, at = Instant.parse("2026-09-05T12:00:00Z") + at.seconds)
          .copy(cuentaId = cuentaId, medio = medio)

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
      MovimientoChanges(
          tipo,
          monto,
          fecha,
          categoriaId,
          etiquetaIds,
          titulo,
          descripcion,
          cuentaId,
          medio,
      )

  private fun insertUnreadable(database: TallybookDatabase, id: String) {
    database.movimientoQueries.insertMovimiento(
        id,
        "INVALID",
        500,
        "2026-09-05",
        null,
        null,
        "invalid-instant",
        "cat_comida",
        "cuenta_efectivo_cop",
        "EFECTIVO",
        ChangeStamp.SEED.value,
        null,
    )
  }

  private fun insertRaw(database: TallybookDatabase, row: Movimiento) {
    database.movimientoQueries.insertMovimiento(
        row.id,
        row.tipo.name,
        row.monto.minorUnits,
        row.fecha.toString(),
        row.titulo,
        row.descripcion,
        row.createdAt.toString(),
        row.categoriaId,
        Cuenta.defaultCashId(row.monto.moneda),
        "EFECTIVO",
        ChangeStamp.SEED.value,
        null,
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
          DataOutcome.Success(RestoreResult.Conflict),
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
          DataOutcome.Success(RestoreResult.Restored),
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
          cuentaId = Cuenta.defaultCashId(monto.moneda),
          medio = MedioPago.EFECTIVO,
          etiquetaIds = etiquetaIds,
      )
}

private const val CATEGORY_HISTORY_BOUND = 1_000
private const val NEQUI = "nequi"
private const val NU = "nu"
private const val BANCOLOMBIA = "bancolombia"
private const val BILLETERA = "billetera"
private val COP_CASH = Cuenta.defaultCashId(Moneda.COP)
private val USD_CASH = Cuenta.defaultCashId(Moneda.USD)
private val LATER = Instant.parse("2026-09-05T13:00:00Z")
private val ETIQUETA_IDS = listOf("tag_a", "tag_b", "tag_c", "tag_d", "tag_e", "tag_f")
