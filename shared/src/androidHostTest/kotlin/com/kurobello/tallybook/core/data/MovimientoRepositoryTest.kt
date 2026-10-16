package com.kurobello.tallybook.core.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MAX_CATEGORIAS_POR_MOVIMIENTO
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoRestoreResult
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
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
            valid.copy(categoriaIds = emptyList()),
            valid.copy(categoriaIds = listOf("")),
            valid.copy(categoriaIds = listOf(" ")),
            valid.copy(categoriaIds = listOf("a", "a")),
            valid.copy(categoriaIds = List(MAX_CATEGORIAS_POR_MOVIMIENTO + 1) { "c$it" }),
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
            categoriaIds = listOf("cat_comida"),
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
            categoriaIds = listOf("cat_sueldo"),
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
            categoriaIds = listOf("cat_transporte"),
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
                  listOf("missing-legacy-category"),
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
                  categoriaIds = listOf("missing-legacy-category"),
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
              valid.copy(categoriaIds = listOf(" ")),
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
  fun `capture rejects nonrestorable legacy rows and preserves their exact stored fields`() =
      runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
          TallybookDatabase.Schema.create(driver)
          val database = TallybookDatabase(driver)
          val repository = MovimientoRepositoryImpl(database, StandardTestDispatcher(testScheduler))
          val whitespace = movimiento("whitespace-category", categoriaIds = listOf(" "))
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
      TallybookDatabase.Schema.create(driver)
      val repository =
          MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
      val valid = movimiento("restore")
      for (row in
          listOf(
              valid.copy(id = " "),
              valid.copy(categoriaIds = listOf(" ")),
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

  @Test
  fun `category history returns the newest described rows within the limit`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val repository =
          MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
      val base = Instant.parse("2026-09-05T12:00:00Z")
      val rows =
          listOf(
              movimiento(
                  "oldest",
                  categoriaIds = listOf("cat_cafe"),
                  descripcion = "cafe",
                  at = base,
              ),
              movimiento("blank", descripcion = "   ", at = base + 1.seconds),
              movimiento("empty", descripcion = "", at = base + 2.seconds),
              movimiento("none", descripcion = null, at = base + 3.seconds),
              movimiento(
                  "tie-a",
                  categoriaIds = listOf("cat_cine"),
                  descripcion = "cine",
                  at = base + 4.seconds,
              ),
              movimiento(
                  "tie-b",
                  categoriaIds = listOf("cat_taxi"),
                  descripcion = "uber",
                  at = base + 4.seconds,
              ),
              movimiento(
                  "newest",
                  categoriaIds = listOf("cat_gasolina"),
                  descripcion = "terpel",
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
          all.data.map(CategoryHistoryRow::descripcion),
      )
      assertEquals(listOf("cat_gasolina", "cat_taxi"), limited.data.map { it.categoriaId })
      assertEquals(base + 5.seconds, all.data.first().createdAt)
    }
  }

  @Test
  fun `category history rejects a non positive limit and wraps unavailable storage`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    try {
      TallybookDatabase.Schema.create(driver)
      val repository =
          MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))

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
      TallybookDatabase.Schema.create(driver)
      val repository =
          MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
      val emissions = mutableListOf<DataOutcome<List<CategoryHistoryRow>>>()
      val collector = backgroundScope.launch {
        repository.observeCategoryHistory(5).collect(emissions::add)
      }
      testScheduler.runCurrent()
      assertEquals(DataOutcome.Success(emptyList<CategoryHistoryRow>()), emissions.last())

      val row = movimiento("one", categoriaIds = listOf("cat_cafe"), descripcion = "cafe")
      assertEquals(DataOutcome.Success(Unit), repository.insert(row))
      testScheduler.runCurrent()
      assertEquals(
          DataOutcome.Success(listOf(CategoryHistoryRow("cafe", "cat_cafe", row.createdAt))),
          emissions.last(),
      )

      repository.update("one", row.changes().copy(categoriaIds = listOf("cat_restaurante")))
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
      TallybookDatabase.Schema.create(driver)
      val database = TallybookDatabase(driver)
      val repository = MovimientoRepositoryImpl(database, StandardTestDispatcher(testScheduler))
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
              (base + index.seconds).toString(),
          )
          database.movimientoQueries.insertMovimientoCategoria("id$index", "cat_comida", 0)
        }
      }

      val rows =
          assertIs<DataOutcome.Success<List<CategoryHistoryRow>>>(
                  repository.observeCategoryHistory(CATEGORY_HISTORY_BOUND).first()
              )
              .data

      assertEquals(CATEGORY_HISTORY_BOUND, rows.size)
      assertEquals("row ${CATEGORY_HISTORY_BOUND + 199}", rows.first().descripcion)
      assertEquals("row 200", rows.last().descripcion)
    }
  }

  @Test
  fun `one and six categorias round-trip in display order`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val repository =
          MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
      val single = movimiento("single", categoriaIds = listOf("z"))
      val maximum =
          movimiento("maximum", categoriaIds = listOf("f", "b", "e", "a", "d", "c"), at = LATER)

      assertEquals(DataOutcome.Success(Unit), repository.insert(single))
      assertEquals(DataOutcome.Success(Unit), repository.insert(maximum))

      assertEquals(DataOutcome.Success(single), repository.getById("single"))
      assertEquals(DataOutcome.Success(maximum), repository.getById("maximum"))
      assertEquals(DataOutcome.Success(listOf(maximum, single)), repository.getAll())
    }
  }

  @Test
  fun `invalid category sets are rejected on update and restore without writing`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val repository =
          MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
      val original = movimiento("valid", categoriaIds = listOf("a", "b"))
      repository.insert(original)
      val invalid =
          listOf(
              emptyList(),
              listOf(""),
              listOf("a", " "),
              listOf("a", "b", "a"),
              listOf("a\u001Eb"),
              listOf("a", "b\u001Fc"),
              List(MAX_CATEGORIAS_POR_MOVIMIENTO + 1) { "c$it" },
          )

      for (ids in invalid) {
        assertInvalid(repository.update(original.id, original.changes().copy(categoriaIds = ids)))
        assertInvalid(repository.restoreDeleted(original.copy(id = "restore", categoriaIds = ids)))
        assertInvalid(repository.insert(original.copy(id = "insert", categoriaIds = ids)))
      }

      assertEquals(DataOutcome.Success(listOf(original)), repository.getAll())
      assertEquals(listOf("valid:a:0", "valid:b:1"), joinRows(driver))
    }
  }

  @Test
  fun `update replaces the whole category set and leaves no stale join rows`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val repository =
          MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
      val original = movimiento("edit", categoriaIds = listOf("a", "b", "c"))
      val sibling = movimiento("sibling", categoriaIds = listOf("a"), at = LATER)
      repository.insert(original)
      repository.insert(sibling)

      val reordered = original.changes().copy(categoriaIds = listOf("d", "c", "a"))
      assertEquals(
          DataOutcome.Success(MovimientoUpdateResult.Updated),
          repository.update(original.id, reordered),
      )

      assertEquals(
          DataOutcome.Success(original.copy(categoriaIds = listOf("d", "c", "a"))),
          repository.getById(original.id),
      )
      assertEquals(DataOutcome.Success(sibling), repository.getById("sibling"))
      assertEquals(listOf("edit:a:2", "edit:c:1", "edit:d:0", "sibling:a:0"), joinRows(driver))
    }
  }

  @Test
  fun `a failing category write rolls back the movimiento insert and update`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val repository =
          MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
      val original = movimiento("kept", categoriaIds = listOf("a", "b"))
      repository.insert(original)
      driver.execute(
          null,
          "CREATE TRIGGER block_category BEFORE INSERT ON movimiento_categoria " +
              "WHEN new.categoria_id = 'bad' BEGIN SELECT RAISE(ABORT, 'blocked'); END",
          0,
      )

      assertLocal(repository.insert(movimiento("new", categoriaIds = listOf("a", "bad"))))
      assertLocal(
          repository.update(
              original.id,
              original.changes().copy(monto = Money(900, Moneda.COP), categoriaIds = listOf("bad")),
          )
      )

      assertEquals(DataOutcome.Success(listOf(original)), repository.getAll())
      assertEquals(listOf("kept:a:0", "kept:b:1"), joinRows(driver))
    }
  }

  @Test
  fun `delete capture and restore carry the categorias and detect a different set`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val repository =
          MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
      val row = movimiento("gone", categoriaIds = listOf("b", "a", "c"))
      val other = movimiento("other", categoriaIds = listOf("a"), at = LATER)
      repository.insert(row)
      repository.insert(other)

      assertEquals(DataOutcome.Success(row), repository.deleteAndCapture(row.id))
      assertEquals(listOf("other:a:0"), joinRows(driver))
      assertEquals(
          DataOutcome.Success(MovimientoRestoreResult.Restored),
          repository.restoreDeleted(row),
      )
      assertEquals(DataOutcome.Success(row), repository.getById(row.id))
      assertEquals(
          DataOutcome.Success(MovimientoRestoreResult.AlreadyPresent),
          repository.restoreDeleted(row),
      )
      for (different in listOf(listOf("b", "a"), listOf("b", "a", "d"), listOf("a", "b", "c"))) {
        assertEquals(
            DataOutcome.Success(MovimientoRestoreResult.Conflict),
            repository.restoreDeleted(row.copy(categoriaIds = different)),
        )
      }
      assertEquals(DataOutcome.Success(row), repository.getById(row.id))

      assertEquals(DataOutcome.Success(Unit), repository.delete(row.id))
      assertEquals(listOf("other:a:0"), joinRows(driver))
    }
  }

  @Test
  fun `category history lists one row per categoria inside a movimiento limit`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val repository =
          MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
      val base = Instant.parse("2026-09-05T12:00:00Z")
      repository.insert(
          movimiento("old", categoriaIds = listOf("cat_cafe"), descripcion = "cafe", at = base)
      )
      repository.insert(
          movimiento(
              "new",
              categoriaIds = listOf("cat_comida", "cat_social", "cat_ocio"),
              descripcion = "cena",
              at = base + 1.seconds,
          )
      )

      val latest =
          assertIs<DataOutcome.Success<List<CategoryHistoryRow>>>(
              repository.observeCategoryHistory(1).first()
          )
      val both =
          assertIs<DataOutcome.Success<List<CategoryHistoryRow>>>(
              repository.observeCategoryHistory(2).first()
          )

      assertEquals(
          listOf("cat_comida", "cat_social", "cat_ocio"),
          latest.data.map(CategoryHistoryRow::categoriaId),
      )
      assertEquals(
          listOf("cat_comida", "cat_social", "cat_ocio", "cat_cafe"),
          both.data.map(CategoryHistoryRow::categoriaId),
      )
    }
  }

  @Test
  fun `observed reads emit when only the category join table changes`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val database = TallybookDatabase(driver)
      val repository = MovimientoRepositoryImpl(database, StandardTestDispatcher(testScheduler))
      val row = movimiento("one", categoriaIds = listOf("a"), descripcion = "cafe")
      repository.insert(row)
      val september = MovimientoDateRange(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30))
      val recent = mutableListOf<DataOutcome<List<Movimiento>>>()
      val inRange = mutableListOf<DataOutcome<List<Movimiento>>>()
      val filtered = mutableListOf<DataOutcome<MovimientoSearchSnapshot>>()
      val history = mutableListOf<DataOutcome<List<CategoryHistoryRow>>>()
      backgroundScope.launch { repository.observeRecent(Moneda.COP, 5).collect(recent::add) }
      backgroundScope.launch {
        repository.observeRange(Moneda.COP, september).collect(inRange::add)
      }
      backgroundScope.launch {
        repository.observeFiltered(MovimientoReadCriteria(Moneda.COP)).collect(filtered::add)
      }
      backgroundScope.launch { repository.observeCategoryHistory(5).collect(history::add) }
      testScheduler.runCurrent()

      database.movimientoQueries.insertMovimientoCategoria("one", "b", 1)
      testScheduler.runCurrent()

      val updated = row.copy(categoriaIds = listOf("a", "b"))
      assertEquals(DataOutcome.Success(listOf(updated)), recent.last())
      assertEquals(DataOutcome.Success(listOf(updated)), inRange.last())
      assertEquals(
          DataOutcome.Success(MovimientoSearchSnapshot(listOf(updated), listOf(2026))),
          filtered.last(),
      )
      assertEquals(
          listOf("a", "b"),
          assertIs<DataOutcome.Success<List<CategoryHistoryRow>>>(history.last()).data.map {
            it.categoriaId
          },
      )
    }
  }

  private fun joinRows(driver: SqlDriver): List<String> =
      driver
          .executeQuery(
              null,
              "SELECT movimiento_id || ':' || categoria_id || ':' || posicion " +
                  "FROM movimiento_categoria ORDER BY 1",
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
      MovimientoChanges(tipo, monto, fecha, categoriaIds, descripcion)

  private fun insertUnreadable(database: TallybookDatabase, id: String) {
    database.movimientoQueries.insertMovimiento(
        id,
        "INVALID",
        500,
        "COP",
        "2026-09-05",
        null,
        "invalid-instant",
    )
    database.movimientoQueries.insertMovimientoCategoria(id, "cat_comida", 0)
  }

  private fun insertRaw(database: TallybookDatabase, row: Movimiento) {
    database.movimientoQueries.insertMovimiento(
        row.id,
        row.tipo.name,
        row.monto.minorUnits,
        row.monto.moneda.name,
        row.fecha.toString(),
        row.descripcion,
        row.createdAt.toString(),
    )
    row.categoriaIds.forEachIndexed { posicion, categoriaId ->
      database.movimientoQueries.insertMovimientoCategoria(row.id, categoriaId, posicion.toLong())
    }
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

  @Test
  fun `restore replaces orphaned join rows left by an interrupted delete`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val database = TallybookDatabase(driver)
      val repository = MovimientoRepositoryImpl(database, StandardTestDispatcher(testScheduler))
      val row = movimiento("gone", categoriaIds = listOf("a", "b"))
      database.movimientoQueries.insertMovimientoCategoria("gone", "a", 0)
      database.movimientoQueries.insertMovimientoCategoria("gone", "stale", 1)

      assertEquals(
          DataOutcome.Success(MovimientoRestoreResult.Restored),
          repository.restoreDeleted(row),
      )

      assertEquals(DataOutcome.Success(row), repository.getById("gone"))
      assertEquals(listOf("gone:a:0", "gone:b:1"), joinRows(driver))
    }
  }

  private fun movimiento(
      id: String,
      tipo: TipoMovimiento = TipoMovimiento.GASTO,
      monto: Money = Money(500, Moneda.COP),
      categoriaIds: List<String> = listOf("cat_comida"),
      descripcion: String? = null,
      at: Instant = Instant.parse("2026-09-05T12:00:00Z"),
  ) =
      Movimiento(
          id = id,
          tipo = tipo,
          monto = monto,
          fecha = LocalDate(2026, 9, 5),
          categoriaIds = categoriaIds,
          descripcion = descripcion,
          createdAt = at,
      )
}

private const val CATEGORY_HISTORY_BOUND = 1_000
private val LATER = Instant.parse("2026-09-05T13:00:00Z")
