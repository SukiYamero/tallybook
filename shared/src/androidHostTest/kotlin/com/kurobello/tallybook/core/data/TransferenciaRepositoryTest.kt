package com.kurobello.tallybook.core.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.RestoreResult
import com.kurobello.tallybook.core.model.TRANSFERENCIA_DESCRIPCION_MAX
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.Transferencia
import com.kurobello.tallybook.core.model.TransferenciaUpdateResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class TransferenciaRepositoryTest {

  @Test
  fun `insert stores one stamp and getAll lists live rows newest first`() = runTest {
    withRepository { driver, changeClock, repository ->
      val older = transferencia("older", COP_CASH, NEQUI)
      val newer = transferencia("newer", NEQUI, NU, at = 1).copy(fecha = TODAY)
      val deleted = transferencia("deleted", NEQUI, COP_CASH, at = 2)

      for (row in listOf(older, newer, deleted)) {
        assertEquals(DataOutcome.Success(Unit), repository.insert(row))
      }
      assertEquals(
          changeClock.issuedStamps.first().value to null,
          driver.storedStamps("transferencia", older.id),
      )
      driver.tombstone("transferencia", deleted.id)

      assertEquals(DataOutcome.Success(newer), repository.getById(newer.id))
      assertEquals(DataOutcome.Success(null), repository.getById(deleted.id))
      assertEquals(DataOutcome.Success(listOf(newer, older)), repository.getAll())
      assertEquals(3, changeClock.issuedStamps.size)
    }
  }

  @Test
  fun `observeRecent lists the newest live rows up to the limit and follows writes`() = runTest {
    withRepository { _, _, repository ->
      val older = transferencia("older", COP_CASH, NEQUI)
      val newer = transferencia("newer", NEQUI, NU, at = 1).copy(fecha = TODAY)
      val newest = transferencia("newest", NEQUI, COP_CASH, at = 2).copy(fecha = TODAY)
      for (row in listOf(older, newer, newest)) repository.insert(row)

      val emissions = mutableListOf<DataOutcome<List<Transferencia>>>()
      backgroundScope.launch { repository.observeRecent(2).collect { emissions += it } }
      runCurrent()
      repository.deleteAndCapture(newest.id)
      runCurrent()

      assertEquals<List<DataOutcome<List<Transferencia>>>>(
          listOf(
              DataOutcome.Success(listOf(newest, newer)),
              DataOutcome.Success(listOf(newer, older)),
          ),
          emissions,
      )
      assertInvalid(repository.observeRecent(0).first())
    }
  }

  @Test
  fun `insert rejects every rule violation without writing`() = runTest {
    withRepository { driver, changeClock, repository ->
      val valid = transferencia("t", COP_CASH, NEQUI)
      driver.tombstone("cuenta", GONE)

      for (invalid in
          listOf(
              valid.copy(id = " "),
              valid.copy(cuentaDestinoId = COP_CASH),
              valid.copy(cuentaOrigenId = "absent"),
              valid.copy(cuentaDestinoId = GONE),
              valid.copy(montoOrigen = Money(0, Moneda.COP), montoDestino = Money(0, Moneda.COP)),
              valid.copy(montoOrigen = Money(-5, Moneda.COP), montoDestino = Money(-5, Moneda.COP)),
              valid.copy(montoDestino = Money(101, Moneda.COP)),
              valid.copy(montoOrigen = Money(100, Moneda.USD)),
              valid.copy(cuentaOrigenId = USD_CASH),
              valid.copy(fecha = LocalDate(2026, 10, 17)),
              valid.copy(descripcion = "a".repeat(TRANSFERENCIA_DESCRIPCION_MAX + 1)),
          )) {
        assertInvalid(repository.insert(invalid))
      }

      assertEquals(DataOutcome.Success(emptyList()), repository.getAll())
      assertEquals(0, changeClock.issuedStamps.size)
    }
  }

  @Test
  fun `amounts up to the domain maximum are stored and larger ones rejected`() = runTest {
    withRepository { _, _, repository ->
      val largest = Money(Money.MAX_MINOR_UNITS, Moneda.COP)
      val tooLarge = Money(Money.MAX_MINOR_UNITS + 1, Moneda.COP)
      val valid =
          transferencia("t", COP_CASH, NEQUI).copy(montoOrigen = largest, montoDestino = largest)

      assertEquals(DataOutcome.Success(Unit), repository.insert(valid))
      assertInvalid(
          repository.insert(valid.copy(id = "big", montoOrigen = tooLarge, montoDestino = tooLarge))
      )
      assertEquals(DataOutcome.Success(listOf(valid)), repository.getAll())
    }
  }

  @Test
  fun `a cross-currency transfer stores each side in its cuenta currency`() = runTest {
    withRepository { _, _, repository ->
      val payment =
          transferencia("pay", USD_CASH, NU)
              .copy(
                  montoOrigen = Money(2_500, Moneda.USD),
                  montoDestino = Money(100_000, Moneda.COP),
              )

      assertEquals(DataOutcome.Success(Unit), repository.insert(payment))

      assertEquals(DataOutcome.Success(payment), repository.getById(payment.id))
    }
  }

  @Test
  fun `insert rejects an archived cuenta on either side`() = runTest {
    withRepository { driver, changeClock, repository ->
      driver.execute(null, "UPDATE cuenta SET archivada = 1 WHERE id = '$NU'", 0)

      assertInvalid(repository.insert(transferencia("to-card", COP_CASH, NU)))
      assertInvalid(repository.insert(transferencia("from-card", NU, COP_CASH)))

      assertEquals(DataOutcome.Success(emptyList()), repository.getAll())
      assertEquals(0, changeClock.issuedStamps.size)
    }
  }

  @Test
  fun `update keeps an archived side while unchanged and rejects one it newly picks`() = runTest {
    withRepository { driver, _, repository ->
      val row = transferencia("t", COP_CASH, NEQUI)
      repository.insert(row)
      driver.execute(null, "UPDATE cuenta SET archivada = 1 WHERE id IN ('$NEQUI', '$NU')", 0)

      val kept = row.copy(descripcion = "ahorro")
      assertEquals(
          DataOutcome.Success(TransferenciaUpdateResult.Updated),
          repository.update(row.id, kept),
      )
      assertInvalid(repository.update(row.id, kept.copy(cuentaDestinoId = NU)))
      assertInvalid(repository.update(row.id, kept.copy(cuentaOrigenId = NU)))
      assertInvalid(
          repository.update(row.id, kept.copy(cuentaOrigenId = NEQUI, cuentaDestinoId = COP_CASH))
      )
      assertEquals(DataOutcome.Success(kept), repository.getById(row.id))
    }
  }

  @Test
  fun `update replaces every field once keeping the creation instant`() = runTest {
    withRepository { driver, changeClock, repository ->
      val original = transferencia("t", COP_CASH, NEQUI)
      repository.insert(original)
      val changes =
          original.copy(
              cuentaOrigenId = USD_CASH,
              cuentaDestinoId = NU,
              montoOrigen = Money(2_500, Moneda.USD),
              montoDestino = Money(100_000, Moneda.COP),
              fecha = TODAY,
              descripcion = "pago",
              createdAt = CREATED + 60.seconds,
          )

      assertEquals(
          DataOutcome.Success(TransferenciaUpdateResult.Updated),
          repository.update(original.id, changes),
      )
      val expected = changes.copy(createdAt = original.createdAt)
      assertEquals(DataOutcome.Success(expected), repository.getById(original.id))
      assertEquals(
          changeClock.issuedStamps.last().value to null,
          driver.storedStamps("transferencia", original.id),
      )
      assertEquals(
          DataOutcome.Success(TransferenciaUpdateResult.Updated),
          repository.update(original.id, expected),
      )
      assertEquals(2, changeClock.issuedStamps.size)
    }
  }

  @Test
  fun `update rejects invalid changes and reports absent or tombstoned rows`() = runTest {
    withRepository { driver, changeClock, repository ->
      val original = transferencia("t", COP_CASH, NEQUI)
      repository.insert(original)
      repository.insert(transferencia("gone", NEQUI, COP_CASH))
      driver.tombstone("transferencia", "gone")

      for (invalid in
          listOf(
              original.copy(id = "other"),
              original.copy(cuentaDestinoId = COP_CASH),
              original.copy(montoDestino = Money(5, Moneda.COP)),
              original.copy(cuentaOrigenId = USD_CASH),
              original.copy(fecha = LocalDate(2026, 10, 17)),
          )) {
        assertInvalid(repository.update(original.id, invalid))
      }
      assertEquals(
          DataOutcome.Success(TransferenciaUpdateResult.NotFound),
          repository.update("absent", original.copy(id = "absent")),
      )
      assertEquals(
          DataOutcome.Success(TransferenciaUpdateResult.NotFound),
          repository.update("gone", original.copy(id = "gone")),
      )
      assertEquals(DataOutcome.Success(original), repository.getById(original.id))
      assertEquals(2, changeClock.issuedStamps.size)
    }
  }

  @Test
  fun `delete tombstones once and restore revives the row with a newer stamp`() = runTest {
    withRepository { driver, changeClock, repository ->
      val row = transferencia("t", COP_CASH, NEQUI, descripcion = "ahorro")
      repository.insert(row)

      assertEquals(DataOutcome.Success(row), repository.deleteAndCapture(row.id))
      val deleteStamp = changeClock.issuedStamps.last()
      assertEquals(
          deleteStamp.value to deleteStamp.value,
          driver.storedStamps("transferencia", row.id),
      )
      assertEquals(DataOutcome.Success(null), repository.deleteAndCapture(row.id))
      assertEquals(DataOutcome.Success(emptyList()), repository.getAll())

      assertEquals(DataOutcome.Success(RestoreResult.Restored), repository.restoreDeleted(row))
      val reviveStamp = changeClock.issuedStamps.last()
      assertTrue(reviveStamp > deleteStamp)
      assertEquals(reviveStamp.value to null, driver.storedStamps("transferencia", row.id))
      assertEquals(DataOutcome.Success(listOf(row)), repository.getAll())
      assertEquals(
          DataOutcome.Success(RestoreResult.AlreadyPresent),
          repository.restoreDeleted(row),
      )
      assertEquals(
          DataOutcome.Success(RestoreResult.Conflict),
          repository.restoreDeleted(row.copy(descripcion = "otra")),
      )
      assertEquals(3, changeClock.issuedStamps.size)
    }
  }

  @Test
  fun `restore of a tombstone with different content is a conflict that keeps it`() = runTest {
    withRepository { driver, changeClock, repository ->
      val row = transferencia("t", COP_CASH, NEQUI)
      repository.insert(row)
      repository.deleteAndCapture(row.id)
      val tombstone = driver.storedStamps("transferencia", row.id)

      for (different in
          listOf(
              row.copy(cuentaOrigenId = NU),
              row.copy(cuentaDestinoId = NU),
              row.copy(montoOrigen = Money(7, Moneda.COP), montoDestino = Money(7, Moneda.COP)),
              row.copy(fecha = LocalDate(2026, 10, 2)),
              row.copy(descripcion = "otra"),
              row.copy(createdAt = CREATED + 1.seconds),
          )) {
        assertEquals(
            DataOutcome.Success(RestoreResult.Conflict),
            repository.restoreDeleted(different),
        )
      }

      assertEquals(tombstone, driver.storedStamps("transferencia", row.id))
      assertEquals(2, changeClock.issuedStamps.size)
    }
  }

  @Test
  fun `restore follows the movimiento cuenta outcomes`() = runTest {
    withRepository { driver, _, repository ->
      val archived = transferencia("archived", COP_CASH, NEQUI)
      val missing = transferencia("missing", NEQUI, COP_CASH)
      val recurrency = transferencia("recurrency", COP_CASH, NU)
      for (row in listOf(archived, missing, recurrency)) {
        repository.insert(row)
        repository.deleteAndCapture(row.id)
      }
      driver.execute(null, "UPDATE cuenta SET archivada = 1 WHERE id = '$NEQUI'", 0)

      assertEquals(DataOutcome.Success(RestoreResult.Restored), repository.restoreDeleted(archived))
      assertEquals(DataOutcome.Success(archived), repository.getById(archived.id))

      repository.deleteAndCapture(archived.id)
      driver.tombstone("cuenta", NEQUI)
      val tombstone = driver.storedStamps("transferencia", missing.id)
      assertEquals(
          DataOutcome.Success(RestoreResult.MissingCuenta),
          repository.restoreDeleted(missing),
      )
      assertEquals(tombstone, driver.storedStamps("transferencia", missing.id))

      driver.execute(null, "UPDATE cuenta SET moneda = 'USD' WHERE id = '$NU'", 0)
      assertEquals(
          DataOutcome.Success(RestoreResult.Conflict),
          repository.restoreDeleted(recurrency),
      )

      val neverStored =
          transferencia("never", COP_CASH, USD_CASH, at = 3)
              .copy(montoDestino = Money(25, Moneda.USD))
      assertEquals(
          DataOutcome.Success(RestoreResult.Restored),
          repository.restoreDeleted(neverStored),
      )
      assertEquals(DataOutcome.Success(neverStored), repository.getById(neverStored.id))
      assertInvalid(repository.restoreDeleted(neverStored.copy(id = "")))
    }
  }

  @Test
  fun `defaultOrigin prefers the newest transfer origin then the primary cash cuenta`() = runTest {
    withRepository { driver, _, repository ->
      assertEquals(DataOutcome.Success(COP_CASH), repository.defaultOrigin(Moneda.COP))

      repository.insert(
          transferencia("older", USD_CASH, COP_CASH)
              .copy(montoDestino = Money(100, Moneda.COP), montoOrigen = Money(1, Moneda.USD))
      )
      repository.insert(transferencia("newest", NEQUI, NU, at = 1))
      assertEquals(DataOutcome.Success(NEQUI), repository.defaultOrigin(Moneda.COP))

      driver.execute(null, "UPDATE cuenta SET archivada = 1 WHERE id = '$NEQUI'", 0)
      assertEquals(DataOutcome.Success(COP_CASH), repository.defaultOrigin(Moneda.COP))
      driver.execute(null, "UPDATE cuenta SET archivada = 0 WHERE id = '$NEQUI'", 0)

      repository.insert(transferencia("card", NU, NEQUI, at = 2))
      assertEquals(DataOutcome.Success(COP_CASH), repository.defaultOrigin(Moneda.COP))

      driver.tombstone("transferencia", "card")
      assertEquals(DataOutcome.Success(NEQUI), repository.defaultOrigin(Moneda.COP))
    }
  }

  @Test
  fun `defaultOrigin without a primary cash cuenta falls back to other non-credit cuentas`() =
      runTest {
        withRepository { driver, _, repository ->
          driver.execute(null, "UPDATE cuenta SET archivada = 1 WHERE id = '$COP_CASH'", 0)
          assertEquals(DataOutcome.Success(NEQUI), repository.defaultOrigin(Moneda.COP))
          assertEquals(DataOutcome.Success(USD_CASH), repository.defaultOrigin(Moneda.USD))
          assertEquals(DataOutcome.Success(NEQUI), repository.defaultOrigin(Moneda.MXN))

          driver.tombstone("cuenta", NEQUI)
          driver.tombstone("cuenta", GONE)
          driver.execute(null, "UPDATE cuenta SET archivada = 1 WHERE id = '$USD_CASH'", 0)
          assertEquals(DataOutcome.Success(null), repository.defaultOrigin(Moneda.COP))
        }
      }

  /** Cash in COP, Nequi, the NU card, cash in USD and a cuenta to tombstone, in that order. */
  private suspend fun TestScope.withRepository(
      block: suspend (SqlDriver, TestChangeClock, TransferenciaRepository) -> Unit
  ) {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    try {
      val database = TallybookDatabase(driver)
      listOf(
              cuenta(COP_CASH, TipoCuenta.EFECTIVO, Moneda.COP, 0),
              cuenta(NEQUI, TipoCuenta.BANCO, Moneda.COP, 1),
              cuenta(NU, TipoCuenta.CREDITO, Moneda.COP, 2),
              cuenta(USD_CASH, TipoCuenta.EFECTIVO, Moneda.USD, 3),
              cuenta(GONE, TipoCuenta.BANCO, Moneda.COP, 4),
          )
          .forEach { database.insertCuentaRow(it, ChangeStamp.SEED) }
      val changeClock = TestChangeClock()
      val repository =
          TransferenciaRepositoryImpl(
              database = database,
              dispatcher = StandardTestDispatcher(testScheduler),
              clock = FixedClock,
              timeZoneProvider = { TimeZone.UTC },
              changeClock = changeClock,
          )
      block(driver, changeClock, repository)
    } finally {
      driver.close()
    }
  }

  private fun cuenta(id: String, tipo: TipoCuenta, moneda: Moneda, order: Int) =
      Cuenta(
          id = id,
          nombre = id,
          tipo = tipo,
          saldoInicial = Money(0, moneda),
          diaCorte = null,
          archivada = false,
          createdAt = CREATED + order.seconds,
          color = tipo.defaultColor,
      )

  private fun transferencia(
      id: String,
      origen: String,
      destino: String,
      at: Int = 0,
      descripcion: String? = null,
  ) =
      Transferencia(
          id = id,
          cuentaOrigenId = origen,
          cuentaDestinoId = destino,
          montoOrigen = Money(100, Moneda.COP),
          montoDestino = Money(100, Moneda.COP),
          fecha = LocalDate(2026, 10, 1),
          descripcion = descripcion,
          createdAt = CREATED + at.seconds,
      )

  private fun assertInvalid(outcome: DataOutcome<*>) {
    assertIs<DataError.InvalidInput>(assertIs<DataOutcome.Failure>(outcome).error)
  }

  private object FixedClock : Clock {
    override fun now(): Instant = Instant.parse("2026-10-16T12:00:00Z")
  }

  private companion object {
    val COP_CASH: String = Cuenta.defaultCashId(Moneda.COP)
    val USD_CASH: String = Cuenta.defaultCashId(Moneda.USD)
    const val NEQUI: String = "nequi"
    const val NU: String = "nu"
    const val GONE: String = "gone"
    val TODAY: LocalDate = LocalDate(2026, 10, 16)
    val CREATED: Instant = Instant.parse("2026-10-01T12:00:00Z")
  }
}
