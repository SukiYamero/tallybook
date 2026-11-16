package com.kurobello.tallybook.core.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.OccurrenceResult
import com.kurobello.tallybook.core.model.OccurrenceUndo
import com.kurobello.tallybook.core.model.OccurrenceUndoResult
import com.kurobello.tallybook.core.model.Recurrencia
import com.kurobello.tallybook.core.model.RecurrenciaWithPending
import com.kurobello.tallybook.core.model.RecurrenciaWrite
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.TipoMovimiento
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
import kotlinx.datetime.LocalDate

class RecurrenciaRepositoryTest {

  @Test
  fun `create stores the movimiento and its recurrencia under one stamp`() = runTest {
    withRepository { fixture ->
      val outcome = fixture.repository.create(rent(), Frecuencia.MENSUAL, today = JAN_31)

      val recurrencia = assertIs<DataOutcome.Success<Recurrencia>>(outcome).data
      assertEquals(
          Recurrencia(
              id = "id-1",
              tipo = TipoMovimiento.GASTO,
              monto = Money(120_000, Moneda.COP),
              categoriaId = "cat_comida",
              titulo = "Alquiler",
              descripcion = "depto",
              cuentaId = COP_CASH,
              medio = MedioPago.EFECTIVO,
              etiquetaIds = listOf("tag_b", "tag_a"),
              frecuencia = Frecuencia.MENSUAL,
              fechaAncla = JAN_31,
              proximaFecha = FEB_28,
              createdAt = CREATED,
          ),
          recurrencia,
      )
      val stamp = fixture.changeClock.issuedStamps.single().value
      assertEquals(stamp to null, fixture.driver.storedStamps("recurrencia", "id-1"))
      assertEquals(stamp to null, fixture.driver.storedStamps("movimiento", "rent"))
      assertEquals("id-1", fixture.driver.recurrenciaIdOf("rent"))
      assertEquals(
          DataOutcome.Success(listOf(RecurrenciaWithPending(recurrencia, emptyList()))),
          fixture.repository.observeLive(today = JAN_31).first(),
      )
    }
  }

  @Test
  fun `a back-dated movimiento starts the schedule after today with nothing pending`() = runTest {
    withRepository { fixture ->
      val today = LocalDate(2026, 11, 15)

      val recurrencia = assertSuccess(fixture.repository.create(rent(), Frecuencia.MENSUAL, today))

      assertEquals(JAN_31, recurrencia.fechaAncla)
      assertEquals(LocalDate(2026, 11, 30), recurrencia.proximaFecha)
      assertEquals(DataOutcome.Success(0), fixture.repository.observePendingCount(today).first())
      assertEquals(listOf<String?>("rent"), fixture.driver.movimientoIds())
    }
  }

  @Test
  fun `a pending occurrence moves no money until it is confirmed`() = runTest {
    withRepository { fixture ->
      val saldos = SaldoRepositoryImpl(fixture.database, StandardTestDispatcher(testScheduler))
      val february = MovimientoDateRange(LocalDate(2026, 2, 1), FEB_28)
      fixture.created(today = JAN_31)
      val totalBefore = assertSuccess(saldos.observeTotales().first()).getValue(Moneda.COP)

      assertEquals(DataOutcome.Success(1), fixture.repository.observePendingCount(FEB_28).first())
      assertEquals(totalBefore, assertSuccess(saldos.observeTotales().first()).getValue(Moneda.COP))
      assertEquals(
          Money(0, Moneda.COP),
          assertSuccess(fixture.movimientos.observeSummary(Moneda.COP, february).first()).gastos,
      )
      assertEquals(listOf<String?>("rent"), fixture.driver.movimientoIds())

      fixture.confirmed(Money(130_000, Moneda.COP), today = FEB_28)

      assertEquals(
          totalBefore - Money(130_000, Moneda.COP),
          assertSuccess(saldos.observeTotales().first()).getValue(Moneda.COP),
      )
      assertEquals(
          Money(130_000, Moneda.COP),
          assertSuccess(fixture.movimientos.observeSummary(Moneda.COP, february).first()).gastos,
      )
    }
  }

