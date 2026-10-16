package com.kurobello.tallybook.core.data

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.MovimientoQueries
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SearchRepositoryTest {
  @Test
  fun `optional dimensions compose AND while exact category ids compose OR and stable order`() =
      runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
          createSchemaWithReferences(driver, listOf("food", "food-child"))
          val repository =
              MovimientoRepositoryImpl(
                  TallybookDatabase(driver),
                  StandardTestDispatcher(testScheduler),
              )
          val rows =
              listOf(
                  movimiento("a", "2026-09-15"),
                  movimiento("c", "2026-09-15"),
                  movimiento("b", "2026-09-15", createdAt = "2026-09-15T11:00:00Z"),
                  movimiento("lower", "2026-09-01"),
                  movimiento("upper", "2026-09-30"),
                  movimiento("before", "2026-08-31"),
                  movimiento("after", "2026-10-01"),
                  movimiento("child", "2026-09-15", categoryId = "food-child"),
                  movimiento("income", "2026-09-15", tipo = TipoMovimiento.INGRESO),
                  movimiento("usd", "2026-09-15", moneda = Moneda.USD),
              )
          for (row in rows) assertEquals(DataOutcome.Success(Unit), repository.insert(row))

          val base =
              MovimientoReadCriteria(Moneda.COP, september, TipoMovimiento.GASTO, setOf("food"))
          val selected =
              assertIs<DataOutcome.Success<MovimientoSearchSnapshot>>(
                      repository.observeFiltered(base).first()
                  )
                  .data
          assertEquals(listOf("upper", "c", "a", "b", "lower"), selected.movimientos.map { it.id })
          assertEquals(listOf(2026), selected.persistedYears)
          assertTrue(selected.hasCurrencyData)
          assertEquals(9, snapshot(repository, MovimientoReadCriteria(Moneda.COP)).movimientos.size)
          assertEquals(
              listOf(rows[9]),
              snapshot(repository, MovimientoReadCriteria(Moneda.USD)).movimientos,
          )
          assertEquals(
              listOf(rows[8]),
              snapshot(
                      repository,
                      MovimientoReadCriteria(Moneda.COP, tipo = TipoMovimiento.INGRESO),
                  )
                  .movimientos,
          )
          assertEquals(
              6,
              snapshot(
                      repository,
                      MovimientoReadCriteria(
                          Moneda.COP,
                          september,
                          TipoMovimiento.GASTO,
                          setOf("food", "food-child"),
                      ),
                  )
                  .movimientos
                  .size,
          )
          assertEquals(
              emptyList(),
              snapshot(
                      repository,
                      MovimientoReadCriteria(Moneda.COP, categoryIds = setOf("missing")),
                  )
                  .movimientos,
          )
          assertEquals(
              emptyList(),
              snapshot(
                      repository,
                      MovimientoReadCriteria(
                          Moneda.COP,
                          tipo = TipoMovimiento.INGRESO,
                          categoryIds = setOf("food-child"),
                      ),
                  )
                  .movimientos,
          )
          val day = LocalDate(2026, 9, 15)
          assertEquals(
              5,
              snapshot(
                      repository,
                      MovimientoReadCriteria(Moneda.COP, MovimientoDateRange(day, day)),
                  )
                  .movimientos
                  .size,
          )
        }
      }

  @Test
  fun `parent filter rolls up to children and a child filter never goes upward`() = runTest {
    withHierarchy { repository, _ ->
      val family = setOf("on-parent", "on-child", "on-sibling")
      assertEquals(family, ids(repository, setOf("food")))
      assertEquals(setOf("on-child"), ids(repository, setOf("food-child")))
      assertEquals(setOf("on-unrelated"), ids(repository, setOf("other")))
      assertEquals(family + "on-unrelated", ids(repository, setOf("food", "other")))
      assertEquals(setOf("on-child", "on-unrelated"), ids(repository, setOf("food-child", "other")))
      assertEquals(family, ids(repository, setOf("food", "food-child")))
    }
  }

  @Test
  fun `the filter combines optional category and tag sets into four selections`() = runTest {
    withHierarchy { repository, _ ->
      repository.insert(tagged("trip-child", "2026-09-15", "food-child", listOf("trip")))
      repository.insert(tagged("trip-work-other", "2026-09-15", "other", listOf("work", "trip")))
      repository.insert(tagged("gift-parent", "2026-09-15", "food", listOf("gift")))
      val untagged = setOf("on-parent", "on-child", "on-sibling", "on-unrelated", "on-otros")
      val taggedIds = setOf("trip-child", "trip-work-other", "gift-parent")

      assertEquals(untagged + taggedIds, ids(repository))
      assertEquals(
          setOf("on-parent", "on-child", "on-sibling", "trip-child", "gift-parent"),
          ids(repository, categoryIds = setOf("food")),
      )
      assertEquals(
          setOf("trip-child", "trip-work-other"),
          ids(repository, etiquetaIds = setOf("trip")),
      )
      assertEquals(
          setOf("trip-child"),
          ids(repository, categoryIds = setOf("food"), etiquetaIds = setOf("trip")),
      )
    }
  }

  @Test
  fun `tags compose OR within the dimension and AND with the category roll-up`() = runTest {
    withHierarchy { repository, _ ->
      repository.insert(tagged("trip-child", "2026-09-15", "food-child", listOf("trip")))
      repository.insert(tagged("work-sibling", "2026-09-15", "food-sibling", listOf("work")))
      repository.insert(tagged("both-other", "2026-09-15", "other", listOf("trip", "work")))
      repository.insert(
          tagged(
              "trip-income",
              "2026-09-15",
              "food",
              listOf("trip"),
              tipo = TipoMovimiento.INGRESO,
          )
      )

      val tripOrWork = setOf("trip", "work")
      assertEquals(
          setOf("trip-child", "work-sibling", "both-other", "trip-income"),
          ids(repository, etiquetaIds = tripOrWork),
      )
      val found = snapshot(repository, MovimientoReadCriteria(Moneda.COP, etiquetaIds = tripOrWork))
      assertEquals(
          found.movimientos.size,
          found.movimientos.map { it.id }.distinct().size,
          "a row matched through several tags is returned once",
      )
      assertEquals(
          listOf("trip", "work"),
          found.movimientos.single { it.id == "both-other" }.etiquetaIds,
      )
      assertEquals(
          setOf("trip-child", "work-sibling"),
          ids(
              repository,
              categoryIds = setOf("food"),
              etiquetaIds = tripOrWork,
              tipo = TipoMovimiento.GASTO,
          ),
      )
      assertEquals(
          setOf("trip-child"),
          ids(repository, categoryIds = setOf("food-child"), etiquetaIds = tripOrWork),
      )
      assertEquals(emptySet(), ids(repository, etiquetaIds = setOf("missing")))
      assertEquals(
          emptySet(),
          ids(repository, categoryIds = setOf("food-child"), etiquetaIds = setOf("gift")),
      )
    }
  }

  @Test
  fun `Otros is filterable and unknown ids are tolerated next to a parent`() = runTest {
    withHierarchy { repository, _ ->
      assertEquals(setOf("on-otros"), ids(repository, setOf(CATEGORIA_OTROS_ID)))
      assertEquals(emptySet(), ids(repository, setOf("missing")))
      assertEquals(
          setOf("on-parent", "on-child", "on-sibling"),
          ids(repository, setOf("missing", "food")),
      )
    }
  }

  @Test
  fun `parent roll-up composes with the type filter and follows category changes`() = runTest {
    withHierarchy { repository, database ->
      repository.insert(
          movimiento(
              "child-income",
              "2026-09-15",
              tipo = TipoMovimiento.INGRESO,
              categoryId = "food-child",
          )
      )

      assertEquals(
          setOf("on-parent", "on-child", "on-sibling"),
          ids(repository, setOf("food"), TipoMovimiento.GASTO),
      )
      assertEquals(setOf("child-income"), ids(repository, setOf("food"), TipoMovimiento.INGRESO))

      database.categoriaQueries.insertCategoria("late-child", "Late", "other", "i", "c")
      repository.insert(movimiento("on-late", "2026-09-15", categoryId = "late-child"))
      assertEquals(setOf("on-unrelated", "on-late"), ids(repository, setOf("other")))
    }
  }

  @Test
  fun `excluded mutations update coherent currency years and cancellation releases one query listener`() =
      runTest {
        val driver = TrackingDriver(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY))
        try {
          val database = createSchemaWithReferences(driver, listOf("food", "other"))
          val repository = MovimientoRepositoryImpl(database, StandardTestDispatcher(testScheduler))
          val criteria =
              MovimientoReadCriteria(Moneda.COP, september, TipoMovimiento.GASTO, setOf("food"))
          val emissions = mutableListOf<DataOutcome<MovimientoSearchSnapshot>>()
          val flow = repository.observeFiltered(criteria)
          val job = backgroundScope.launch { flow.collect(emissions::add) }
          runCurrent()
          assertEquals(
              DataOutcome.Success(MovimientoSearchSnapshot(emptyList(), emptyList())),
              emissions.single(),
          )
          assertEquals(1, driver.listeners)
          assertEquals(
              setOf("movimiento", "movimiento_etiqueta", "categoria"),
              driver.listenerKeys,
          )

          val matching = movimiento("matching", "2026-09-15")
          assertEquals(DataOutcome.Success(Unit), repository.insert(matching))
          runCurrent()
          assertEquals(
              DataOutcome.Success(MovimientoSearchSnapshot(listOf(matching), listOf(2026))),
              emissions.last(),
          )
          assertEquals(
              DataOutcome.Success(Unit),
              repository.insert(
                  movimiento(
                      "excluded",
                      "2024-01-01",
                      tipo = TipoMovimiento.INGRESO,
                      categoryId = "other",
                  )
              ),
          )
          runCurrent()
          assertEquals(
              DataOutcome.Success(MovimientoSearchSnapshot(listOf(matching), listOf(2026, 2024))),
              emissions.last(),
          )
          assertEquals(DataOutcome.Success(Unit), repository.delete(matching.id))
          runCurrent()
          val noResults =
              assertIs<DataOutcome.Success<MovimientoSearchSnapshot>>(emissions.last()).data
          assertEquals(emptyList(), noResults.movimientos)
          assertTrue(noResults.hasCurrencyData)
          assertEquals(DataOutcome.Success(Unit), repository.delete("excluded"))
          runCurrent()
          assertEquals(
              DataOutcome.Success(MovimientoSearchSnapshot(emptyList(), emptyList())),
              emissions.last(),
          )

          repeat(5) { index ->
            database.transaction {
              database.movimientoQueries.insertMovimiento(
                  "transaction-$index",
                  "GASTO",
                  100,
                  "COP",
                  "2026-09-15",
                  null,
                  null,
                  "2026-09-15T12:00:00Z",
                  "food",
              )
              database.movimientoQueries.insertMovimiento(
                  "old-$index",
                  "INGRESO",
                  100,
                  "COP",
                  "2023-01-01",
                  null,
                  null,
                  "2026-09-15T12:00:00Z",
                  "other",
              )
            }
            runCurrent()
            database.transaction {
              database.movimientoQueries.deleteMovimiento("transaction-$index")
              database.movimientoQueries.deleteMovimientoEtiquetas("transaction-$index")
              database.movimientoQueries.deleteMovimiento("old-$index")
              database.movimientoQueries.deleteMovimientoEtiquetas("old-$index")
            }
            runCurrent()
          }
          for (emission in emissions) {
            val value = assertIs<DataOutcome.Success<MovimientoSearchSnapshot>>(emission).data
            assertTrue(value.movimientos.all { it.fecha.year in value.persistedYears })
          }
          job.cancelAndJoin()
          assertEquals(0, driver.listeners)
          assertEquals(DataOutcome.Success(Unit), repository.insert(matching))
          assertEquals(listOf(matching), snapshot(repository, criteria).movimientos)
          runCurrent()
          assertEquals(0, driver.listeners)
        } finally {
          driver.close()
        }
      }

  @Test
  fun `invalid filter ids and unsupported cross year ranges never access storage`() = runTest {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val real = TallybookDatabase(driver)
      val database =
          object : TallybookDatabase by real {
            override val movimientoQueries: MovimientoQueries
              get() = error("invalid criteria must not access storage")
          }
      val repository = MovimientoRepositoryImpl(database, StandardTestDispatcher(testScheduler))
      val invalid =
          listOf(
              MovimientoReadCriteria(Moneda.COP, categoryIds = setOf("")),
              MovimientoReadCriteria(Moneda.COP, categoryIds = setOf(" \t")),
              MovimientoReadCriteria(Moneda.COP, etiquetaIds = setOf("")),
              MovimientoReadCriteria(
                  Moneda.COP,
                  categoryIds = setOf("food"),
                  etiquetaIds = setOf(" "),
              ),
              MovimientoReadCriteria(
                  Moneda.COP,
                  MovimientoDateRange(LocalDate(-1, 1, 1), LocalDate(0, 1, 1)),
              ),
              MovimientoReadCriteria(
                  Moneda.COP,
                  MovimientoDateRange(LocalDate(9999, 12, 31), LocalDate(10000, 1, 1)),
              ),
          )
      for (criteria in invalid) assertIs<DataError.InvalidInput>(
          assertIs<DataOutcome.Failure>(repository.observeFiltered(criteria).first()).error
      )
    }
  }

  @Test
  fun `literal category ids remain bound and All includes the entire persisted year domain`() =
      runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
          val ids = listOf("quote' OR 1=1 --", "percent%", "underscore_", "unrelated")
          createSchemaWithReferences(driver, ids + "food")
          val repository =
              MovimientoRepositoryImpl(
                  TallybookDatabase(driver),
                  StandardTestDispatcher(testScheduler),
              )
          val dates = listOf("-0001-01-01", "0000-01-01", "+10000-01-01", "2026-01-01")
          for ((index, id) in ids.withIndex()) repository.insert(
              movimiento("row-$index", dates[index], categoryId = id)
          )
          val selected =
              snapshot(
                  repository,
                  MovimientoReadCriteria(Moneda.COP, categoryIds = ids.take(3).toSet()),
              )
          assertEquals(setOf("row-0", "row-1", "row-2"), selected.movimientos.map { it.id }.toSet())
          assertEquals(listOf(10000, 2026, 0, -1), selected.persistedYears)
          assertEquals(4, snapshot(repository, MovimientoReadCriteria(Moneda.COP)).movimientos.size)
          repository.insert(movimiento("lower", "2025-12-31"))
          val crossYear =
              MovimientoReadCriteria(
                  Moneda.COP,
                  MovimientoDateRange(LocalDate(2025, 12, 31), LocalDate(2026, 1, 1)),
              )
          assertEquals(
              listOf("row-3", "lower"),
              snapshot(repository, crossYear).movimientos.map { it.id },
          )
          val sameExoticYear =
              MovimientoReadCriteria(
                  Moneda.COP,
                  MovimientoDateRange(LocalDate(-1, 1, 1), LocalDate(-1, 12, 31)),
              )
          assertEquals(
              listOf("row-0"),
              snapshot(repository, sameExoticYear).movimientos.map { it.id },
          )
        }
      }

  @Test
  fun `structural rows and years execute inside the same real driver transaction`() = runTest {
    val driver = TrackingDriver(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY))
    try {
      createSchemaWithReferences(driver, listOf("food"))
      val repository =
          MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
      repository.insert(movimiento("selected", "2026-09-15"))
      repository.insert(movimiento("excluded", "2024-01-01"))
      driver.readTransactions.clear()
      snapshot(repository, MovimientoReadCriteria(Moneda.COP, september))
      assertEquals(2, driver.readTransactions.size)
      val rowTransaction = driver.readTransactions.first()
      assertTrue(rowTransaction != null)
      assertTrue(driver.readTransactions.last() === rowTransaction)
    } finally {
      driver.close()
    }
  }

  @Test
  fun `mapping and metadata failures unregister and a fresh subscription reads recovered storage`() =
      runTest {
        val driver = TrackingDriver(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY))
        try {
          TallybookDatabase.Schema.create(driver)
          val database = TallybookDatabase(driver)
          val repository = MovimientoRepositoryImpl(database, StandardTestDispatcher(testScheduler))
          val flow = repository.observeFiltered(MovimientoReadCriteria(Moneda.COP, september))
          val emissions = mutableListOf<DataOutcome<MovimientoSearchSnapshot>>()
          val job = backgroundScope.launch { flow.collect(emissions::add) }
          runCurrent()
          database.movimientoQueries.insertMovimiento(
              "invalid",
              "GASTO",
              0,
              "COP",
              "2026-09-15",
              null,
              null,
              "2026-09-15T12:00:00Z",
              "food",
          )
          runCurrent()
          assertIs<DataError.Local>(assertIs<DataOutcome.Failure>(emissions.last()).error)
          assertTrue(job.isCompleted)
          assertEquals(0, driver.listeners)
          repository.delete("invalid")
          assertEquals(
              MovimientoSearchSnapshot(emptyList(), emptyList()),
              snapshot(repository, MovimientoReadCriteria(Moneda.COP, september)),
          )

          database.movimientoQueries.insertMovimiento(
              "invalid-year",
              "GASTO",
              100,
              "COP",
              "02026-09-15",
              null,
              null,
              "2026-09-15T12:00:00Z",
              "other",
          )
          assertIs<DataError.Local>(assertIs<DataOutcome.Failure>(flow.first()).error)
          repository.delete("invalid-year")
          assertEquals(
              DataOutcome.Success(MovimientoSearchSnapshot(emptyList(), emptyList())),
              flow.first(),
          )
          runCurrent()
          assertEquals(0, driver.listeners)
        } finally {
          driver.close()
        }
      }

  @Test
  fun `construction and closed storage failures become Local while cancellation escapes`() =
      runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
          TallybookDatabase.Schema.create(driver)
          val real = TallybookDatabase(driver)
          var unavailable = true
          var cancelled = false
          val database =
              object : TallybookDatabase by real {
                override val movimientoQueries: MovimientoQueries
                  get() {
                    if (cancelled) throw CancellationException("cancelled")
                    return if (unavailable) error("unavailable") else real.movimientoQueries
                  }
              }
          val repository = MovimientoRepositoryImpl(database, StandardTestDispatcher(testScheduler))
          val flow = repository.observeFiltered(MovimientoReadCriteria(Moneda.COP))
          assertIs<DataError.Local>(assertIs<DataOutcome.Failure>(flow.first()).error)
          unavailable = false
          assertEquals(
              DataOutcome.Success(MovimientoSearchSnapshot(emptyList(), emptyList())),
              flow.first(),
          )
          cancelled = true
          assertFailsWith<CancellationException> { flow.first() }
          cancelled = false
          driver.close()
          assertIs<DataError.Local>(assertIs<DataOutcome.Failure>(flow.first()).error)
        } finally {
          driver.close()
        }
      }

  @Test
  fun `a synchronous read completing after cancellation never publishes its snapshot`() = runTest {
    val driver = TrackingDriver(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY))
    try {
      createSchemaWithReferences(driver, listOf("food"))
      val repository =
          MovimientoRepositoryImpl(TallybookDatabase(driver), StandardTestDispatcher(testScheduler))
      repository.insert(movimiento("selected", "2026-09-15"))
      val emissions = mutableListOf<DataOutcome<MovimientoSearchSnapshot>>()
      lateinit var job: Job
      driver.afterStructuralRead = { job.cancel() }
      job = backgroundScope.launch {
        repository.observeFiltered(MovimientoReadCriteria(Moneda.COP)).collect(emissions::add)
      }
      runCurrent()
      job.join()
      assertTrue(job.isCancelled)
      assertEquals(emptyList(), emissions)
      assertEquals(0, driver.listeners)
      assertEquals(
          listOf("selected"),
          snapshot(repository, MovimientoReadCriteria(Moneda.COP)).movimientos.map { it.id },
      )
    } finally {
      driver.close()
    }
  }

  @Test
  fun `All year and sparse category and tag generated plans retain stable index order`() = runTest {
    val driver = TrackingDriver(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY))
    try {
      createSchemaWithReferences(driver, listOf("sparse", "food"), listOf("trip"))
      val repository =
          MovimientoRepositoryImpl(
              TallybookDatabase(driver),
              StandardTestDispatcher(testScheduler),
          )
      for (index in 0..11) repository.insert(
          movimiento(
              "row-$index",
              if (index % 2 == 0) "2026-09-15" else "2024-01-01",
              if (index % 3 == 0) Moneda.USD else Moneda.COP,
              categoryId = if (index % 4 == 0) "sparse" else "food",
              etiquetaIds = if (index % 5 == 0) listOf("trip") else emptyList(),
          )
      )
      val criteria =
          listOf(
              MovimientoReadCriteria(Moneda.COP),
              MovimientoReadCriteria(
                  Moneda.COP,
                  MovimientoDateRange(LocalDate(2026, 1, 1), LocalDate(2026, 9, 15)),
              ),
              MovimientoReadCriteria(
                  Moneda.COP,
                  tipo = TipoMovimiento.GASTO,
                  categoryIds = setOf("sparse"),
              ),
              MovimientoReadCriteria(Moneda.COP, etiquetaIds = setOf("trip")),
              MovimientoReadCriteria(
                  Moneda.COP,
                  categoryIds = setOf("sparse"),
                  etiquetaIds = setOf("trip"),
              ),
          )
      for (selection in criteria) {
        snapshot(repository, selection)
        val read = requireNotNull(driver.structuralRead)
        val plan =
            driver
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
        assertTrue(plan.any { "movimiento_recent" in it })
        assertTrue(plan.none { "TEMP B-TREE FOR ORDER BY" in it })
        println(
            "Search structural plan categories=${selection.categoryIds.size} " +
                "tags=${selection.etiquetaIds.size} ranged=${selection.range != null}: $plan"
        )
      }
    } finally {
      driver.close()
    }
  }

  private suspend fun TestScope.withHierarchy(
      block: suspend (MovimientoRepository, TallybookDatabase) -> Unit
  ) {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      TallybookDatabase.Schema.create(driver)
      val database = TallybookDatabase(driver)
      val categories =
          listOf(
              Triple("food", "Food", null),
              Triple("food-child", "Groceries", "food"),
              Triple("food-sibling", "Dining", "food"),
              Triple("other", "Other root", null),
              Triple(CATEGORIA_OTROS_ID, "Otros", null),
          )
      for ((id, nombre, padreId) in categories) {
        database.categoriaQueries.insertCategoria(id, nombre, padreId, "icon", "color")
      }
      for (id in listOf("trip", "work", "gift")) {
        database.etiquetaQueries.insertEtiqueta(id, id, null, "2026-01-01T00:00:00Z")
      }
      val repository = MovimientoRepositoryImpl(database, StandardTestDispatcher(testScheduler))
      val rows =
          mapOf(
              "on-parent" to "food",
              "on-child" to "food-child",
              "on-sibling" to "food-sibling",
              "on-unrelated" to "other",
              "on-otros" to CATEGORIA_OTROS_ID,
          )
      for ((id, categoryId) in rows) repository.insert(
          movimiento(id, "2026-09-15", categoryId = categoryId)
      )
      block(repository, database)
    }
  }

  private suspend fun ids(
      repository: MovimientoRepository,
      categoryIds: Set<String> = emptySet(),
      tipo: TipoMovimiento? = null,
      etiquetaIds: Set<String> = emptySet(),
  ): Set<String> =
      snapshot(
              repository,
              MovimientoReadCriteria(
                  Moneda.COP,
                  tipo = tipo,
                  categoryIds = categoryIds,
                  etiquetaIds = etiquetaIds,
              ),
          )
          .movimientos
          .map { it.id }
          .toSet()

  private suspend fun snapshot(repository: MovimientoRepository, criteria: MovimientoReadCriteria) =
      assertIs<DataOutcome.Success<MovimientoSearchSnapshot>>(
              repository.observeFiltered(criteria).first()
          )
          .data

  private class TrackingDriver(private val delegate: SqlDriver) : SqlDriver by delegate {
    var listeners = 0
      private set

    val listenerKeys = mutableSetOf<String>()
    val readTransactions = mutableListOf<Any?>()
    var structuralRead: StructuralRead? = null
      private set

    var afterStructuralRead: (() -> Unit)? = null

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> {
      val structural = sql.trimStart().startsWith("SELECT movimiento_con_etiquetas.id")
      if (sql.trimStart().startsWith("SELECT")) readTransactions += currentTransaction()
      if (structural) structuralRead = StructuralRead(sql, parameters, binders)
      val result = delegate.executeQuery(identifier, sql, mapper, parameters, binders)
      if (structural) {
        val callback = afterStructuralRead
        afterStructuralRead = null
        callback?.invoke()
      }
      return result
    }

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

  private data class StructuralRead(
      val sql: String,
      val parameters: Int,
      val binders: (SqlPreparedStatement.() -> Unit)?,
  )

  private fun movimiento(
      id: String,
      fecha: String,
      moneda: Moneda = Moneda.COP,
      tipo: TipoMovimiento = TipoMovimiento.GASTO,
      categoryId: String = "food",
      createdAt: String = "2026-09-15T12:00:00Z",
      etiquetaIds: List<String> = emptyList(),
  ) =
      Movimiento(
          id,
          tipo,
          Money(100, moneda),
          LocalDate.parse(fecha),
          categoryId,
          "Lunch",
          null,
          Instant.parse(createdAt),
          etiquetaIds,
      )

  private fun tagged(
      id: String,
      fecha: String,
      categoryId: String,
      etiquetaIds: List<String>,
      tipo: TipoMovimiento = TipoMovimiento.GASTO,
  ) = movimiento(id, fecha, tipo = tipo, categoryId = categoryId, etiquetaIds = etiquetaIds)

  private companion object {
    val september = MovimientoDateRange(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30))
  }
}
