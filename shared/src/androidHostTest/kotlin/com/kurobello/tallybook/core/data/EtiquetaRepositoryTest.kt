package com.kurobello.tallybook.core.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.CategoriaRules
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.EtiquetaWrite
import com.kurobello.tallybook.core.model.Moneda
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class EtiquetaRepositoryTest {

  @Test
  fun `create trims the name and lists etiquetas by name ignoring case and accents`() = runTest {
    withRepository { _, repository ->
      val viaje = repository.create("  viaje Chile ").saved()
      val arbol = repository.create("Árbol").saved()
      val empresa = repository.create("Empresa").saved()

      assertEquals(Etiqueta("tag-1", "viaje Chile", NOW), viaje)
      assertEquals(DataOutcome.Success(listOf(arbol, empresa, viaje)), repository.getAll())
      assertEquals(
          DataOutcome.Success(listOf(arbol, empresa, viaje)),
          repository.observeAll().first(),
      )
    }
  }

  @Test
  fun `create offers the existing etiqueta for a case or accent duplicate without writing`() =
      runTest {
        withRepository { database, repository ->
          val cafe = repository.create("Café").saved()
          var notifications = 0
          database.etiquetaQueries.selectAllEtiquetas().addListener { notifications++ }

          assertEquals(
              DataOutcome.Success(EtiquetaWrite.Duplicate(cafe)),
              repository.create(" CAFE "),
          )
          assertEquals(0, notifications)
          assertEquals(listOf(cafe), repository.getAll().successData())
        }
      }

  @Test
  fun `create and rename reject a blank or too long name without writing`() = runTest {
    withRepository { _, repository ->
      val empresa = repository.create("Empresa").saved()

      listOf("   ", "x".repeat(CategoriaRules.NOMBRE_MAX + 1)).forEach { nombre ->
        assertInvalid(repository.create(nombre))
        assertInvalid(repository.rename(empresa.id, nombre))
      }
      assertEquals(listOf(empresa), repository.getAll().successData())
      assertIs<EtiquetaWrite.Saved>(
          repository.create(" ${"x".repeat(CategoriaRules.NOMBRE_MAX)} ").successData()
      )
    }
  }

  @Test
  fun `rename keeps identity and refuses a name another etiqueta already has`() = runTest {
    withRepository { _, repository ->
      val empresa = repository.create("Empresa").saved()
      val viaje = repository.create("Viaje").saved()

      val renamed = repository.rename(empresa.id, " EMPRESA S.A. ")
      val expected = empresa.copy(nombre = "EMPRESA S.A.")
      assertEquals(DataOutcome.Success(EtiquetaWrite.Saved(expected)), renamed)
      assertEquals(
          DataOutcome.Success(EtiquetaWrite.Saved(expected.copy(nombre = "empresa s.a."))),
          repository.rename(empresa.id, "empresa s.a."),
      )
      assertEquals(
          DataOutcome.Success(EtiquetaWrite.Duplicate(viaje)),
          repository.rename(empresa.id, "víaje"),
      )
      assertInvalid(repository.rename("missing", "Otra"))
      assertEquals(
          listOf("empresa s.a.", "Viaje"),
          repository.getAll().successData().map(Etiqueta::nombre),
      )
    }
  }

  @Test
  fun `counts cover the moneda for the list and every moneda for a single etiqueta`() = runTest {
    withRepository { database, repository ->
      val empresa = repository.create("Empresa").saved()
      val viaje = repository.create("Viaje").saved()
      val unused = repository.create("Sin uso").saved()
      database.insertMovimiento("cop-both", Moneda.COP, empresa.id, viaje.id)
      database.insertMovimiento("cop-empresa", Moneda.COP, empresa.id)
      database.insertMovimiento("usd-empresa", Moneda.USD, empresa.id)

      assertEquals(
          DataOutcome.Success(mapOf(empresa.id to 2L, viaje.id to 1L)),
          repository.observeMovimientoCounts(Moneda.COP).first(),
      )
      assertEquals(DataOutcome.Success(3L), repository.countMovimientos(empresa.id))
      assertEquals(DataOutcome.Success(0L), repository.countMovimientos(unused.id))
    }
  }

  @Test
  fun `delete unlinks the etiqueta from its movimientos and keeps the movimientos`() = runTest {
    withRepository { database, repository ->
      val empresa = repository.create("Empresa").saved()
      val viaje = repository.create("Viaje").saved()
      database.insertMovimiento("both", Moneda.COP, empresa.id, viaje.id)
      database.insertMovimiento("only", Moneda.USD, empresa.id)
      var movimientoNotifications = 0
      database.movimientoQueries.selectAllMovimientos().addListener { movimientoNotifications++ }

      assertEquals(DataOutcome.Success(Unit), repository.delete(empresa.id))

      assertEquals(1, movimientoNotifications)
      assertEquals(listOf(viaje), repository.getAll().successData())
      assertEquals(
          mapOf("both" to listOf(viaje.id), "only" to emptyList()),
          database.movimientoQueries.selectAllMovimientos().executeAsList().associate {
            it.id to it.toDomain().etiquetaIds
          },
      )
      assertEquals(DataOutcome.Success(0L), repository.countMovimientos(empresa.id))
      assertInvalid(repository.delete(empresa.id))
    }
  }

  private suspend fun TestScope.withRepository(
      block: suspend (TallybookDatabase, EtiquetaRepository) -> Unit
  ) {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    var nextId = 0
    try {
      val database = TallybookDatabase(driver)
      val repository =
          EtiquetaRepositoryImpl(
              database = database,
              dispatcher = StandardTestDispatcher(testScheduler),
              idGenerator = IdGenerator { "tag-${++nextId}" },
              clock = FixedClock,
          )
      block(database, repository)
    } finally {
      driver.close()
    }
  }

  private fun TallybookDatabase.insertMovimiento(
      id: String,
      moneda: Moneda,
      vararg etiquetaIds: String,
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
          "cat_comida",
      )
      etiquetaIds.forEachIndexed { posicion, etiquetaId ->
        movimientoQueries.insertMovimientoEtiqueta(id, etiquetaId, posicion.toLong())
      }
    }
  }

  private fun <T> DataOutcome<T>.successData(): T = assertIs<DataOutcome.Success<T>>(this).data

  private fun DataOutcome<EtiquetaWrite>.saved(): Etiqueta =
      assertIs<EtiquetaWrite.Saved>(successData()).etiqueta

  private fun assertInvalid(outcome: DataOutcome<*>) {
    assertIs<DataError.InvalidInput>(assertIs<DataOutcome.Failure>(outcome).error)
  }

  private object FixedClock : Clock {
    override fun now(): Instant = NOW
  }

  private companion object {
    val NOW: Instant = Instant.parse("2026-10-16T12:00:00Z")
  }
}
