package com.kurobello.tallybook.core.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.BudgetMonth
import com.kurobello.tallybook.core.model.BudgetState
import com.kurobello.tallybook.core.model.BudgetStatus
import com.kurobello.tallybook.core.model.BudgetTouch
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.CategoriaDelete
import com.kurobello.tallybook.core.model.CategoriaDraft
import com.kurobello.tallybook.core.model.CategoriaWrite
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.Presupuesto
import com.kurobello.tallybook.core.model.PresupuestoWrite
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

class PresupuestoRepositoryTest {

  @Test
  fun createStampsOnce() = runTest {
    withRepository { fixture ->
      val saved = fixture.created("cat_viajes", cop(1_000))

      assertEquals(
          Presupuesto(id = "id-1", categoriaId = "cat_viajes", monto = cop(1_000), createdAt = NOW),
          saved,
      )
      assertEquals(
          fixture.changeClock.issuedStamps.single().value to null,
          fixture.driver.storedStamps("presupuesto", "id-1"),
      )
    }
  }

  @Test
  fun createDuplicateReturnsExisting() = runTest {
    withRepository { fixture ->
      val first = fixture.created("cat_viajes", cop(1_000))

      val outcome = fixture.repository.create("cat_viajes", cop(5_000), TODAY)

      assertEquals(
          PresupuestoWrite.Duplicate(first),
          assertIs<DataOutcome.Success<PresupuestoWrite>>(outcome).data,
      )
      assertEquals(1, fixture.changeClock.issuedStamps.size)
    }
  }

  @Test
  fun createSameCategoryOtherCurrencySucceeds() = runTest {
    withRepository { fixture ->
      fixture.created("cat_viajes", cop(1_000))

      val usd = fixture.created("cat_viajes", Money(1_000, Moneda.USD))

      assertEquals(Moneda.USD, usd.monto.moneda)
      assertEquals(2, fixture.statuses().size)
    }
  }

  @Test
  fun createRejectsOtros() = runTest {
    withRepository { fixture ->
      val outcome = fixture.repository.create(CATEGORIA_OTROS_ID, cop(1_000), TODAY)

      assertEquals(
          PresupuestoWrite.UnknownCategoria,
          assertIs<DataOutcome.Success<PresupuestoWrite>>(outcome).data,
      )
      assertEquals(emptyList(), fixture.changeClock.issuedStamps)
    }
  }

  @Test
  fun createRejectsMissingAndDeletedCategoria() = runTest {
    withRepository { fixture ->
      val custom = fixture.customCategoria()
      fixture.categorias.delete(custom)

      listOf("cat_never_seeded", custom).forEach { id ->
        val outcome = fixture.repository.create(id, cop(1_000), TODAY)
        assertEquals(
            PresupuestoWrite.UnknownCategoria,
            assertIs<DataOutcome.Success<PresupuestoWrite>>(outcome).data,
        )
      }
    }
  }

  @Test
  fun createRejectsNonPositiveMonto() = runTest {
    withRepository { fixture ->
      val outcome = fixture.repository.create("cat_viajes", cop(0), TODAY)

      assertIs<DataError.InvalidInput>(assertIs<DataOutcome.Failure>(outcome).error)
    }
  }

  @Test
  fun createAndUpdateMontoRejectAboveTheCap() = runTest {
    withRepository { fixture ->
      val overCap = Money(Money.MAX_MINOR_UNITS + 1, Moneda.COP)

      assertIs<DataError.InvalidInput>(
          assertIs<DataOutcome.Failure>(fixture.repository.create("cat_viajes", overCap, TODAY))
              .error
      )

      fixture.created("cat_viajes", cop(1_000))
      assertIs<DataError.InvalidInput>(
          assertIs<DataOutcome.Failure>(fixture.repository.updateMonto("id-1", overCap)).error
      )
      assertEquals(
          Money.MAX_MINOR_UNITS,
          assertIs<PresupuestoWrite.Saved>(
                  assertIs<DataOutcome.Success<PresupuestoWrite>>(
                          fixture.repository.updateMonto(
                              "id-1",
                              Money(Money.MAX_MINOR_UNITS, Moneda.COP),
                          )
                      )
                      .data
              )
              .presupuesto
              .monto
              .minorUnits,
      )
    }
  }