  @Test
  fun `occurrences pending through today follow the anchor across month ends`() = runTest {
    withRepository { fixture ->
      val recurrencia = fixture.created(today = JAN_31)

      assertEquals(
          DataOutcome.Success(
              listOf(
                  RecurrenciaWithPending(
                      recurrencia,
                      listOf(FEB_28, LocalDate(2026, 3, 31), LocalDate(2026, 4, 30)),
                  )
              )
          ),
          fixture.repository.observeLive(today = LocalDate(2026, 4, 30)).first(),
      )
      assertEquals(
          DataOutcome.Success(2),
          fixture.repository.observePendingCount(today = LocalDate(2026, 4, 29)).first(),
      )
      assertEquals(
          DataOutcome.Success(0),
          fixture.repository.observePendingCount(today = LocalDate(2026, 2, 27)).first(),
      )
    }
  }

  @Test
  fun `confirm with a different amount inserts the movimiento and advances under one stamp`() =
      runTest {
        withRepository { fixture ->
          fixture.created(today = JAN_31)
          val paidOn = LocalDate(2026, 3, 2)

          val outcome =
              fixture.repository.confirm(
                  "id-1",
                  today = LocalDate(2026, 3, 5),
                  monto = Money(130_000, Moneda.COP),
                  fecha = paidOn,
              )

          val undo = assertIs<OccurrenceResult.Done>(assertSuccess(outcome)).undo
          assertEquals(FEB_28, undo.occurrence)
          assertEquals("id-2", undo.movimientoId)
          assertEquals(
              Movimiento(
                  id = "id-2",
                  tipo = TipoMovimiento.GASTO,
                  monto = Money(130_000, Moneda.COP),
                  fecha = paidOn,
                  categoriaId = "cat_comida",
                  titulo = "Alquiler",
                  descripcion = "depto",
                  createdAt = NOW,
                  cuentaId = COP_CASH,
                  medio = MedioPago.EFECTIVO,
                  etiquetaIds = listOf("tag_b", "tag_a"),
              ),
              fixture.movimientos.getById("id-2").successData(),
          )
          assertEquals("id-1", fixture.driver.recurrenciaIdOf("id-2"))
          val stored = fixture.stored("id-1")
          assertEquals(LocalDate(2026, 3, 31), stored.proximaFecha)
          assertEquals(Money(130_000, Moneda.COP), stored.monto)
          assertEquals(JAN_31, stored.fechaAncla)
          val stamp = fixture.changeClock.issuedStamps.last().value
          assertEquals(2, fixture.changeClock.issuedStamps.size)
          assertEquals(stamp to null, fixture.driver.storedStamps("recurrencia", "id-1"))
          assertEquals(stamp to null, fixture.driver.storedStamps("movimiento", "id-2"))
        }
      }

  @Test
  fun `skip advances the next date and keeps the amount without a movimiento`() = runTest {
    withRepository { fixture ->
      fixture.created(today = JAN_31)

      val undo =
          assertIs<OccurrenceResult.Done>(
                  assertSuccess(fixture.repository.skip("id-1", today = FEB_28))
              )
              .undo

      assertNull(undo.movimientoId)
      val stored = fixture.stored("id-1")
      assertEquals(LocalDate(2026, 3, 31), stored.proximaFecha)
      assertEquals(Money(120_000, Moneda.COP), stored.monto)
      assertEquals(listOf("rent"), fixture.driver.movimientoIds())
      assertEquals(
          fixture.changeClock.issuedStamps.last().value to null,
          fixture.driver.storedStamps("recurrencia", "id-1"),
      )
    }
  }

  @Test
  fun `nothing is confirmed or skipped before the next date is due`() = runTest {
    withRepository { fixture ->
      fixture.created(today = JAN_31)
      val stamps = fixture.changeClock.issuedStamps.size

      assertEquals(
          DataOutcome.Success(OccurrenceResult.NothingPending),
          fixture.repository.confirm(
              "id-1",
              today = LocalDate(2026, 2, 27),
              monto = Money(120_000, Moneda.COP),
              fecha = LocalDate(2026, 2, 27),
          ),
      )
      assertEquals(
          DataOutcome.Success(OccurrenceResult.NothingPending),
          fixture.repository.skip("id-1", today = LocalDate(2026, 2, 27)),
      )
      assertEquals(stamps, fixture.changeClock.issuedStamps.size)
      assertInvalid(fixture.repository.skip("missing", today = FEB_28))
    }
  }

