package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.BudgetNotice
import com.kurobello.tallybook.core.model.BudgetState
import com.kurobello.tallybook.core.model.BudgetTouch
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Presupuesto
import com.kurobello.tallybook.core.model.budgetStatus
import com.kurobello.tallybook.core.ui.feedback.ManagementFeedbackKind
import com.kurobello.tallybook.core.ui.feedback.ManagementSubject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ManageMovimientoBudgetNoticeTest {

  @Test
  fun editedGastoMergesNoticeIntoSaved() = runTest {
    val f = fixture()
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onAmountChanged("20.00")

    saveAndHide(f)

    val feedback = assertNotNull(f.feedback.state.value.feedback)
    assertEquals(ManagementSubject.Movimiento, feedback.subject)
    assertEquals(ManagementFeedbackKind.Saved, feedback.kind)
    assertEquals(BudgetNotice("Food", BudgetState.NEAR, usd(300), null), feedback.budget)
    assertEquals(1, f.feedback.state.value.feedbackQueue.size)
    assertEquals(
        listOf(listOf(BudgetTouch("lunch", "food", Moneda.USD, LocalDate(2026, 9, 15)))),
        f.presupuestos.noticeReads,
    )
  }

  @Test
  fun editMovingGastoOutOfBudgetSendsNone() = runTest {
    val f = fixture()
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onCategoriaSelected("fun")

    saveAndHide(f)

    val feedback = assertNotNull(f.feedback.state.value.feedback)
    assertEquals(ManagementFeedbackKind.Saved, feedback.kind)
    assertNull(feedback.budget)
  }

  @Test
  fun failedNoticeReadStillReportsTheSave() = runTest {
    val f = fixture()
    f.presupuestos.noticeFailure = DataError.Local(IllegalStateException())
    f.viewModel.open("movement")
    runCurrent()
    f.viewModel.onAmountChanged("20.00")

    saveAndHide(f)

    val feedback = assertNotNull(f.feedback.state.value.feedback)
    assertEquals(ManagementFeedbackKind.Saved, feedback.kind)
    assertNull(feedback.budget)
  }

  private fun TestScope.fixture(): ManageMovimientoFixture {
    val f = ManageMovimientoFixture(backgroundScope)
    f.categorias.values.value =
        DataOutcome.Success(
            listOf(
                managementCategoria("food"),
                managementCategoria("lunch", parent = "food"),
                managementCategoria("fun"),
            )
        )
    f.repository.current = managementMovimiento().copy(categoriaId = "lunch")
    f.presupuestos.statuses.value =
        DataOutcome.Success(
            listOf(
                budgetStatus(
                    Presupuesto(
                        "food-budget",
                        "food",
                        usd(2_000),
                        Instant.parse("2026-01-01T00:00:00Z"),
                    ),
                    "Food",
                    null,
                    usd(1_700),
                    LocalDate(2026, 9, 15),
                )
            )
        )
    return f
  }

  private fun TestScope.saveAndHide(f: ManageMovimientoFixture) {
    val origin = assertNotNull(f.viewModel.uiState.value.session).token
    f.viewModel.onSubmit()
    runCurrent()
    f.viewModel.onSheetHidden(origin)
  }

  private fun usd(cents: Long) = Money(cents, Moneda.USD)
}
