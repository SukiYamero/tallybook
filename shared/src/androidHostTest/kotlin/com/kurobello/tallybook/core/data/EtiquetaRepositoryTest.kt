package com.kurobello.tallybook.core.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.ETIQUETA_DESCRIPCION_MAX
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.EtiquetaWrite
import com.kurobello.tallybook.core.model.Moneda
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class EtiquetaRepositoryTest {

  @Test
  fun `create and update store the stamp their transaction issued`() = runTest {
    val changeClock = TestChangeClock()
    withRepository(changeClock) { database, repository ->
      val created = repository.create("Viaje").saved()
      val storedStamp = {
        database.etiquetaQueries.selectEtiquetaById(created.id).executeAsOne().updated_at
      }
      assertEquals(changeClock.issuedStamps.single().value, storedStamp())

      repository.update(created.id, "Viaje", "con Ana")
      assertEquals(2, changeClock.issuedStamps.size)
      assertEquals(changeClock.issuedStamps.last().value, storedStamp())
    }
  }

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
  fun `create and update reject a blank or over 30 character name without writing`() = runTest {
    withRepository { _, repository ->
      val empresa = repository.create("Empresa").saved()

      listOf("   ", "x".repeat(31)).forEach { nombre ->
        assertInvalid(repository.create(nombre))
        assertInvalid(repository.update(empresa.id, nombre, descripcion = null))
      }
      assertEquals(listOf(empresa), repository.getAll().successData())
      assertIs<EtiquetaWrite.Saved>(repository.create(" ${"x".repeat(30)} ").successData())
    }
  }

  @Test
  fun `update keeps identity and refuses a name another etiqueta already has`() = runTest {
    withRepository { _, repository ->
      val empresa = repository.create("Empresa").saved()
      val viaje = repository.create("Viaje").saved()

      val renamed = repository.update(empresa.id, " EMPRESA S.A. ", descripcion = null)
      val expected = empresa.copy(nombre = "EMPRESA S.A.")
      assertEquals(DataOutcome.Success(EtiquetaWrite.Saved(expected)), renamed)
      assertEquals(
          DataOutcome.Success(EtiquetaWrite.Saved(expected.copy(nombre = "empresa s.a."))),
          repository.update(empresa.id, "empresa s.a.", descripcion = null),
      )
      assertEquals(
          DataOutcome.Success(EtiquetaWrite.Duplicate(viaje)),
          repository.update(empresa.id, "víaje", descripcion = "Otra"),
      )
      assertInvalid(repository.update("missing", "Otra", descripcion = null))
      assertEquals(
          listOf("empresa s.a.", "Viaje"),
          repository.getAll().successData().map(Etiqueta::nombre),
      )
    }
  }

  @Test
  fun `create and update store a normalized description and clear a blank one`() = runTest {
    withRepository { _, repository ->
      val empresa =
          repository.create("Empresa", descripcion = "  Gastos   de\n la  oficina ").saved()
      assertEquals("Gastos de la oficina", empresa.descripcion)

      val updated = repository.update(empresa.id, "Empresa", descripcion = " Reembolsables ")
      assertEquals(
          DataOutcome.Success(EtiquetaWrite.Saved(empresa.copy(descripcion = "Reembolsables"))),
          updated,
      )
      assertEquals("Reembolsables", repository.getAll().successData().single().descripcion)

      repository.update(empresa.id, "Empresa", descripcion = "   ")
      assertNull(repository.getAll().successData().single().descripcion)
      assertNull(repository.create("Viaje", descripcion = " ").saved().descripcion)
    }
  }

  @Test
  fun `create and update reject a too long description without writing`() = runTest {
    withRepository { _, repository ->
      val empresa = repository.create("Empresa", descripcion = "Oficina").saved()
      val tooLong = "x".repeat(ETIQUETA_DESCRIPCION_MAX + 1)

      assertInvalid(repository.create("Viaje", descripcion = tooLong))
      assertInvalid(repository.update(empresa.id, "Empresa", descripcion = tooLong))
      assertEquals(listOf(empresa), repository.getAll().successData())
      val longest = " ${"x".repeat(ETIQUETA_DESCRIPCION_MAX)} "
      assertEquals(
          "x".repeat(ETIQUETA_DESCRIPCION_MAX),
          repository.update(empresa.id, "Empresa", longest).saved().descripcion,
      )
    }
  }

  @Test
  fun `an update that changes nothing does not write`() = runTest {
    withRepository { database, repository ->
      val empresa = repository.create("Empresa", descripcion = "Oficina").saved()
      var notifications = 0
      database.etiquetaQueries.selectAllEtiquetas().addListener { notifications++ }

      assertEquals(
          DataOutcome.Success(EtiquetaWrite.Saved(empresa)),
          repository.update(empresa.id, " Empresa ", descripcion = " Oficina "),
      )
      assertEquals(0, notifications)
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

  @Test
  fun `delete stamps the tombstone and every live movimiento that loses the etiqueta alike`() =
      runTest {
        val changeClock = TestChangeClock()
        withStorage(changeClock) { driver, database, repository ->
          val empresa = repository.create("Empresa").saved()
          val viaje = repository.create("Viaje").saved()
          database.insertMovimiento("both", Moneda.COP, empresa.id, viaje.id)
          database.insertMovimiento("only", Moneda.USD, empresa.id)
          database.insertMovimiento("untouched", Moneda.COP, viaje.id)

          assertEquals(DataOutcome.Success(Unit), repository.delete(empresa.id))

          assertEquals(3, changeClock.issuedStamps.size)
          val stamp = changeClock.issuedStamps.last().value
          assertEquals(stamp to stamp, driver.storedStamps("etiqueta", empresa.id))
          assertEquals(
              mapOf(
                  "both" to stamp,
                  "only" to stamp,
                  "untouched" to ChangeStamp.SEED.value,
              ),
              database.movimientoQueries.selectAllMovimientos().executeAsList().associate {
                it.id to it.updated_at
              },
          )
          assertEquals(listOf(viaje), repository.getAll().successData())
          assertInvalid(repository.update(empresa.id, "Otra", descripcion = null))
          assertInvalid(repository.delete(empresa.id))
          assertEquals(3, changeClock.issuedStamps.size)
        }
      }

  @Test
  fun `a deleted etiqueta name can be reused and gets a new id`() = runTest {
    withRepository { _, repository ->
      val empresa = repository.create("Empresa").saved()
      repository.delete(empresa.id)

      val again = repository.create(" empresa ").saved()

      assertEquals(Etiqueta("tag-2", "empresa", NOW), again)
      assertEquals(listOf(again), repository.getAll().successData())
      assertEquals(listOf(again), repository.observeAll().first().successData())
    }
  }

  @Test
  fun `counts skip tombstoned movimientos`() = runTest {
    withStorage { driver, database, repository ->
      val empresa = repository.create("Empresa").saved()
      database.insertMovimiento("live", Moneda.COP, empresa.id)
      database.insertMovimiento("tombstoned", Moneda.COP, empresa.id)
      driver.tombstone("movimiento", "tombstoned")

      assertEquals(
          DataOutcome.Success(mapOf(empresa.id to 1L)),
          repository.observeMovimientoCounts(Moneda.COP).first(),
      )
      assertEquals(DataOutcome.Success(1L), repository.countMovimientos(empresa.id))
    }
  }

  private suspend fun TestScope.withRepository(
      changeClock: ChangeClock = TestChangeClock(),
      block: suspend (TallybookDatabase, EtiquetaRepository) -> Unit,
  ) = withStorage(changeClock) { _, database, repository -> block(database, repository) }

  private suspend fun TestScope.withStorage(
      changeClock: ChangeClock = TestChangeClock(),
      block: suspend (SqlDriver, TallybookDatabase, EtiquetaRepository) -> Unit,
  ) {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    var nextId = 0
    try {
      val database = TallybookDatabase(driver).apply { seedCashCuentas() }
      val repository =
          EtiquetaRepositoryImpl(
              database = database,
              dispatcher = StandardTestDispatcher(testScheduler),
              idGenerator = IdGenerator { "tag-${++nextId}" },
              clock = FixedClock,
              changeClock = changeClock,
          )
      block(driver, database, repository)
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
          "2026-10-01",
          null,
          null,
          "2026-10-01T12:00:00Z",
          "cat_comida",
          Cuenta.defaultCashId(moneda),
          "EFECTIVO",
          ChangeStamp.SEED.value,
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