  @Test
  fun spentIncludesChildrenOnlyForParentBudget() = runTest {
    withRepository { fixture ->
      fixture.created("cat_viajes", cop(100_000))
      fixture.created("cat_vuelos", cop(100_000))
      fixture.gasto("a", "cat_viajes", 100)
      fixture.gasto("b", "cat_vuelos", 200)
      fixture.gasto("c", "cat_finanzas", 400)

      val spent = fixture.statuses().associate { it.presupuesto.categoriaId to it.spent.minorUnits }

      assertEquals(mapOf("cat_viajes" to 300L, "cat_vuelos" to 200L), spent)
    }
  }

  @Test
  fun spentIgnoresOtherCurrencies() = runTest {
    withRepository { fixture ->
      fixture.created("cat_viajes", cop(100_000))
      fixture.gasto("cop", "cat_viajes", 100)
      fixture.gasto("usd", "cat_viajes", 900, cuentaId = USD_CASH)

      assertEquals(100L, fixture.statuses().single().spent.minorUnits)
    }
  }

  @Test
  fun spentIgnoresIngresosTombstonesAndPreviousMonth() = runTest {
    withRepository { fixture ->
      fixture.created("cat_viajes", cop(100_000))
      fixture.gasto("counted", "cat_viajes", 100)
      fixture.gasto("ingreso", "cat_viajes", 200, tipo = TipoMovimiento.INGRESO)
      fixture.gasto("deleted", "cat_viajes", 400)
      fixture.driver.tombstone("movimiento", "deleted")
      fixture.gasto("october", "cat_viajes", 800, fecha = LocalDate(2026, 10, 31))
      fixture.gasto("future", "cat_viajes", 1_600, fecha = LocalDate(2026, 11, 19))

      assertEquals(100L, fixture.statuses().single().spent.minorUnits)
    }
  }

  @Test
  fun hiddenCategoryKeepsCounting() = runTest {
    withRepository { fixture ->
      fixture.created("cat_viajes", cop(100_000))
      fixture.gasto("a", "cat_viajes", 100)
      assertIs<DataOutcome.Success<Unit>>(fixture.categorias.setOculta("cat_viajes", true))

      assertEquals(100L, fixture.statuses().single().spent.minorUnits)
    }
  }

  @Test
  fun statusesAreSortedByBudgetOrder() = runTest {
    withRepository { fixture ->
      fixture.created("cat_viajes", cop(1_000))
      fixture.created("cat_finanzas", cop(1_000))
      fixture.gasto("a", "cat_finanzas", 2_000)

      assertEquals(
          listOf("cat_finanzas" to BudgetState.OVER, "cat_viajes" to BudgetState.FINE),
          fixture.statuses().map { it.presupuesto.categoriaId to it.state },
      )
    }
  }

  @Test
  fun updateMontoBumpsStamp() = runTest {
    withRepository { fixture ->
      fixture.created("cat_viajes", cop(1_000))

      val outcome = fixture.repository.updateMonto("id-1", cop(2_500))

      assertEquals(
          PresupuestoWrite.Saved(
              Presupuesto("id-1", "cat_viajes", cop(2_500), createdAt = NOW),
          ),
          assertIs<DataOutcome.Success<PresupuestoWrite>>(outcome).data,
      )
      assertEquals(
          fixture.changeClock.issuedStamps.last().value to null,
          fixture.driver.storedStamps("presupuesto", "id-1"),
      )
      assertEquals(2, fixture.changeClock.issuedStamps.size)
      assertEquals(2_500L, fixture.statuses().single().presupuesto.monto.minorUnits)
    }
  }

  @Test
  fun updateMontoRejectsUnknownIdAndOtherCurrency() = runTest {
    withRepository { fixture ->
      fixture.created("cat_viajes", cop(1_000))

      listOf("missing" to cop(1_000), "id-1" to Money(1_000, Moneda.USD)).forEach { (id, monto) ->
        val outcome = fixture.repository.updateMonto(id, monto)
        assertIs<DataError.InvalidInput>(assertIs<DataOutcome.Failure>(outcome).error)
      }
    }
  }

  @Test
  fun deleteTombstones() = runTest {
    withRepository { fixture ->
      fixture.created("cat_viajes", cop(1_000))

      assertEquals(DataOutcome.Success(Unit), fixture.repository.delete("id-1"))

      val stamp = fixture.changeClock.issuedStamps.last().value
      assertEquals(stamp to stamp, fixture.driver.storedStamps("presupuesto", "id-1"))
      assertEquals(emptyList(), fixture.statuses())
      assertIs<DataOutcome.Failure>(fixture.repository.delete("id-1"))
    }
  }

