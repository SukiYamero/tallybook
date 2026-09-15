package com.kurobello.tallybook.core.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
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
