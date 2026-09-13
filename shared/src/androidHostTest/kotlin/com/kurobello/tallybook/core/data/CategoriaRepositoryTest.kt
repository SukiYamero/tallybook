package com.kurobello.tallybook.core.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class CategoriaRepositoryTest {

  @Test
  fun `obtenerTodas maps database rows to domain categorias`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    val database = TallybookDatabase(driver)
    val repository = CategoriaRepositoryImpl(database, StandardTestDispatcher(testScheduler))
    val expected =
        Categoria(
            id = "cat_comida",
            nombre = "Comida",
            padreId = null,
            icono = "utensils",
            color = "amber",
        )
    database.categoriaQueries.insertCategoria(
        id = expected.id,
        nombre = expected.nombre,
        padre_id = expected.padreId,
        icono = expected.icono,
        color = expected.color,
    )

    assertEquals(DataOutcome.Success(listOf(expected)), repository.obtenerTodas())

    driver.close()
  }
}