  @Test
  fun deletingCustomCategoryTombstonesItsBudgets() = runTest {
    withRepository { fixture ->
      val custom = fixture.customCategoria()
      fixture.created(custom, cop(1_000))
      fixture.created(custom, Money(1_000, Moneda.USD))
      fixture.created("cat_viajes", cop(1_000))

      assertEquals(
          DataOutcome.Success(CategoriaDelete.Deleted),
          fixture.categorias.delete(custom),
      )

      val stamp = fixture.changeClock.issuedStamps.last().value
      assertEquals(stamp to stamp, fixture.driver.storedStamps("categoria", custom))
      assertEquals(stamp to stamp, fixture.driver.storedStamps("presupuesto", "id-2"))
      assertEquals(stamp to stamp, fixture.driver.storedStamps("presupuesto", "id-3"))
      assertNull(fixture.driver.storedStamps("presupuesto", "id-4")?.second)
      assertEquals(listOf("cat_viajes"), fixture.statuses().map { it.presupuesto.categoriaId })
    }
  }

  @Test
  fun historyReturnsSixZeroFilledMonths() = runTest {
    withRepository { fixture ->
      fixture.created("cat_viajes", cop(100_000))
      fixture.gasto("jun", "cat_vuelos", 700, fecha = LocalDate(2026, 6, 1))
      fixture.gasto("may", "cat_viajes", 999, fecha = LocalDate(2026, 5, 31))
      fixture.gasto("sep", "cat_viajes", 500, fecha = LocalDate(2026, 9, 30))
      fixture.gasto("nov", "cat_viajes", 300)
      fixture.gasto("other", "cat_finanzas", 5_000)
      fixture.gasto("oct-other", "cat_finanzas", 1, fecha = LocalDate(2026, 10, 5))

      val history =
          assertIs<DataOutcome.Success<List<BudgetMonth>>>(
                  fixture.repository.history("id-1", TODAY)
              )
              .data

      assertEquals(
          listOf(
                  Triple(6, 700L, true),
                  Triple(7, 0L, false),
                  Triple(8, 0L, false),
                  Triple(9, 500L, true),
                  Triple(10, 0L, true),
                  Triple(11, 300L, true),
              )
              .map { (m, v, recorded) -> BudgetMonth(YearMonth(2026, m), cop(v), recorded) },
          history,
      )
    }
  }

  @Test
  fun historyOfUnknownBudgetFails() = runTest {
    withRepository { fixture ->
      assertIs<DataError.InvalidInput>(
          assertIs<DataOutcome.Failure>(fixture.repository.history("missing", TODAY)).error
      )
    }
  }

  @Test
  fun suggestionUsesWindow() = runTest {
    withRepository { fixture ->
      fixture.gasto("jul", "cat_viajes", 90_000, fecha = LocalDate(2026, 7, 10))
      fixture.gasto("aug", "cat_viajes", 3_000, fecha = LocalDate(2026, 8, 10))
      fixture.gasto("sep", "cat_vuelos", 3_000, fecha = LocalDate(2026, 9, 10))
      fixture.gasto("oct", "cat_finanzas", 5, fecha = LocalDate(2026, 10, 10))
      fixture.gasto("nov", "cat_viajes", 99_999)

      val suggestion = fixture.repository.suggestion("cat_viajes", Moneda.COP, TODAY)

      assertEquals(DataOutcome.Success<Money?>(cop(2_000)), suggestion)
      assertEquals(
          DataOutcome.Success<Money?>(null),
          fixture.repository.suggestion("cat_viajes", Moneda.USD, TODAY),
      )
    }
  }

  @Test
  fun noticePicksChildOverParent() = runTest {
    withRepository { fixture ->
      fixture.created("cat_viajes", cop(1_000))
      fixture.created("cat_vuelos", cop(1_000))
      fixture.gasto("a", "cat_viajes", 1_000)
      fixture.gasto("b", "cat_vuelos", 2_000)
      val touch = BudgetTouch("cat_vuelos", "cat_viajes", Moneda.COP, TODAY)

      val notice =
          assertIs<DataOutcome.Success<com.kurobello.tallybook.core.model.BudgetNotice?>>(
                  fixture.repository.notice(listOf(touch), TODAY)
              )
              .data

      assertEquals("cat_vuelos", notice?.categoriaNombre)
      assertEquals(BudgetState.OVER, notice?.state)
      assertEquals(cop(1_000), notice?.amount)
    }
  }

