package com.kurobello.tallybook.core.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoriaDelete
import com.kurobello.tallybook.core.model.CategoriaDraft
import com.kurobello.tallybook.core.model.CategoriaOrigen
import com.kurobello.tallybook.core.model.CategoriaRules
import com.kurobello.tallybook.core.model.CategoriaWrite
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class CategoriaRepositoryTest {

  @Test
  fun `getAll maps database rows to domain categorias`() = runTest {
    withRepository { database, repository ->
      database.seed(comida)

      assertEquals(DataOutcome.Success(listOf(comida)), repository.getAll())
    }
  }

  @Test
  fun `create trims the draft and stores a visible custom root categoria`() = runTest {
    withRepository { _, repository ->
      val saved = repository.create(draft("  Gym  ", descripcion = "   "))

      val expected =
          Categoria(
              id = "custom-1",
              nombre = "Gym",
              padreId = null,
              icono = CategoryIcon.DUMBBELL,
              color = CategoryTint.NEUTRAL,
              descripcion = null,
              origen = CategoriaOrigen.USUARIO,
              oculta = false,
          )
      assertEquals(DataOutcome.Success(CategoriaWrite.Saved(expected)), saved)
      assertEquals(DataOutcome.Success(listOf(expected)), repository.getAll())
      assertEquals(
          "Weekly sessions",
          repository
              .create(draft("Pilates", descripcion = " Weekly sessions "))
              .savedCategoria()
              .descripcion,
      )
    }
  }

  @Test
  fun `create returns the existing categoria for an accent or case duplicate without writing`() =
      runTest {
        withRepository { database, repository ->
          database.seed(cafe)
          val gym = repository.create(draft("Gym")).savedCategoria()
          repository.setOculta(gym.id, oculta = true)
          var notifications = 0
          database.categoriaQueries.selectAllCategorias().addListener { notifications++ }

          assertEquals(
              DataOutcome.Success(CategoriaWrite.Duplicate(cafe)),
              repository.create(draft(" CAFE ")),
          )
          assertEquals(
              DataOutcome.Success(CategoriaWrite.Duplicate(gym.copy(oculta = true))),
              repository.create(draft("gym")),
          )
          assertEquals(0, notifications)
          assertEquals(2, repository.getAll().rows().size)
        }
      }

  @Test
  fun `create and update reject an invalid nombre or descripcion without writing`() = runTest {
    withRepository { _, repository ->
      val custom = repository.create(draft("Gym")).savedCategoria()
      val invalid =
          listOf(
              draft("   "),
              draft("x".repeat(CategoriaRules.NOMBRE_MAX + 1)),
              draft("Pilates", descripcion = "x".repeat(CategoriaRules.DESCRIPCION_MAX + 1)),
          )

      invalid.forEach { candidate ->
        assertInvalid(repository.create(candidate))
        assertInvalid(repository.update(custom.id, candidate))
      }
      assertEquals(listOf(custom), repository.getAll().rows())
      assertIs<CategoriaWrite.Saved>(
          repository
              .create(
                  draft(
                      " ${"x".repeat(CategoriaRules.NOMBRE_MAX)} ",
                      descripcion = "x".repeat(CategoriaRules.DESCRIPCION_MAX),
                  )
              )
              .successData()
      )
    }
  }

  @Test
  fun `update renames a custom categoria and rejects seeded or unknown targets`() = runTest {
    withRepository { database, repository ->
      database.seed(comida)
      val gym = repository.create(draft("Gym")).savedCategoria()
      val yoga = repository.create(draft("Yoga")).savedCategoria()
      repository.setOculta(gym.id, oculta = true)

      val renamed =
          repository.update(
              gym.id,
              CategoriaDraft("GYM", CategoryIcon.HEART_PULSE, CategoryTint.ROSE, " Mornings "),
          )

      val expected =
          gym.copy(
              nombre = "GYM",
              icono = CategoryIcon.HEART_PULSE,
              color = CategoryTint.ROSE,
              descripcion = "Mornings",
              oculta = true,
          )
      assertEquals(DataOutcome.Success(CategoriaWrite.Saved(expected)), renamed)
      assertEquals(
          DataOutcome.Success(CategoriaWrite.Duplicate(expected)),
          repository.update(yoga.id, draft("gym")),
      )
      assertEquals(
          DataOutcome.Success(CategoriaWrite.Duplicate(comida)),
          repository.update(yoga.id, draft("comida")),
      )
      assertInvalid(repository.update(comida.id, draft("Food")))
      assertInvalid(repository.update("missing", draft("Pilates")))
      assertEquals(setOf(comida, expected, yoga), repository.getAll().rows().toSet())
    }
  }

  @Test
  fun `setOculta hides any categoria but Otros and skips unchanged values`() = runTest {
    withRepository { database, repository ->
      database.seed(comida, otros)
      var notifications = 0
      database.categoriaQueries.selectAllCategorias().addListener { notifications++ }

      assertEquals(DataOutcome.Success(Unit), repository.setOculta(comida.id, oculta = true))
      assertEquals(DataOutcome.Success(Unit), repository.setOculta(comida.id, oculta = true))
      assertEquals(1, notifications)
      assertInvalid(repository.setOculta(CATEGORIA_OTROS_ID, oculta = true))
      assertInvalid(repository.setOculta("missing", oculta = true))
      assertEquals(
          mapOf(comida.id to true, CATEGORIA_OTROS_ID to false),
          repository.getAll().rows().associate { it.id to it.oculta },
      )

      assertEquals(DataOutcome.Success(Unit), repository.setOculta(comida.id, oculta = false))
      assertEquals(false, repository.getAll().rows().single { it.id == comida.id }.oculta)
    }
  }

  @Test
  fun `delete removes an unused custom categoria and keeps one referenced in any moneda`() =
      runTest {
        withRepository { database, repository ->
          database.seed(comida)
          val unused = repository.create(draft("Gym")).savedCategoria()
          val used = repository.create(draft("Yoga")).savedCategoria()
          database.insertMovimiento("usd", Moneda.USD, listOf(used.id))
          database.insertMovimiento("cop", Moneda.COP, listOf(comida.id, used.id))

          assertEquals(DataOutcome.Success(0L), repository.countMovimientos(unused.id))
          assertEquals(DataOutcome.Success(2L), repository.countMovimientos(used.id))
          assertEquals(DataOutcome.Success(CategoriaDelete.Deleted), repository.delete(unused.id))
          assertEquals(DataOutcome.Success(CategoriaDelete.InUse(2)), repository.delete(used.id))
          assertInvalid(repository.delete(comida.id))
          assertInvalid(repository.delete(unused.id))
          assertEquals(setOf(comida, used), repository.getAll().rows().toSet())
        }
      }

  @Test
  fun `movimiento counts cover the moneda only and roll children up into their parent`() = runTest {
    withRepository { database, repository ->
      val parent = comida.copy(id = "cat_parent")
      val firstChild = comida.copy(id = "cat_child_1", padreId = parent.id)
      val secondChild = comida.copy(id = "cat_child_2", padreId = parent.id)
      database.seed(parent, firstChild, secondChild, otros)
      val custom = repository.create(draft("Gym")).savedCategoria()
      database.insertMovimiento("both-children", Moneda.COP, listOf(firstChild.id, secondChild.id))
      database.insertMovimiento("parent", Moneda.COP, listOf(parent.id))
      database.insertMovimiento("custom", Moneda.COP, listOf(custom.id, otros.id))
      database.insertMovimiento("other-moneda", Moneda.USD, listOf(firstChild.id, custom.id))

      assertEquals(
          DataOutcome.Success(
              mapOf(
                  parent.id to 2L,
                  firstChild.id to 1L,
                  secondChild.id to 1L,
                  custom.id to 1L,
                  otros.id to 1L,
              )
          ),
          repository.observeMovimientoCounts(Moneda.COP).first(),
      )
    }
  }

  private suspend fun TestScope.withRepository(
      block: suspend (TallybookDatabase, CategoriaRepository) -> Unit
  ) {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    var nextId = 0
    try {
      val database = TallybookDatabase(driver)
      val repository =
          CategoriaRepositoryImpl(
              database,
              StandardTestDispatcher(testScheduler),
              IdGenerator { "custom-${++nextId}" },
          )
      block(database, repository)
    } finally {
      driver.close()
    }
  }

  private fun TallybookDatabase.seed(vararg categorias: Categoria) {
    categorias.forEach {
      categoriaQueries.insertCategoria(
          it.id,
          it.nombre,
          it.padreId,
          it.icono.storageKey,
          it.color.storageKey,
      )
    }
  }

  private fun TallybookDatabase.insertMovimiento(
      id: String,
      moneda: Moneda,
      categoriaIds: List<String>,
  ) {
    transaction {
      movimientoQueries.insertMovimiento(
          id,
          "GASTO",
          1_000,
          moneda.name,
          "2026-10-01",
          null,
          null,
          "2026-10-01T12:00:00Z",
      )
      categoriaIds.forEachIndexed { posicion, categoriaId ->
        movimientoQueries.insertMovimientoCategoria(id, categoriaId, posicion.toLong())
      }
    }
  }

  private fun draft(nombre: String, descripcion: String? = null) =
      CategoriaDraft(nombre, CategoryIcon.DUMBBELL, CategoryTint.NEUTRAL, descripcion)

  private fun <T> DataOutcome<T>.successData(): T = assertIs<DataOutcome.Success<T>>(this).data

  private fun DataOutcome<CategoriaWrite>.savedCategoria(): Categoria =
      assertIs<CategoriaWrite.Saved>(successData()).categoria

  private fun DataOutcome<List<Categoria>>.rows(): List<Categoria> = successData()

  private fun assertInvalid(outcome: DataOutcome<*>) {
    assertIs<DataError.InvalidInput>(assertIs<DataOutcome.Failure>(outcome).error)
  }

  private companion object {
    val comida = Categoria("cat_comida", "Comida", null, CategoryIcon.UTENSILS, CategoryTint.AMBER)
    val cafe = Categoria("cat_cafe", "Café", null, CategoryIcon.COFFEE, CategoryTint.AMBER)
    val otros =
        Categoria(
            CATEGORIA_OTROS_ID,
            "Otros",
            null,
            CategoryIcon.CIRCLE_DASHED,
            CategoryTint.NEUTRAL,
        )
  }
}
