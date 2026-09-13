package com.kurobello.tallybook.core.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class MovimientoRepositoryTest {

  @Test
  fun `insertar and obtenerTodos round-trip a domain movimiento`() = runTest {
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
        )

    assertEquals(DataOutcome.Success(Unit), repository.insertar(movimiento))
    assertEquals(DataOutcome.Success(listOf(movimiento)), repository.obtenerTodos())

    driver.close()
  }

  @Test
  fun `insertar maps a duplicate id to a local error`() = runTest {
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
        )

    assertIs<DataOutcome.Success<Unit>>(repository.insertar(movimiento))
    val failure = assertIs<DataOutcome.Failure>(repository.insertar(movimiento))

    assertIs<DataError.Local>(failure.error)
    assertTrue(failure.error.cause != null)
    driver.close()
  }

  @Test
  fun `eliminar removes the movimiento`() = runTest {
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
        )

    repository.insertar(movimiento)

    assertEquals(DataOutcome.Success(Unit), repository.eliminar(movimiento.id))
    assertEquals(DataOutcome.Success(emptyList()), repository.obtenerTodos())
    driver.close()
  }
}