  @Test
  fun `undo of a confirm restores the date and amount and tombstones the movimiento`() = runTest {
    withRepository { fixture ->
      val created = fixture.created(today = JAN_31)
      val undo = fixture.confirmed(Money(130_000, Moneda.COP))

      assertEquals(
          DataOutcome.Success(OccurrenceUndoResult.Undone),
          fixture.repository.undo(undo),
      )

      assertEquals(created, fixture.stored("id-1"))
      val stamp = fixture.changeClock.issuedStamps.last().value
      assertEquals(3, fixture.changeClock.issuedStamps.size)
      assertEquals(stamp to null, fixture.driver.storedStamps("recurrencia", "id-1"))
      assertEquals(stamp to stamp, fixture.driver.storedStamps("movimiento", "id-2"))
      assertEquals(DataOutcome.Success(null), fixture.movimientos.getById("id-2"))
      assertEquals(
          DataOutcome.Success(OccurrenceUndoResult.Conflict),
          fixture.repository.undo(undo),
      )
    }
  }

  @Test
  fun `undo of a skip restores the next date`() = runTest {
    withRepository { fixture ->
      val created = fixture.created(today = JAN_31)
      val undo =
          assertIs<OccurrenceResult.Done>(
                  assertSuccess(fixture.repository.skip("id-1", today = FEB_28))
              )
              .undo

      assertEquals(
          DataOutcome.Success(OccurrenceUndoResult.Undone),
          fixture.repository.undo(undo),
      )

      assertEquals(created, fixture.stored("id-1"))
    }
  }

  @Test
  fun `undo reports a conflict without writing once either row changed`() = runTest {
    withRepository { fixture ->
      fixture.created(today = JAN_31)
      val editedRecurrencia = fixture.confirmed(Money(130_000, Moneda.COP))
      val current = fixture.stored("id-1")
      fixture.repository.update(
          "id-1",
          current.draft.copy(titulo = "Renta"),
          today = FEB_28,
      )
      val afterEdit = fixture.driver.storedStamps("recurrencia", "id-1")

      assertEquals(
          DataOutcome.Success(OccurrenceUndoResult.Conflict),
          fixture.repository.undo(editedRecurrencia),
      )
      assertEquals(afterEdit, fixture.driver.storedStamps("recurrencia", "id-1"))
      assertEquals("id-2", fixture.movimientos.getById("id-2").successData()?.id)

      val editedMovimiento = fixture.confirmed(Money(130_000, Moneda.COP), today = MAR_31)
      val movimiento = checkNotNull(fixture.movimientos.getById("id-3").successData())
      fixture.movimientos.update("id-3", movimiento.changes().copy(titulo = "Marzo"))

      assertEquals(
          DataOutcome.Success(OccurrenceUndoResult.Conflict),
          fixture.repository.undo(editedMovimiento),
      )
      assertEquals(LocalDate(2026, 4, 30), fixture.stored("id-1").proximaFecha)
    }
  }

  @Test
  fun `the schedule only changes with nothing pending and then re-anchors`() = runTest {
    withRepository { fixture ->
      val created = fixture.created(today = JAN_31)
      val moved = created.draft.copy(frecuencia = Frecuencia.SEMANAL, proximaFecha = MAR_31)

      assertEquals(
          DataOutcome.Success(RecurrenciaWrite.ScheduleLocked),
          fixture.repository.update("id-1", moved, today = FEB_28),
      )
      assertEquals(created, fixture.stored("id-1"))

      val renamed =
          fixture.repository.update("id-1", created.draft.copy(titulo = "Renta"), today = FEB_28)
      assertEquals(
          DataOutcome.Success(RecurrenciaWrite.Saved(created.copy(titulo = "Renta"))),
          renamed,
      )

      val saved = fixture.repository.update("id-1", moved, today = LocalDate(2026, 2, 27))

      val expected =
          created.copy(frecuencia = Frecuencia.SEMANAL, fechaAncla = MAR_31, proximaFecha = MAR_31)
      assertEquals(DataOutcome.Success(RecurrenciaWrite.Saved(expected)), saved)
      assertEquals(expected, fixture.stored("id-1"))
    }
  }

