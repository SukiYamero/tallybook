package com.kurobello.tallybook.feature.budgets.viewmodel

import com.kurobello.tallybook.core.data.CivilToday
import com.kurobello.tallybook.core.data.FakeCuentaRepository
import com.kurobello.tallybook.core.data.FakePresupuestoRepository
import com.kurobello.tallybook.core.data.MutableClock
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.BudgetMonth
import com.kurobello.tallybook.core.model.BudgetStatus
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.Presupuesto
import com.kurobello.tallybook.core.model.PresupuestoWrite
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.budgetStatus
import com.kurobello.tallybook.core.text.AmountInputError
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddFakeCategorias
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddFakePreferences
import com.kurobello.tallybook.feature.movimientos.viewmodel.COP_CASH
import com.kurobello.tallybook.feature.movimientos.viewmodel.addTestCategoria
import com.kurobello.tallybook.feature.movimientos.viewmodel.testCuenta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModelTest {

  @Test
  fun loadsSortedStatusesWithFineCountAndDaysLeft() = runTest {
    val f = fixture()

    val content = assertIs<RetainedLoadState.Ready<BudgetsContent>>(f.state().content).value

    assertEquals(listOf("food", "rent", "fun", "gym"), content.statuses.map { it.presupuesto.id })
    assertEquals(2, content.fineCount)
    assertEquals(16, content.daysLeft)
    assertEquals(YearMonth(2026, 11), content.month)
  }

  @Test
  fun createLoadsSuggestionForCategoryAndPrimaryMoneda() = runTest {
    val f = fixture()
    f.presupuestos.suggestions["cat_food" to Moneda.COP] = cop(430_000)

    f.viewModel.onCreate()
    f.viewModel.onCategorySelected("cat_food")
    runCurrent()

    val editor = assertNotNull(f.state().editor)
    assertEquals(Moneda.COP, editor.moneda)
    assertEquals(cop(430_000), editor.suggestion)
    assertEquals(listOf("cat_food" to Moneda.COP), f.presupuestos.suggestionReads)
  }

  @Test
  fun monedaSelectorListsOnlyAccountCurrencies() = runTest {
    val archivedClp = testCuenta("old-clp", TipoCuenta.BANCO, Moneda.CLP, archivada = true)
    val f = fixture(cuentas = listOf(COP_CASH, usdCash, archivedClp), primary = Moneda.USD)
    f.presupuestos.suggestions["cat_food" to Moneda.COP] = cop(430_000)

    f.viewModel.onCreate()

    val editor = assertNotNull(f.state().editor)
    assertEquals(listOf(Moneda.USD, Moneda.COP), editor.monedas)
    assertEquals(Moneda.USD, editor.moneda)

    f.viewModel.onCategorySelected("cat_food")
    runCurrent()
    f.viewModel.onMonedaSelected(Moneda.COP)
    f.viewModel.onMonedaSelected(Moneda.CLP)
    runCurrent()

    assertEquals(Moneda.COP, f.state().editor?.moneda)
    assertEquals(cop(430_000), f.state().editor?.suggestion)
    assertEquals(
        listOf("cat_food" to Moneda.USD, "cat_food" to Moneda.COP),
        f.presupuestos.suggestionReads,
    )
  }

  @Test
  fun aFailedSuggestionReadLeavesNoSuggestion() = runTest {
    val f = fixture()
    f.presupuestos.suggestionFailure = DataError.Local(IllegalStateException("disk"))

    f.viewModel.onCreate()
    f.viewModel.onCategorySelected("cat_food")
    runCurrent()

    assertNull(f.state().editor?.suggestion)
  }

  @Test
  fun useSuggestionFillsAmount() = runTest {
    val f = fixture()
    f.presupuestos.suggestions["cat_food" to Moneda.COP] = cop(430_000)
    f.viewModel.onCreate()
    f.viewModel.onCategorySelected("cat_food")
    runCurrent()

    f.viewModel.onUseSuggestion()

    assertEquals("430000,00", f.state().editor?.amountInput)
  }

  @Test
  fun saveRejectsAmountAboveCap() = runTest {
    val f = fixture()
    f.viewModel.onCreate()
    f.viewModel.onCategorySelected("cat_food")

    f.viewModel.onAmountChanged("100000000000000")
    f.viewModel.onSave()
    runCurrent()

    assertEquals(AmountInputError.OVERFLOW, f.state().editor?.amountError)
    assertEquals(emptyList(), f.presupuestos.created)

    f.viewModel.onAmountChanged("0")
    f.viewModel.onSave()
    runCurrent()

    assertEquals(AmountInputError.NOT_POSITIVE, f.state().editor?.amountError)
    assertEquals(emptyList(), f.presupuestos.created)

    f.viewModel.onAmountChanged("1,5,5")
    f.viewModel.onSave()
    runCurrent()

    assertEquals(AmountInputError.MALFORMED, f.state().editor?.amountError)
    assertEquals(emptyList(), f.presupuestos.created)

    f.viewModel.onAmountChanged("12")
    assertNull(f.state().editor?.amountError)
  }

  @Test
  fun saveCreatesTheBudgetAndClosesTheEditor() = runTest {
    val f = fixture()
    f.viewModel.onCreate()
    f.viewModel.onCategorySelected("cat_food")
    f.viewModel.onAmountChanged("500000")

    f.viewModel.onSave()
    runCurrent()

    assertEquals(listOf("cat_food" to cop(500_000)), f.presupuestos.created)
    assertEquals(true, f.state().sheetClosing)
    assertNotNull(f.state().editor)

    f.viewModel.onSheetHidden()

    assertNull(f.state().editor)
    assertEquals(false, f.state().sheetClosing)
  }

  @Test
  fun dismissingTheDetailKeepsItUntilTheSheetHides() = runTest {
    val f = fixture()
    f.viewModel.onOpen("food")

    f.viewModel.onDismissDetail()

    assertEquals(true, f.state().sheetClosing)
    assertNotNull(f.state().detail)

    f.viewModel.onSheetHidden()

    assertNull(f.state().detail)
  }

  @Test
  fun dismissingAnEditorOpenedFromTheDetailReturnsToIt() = runTest {
    val f = fixture()
    f.viewModel.onOpen("food")
    f.viewModel.onEditAmount()

    f.viewModel.onDismissEditor()

    assertNull(f.state().editor)
    assertNotNull(f.state().detail)
    assertEquals(false, f.state().sheetClosing)
  }

  @Test
  fun duplicateSwitchesToEditingExisting() = runTest {
    val f = fixture()
    val existing = presupuesto("food", cop(500_000))
    f.presupuestos.createResult = PresupuestoWrite.Duplicate(existing)
    f.viewModel.onCreate()
    f.viewModel.onCategorySelected("cat_food")
    f.viewModel.onAmountChanged("300000")

    f.viewModel.onSave()
    runCurrent()

    val editor = assertNotNull(f.state().editor)
    assertEquals("food", editor.editingId)
    assertEquals(existing, editor.duplicateOf)
    assertEquals("500000,00", editor.amountInput)
    assertEquals(false, editor.saving)
  }

  @Test
  fun editAmountUpdates() = runTest {
    val f = fixture()
    f.viewModel.onOpen("food")
    f.viewModel.onEditAmount()
    assertEquals("500000,00", f.state().editor?.amountInput)

    f.viewModel.onAmountChanged("700000")
    f.viewModel.onSave()
    runCurrent()

    assertEquals(listOf("food" to cop(700_000)), f.presupuestos.updated)
    assertNull(f.state().editor)
    assertEquals(cop(700_000), f.state().detail?.status?.presupuesto?.monto)
  }

  @Test
  fun deleteNeedsConfirmation() = runTest {
    val f = fixture()
    f.viewModel.onOpen("food")

    f.viewModel.onConfirmDelete()
    runCurrent()
    assertEquals(emptyList(), f.presupuestos.deleted)

    f.viewModel.onRequestDelete()
    assertEquals(true, f.state().detail?.confirmingDelete)
    f.viewModel.onConfirmDelete()
    runCurrent()

    assertEquals(listOf("food"), f.presupuestos.deleted)
    assertEquals(true, f.state().sheetClosing)

    f.viewModel.onSheetHidden()

    assertNull(f.state().detail)
  }

  @Test
  fun aSecondConfirmWhileDeletingIsIgnored() = runTest {
    val f = fixture()
    f.viewModel.onOpen("food")
    f.viewModel.onRequestDelete()

    f.viewModel.onConfirmDelete()
    f.viewModel.onConfirmDelete()
    runCurrent()

    assertEquals(listOf("food"), f.presupuestos.deleted)
    assertEquals(false, f.state().detail?.deleteFailed)
  }

  @Test
  fun cancelDeleteKeepsTheBudget() = runTest {
    val f = fixture()
    f.viewModel.onOpen("food")
    f.viewModel.onRequestDelete()

    f.viewModel.onCancelDelete()
    f.viewModel.onConfirmDelete()
    runCurrent()

    assertEquals(false, f.state().detail?.confirmingDelete)
    assertEquals(emptyList(), f.presupuestos.deleted)
  }

  @Test
  fun exposesPrimaryMonedaForRowCurrencyCodes() = runTest {
    val f = fixture(cuentas = listOf(COP_CASH, usdCash), primary = Moneda.USD)

    assertEquals(Moneda.USD, f.state().primaryMoneda)
  }

  @Test
  fun viewMovementsSearchesTheCurrentMonthOnlyOnceTheSheetHides() = runTest {
    val f = fixture()
    val effects = mutableListOf<BudgetsEffect>()
    backgroundScope.launch { f.viewModel.effects.collect { effects += it } }
    f.viewModel.onOpen("food")

    f.viewModel.onViewMovements()
    runCurrent()

    assertEquals(emptyList(), effects)
    assertEquals(true, f.state().sheetClosing)
    assertNotNull(f.state().detail)

    f.viewModel.onSheetHidden()
    runCurrent()

    assertEquals(
        listOf<BudgetsEffect>(
            BudgetsEffect.OpenSearch(
                categoriaId = "cat_food",
                moneda = Moneda.COP,
                range = MovimientoDateRange(LocalDate(2026, 11, 1), LocalDate(2026, 11, 15)),
            )
        ),
        effects,
    )
    assertNull(f.state().detail)
  }

  @Test
  fun openLoadsSixMonthHistory() = runTest {
    val f = fixture()
    val months =
        (6..11).map { BudgetMonth(YearMonth(2026, it), cop(100_000L * it), recorded = true) }
    f.presupuestos.history = months

    f.viewModel.onOpen("food")
    assertNull(f.state().detail?.history)
    runCurrent()

    assertEquals(months, f.state().detail?.history)
    assertEquals("food", f.state().detail?.status?.presupuesto?.id)
  }

  @Test
  fun failedHistoryReadFlagsDetail() = runTest {
    val f = fixture()
    f.presupuestos.historyFailure = DataError.Local(IllegalStateException("disk"))

    f.viewModel.onOpen("food")
    runCurrent()

    val detail = assertNotNull(f.state().detail)
    assertEquals(true, detail.historyFailed)
    assertNull(detail.history)
  }

  @Test
  fun failedReadKeepsPreviousContent() = runTest {
    val f = fixture()
    val previous = assertIs<RetainedLoadState.Ready<BudgetsContent>>(f.state().content).value

    f.presupuestos.statuses.value = DataOutcome.Failure(DataError.Local(IllegalStateException()))
    runCurrent()

    val failed = assertIs<RetainedLoadState.Failed<BudgetsContent, DataError>>(f.state().content)
    assertEquals(previous, failed.previous)

    f.presupuestos.statuses.value = DataOutcome.Success(emptyList())
    f.viewModel.onRetry()
    runCurrent()

    assertIs<RetainedLoadState.Ready<BudgetsContent>>(f.state().content)
  }

  @Test
  fun exposesCategoryCatalogForPicker() = runTest {
    val f = fixture()

    assertEquals(listOf("cat_food"), f.state().categorias.map { it.id })
  }

  @Test
  fun saveFailureFlagsEditor() = runTest {
    val f = fixture()
    f.presupuestos.writeFailure = DataError.Local(IllegalStateException("disk"))
    f.viewModel.onCreate()
    f.viewModel.onCategorySelected("cat_food")
    f.viewModel.onAmountChanged("500000")

    f.viewModel.onSave()
    runCurrent()

    assertEquals(true, f.state().editor?.saveFailed)
    assertEquals(false, f.state().editor?.saving)

    f.viewModel.onAmountChanged("400000")
    assertEquals(false, f.state().editor?.saveFailed)
  }

  @Test
  fun deleteFailureKeepsDetailOpen() = runTest {
    val f = fixture()
    f.presupuestos.writeFailure = DataError.Local(IllegalStateException("disk"))
    f.viewModel.onOpen("food")
    f.viewModel.onRequestDelete()

    f.viewModel.onConfirmDelete()
    runCurrent()

    val detail = assertNotNull(f.state().detail)
    assertEquals(true, detail.deleteFailed)
    assertEquals(false, detail.confirmingDelete)
  }

  @Test
  fun resumeOnNewMonthRereadsStatuses() = runTest {
    val f = fixture()

    f.clock.now = Instant.parse("2026-12-01T12:00:00Z")
    f.viewModel.onResume()
    runCurrent()

    assertEquals(LocalDate(2026, 12, 1), f.presupuestos.observedDays.last())
    val content = assertIs<RetainedLoadState.Ready<BudgetsContent>>(f.state().content).value
    assertEquals(YearMonth(2026, 12), content.month)
    assertEquals(31, content.daysLeft)
  }

  private class Fixture(
      val viewModel: BudgetsViewModel,
      val presupuestos: FakePresupuestoRepository,
      val clock: MutableClock,
  ) {
    fun state() = viewModel.uiState.value
  }

  private val usdCash: Cuenta = testCuenta("usd-cash", TipoCuenta.EFECTIVO, Moneda.USD)

  private fun TestScope.fixture(
      cuentas: List<Cuenta> = listOf(COP_CASH),
      primary: Moneda = Moneda.COP,
  ): Fixture {
    val today = LocalDate(2026, 11, 15)
    val repository =
        FakePresupuestoRepository(
            listOf(
                status("food", 500_000, spent = 600_000, today),
                status("rent", 500_000, spent = 450_000, today),
                status("fun", 200_000, spent = 10_000, today),
                status("gym", 100_000, spent = 0, today),
            )
        )
    val clock = MutableClock(Instant.parse("2026-11-15T12:00:00Z"))
    val viewModel =
        BudgetsViewModel(
            presupuestos = repository,
            cuentas = FakeCuentaRepository(cuentas),
            categorias =
                AddFakeCategorias(DataOutcome.Success(listOf(addTestCategoria("cat_food")))),
            preferences =
                AddFakePreferences(
                    DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(primary)))
                ),
            today = CivilToday(clock, TimeZoneProvider { TimeZone.UTC }),
            scope = backgroundScope,
        )
    runCurrent()
    return Fixture(viewModel, repository, clock)
  }
}

private fun cop(units: Long): Money = Money(units * 100, Moneda.COP)

private fun presupuesto(id: String, monto: Money) =
    Presupuesto(id, "cat_$id", monto, Instant.parse("2026-01-01T00:00:00Z"))

private fun status(id: String, monto: Long, spent: Long, today: LocalDate): BudgetStatus =
    budgetStatus(
        presupuesto(id, cop(monto)),
        id.replaceFirstChar { it.uppercase() },
        null,
        cop(spent),
        today,
    )
