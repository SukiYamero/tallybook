package com.kurobello.tallybook.feature.history.viewmodel

import com.kurobello.tallybook.core.model.Actividad
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Direccion
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.Transferencia
import com.kurobello.tallybook.feature.history.ui.HistoryDayGroupUi
import com.kurobello.tallybook.feature.history.ui.HistoryMovimientoRowUi
import com.kurobello.tallybook.feature.history.ui.HistoryTransferenciaRowUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class HistoryProjectionTest {
  @Test
  fun emptyPeriodHasZeroTotalsAndNoGroupsInTheSelectedCurrency() {
    for (moneda in listOf(Moneda.COP, Moneda.CLP)) {
      val content =
          assertIs<HistoryProjection.Content>(
              projectMovimientos(moneda, september, emptyList(), emptyMap(), "Unknown category")
          )
      assertEquals(Money(0, moneda), content.summary.ingresos)
      assertEquals(Money(0, moneda), content.summary.gastos)
      assertEquals(Money(0, moneda), content.summary.balance)
      assertEquals(0, content.movementCount)
      assertEquals(emptyList(), content.groups)
    }
  }

  @Test
  fun incomeOnlyAndExpenseOnlyPeriodsRetainTheirExactMinorUnits() {
    for (moneda in listOf(Moneda.COP, Moneda.CLP)) {
      val income =
          assertIs<HistoryProjection.Content>(
              projectMovimientos(
                  moneda,
                  september,
                  listOf(movimiento("income", TipoMovimiento.INGRESO, 1250, moneda)),
                  emptyMap(),
                  "Unknown category",
              )
          )
      assertEquals(Money(1250, moneda), income.summary.ingresos)
      assertEquals(Money(0, moneda), income.summary.gastos)
      assertEquals(Money(1250, moneda), income.summary.balance)
      assertEquals(1, income.movementCount)

      val expense =
          assertIs<HistoryProjection.Content>(
              projectMovimientos(
                  moneda,
                  september,
                  listOf(movimiento("expense", TipoMovimiento.GASTO, 350, moneda)),
                  emptyMap(),
                  "Unknown category",
              )
          )
      assertEquals(Money(0, moneda), expense.summary.ingresos)
      assertEquals(Money(350, moneda), expense.summary.gastos)
      assertEquals(Money(-350, moneda), expense.summary.balance)
      assertEquals(1, expense.movementCount)
    }
  }

  @Test
  fun mixedPeriodTotalsAndGroupedCountDescribeTheSameCompleteSnapshot() {
    val rows =
        listOf(
            movimiento("income-a", TipoMovimiento.INGRESO, 1050, fecha = LocalDate(2026, 9, 30)),
            movimiento("expense-a", TipoMovimiento.GASTO, 425, fecha = LocalDate(2026, 9, 30)),
            movimiento("income-b", TipoMovimiento.INGRESO, 225, fecha = LocalDate(2026, 9, 1)),
            movimiento("expense-b", TipoMovimiento.GASTO, 75, fecha = LocalDate(2026, 9, 1)),
        )
    val content =
        assertIs<HistoryProjection.Content>(
            projectMovimientos(Moneda.COP, september, rows, emptyMap(), "Unknown category")
        )

    assertEquals(Money(1275, Moneda.COP), content.summary.ingresos)
    assertEquals(Money(500, Moneda.COP), content.summary.gastos)
    assertEquals(Money(775, Moneda.COP), content.summary.balance)
    assertEquals(4, content.movementCount)
    assertEquals(
        listOf(LocalDate(2026, 9, 30), LocalDate(2026, 9, 1)),
        content.groups.map { it.fecha },
    )
    assertEquals(listOf(2, 2), content.groups.map { it.movimientoRows.size })
    assertEquals(
        listOf("income-a", "expense-a", "income-b", "expense-b"),
        content.groups.flatMap { it.movimientoRows }.map { it.id },
    )
  }

  @Test
  fun validSubtotalExtremesAllowLargestPositiveAndNegativeBalances() {
    val cases =
        listOf(TipoMovimiento.INGRESO to Long.MAX_VALUE, TipoMovimiento.GASTO to -Long.MAX_VALUE)
    for ((tipo, expectedBalance) in cases) {
      val content =
          assertIs<HistoryProjection.Content>(
              projectMovimientos(
                  Moneda.COP,
                  september,
                  listOf(movimiento("maximum", tipo, Long.MAX_VALUE)),
                  emptyMap(),
                  "Unknown category",
              )
          )
      assertEquals(Money(expectedBalance, Moneda.COP), content.summary.balance)
      assertEquals(1, content.movementCount)
      assertEquals(
          Money(Long.MAX_VALUE, Moneda.COP),
          content.groups.single().movimientoRows.single().monto,
      )
    }
  }

  @Test
  fun twoValidMaximumSubtotalsProduceZeroBalance() {
    val rows =
        listOf(
            movimiento("income", TipoMovimiento.INGRESO, Long.MAX_VALUE),
            movimiento("expense", TipoMovimiento.GASTO, Long.MAX_VALUE),
        )
    val content =
        assertIs<HistoryProjection.Content>(
            projectMovimientos(Moneda.COP, september, rows, emptyMap(), "Unknown category")
        )
    assertEquals(Money(Long.MAX_VALUE, Moneda.COP), content.summary.ingresos)
    assertEquals(Money(Long.MAX_VALUE, Moneda.COP), content.summary.gastos)
    assertEquals(Money(0, Moneda.COP), content.summary.balance)
    assertEquals(2, content.movementCount)
  }

  @Test
  fun incomeSubtotalOverflowIsFailureEvenWhenTheFinalNetBalanceWouldFit() {
    val rows =
        listOf(
            movimiento("z-expense", TipoMovimiento.GASTO, Long.MAX_VALUE),
            movimiento("b-income", TipoMovimiento.INGRESO, Long.MAX_VALUE),
            movimiento("a-income", TipoMovimiento.INGRESO, 1),
        )
    assertEquals(
        HistoryProjection.Failure(HistoryFailure.CalculationOverflow),
        projectMovimientos(Moneda.COP, september, rows, emptyMap(), "Unknown category"),
    )
  }

  @Test
  fun expenseSubtotalOverflowIsFailureEvenWhenTheFinalNetBalanceWouldFit() {
    val rows =
        listOf(
            movimiento("z-income", TipoMovimiento.INGRESO, Long.MAX_VALUE),
            movimiento("b-expense", TipoMovimiento.GASTO, Long.MAX_VALUE),
            movimiento("a-expense", TipoMovimiento.GASTO, 1),
        )
    assertEquals(
        HistoryProjection.Failure(HistoryFailure.CalculationOverflow),
        projectMovimientos(Moneda.COP, september, rows, emptyMap(), "Unknown category"),
    )
  }

  @Test
  fun nonblankTituloIsTheTitleAndNullOrBlankTitulosUseCategory() {
    val rows =
        listOf(
            movimiento("described", titulo = "  Lunch with Ana  "),
            movimiento("absent", titulo = null),
            movimiento("blank", titulo = " \n\t "),
        )
    val content =
        assertIs<HistoryProjection.Content>(
            projectMovimientos(
                Moneda.COP,
                september,
                rows,
                mapOf(food.id to food),
                "Unknown category",
            )
        )
    val projected = content.groups.single().movimientoRows

    assertEquals(listOf("  Lunch with Ana  ", "Food", "Food"), projected.map { it.title })
    assertEquals(listOf("Food", "Food", "Food"), projected.map { it.categoryLabel })
    assertEquals(listOf(food, food, food), projected.map { it.categoria })
    assertEquals(rows, projected.map { it.movimiento })
  }

  @Test
  fun missingCategoryUsesTheSuppliedLocalizedLabelAndKeepsTituloAndMoney() {
    val rows =
        listOf(
            movimiento("described", titulo = "Bus", categoriaId = "missing"),
            movimiento("fallback", categoriaId = "missing"),
        )
    val content =
        assertIs<HistoryProjection.Content>(
            projectMovimientos(Moneda.COP, september, rows, emptyMap(), "Categoría desconocida")
        )
    val projected = content.groups.single().movimientoRows

    assertEquals(listOf("Bus", "Categoría desconocida"), projected.map { it.title })
    assertEquals(
        listOf("Categoría desconocida", "Categoría desconocida"),
        projected.map { it.categoryLabel },
    )
    assertNull(projected.first().categoria)
    assertNull(projected.last().categoria)
    assertEquals(Money(200, Moneda.COP), content.summary.gastos)
    assertEquals(rows.map { it.monto }, projected.map { it.monto })
  }

  @Test
  fun categoryAndFallbackChangesRebuildPresentationWithoutChangingFinancialResults() {
    val rows = listOf(movimiento("known"), movimiento("missing", categoriaId = "missing"))
    val initial =
        assertIs<HistoryProjection.Content>(
            projectMovimientos(
                Moneda.COP,
                september,
                rows,
                mapOf(food.id to food),
                "Unknown category",
            )
        )
    val updatedFood =
        food.copy(nombre = "Comida", icono = CategoryIcon.COFFEE, color = CategoryTint.BLUE)
    val updated =
        assertIs<HistoryProjection.Content>(
            projectMovimientos(
                Moneda.COP,
                september,
                rows,
                mapOf(updatedFood.id to updatedFood),
                "Categoría desconocida",
            )
        )

    assertEquals(
        listOf("Food", "Unknown category"),
        initial.groups.single().movimientoRows.map { it.title },
    )
    assertEquals(
        listOf("Comida", "Categoría desconocida"),
        updated.groups.single().movimientoRows.map { it.title },
    )
    assertEquals(updatedFood, updated.groups.single().movimientoRows.first().categoria)
    assertEquals(initial.summary, updated.summary)
    assertEquals(initial.movementCount, updated.movementCount)
    assertEquals(
        initial.groups.single().movimientoRows.map { it.key },
        updated.groups.single().movimientoRows.map { it.key },
    )
    assertEquals(initial.groups.single().key, updated.groups.single().key)
  }

  @Test
  fun equalDateRowsKeepRepositoryOrderWithoutComparingParsedCreatedAt() {
    val rows =
        listOf(
            movimiento("b").copy(createdAt = Instant.parse("2026-09-15T12:00:00Z")),
            movimiento("a").copy(createdAt = Instant.parse("2026-09-15T12:00:00.900Z")),
        )
    val content =
        assertIs<HistoryProjection.Content>(
            projectMovimientos(Moneda.COP, september, rows, emptyMap(), "Unknown category")
        )

    assertEquals(rows, content.groups.single().movimientoRows.map { it.movimiento })
    assertEquals(LocalDate(2026, 9, 15), content.groups.single().fecha)
    assertEquals(
        listOf(LocalDate(2026, 9, 15), LocalDate(2026, 9, 15)),
        content.groups.single().movimientoRows.map { it.fecha },
    )
  }

  @Test
  fun dateHeaderAndMovementKeysHaveDistinctNamespacesEvenForAdversarialIds() {
    val row = movimiento("history-day:2026-09-15")
    val content =
        assertIs<HistoryProjection.Content>(
            projectMovimientos(Moneda.COP, september, listOf(row), emptyMap(), "Unknown category")
        )
    val group = content.groups.single()

    assertEquals(row.id, group.movimientoRows.single().id)
    assertNotEquals(group.key, group.movimientoRows.single().key)
    val rebuilt =
        assertIs<HistoryProjection.Content>(
            projectMovimientos(Moneda.COP, september, listOf(row), emptyMap(), "Otro idioma")
        )
    assertEquals(group.key, rebuilt.groups.single().key)
    assertEquals(
        group.movimientoRows.single().key,
        rebuilt.groups.single().movimientoRows.single().key,
    )
  }

  @Test
  fun rowsOutsideTheRangeAreRejectedInsteadOfFilteredOrCounted() {
    for (date in listOf(LocalDate(2026, 8, 31), LocalDate(2026, 10, 1))) {
      assertFailsWith<IllegalArgumentException> {
        projectMovimientos(
            Moneda.COP,
            september,
            listOf(movimiento("outside", fecha = date)),
            emptyMap(),
            "Unknown category",
        )
      }
    }
  }

  @Test
  fun mixedCurrencyAndNonpositiveMovimientoInputsAreRejected() {
    assertFailsWith<IllegalArgumentException> {
      projectMovimientos(
          Moneda.COP,
          september,
          listOf(movimiento("wrong-currency", moneda = Moneda.USD)),
          emptyMap(),
          "Unknown category",
      )
    }
    for (amount in listOf(0L, -1L)) {
      assertFailsWith<IllegalArgumentException> {
        projectMovimientos(
            Moneda.COP,
            september,
            listOf(movimiento("invalid-amount", amount = amount)),
            emptyMap(),
            "Unknown category",
        )
      }
    }
  }

  @Test
  fun invalidSnapshotInvariantsCannotBeHiddenByEarlierSubtotalOverflow() {
    val overflowing =
        listOf(
            movimiento("maximum", TipoMovimiento.INGRESO, Long.MAX_VALUE),
            movimiento("one", TipoMovimiento.INGRESO, 1),
        )
    for (invalid in
        listOf(
            movimiento("currency", moneda = Moneda.USD),
            movimiento("date", fecha = LocalDate(2026, 10, 1)),
            movimiento("amount", amount = 0),
        )) {
      assertFailsWith<IllegalArgumentException> {
        projectMovimientos(
            Moneda.COP,
            september,
            overflowing + invalid,
            emptyMap(),
            "Unknown category",
        )
      }
    }
  }

  @Test
  fun nonDescendingDatesAreRejectedInsteadOfRegroupingOutOfOrderRows() {
    val rows =
        listOf(
            movimiento("latest", fecha = LocalDate(2026, 9, 30)),
            movimiento("earliest", fecha = LocalDate(2026, 9, 1)),
            movimiento("late-again", fecha = LocalDate(2026, 9, 30)),
        )
    assertFailsWith<IllegalArgumentException> {
      projectMovimientos(Moneda.COP, september, rows, emptyMap(), "Unknown category")
    }
  }

  @Test
  fun transfersShareDayGroupsButStayOutOfTotalsAndTheMovementCount() {
    val interna = transferencia("interna", 900, LocalDate(2026, 9, 30))
    val entrada = transferencia("entrada", 4000, LocalDate(2026, 9, 15), Direccion.ENTRADA)
    val actividades =
        listOf(
            interna,
            Actividad.DeMovimiento(movimiento("gasto", fecha = LocalDate(2026, 9, 30))),
            entrada,
            Actividad.DeMovimiento(movimiento("ingreso", TipoMovimiento.INGRESO, 250)),
        )
    val content =
        assertIs<HistoryProjection.Content>(
            projectHistory(
                Moneda.COP,
                september,
                actividades,
                emptyMap(),
                "Unknown category",
                mapOf(NEQUI.id to NEQUI),
            )
        )

    assertEquals(Money(250, Moneda.COP), content.summary.ingresos)
    assertEquals(Money(100, Moneda.COP), content.summary.gastos)
    assertEquals(2, content.movementCount)
    assertEquals(2, content.transferCount)
    assertEquals(
        listOf(listOf("interna", "gasto"), listOf("entrada", "ingreso")),
        content.groups.map { group -> group.rows.map { it.id } },
    )
    val row = assertIs<HistoryTransferenciaRowUi>(content.groups.first().rows.first())
    assertEquals(interna, row.actividad)
    assertEquals(NEQUI, row.origen)
    assertNull(row.destino)
    assertEquals(Money(900, Moneda.COP), row.monto)
    assertNotEquals(
        content.groups.first().rows[0].key,
        HistoryMovimientoRowUi(movimiento("interna"), null, "", "").key,
    )
  }

  @Test
  fun aPeriodWithOnlyTransfersIsNotEmptyAndCountsNoMovements() {
    val content =
        assertIs<HistoryProjection.Content>(
            projectHistory(
                Moneda.COP,
                september,
                listOf(transferencia("move", 900, LocalDate(2026, 9, 15))),
                emptyMap(),
                "Unknown category",
            )
        )
    assertEquals(0, content.movementCount)
    assertEquals(1, content.transferCount)
    assertEquals(false, content.isEmpty)
    assertEquals(Money(0, Moneda.COP), content.summary.balance)
  }

  @Test
  fun aTransferSideInAnotherCurrencyIsRejected() {
    val usd =
        Actividad.DeTransferencia(
            transferencia("usd", 10, LocalDate(2026, 9, 15)).transferencia,
            Money(10, Moneda.USD),
            Direccion.SALIDA,
        )
    assertFailsWith<IllegalArgumentException> {
      projectHistory(Moneda.COP, september, listOf(usd), emptyMap(), "Unknown category")
    }
  }

  private fun transferencia(
      id: String,
      amount: Long,
      fecha: LocalDate,
      direccion: Direccion = Direccion.INTERNA,
  ): Actividad.DeTransferencia =
      Actividad.DeTransferencia(
          Transferencia(
              id,
              NEQUI.id,
              "nu",
              Money(amount, Moneda.COP),
              Money(amount, Moneda.COP),
              fecha,
              null,
              Instant.parse("2026-09-15T13:00:00Z"),
          ),
          Money(amount, Moneda.COP),
          direccion,
      )

  private fun projectMovimientos(
      moneda: Moneda,
      range: MovimientoDateRange,
      movimientos: List<Movimiento>,
      categoriasById: Map<String, Categoria>,
      unknownCategoryLabel: String,
  ): HistoryProjection =
      projectHistory(
          moneda,
          range,
          movimientos.map(Actividad::DeMovimiento),
          categoriasById,
          unknownCategoryLabel,
      )

  private val HistoryDayGroupUi.movimientoRows: List<HistoryMovimientoRowUi>
    get() = rows.map { assertIs<HistoryMovimientoRowUi>(it) }

  private fun movimiento(
      id: String,
      tipo: TipoMovimiento = TipoMovimiento.GASTO,
      amount: Long = 100,
      moneda: Moneda = Moneda.COP,
      fecha: LocalDate = LocalDate(2026, 9, 15),
      titulo: String? = null,
      categoriaId: String = "food",
  ): Movimiento =
      Movimiento(
          id,
          tipo,
          Money(amount, moneda),
          fecha,
          categoriaId,
          titulo,
          null,
          Instant.parse("2026-09-15T12:00:00Z"),
          Cuenta.defaultCashId(moneda),
          MedioPago.EFECTIVO,
      )

  private companion object {
    val september = MovimientoDateRange(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30))
    val food = Categoria("food", "Food", null, CategoryIcon.UTENSILS, CategoryTint.AMBER)
    val NEQUI =
        Cuenta(
            "nequi",
            "Nequi",
            TipoCuenta.BANCO,
            Money(0, Moneda.COP),
            null,
            false,
            Instant.parse("2026-01-01T00:00:00Z"),
        )
  }
}