  @Test
  fun `an unchanged draft writes nothing`() = runTest {
    withRepository { fixture ->
      val created = fixture.created(today = JAN_31)
      val stamps = fixture.changeClock.issuedStamps.size

      assertEquals(
          DataOutcome.Success(RecurrenciaWrite.Saved(created)),
          fixture.repository.update("id-1", created.draft, today = FEB_28),
      )
      assertEquals(stamps, fixture.changeClock.issuedStamps.size)
    }
  }

  @Test
  fun `delete tombstones the recurrencia and keeps its movimientos`() = runTest {
    withRepository { fixture ->
      fixture.created(today = JAN_31)
      fixture.confirmed(Money(130_000, Moneda.COP))

      assertEquals(DataOutcome.Success(Unit), fixture.repository.delete("id-1"))

      val stamp = fixture.changeClock.issuedStamps.last().value
      assertEquals(stamp to stamp, fixture.driver.storedStamps("recurrencia", "id-1"))
      assertEquals(emptyList(), fixture.driver.recurrenciaLinks())
      assertEquals(listOf("id-2", "rent"), fixture.driver.movimientoIds())
      assertEquals("id-1", fixture.driver.recurrenciaIdOf("id-2"))
      assertEquals(DataOutcome.Success(emptyList()), fixture.repository.observeLive(MAR_31).first())
      assertEquals(DataOutcome.Success(0), fixture.repository.observePendingCount(MAR_31).first())
      assertInvalid(fixture.repository.skip("id-1", today = MAR_31))
      assertInvalid(fixture.repository.delete("id-1"))
    }
  }

  @Test
  fun `an archived cuenta or deleted categoria fails confirm like a movimiento write`() = runTest {
    withRepository(extraCategoriaIds = listOf("custom")) { fixture ->
      fixture.repository.create(
          rent().copy(cuentaId = "nequi", medio = MedioPago.DEBITO),
          Frecuencia.MENSUAL,
          today = JAN_31,
      )
      fixture.repository.create(
          rent(id = "gym").copy(categoriaId = "custom"),
          Frecuencia.MENSUAL,
          today = JAN_31,
      )
      fixture.database.cuentaQueries.updateCuentaArchivada(1L, ChangeStamp.SEED.value, "nequi")
      fixture.driver.tombstone("categoria", "custom")
      val stamps = fixture.changeClock.issuedStamps.size

      for (id in listOf("id-1", "id-2")) {
        assertInvalid(fixture.repository.confirm(id, FEB_28, Money(120_000, Moneda.COP), FEB_28))
      }

      assertEquals(stamps, fixture.changeClock.issuedStamps.size)
      assertEquals(listOf("gym", "rent"), fixture.driver.movimientoIds())
      assertEquals(FEB_28, fixture.stored("id-1").proximaFecha)
    }
  }

  @Test
  fun `create rejects what a movimiento insert rejects`() = runTest {
    withRepository { fixture ->
      assertInvalid(
          fixture.repository.create(
              rent().copy(medio = MedioPago.DEBITO),
              Frecuencia.MENSUAL,
              JAN_31,
          )
      )
      assertInvalid(
          fixture.repository.create(rent().copy(categoriaId = "gone"), Frecuencia.MENSUAL, JAN_31)
      )
      assertEquals(emptyList(), fixture.driver.movimientoIds())
      assertEquals(0, fixture.changeClock.issuedStamps.size)
    }
  }

  @Test
  fun `deleting an etiqueta drops it from the recurrencia under the delete stamp`() = runTest {
    withRepository { fixture ->
      fixture.created(today = JAN_31)
      val etiquetas =
          EtiquetaRepositoryImpl(
              fixture.database,
              StandardTestDispatcher(testScheduler),
              { "unused" },
              FixedClock,
              fixture.changeClock,
          )

      etiquetas.delete("tag_a")

      assertEquals(listOf("tag_b"), fixture.stored("id-1").etiquetaIds)
      assertEquals(
          fixture.changeClock.issuedStamps.last().value to null,
          fixture.driver.storedStamps("recurrencia", "id-1"),
      )
    }
  }

