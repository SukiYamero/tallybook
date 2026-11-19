package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.FakePresupuestoRepository
import com.kurobello.tallybook.core.model.BudgetNotice
import com.kurobello.tallybook.core.model.BudgetState
import com.kurobello.tallybook.core.model.BudgetStatus
import com.kurobello.tallybook.core.model.BudgetTouch
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Presupuesto
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.budgetStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AddMovimientoBudgetNoticeTest {

  @Test
  fun savedGastoInNearBudgetSendsNotice() = runTest {
    val f = fixture()

    f.save()

    assertEquals(
        AddMovimientoEffect.Saved(
            BudgetNotice("Food", BudgetState.NEAR, Money(1_500_000, Moneda.COP), null)
        ),
        f.viewModel.effects.first(),
    )
    assertEquals(
        listOf(listOf(BudgetTouch("lunch", "food", Moneda.COP, TODAY))),
        f.presupuestos.noticeReads,
    )
  }

  @Test
  fun savedIngresoSendsNoNotice() = runTest {
    val f = fixture()
    f.viewModel.onTipoChanged(TipoMovimiento.INGRESO)

    f.save()

    assertEquals(AddMovimientoEffect.Saved(null), f.viewModel.effects.first())
    assertEquals(1, f.movimientos.inserted.size)
    assertTrue(f.presupuestos.noticeReads.isEmpty())
  }

  @Test
  fun backdatedGastoSendsNoNotice() = runTest {
    val f = fixture()
    f.viewModel.onFechaChanged(LocalDate(2026, 8, 31))

    f.save()

    assertEquals(AddMovimientoEffect.Saved(null), f.viewModel.effects.first())
    assertEquals(1, f.movimientos.inserted.size)
  }

  @Test
  fun noticeReadFailureStillSaves() = runTest {
    val f = fixture()
    f.presupuestos.noticeFailure = DataError.Local(IllegalStateException())

    f.save()

    assertEquals(AddMovimientoEffect.Saved(null), f.viewModel.effects.first())
    assertEquals(1, f.movimientos.inserted.size)
    assertEquals(1, f.presupuestos.noticeReads.size)
  }

  @Test
  fun failedSaveReadsNoNotice() = runTest {
    val f = fixture()
    f.movimientos.insertResult = DataOutcome.Failure(DataError.Local(IllegalStateException()))

    f.save()

    assertEquals(MovimientoFormError.SAVE_FAILED, f.viewModel.uiState.value.error)
    assertTrue(f.presupuestos.noticeReads.isEmpty())
  }

  private class Fixture(
      val viewModel: AddMovimientoViewModel,
      val movimientos: AddFakeMovimientos,
      val presupuestos: FakePresupuestoRepository,
      private val scope: TestScope,
  ) {
    fun save() {
      viewModel.onAmountChanged("10000")
      viewModel.onCategoriaSelected("lunch")
      viewModel.onSubmit()
      scope.runCurrent()
    }
  }

  private fun TestScope.fixture(): Fixture {
    val movimientos = AddFakeMovimientos()
    val presupuestos = FakePresupuestoRepository(listOf(foodBudget()))
    val viewModel =
        createAddViewModel(
            movimientoRepository = movimientos,
            categoriaRepository = AddFakeCategorias(DataOutcome.Success(CATEGORIAS)),
            preferencesRepository = addPreferences(Moneda.COP),
            presupuestoRepository = presupuestos,
            scope = backgroundScope,
        )
    runCurrent()
    return Fixture(viewModel, movimientos, presupuestos, this)
  }

  private companion object {
    val TODAY = LocalDate(2026, 9, 15)
    val CATEGORIAS =
        listOf(
            Categoria("food", "Food", null, CategoryIcon.UTENSILS, CategoryTint.AMBER),
            Categoria("lunch", "Lunch", "food", CategoryIcon.UTENSILS, CategoryTint.AMBER),
        )

    fun foodBudget(): BudgetStatus =
        budgetStatus(
            Presupuesto(
                "food-budget",
                "food",
                Money(10_000_000, Moneda.COP),
                Instant.parse("2026-01-01T00:00:00Z"),
            ),
            "Food",
            null,
            Money(8_500_000, Moneda.COP),
            TODAY,
        )
  }
}
