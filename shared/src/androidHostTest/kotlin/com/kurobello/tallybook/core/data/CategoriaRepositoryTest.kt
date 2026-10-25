package com.kurobello.tallybook.core.data

import app.cash.sqldelight.db.SqlDriver
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
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Cuenta
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
  fun `seeding stores SEED while create update and hide store the stamp they issued`() = runTest {
    val changeClock = TestChangeClock()
    withRepository(changeClock) { database, repository ->
      val storedStamp = { id: String ->
        database.categoriaQueries.selectCategoriaById(id).executeAsOne().updated_at
      }

      repository.ensureSeeded(
          listOf(Categoria("cat_comida", "Comida", null, CategoryIcon.TAG, CategoryTint.NEUTRAL))
      )
      assertEquals(ChangeStamp.SEED.value, storedStamp("cat_comida"))
      assertEquals(emptyList(), changeClock.issuedStamps)

      val created = repository.create(draft("Gimnasio")).savedCategoria()
      assertEquals(changeClock.issuedStamps.single().value, storedStamp(created.id))

      repository.update(created.id, draft("Gimnasio", descripcion = "pesas"))
      assertEquals(2, changeClock.issuedStamps.size)
      assertEquals(changeClock.issuedStamps.last().value, storedStamp(created.id))

      repository.setOculta("cat_comida", oculta = true)
      assertEquals(3, changeClock.issuedStamps.size)
      assertEquals(changeClock.issuedStamps.last().value, storedStamp("cat_comida"))
    }
  }

  @Test
  fun `localizing seeded names keeps the SEED stamp and issues none`() = runTest {
    val changeClock = TestChangeClock()
    withRepository(changeClock) { database, repository ->
      repository.ensureSeeded(listOf(comida))

      assertEquals(
          DataOutcome.Success(Unit),
          repository.localizeSeeded(listOf(comida.copy(nombre = "Food"))),
      )

      val stored = database.categoriaQueries.selectCategoriaById(comida.id).executeAsOne()
      assertEquals("Food" to ChangeStamp.SEED.value, stored.nombre to stored.updated_at)
      assertEquals(emptyList(), changeClock.issuedStamps)
    }
  }

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
  fun `create and update reject a blank or over 30 character nombre or a long descripcion`() =
      runTest {
        withRepository { _, repository ->
          val custom = repository.create(draft("Gym")).savedCategoria()
          val invalid =
              listOf(
                  draft("   "),
                  draft("x".repeat(31)),
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
                          " ${"x".repeat(30)} ",
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
          val usedOnce = repository.create(draft("Pilates")).savedCategoria()
          database.insertMovimiento("usd", Moneda.USD, used.id)
          database.insertMovimiento("cop", Moneda.COP, used.id)
          database.insertMovimiento("comida", Moneda.COP, comida.id)
          database.insertMovimiento("once", Moneda.COP, usedOnce.id)

          assertEquals(DataOutcome.Success(0L), repository.countMovimientos(unused.id))
          assertEquals(DataOutcome.Success(2L), repository.countMovimientos(used.id))
          assertEquals(DataOutcome.Success(CategoriaDelete.Deleted), repository.delete(unused.id))
          assertEquals(DataOutcome.Success(CategoriaDelete.InUse(2)), repository.delete(used.id))
          assertEquals(
              DataOutcome.Success(CategoriaDelete.InUse(1)),
              repository.delete(usedOnce.id),
          )
          assertInvalid(repository.delete(comida.id))
          assertInvalid(repository.delete(unused.id))
          assertEquals(setOf(comida, used, usedOnce), repository.getAll().rows().toSet())
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
      database.insertMovimiento("first-child", Moneda.COP, firstChild.id)
      database.insertMovimiento("second-child", Moneda.COP, secondChild.id)
      database.insertMovimiento("parent", Moneda.COP, parent.id)
      database.insertMovimiento("custom", Moneda.COP, custom.id)
      database.insertMovimiento("pending", Moneda.COP, otros.id)
      database.insertMovimiento("other-moneda", Moneda.USD, firstChild.id)

      assertEquals(
          DataOutcome.Success(
              mapOf(
                  parent.id to 3L,
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

  @Test
  fun `delete tombstones the categoria with one stamp and hides it from every read`() = runTest {
    val changeClock = TestChangeClock()
    withStorage(changeClock) { driver, database, repository ->
      database.seed(comida)
      val gym = repository.create(draft("Gym")).savedCategoria()
      database.insertMovimiento("captured", Moneda.COP, gym.id)
      driver.tombstone("movimiento", "captured")

      assertEquals(DataOutcome.Success(0L), repository.countMovimientos(gym.id))
      assertEquals(
          DataOutcome.Success(emptyMap()),
          repository.observeMovimientoCounts(Moneda.COP).first(),
      )
      assertEquals(DataOutcome.Success(CategoriaDelete.Deleted), repository.delete(gym.id))

      assertEquals(2, changeClock.issuedStamps.size)
      val stamp = changeClock.issuedStamps.last().value
      assertEquals(stamp to stamp, driver.storedStamps("categoria", gym.id))
      assertEquals(listOf(comida), repository.getAll().rows())
      assertEquals(listOf(comida), repository.observeAll().first().rows())
      assertInvalid(repository.update(gym.id, draft("Pilates")))
      assertInvalid(repository.setOculta(gym.id, oculta = true))
      assertInvalid(repository.delete(gym.id))
      assertEquals(2, changeClock.issuedStamps.size)
    }
  }

  @Test
  fun `a deleted categoria name can be reused and gets a new id`() = runTest {
    withRepository { _, repository ->
      val gym = repository.create(draft("Gym")).savedCategoria()
      repository.delete(gym.id)

      val again = repository.create(draft(" gym ")).savedCategoria()

      assertEquals(gym.copy(id = "custom-2", nombre = "gym"), again)
      assertEquals(listOf(again), repository.getAll().rows())
    }
  }

  @Test
  fun `unchanged custom update and visibility writes issue no stamp`() = runTest {
    val changeClock = TestChangeClock()
    withStorage(changeClock) { driver, database, repository ->
      database.seed(comida)
      val gym = repository.create(draft("Gym", descripcion = "pesas")).savedCategoria()
      val stored = driver.storedStamps("categoria", gym.id)

      assertEquals(
          DataOutcome.Success(CategoriaWrite.Saved(gym)),
          repository.update(gym.id, draft(" Gym ", descripcion = " pesas ")),
      )
      assertEquals(DataOutcome.Success(Unit), repository.setOculta(comida.id, oculta = false))

      assertEquals(1, changeClock.issuedStamps.size)
      assertEquals(stored, driver.storedStamps("categoria", gym.id))
      assertEquals(
          ChangeStamp.SEED.value to null,
          driver.storedStamps("categoria", comida.id),
      )
    }
  }

  private suspend fun TestScope.withRepository(
      changeClock: ChangeClock = TestChangeClock(),
      block: suspend (TallybookDatabase, CategoriaRepository) -> Unit,
  ) = withStorage(changeClock) { _, database, repository -> block(database, repository) }

  private suspend fun TestScope.withStorage(
      changeClock: ChangeClock = TestChangeClock(),
      block: suspend (SqlDriver, TallybookDatabase, CategoriaRepository) -> Unit,
  ) {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    var nextId = 0
    try {
      val database = TallybookDatabase(driver).apply { seedCashCuentas() }
      val repository =
          CategoriaRepositoryImpl(
              database,
              StandardTestDispatcher(testScheduler),
              IdGenerator { "custom-${++nextId}" },
              changeClock,
          )
      block(driver, database, repository)
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
          ChangeStamp.SEED.value,
      )
    }
  }

  private fun TallybookDatabase.insertMovimiento(
      id: String,
      moneda: Moneda,
      categoriaId: String,
  ) {
    movimientoQueries.insertMovimiento(
        id,
        "GASTO",
        1_000,
        "2026-10-01",
        null,
        null,
        "2026-10-01T12:00:00Z",
        categoriaId,
        Cuenta.defaultCashId(moneda),
        "EFECTIVO",
        ChangeStamp.SEED.value,
    )
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