  private class Fixture(
      val driver: SqlDriver,
      val database: TallybookDatabase,
      val changeClock: TestChangeClock,
      val repository: RecurrenciaRepository,
      val movimientos: MovimientoRepository,
  ) {
    suspend fun created(today: LocalDate): Recurrencia =
        assertSuccess(repository.create(rent(), Frecuencia.MENSUAL, today))

    suspend fun confirmed(monto: Money, today: LocalDate = FEB_28): OccurrenceUndo =
        assertIs<OccurrenceResult.Done>(
                assertSuccess(repository.confirm("id-1", today, monto, fecha = today))
            )
            .undo

    suspend fun stored(id: String): Recurrencia =
        assertSuccess(repository.observeLive(JAN_31).first())
            .single { it.recurrencia.id == id }
            .recurrencia
  }

  private suspend fun TestScope.withRepository(
      extraCategoriaIds: List<String> = emptyList(),
      block: suspend (Fixture) -> Unit,
  ) {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val database = createSchemaWithReferences(driver, extraCategoriaIds, listOf("tag_a", "tag_b"))
      database.insertCuentaRow(
          cashCuenta(Moneda.COP).copy(id = "nequi", nombre = "Nequi", tipo = TipoCuenta.BANCO),
          ChangeStamp.SEED,
      )
      val changeClock = TestChangeClock()
      val dispatcher = StandardTestDispatcher(testScheduler)
      var nextId = 0
      block(
          Fixture(
              driver = driver,
              database = database,
              changeClock = changeClock,
              repository =
                  RecurrenciaRepositoryImpl(
                      database = database,
                      dispatcher = dispatcher,
                      idGenerator = IdGenerator { "id-${++nextId}" },
                      clock = FixedClock,
                      changeClock = changeClock,
                  ),
              movimientos = MovimientoRepositoryImpl(database, dispatcher, changeClock),
          )
      )
    }
  }

  private object FixedClock : Clock {
    override fun now(): Instant = NOW
  }
}

private fun rent(id: String = "rent") =
    Movimiento(
        id = id,
        tipo = TipoMovimiento.GASTO,
        monto = Money(120_000, Moneda.COP),
        fecha = JAN_31,
        categoriaId = "cat_comida",
        titulo = "Alquiler",
        descripcion = "depto",
        createdAt = CREATED,
        cuentaId = COP_CASH,
        medio = MedioPago.EFECTIVO,
        etiquetaIds = listOf("tag_b", "tag_a"),
    )

private fun <T> assertSuccess(outcome: DataOutcome<T>): T =
    assertIs<DataOutcome.Success<T>>(outcome).data

private fun <T> DataOutcome<T>.successData(): T = assertSuccess(this)

private fun assertInvalid(outcome: DataOutcome<*>) {
  assertIs<DataError.InvalidInput>(assertIs<DataOutcome.Failure>(outcome).error)
}

private fun SqlDriver.recurrenciaIdOf(movimientoId: String): String? =
    strings("SELECT recurrencia_id FROM movimiento WHERE id = '$movimientoId'").single()

private fun SqlDriver.movimientoIds(): List<String?> =
    strings("SELECT id FROM movimiento WHERE deleted_at IS NULL ORDER BY id")

private fun SqlDriver.recurrenciaLinks(): List<String?> =
    strings("SELECT recurrencia_id || ':' || etiqueta_id FROM recurrencia_etiqueta")

private fun SqlDriver.strings(sql: String): List<String?> =
    executeQuery(
            null,
            sql,
            { cursor ->
              val rows = mutableListOf<String?>()
              while (cursor.next().value) rows += cursor.getString(0)
              QueryResult.Value(rows)
            },
            0,
        )
        .value

private val COP_CASH = Cuenta.defaultCashId(Moneda.COP)
private val JAN_31 = LocalDate(2026, 1, 31)
private val FEB_28 = LocalDate(2026, 2, 28)
private val MAR_31 = LocalDate(2026, 3, 31)
private val CREATED = Instant.parse("2026-01-31T15:00:00Z")
private val NOW = Instant.parse("2026-02-28T18:00:00Z")