  @Test
  fun noticeIsNullWhenNothingTouchedIsOff() = runTest {
    withRepository { fixture ->
      fixture.created("cat_viajes", cop(1_000))
      fixture.gasto("a", "cat_viajes", 2_000)
      val untouched = BudgetTouch("cat_finanzas", null, Moneda.COP, TODAY)

      assertEquals(
          DataOutcome.Success(null),
          fixture.repository.notice(listOf(untouched), TODAY),
      )
    }
  }

  @Test
  fun maxChangeStampIncludesPresupuesto() = runTest {
    withRepository { fixture ->
      fixture.created("cat_viajes", cop(1_000))
      fixture.repository.delete("id-1")

      val max = fixture.database.changeStampQueries.selectMaxChangeStamp().executeAsOne().max_stamp

      assertEquals(fixture.changeClock.issuedStamps.last().value, max)
      assertNotEquals(com.kurobello.tallybook.core.model.ChangeStamp.SEED.value, max)
    }
  }

  private class Fixture(
      val driver: SqlDriver,
      val database: TallybookDatabase,
      val changeClock: TestChangeClock,
      val repository: PresupuestoRepository,
      val categorias: CategoriaRepository,
  ) {
    suspend fun created(categoriaId: String, monto: Money): Presupuesto =
        assertIs<PresupuestoWrite.Saved>(
                assertIs<DataOutcome.Success<PresupuestoWrite>>(
                        repository.create(categoriaId, monto, TODAY)
                    )
                    .data
            )
            .presupuesto

    suspend fun statuses(): List<BudgetStatus> =
        assertIs<DataOutcome.Success<List<BudgetStatus>>>(repository.observeStatuses(TODAY).first())
            .data

    fun gasto(
        id: String,
        categoriaId: String,
        minor: Long,
        fecha: LocalDate = TODAY,
        tipo: TipoMovimiento = TipoMovimiento.GASTO,
        cuentaId: String = COP_CASH,
    ) {
      database.insertLegacy(
          Movimiento(
              id = id,
              tipo = tipo,
              monto = Money(minor, if (cuentaId == USD_CASH) Moneda.USD else Moneda.COP),
              fecha = fecha,
              categoriaId = categoriaId,
              titulo = null,
              descripcion = null,
              createdAt = NOW,
              cuentaId = cuentaId,
              medio = MedioPago.EFECTIVO,
          )
      )
    }

    suspend fun customCategoria(): String {
      val draft = CategoriaDraft("Custom", CategoryIcon.TAG, CategoryTint.NEUTRAL, null)
      return assertIs<CategoriaWrite.Saved>(
              assertIs<DataOutcome.Success<CategoriaWrite>>(categorias.create(draft)).data
          )
          .categoria
          .id
    }
  }

  private suspend fun TestScope.withRepository(block: suspend (Fixture) -> Unit) {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
      val database = createSchemaWithReferences(driver)
      val changeClock = TestChangeClock()
      val dispatcher = StandardTestDispatcher(testScheduler)
      var nextId = 0
      val idGenerator = IdGenerator { "id-${++nextId}" }
      block(
          Fixture(
              driver = driver,
              database = database,
              changeClock = changeClock,
              repository =
                  PresupuestoRepositoryImpl(
                      database = database,
                      dispatcher = dispatcher,
                      idGenerator = idGenerator,
                      clock = FixedClock,
                      changeClock = changeClock,
                  ),
              categorias =
                  CategoriaRepositoryImpl(
                      database = database,
                      dispatcher = dispatcher,
                      idGenerator = { "custom-${++nextId}" },
                      changeClock = changeClock,
                  ),
          )
      )
    }
  }

  private object FixedClock : Clock {
    override fun now(): Instant = NOW
  }
}

private fun cop(minor: Long) = Money(minor, Moneda.COP)

private val TODAY = LocalDate(2026, 11, 18)
private val NOW = Instant.parse("2026-11-18T18:00:00Z")
private val COP_CASH = Cuenta.defaultCashId(Moneda.COP)
private val USD_CASH = Cuenta.defaultCashId(Moneda.USD)
