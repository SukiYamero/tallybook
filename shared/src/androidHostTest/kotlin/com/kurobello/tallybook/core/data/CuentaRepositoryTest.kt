package com.kurobello.tallybook.core.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaArchive
import com.kurobello.tallybook.core.model.CuentaDelete
import com.kurobello.tallybook.core.model.CuentaDraft
import com.kurobello.tallybook.core.model.CuentaRules
import com.kurobello.tallybook.core.model.CuentaWrite
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.Transferencia
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class CuentaRepositoryTest {

  @Test
  fun `create trims the name and stores the signed opening balance with one stamp`() = runTest {
    withRepository { driver, _, changeClock, repository ->
      val card = repository.create(draft("  NU ", TipoCuenta.CREDITO, -150_000, 15)).saved()

      val expected =
          Cuenta(
              id = "cuenta-1",
              nombre = "NU",
              tipo = TipoCuenta.CREDITO,
              saldoInicial = Money(-150_000, Moneda.COP),
              diaCorte = 15,
              archivada = false,
              createdAt = NOW,
          )
      assertEquals(expected, card)
      assertEquals(DataOutcome.Success(expected), repository.getById(card.id))
      assertEquals(DataOutcome.Success(listOf(COP_CASH, expected)), repository.observeAll().first())
      val stamp = changeClock.issuedStamps.single().value
      assertEquals(stamp to null, driver.storedStamps("cuenta", card.id))
    }
  }

  @Test
  fun `create detects duplicates per currency ignoring case and accents but not tombstones`() =
      runTest {
        withRepository { driver, _, changeClock, repository ->
          val nequi = repository.create(draft("Nequí", TipoCuenta.BANCO)).saved()
          val archived = repository.create(draft("Ahorros", TipoCuenta.BANCO)).saved()
          repository.setArchivada(archived.id, true)
          val stampsBefore = changeClock.issuedStamps.size

          assertEquals(
              DataOutcome.Success(CuentaWrite.Duplicate(nequi)),
              repository.create(draft(" NEQUI ", TipoCuenta.EFECTIVO)),
          )
          assertEquals(
              DataOutcome.Success(CuentaWrite.Duplicate(archived.copy(archivada = true))),
              repository.create(draft("ahorros", TipoCuenta.BANCO)),
          )
          assertEquals(stampsBefore, changeClock.issuedStamps.size)

          val usd = repository.create(draft("Nequi", TipoCuenta.BANCO, moneda = Moneda.USD)).saved()
          assertEquals(Moneda.USD, usd.moneda)

          driver.tombstone("cuenta", nequi.id)
          val tombstone = driver.storedStamps("cuenta", nequi.id)
          val reused = repository.create(draft("Nequi", TipoCuenta.BANCO)).saved()
          assertTrue(reused.id != nequi.id)
          assertEquals(tombstone, driver.storedStamps("cuenta", nequi.id))
        }
      }

  @Test
  fun `create and update reject invalid drafts without writing`() = runTest {
    withRepository { _, _, changeClock, repository ->
      val invalid =
          listOf(
              draft(" ", TipoCuenta.BANCO),
              draft("a".repeat(CuentaRules.NOMBRE_MAX + 1), TipoCuenta.BANCO),
              draft("Nequi", TipoCuenta.BANCO, diaCorte = 15),
              draft("NU", TipoCuenta.CREDITO, diaCorte = 0),
              draft("NU", TipoCuenta.CREDITO, diaCorte = 32),
          )
      invalid.forEach { assertInvalid(repository.create(it)) }
      invalid.forEach { assertInvalid(repository.update(COP_CASH.id, it)) }
      assertInvalid(repository.update("absent", draft("Nequi", TipoCuenta.BANCO)))
      assertEquals(0, changeClock.issuedStamps.size)
      assertEquals(DataOutcome.Success(listOf(COP_CASH)), repository.observeAll().first())
    }
  }

  @Test
  fun `update saves new values once and a no-op or self-match writes nothing`() = runTest {
    withRepository { driver, _, changeClock, repository ->
      val nequi = repository.create(draft("Nequi", TipoCuenta.BANCO)).saved()

      val renamed = repository.update(nequi.id, draft("NEQUI", TipoCuenta.BANCO, 2_000)).saved()
      assertEquals(nequi.copy(nombre = "NEQUI", saldoInicial = Money(2_000, Moneda.COP)), renamed)
      assertEquals(2, changeClock.issuedStamps.size)
      assertEquals(
          changeClock.issuedStamps.last().value to null,
          driver.storedStamps("cuenta", nequi.id),
      )

      assertEquals(
          DataOutcome.Success(CuentaWrite.Saved(renamed)),
          repository.update(nequi.id, draft(" NEQUI ", TipoCuenta.BANCO, 2_000)),
      )
      assertEquals(
          DataOutcome.Success(CuentaWrite.Duplicate(COP_CASH)),
          repository.update(nequi.id, draft("efectivo", TipoCuenta.BANCO)),
      )
      assertEquals(2, changeClock.issuedStamps.size)
    }
  }

  @Test
  fun `the currency is locked only while live movimientos or transfers reference the cuenta`() =
      runTest {
        withRepository { driver, database, changeClock, repository ->
          val nequi = repository.create(draft("Nequi", TipoCuenta.BANCO)).saved()
          val toUsd = draft("Nequi", TipoCuenta.BANCO, moneda = Moneda.USD)
          database.insertLegacy(movimiento("mov", nequi.id))
          database.insertTransferenciaRow(transferencia("in", COP_CASH.id, nequi.id), SEED)
          database.insertTransferenciaRow(transferencia("out", nequi.id, COP_CASH.id), SEED)
          val stamps = changeClock.issuedStamps.size

          for (reference in listOf("movimiento" to "mov", "transferencia" to "in")) {
            assertEquals(
                DataOutcome.Success(CuentaWrite.CurrencyLocked),
                repository.update(nequi.id, toUsd),
            )
            driver.tombstone(reference.first, reference.second)
          }
          assertEquals(
              DataOutcome.Success(CuentaWrite.CurrencyLocked),
              repository.update(nequi.id, toUsd),
          )
          assertEquals(stamps, changeClock.issuedStamps.size)
          assertEquals(
              DataOutcome.Success(CuentaWrite.Saved(nequi)),
              repository.update(nequi.id, draft("Nequi", TipoCuenta.BANCO)),
          )

          driver.tombstone("transferencia", "out")
          assertEquals(
              nequi.copy(saldoInicial = Money(0, Moneda.USD)),
              repository.update(nequi.id, toUsd).saved(),
          )
          assertEquals(stamps + 1, changeClock.issuedStamps.size)
        }
      }

  @Test
  fun `the type is locked only while live movimientos or transfers reference the cuenta`() =
      runTest {
        withRepository { driver, database, changeClock, repository ->
          val nequi = repository.create(draft("Nequi", TipoCuenta.BANCO)).saved()
          val asCash = draft("Nequi", TipoCuenta.EFECTIVO)
          database.insertLegacy(movimiento("mov", nequi.id))
          database.insertTransferenciaRow(transferencia("out", nequi.id, COP_CASH.id), SEED)
          val stamps = changeClock.issuedStamps.size

          assertEquals(
              DataOutcome.Success(CuentaWrite.TypeLocked),
              repository.update(nequi.id, asCash),
          )
          driver.tombstone("movimiento", "mov")
          assertEquals(
              DataOutcome.Success(CuentaWrite.TypeLocked),
              repository.update(nequi.id, asCash),
          )
          assertEquals(stamps, changeClock.issuedStamps.size)

          driver.tombstone("transferencia", "out")
          assertEquals(
              nequi.copy(tipo = TipoCuenta.EFECTIVO),
              repository.update(nequi.id, asCash).saved(),
          )
        }
      }

  @Test
  fun `a default cash cuenta keeps its currency even while unused`() = runTest {
    withRepository { _, _, changeClock, repository ->
      assertEquals(
          DataOutcome.Success(CuentaWrite.CurrencyLocked),
          repository.update(
              COP_CASH.id,
              draft("Efectivo", TipoCuenta.EFECTIVO, moneda = Moneda.USD),
          ),
      )
      assertEquals(0, changeClock.issuedStamps.size)
      assertEquals(
          COP_CASH.copy(nombre = "Billetera", saldoInicial = Money(5_000, Moneda.COP)),
          repository.update(COP_CASH.id, draft("Billetera", TipoCuenta.EFECTIVO, 5_000)).saved(),
      )
    }
  }

  @Test
  fun `archiving the last active cuenta is refused and unarchiving is allowed`() = runTest {
    withRepository { _, _, changeClock, repository ->
      assertEquals(
          DataOutcome.Success(CuentaArchive.LastActive),
          repository.setArchivada(COP_CASH.id, true),
      )
      val nequi = repository.create(draft("Nequi", TipoCuenta.BANCO)).saved()

      assertEquals(
          DataOutcome.Success(CuentaArchive.Updated),
          repository.setArchivada(COP_CASH.id, true),
      )
      assertEquals(
          DataOutcome.Success(CuentaArchive.LastActive),
          repository.setArchivada(nequi.id, true),
      )
      assertEquals(
          DataOutcome.Success(CuentaArchive.Updated),
          repository.setArchivada(COP_CASH.id, true),
      )
      assertEquals(2, changeClock.issuedStamps.size)
      assertEquals(
          DataOutcome.Success(COP_CASH.copy(archivada = true)),
          repository.getById(COP_CASH.id),
      )
      assertEquals(
          DataOutcome.Success(CuentaArchive.Updated),
          repository.setArchivada(COP_CASH.id, false),
      )
      assertEquals(DataOutcome.Success(COP_CASH), repository.getById(COP_CASH.id))
      assertEquals(3, changeClock.issuedStamps.size)
      assertInvalid(repository.setArchivada("absent", true))
    }
  }

  @Test
  fun `delete refuses a referenced or last active cuenta and tombstones an unused one`() = runTest {
    withRepository { driver, database, changeClock, repository ->
      assertEquals(DataOutcome.Success(CuentaDelete.LastActive), repository.delete(COP_CASH.id))
      val nequi = repository.create(draft("Nequi", TipoCuenta.BANCO)).saved()
      database.insertLegacy(movimiento("mov", nequi.id))
      database.insertLegacy(movimiento("gone", nequi.id))
      driver.tombstone("movimiento", "gone")
      database.insertTransferenciaRow(transferencia("in", COP_CASH.id, nequi.id), SEED)
      database.insertTransferenciaRow(transferencia("out", nequi.id, COP_CASH.id), SEED)
      val stamps = changeClock.issuedStamps.size

      assertEquals(DataOutcome.Success(CuentaDelete.InUse(3)), repository.delete(nequi.id))
      for (id in listOf("in", "out")) driver.tombstone("transferencia", id)
      driver.tombstone("movimiento", "mov")
      assertEquals(stamps, changeClock.issuedStamps.size)

      assertEquals(DataOutcome.Success(CuentaDelete.Deleted), repository.delete(nequi.id))
      val stamp = changeClock.issuedStamps.last().value
      assertEquals(stamps + 1, changeClock.issuedStamps.size)
      assertEquals(stamp to stamp, driver.storedStamps("cuenta", nequi.id))
      assertEquals(DataOutcome.Success(null), repository.getById(nequi.id))
      assertEquals(DataOutcome.Success(listOf(COP_CASH)), repository.observeAll().first())
      assertInvalid(repository.delete(nequi.id))
    }
  }

  @Test
  fun `an archived cuenta can be deleted while another one stays active`() = runTest {
    withRepository { _, _, _, repository ->
      val nequi = repository.create(draft("Nequi", TipoCuenta.BANCO)).saved()
      repository.setArchivada(COP_CASH.id, true)

      assertEquals(DataOutcome.Success(CuentaDelete.LastActive), repository.delete(nequi.id))
      assertEquals(DataOutcome.Success(CuentaDelete.Deleted), repository.delete(COP_CASH.id))
      assertEquals(DataOutcome.Success(listOf(nequi)), repository.observeAll().first())
    }
  }

  @Test
  fun `every mutation advances the stamp`() = runTest {
    withRepository { _, _, changeClock, repository ->
      val nequi = repository.create(draft("Nequi", TipoCuenta.BANCO)).saved()
      repository.update(nequi.id, draft("Nequi Ahorro", TipoCuenta.BANCO))
      repository.setArchivada(nequi.id, true)
      repository.delete(nequi.id)

      val stamps = changeClock.issuedStamps
      assertEquals(4, stamps.size)
      assertEquals(stamps.sortedBy { it.value }, stamps)
      assertEquals(stamps.distinct(), stamps)
    }
  }

  private suspend fun TestScope.withRepository(
      block: suspend (SqlDriver, TallybookDatabase, TestChangeClock, CuentaRepository) -> Unit
  ) {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    var nextId = 0
    try {
      val database = TallybookDatabase(driver).apply { insertCuentaRow(COP_CASH, ChangeStamp.SEED) }
      val changeClock = TestChangeClock()
      val repository =
          CuentaRepositoryImpl(
              database = database,
              dispatcher = StandardTestDispatcher(testScheduler),
              idGenerator = IdGenerator { "cuenta-${++nextId}" },
              clock = FixedClock,
              changeClock = changeClock,
          )
      block(driver, database, changeClock, repository)
    } finally {
      driver.close()
    }
  }

  private fun draft(
      nombre: String,
      tipo: TipoCuenta,
      saldoInicial: Long = 0,
      diaCorte: Int? = null,
      moneda: Moneda = Moneda.COP,
  ) = CuentaDraft(nombre, tipo, Money(saldoInicial, moneda), diaCorte)

  private fun movimiento(id: String, cuentaId: String) =
      Movimiento(
          id = id,
          tipo = TipoMovimiento.GASTO,
          monto = Money(500, Moneda.COP),
          fecha = LocalDate(2026, 10, 1),
          categoriaId = "cat_comida",
          titulo = null,
          descripcion = null,
          createdAt = NOW,
          cuentaId = cuentaId,
          medio = MedioPago.DEBITO,
      )

  private fun transferencia(id: String, origen: String, destino: String) =
      Transferencia(
          id = id,
          cuentaOrigenId = origen,
          cuentaDestinoId = destino,
          montoOrigen = Money(100, Moneda.COP),
          montoDestino = Money(100, Moneda.COP),
          fecha = LocalDate(2026, 10, 1),
          descripcion = null,
          createdAt = NOW,
      )

  private fun DataOutcome<CuentaWrite>.saved(): Cuenta =
      assertIs<CuentaWrite.Saved>(assertIs<DataOutcome.Success<CuentaWrite>>(this).data).cuenta

  private fun assertInvalid(outcome: DataOutcome<*>) {
    assertIs<DataError.InvalidInput>(assertIs<DataOutcome.Failure>(outcome).error)
  }

  private object FixedClock : Clock {
    override fun now(): Instant = NOW
  }

  private companion object {
    val NOW: Instant = Instant.parse("2026-10-16T12:00:00Z")
    val COP_CASH: Cuenta = cashCuenta(Moneda.COP)
    val SEED: ChangeStamp = ChangeStamp.SEED
  }
}
