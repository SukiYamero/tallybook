package com.kurobello.tallybook.core.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class CategorySeedTest {

  @Test
  fun `seed provider passes generated category resources to its resolver`() = runTest {
    val categories =
        CategorySeedProvider { resource -> resource.key.removePrefix("string:") }.getCategories()

    assertEquals(58, categories.size)
    assertEquals("category_comida", categories.first { it.id == "cat_comida" }.nombre)
    assertEquals("category_reembolso", categories.first { it.id == "cat_reembolso" }.nombre)
  }

  @Test
  fun `ensure seeded inserts every category into an empty database`() = runTest {
    withRepository(StandardTestDispatcher(testScheduler)) { database, repository ->
      assertEquals(DataOutcome.Success(Unit), repository.ensureSeeded(seedCategorias()))

      assertEquals(58, database.categoriaQueries.selectAllCategorias().executeAsList().size)
    }
  }

  @Test
  fun `ensure seeded remains idempotent`() = runTest {
    withRepository(StandardTestDispatcher(testScheduler)) { database, repository ->
      repository.ensureSeeded(seedCategorias())
      assertEquals(DataOutcome.Success(Unit), repository.ensureSeeded(seedCategorias()))

      assertEquals(58, database.categoriaQueries.selectAllCategorias().executeAsList().size)
    }
  }

  @Test
  fun `ensure seeded preserves an existing category row`() = runTest {
    withRepository(StandardTestDispatcher(testScheduler)) { database, repository ->
      database.categoriaQueries.insertCategoria(
          id = "cat_comida",
          nombre = "Manual name",
          padre_id = null,
          icono = CategoryIcon.COFFEE.storageKey,
          color = CategoryTint.INFO.storageKey,
      )

      assertEquals(DataOutcome.Success(Unit), repository.ensureSeeded(seedCategorias()))

      val comida =
          database.categoriaQueries.selectAllCategorias().executeAsList().first {
            it.id == "cat_comida"
          }
      assertEquals("Manual name", comida.nombre)
      assertEquals(CategoryIcon.COFFEE.storageKey, comida.icono)
      assertEquals(CategoryTint.INFO.storageKey, comida.color)
    }
  }

  private fun seedCategorias(): List<Categoria> =
      CategorySeedCatalog.categories.map { seed ->
        Categoria(
            id = seed.id,
            nombre = "Name ${seed.id}",
            padreId = seed.padreId,
            icono = seed.icono,
            color = seed.color,
        )
      }

  private suspend fun withRepository(
      dispatcher: CoroutineDispatcher,
      block: suspend (TallybookDatabase, CategoriaRepository) -> Unit,
  ) {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    try {
      val database = TallybookDatabase(driver)
      block(database, CategoriaRepositoryImpl(database, dispatcher))
    } finally {
      driver.close()
    }
  }
}
