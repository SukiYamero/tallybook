package com.kurobello.tallybook.core.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoriaDraft
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.ChangeStamp
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

    assertEquals(59, categories.size)
    assertEquals("category_comida", categories.first { it.id == "cat_comida" }.nombre)
    assertEquals("category_reembolso", categories.first { it.id == "cat_reembolso" }.nombre)
    assertEquals("category_otros", categories.first { it.id == CATEGORIA_OTROS_ID }.nombre)
  }

  @Test
  fun `ensure seeded inserts every category into an empty database`() = runTest {
    withRepository(StandardTestDispatcher(testScheduler)) { database, repository ->
      assertEquals(DataOutcome.Success(Unit), repository.ensureSeeded(seedCategorias()))

      assertEquals(59, database.categoriaQueries.selectAllCategorias().executeAsList().size)
    }
  }

  @Test
  fun `ensure seeded remains idempotent`() = runTest {
    withRepository(StandardTestDispatcher(testScheduler)) { database, repository ->
      repository.ensureSeeded(seedCategorias())
      assertEquals(DataOutcome.Success(Unit), repository.ensureSeeded(seedCategorias()))

      assertEquals(59, database.categoriaQueries.selectAllCategorias().executeAsList().size)
    }
  }

  @Test
  fun `ensure seeded adds Otros to an install seeded before it existed`() = runTest {
    withRepository(StandardTestDispatcher(testScheduler)) { database, repository ->
      repository.ensureSeeded(seedCategorias().filterNot { it.id == CATEGORIA_OTROS_ID })
      assertEquals(58, database.categoriaQueries.selectAllCategorias().executeAsList().size)

      assertEquals(DataOutcome.Success(Unit), repository.ensureSeeded(seedCategorias()))

      val otros =
          database.categoriaQueries.selectAllCategorias().executeAsList().single {
            it.id == CATEGORIA_OTROS_ID
          }
      assertEquals(null, otros.padre_id)
      assertEquals(CategoryIcon.CIRCLE_DASHED.storageKey, otros.icono)
      assertEquals(CategoryTint.NEUTRAL.storageKey, otros.color)
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
          updated_at = ChangeStamp.SEED.value,
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

  @Test
  fun `localize seeded renames existing rows only and skips unchanged names`() = runTest {
    withRepository(StandardTestDispatcher(testScheduler)) { database, repository ->
      repository.ensureSeeded(seedCategorias())
      val english =
          seedCategorias().map { if (it.id == "cat_comida") it.copy(nombre = "Food") else it } +
              seedCategorias().first().copy(id = "cat_missing", nombre = "Missing")
      var notifications = 0
      database.categoriaQueries.selectAllCategorias().addListener { notifications++ }

      assertEquals(DataOutcome.Success(Unit), repository.localizeSeeded(english))
      assertEquals(DataOutcome.Success(Unit), repository.localizeSeeded(english))

      val rows = database.categoriaQueries.selectAllCategorias().executeAsList()
      assertEquals(59, rows.size)
      assertEquals("Food", rows.first { it.id == "cat_comida" }.nombre)
      assertEquals(1, notifications)
    }
  }

  @Test
  fun `localize seeded never renames a custom row even when a seed shares its id`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    try {
      val database = TallybookDatabase(driver)
      val repository =
          CategoriaRepositoryImpl(
              database,
              StandardTestDispatcher(testScheduler),
              IdGenerator { "cat_gym" },
              TestChangeClock(),
          )
      repository.ensureSeeded(seedCategorias())
      repository.create(
          CategoriaDraft("Gym", CategoryIcon.DUMBBELL, CategoryTint.NEUTRAL, descripcion = null)
      )
      val localized =
          seedCategorias().map { if (it.id == "cat_comida") it.copy(nombre = "Food") else it } +
              seedCategorias().first().copy(id = "cat_gym", nombre = "Renamed")

      assertEquals(DataOutcome.Success(Unit), repository.localizeSeeded(localized))

      val names =
          database.categoriaQueries.selectAllCategorias().executeAsList().associate {
            it.id to it.nombre
          }
      assertEquals("Gym", names.getValue("cat_gym"))
      assertEquals("Food", names.getValue("cat_comida"))
    } finally {
      driver.close()
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
      block(
          database,
          CategoriaRepositoryImpl(
              database,
              dispatcher,
              IdGenerator { error("Not used by this test") },
              TestChangeClock(),
          ),
      )
    } finally {
      driver.close()
    }
  }
}
