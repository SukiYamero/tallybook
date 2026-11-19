package com.kurobello.tallybook.feature.budgets.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kurobello.tallybook.core.model.BudgetStatus
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Presupuesto
import com.kurobello.tallybook.core.model.budgetStatus
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.budgets.viewmodel.BudgetsActions
import com.kurobello.tallybook.feature.budgets.viewmodel.BudgetsContent
import com.kurobello.tallybook.feature.budgets.viewmodel.BudgetsUiState
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.budgets_line_over
import tallybook.shared.generated.resources.budgets_line_per_day
import tallybook.shared.generated.resources.budgets_name_with_moneda
import tallybook.shared.generated.resources.budgets_summary

class BudgetsScreenTest {

  @get:Rule val composeRule = createComposeRule()

  private val today = LocalDate(2026, 11, 20)
  private val over = status("food", "Comida", monto = cop(500_000), spent = cop(520_000))
  private val fine = status("fun", "Ocio", monto = cop(200_000), spent = cop(90_000))
  private val dollars = status("trip", "Viajes", monto = usd(100), spent = usd(10))

  @Test
  fun rowsShowTheirStateLineWithoutGroupHeaders() {
    setScreen(listOf(over, fine))

    composeRule
        .onNodeWithText(text(Res.string.budgets_line_over, cop(20_000).format()), true)
        .assertExists()
    composeRule
        .onNodeWithText(
            text(Res.string.budgets_line_per_day, cop(110_000).format(), cop(10_000).format()),
            true,
        )
        .assertExists()
    composeRule.onNodeWithText(text(Res.string.budgets_summary, 1, 2), true).assertExists()
    composeRule.onAllNodes(isHeading()).assertCountEquals(1)
  }

  @Test
  fun aRowOutsideThePrimaryCurrencyShowsItsCode() {
    setScreen(listOf(fine, dollars))

    composeRule
        .onNodeWithText(text(Res.string.budgets_name_with_moneda, "Viajes", "USD"), true)
        .assertExists()
    composeRule.onNodeWithText("Ocio", true).assertExists()
  }

  @Test
  fun tappingARowOpensItsBudget() {
    val opened = mutableListOf<String>()
    setScreen(listOf(over), onOpen = { opened += it })

    composeRule.onNodeWithText("Comida", true).performClick()

    composeRule.runOnIdle { assertEquals(listOf("food"), opened) }
  }

  private fun setScreen(statuses: List<BudgetStatus>, onOpen: (String) -> Unit = {}) {
    val state =
        BudgetsUiState(
            content =
                RetainedLoadState.Ready(
                    BudgetsContent(
                        statuses = statuses,
                        fineCount = statuses.count { it === fine },
                        daysLeft = 11,
                        month = YearMonth(2026, 11),
                    )
                ),
            primaryMoneda = Moneda.COP,
        )
    composeRule.setContent {
      TallybookTheme { BudgetsScreen(state, RecordingActions(onOpen), onBack = {}) }
    }
  }

  private fun status(id: String, nombre: String, monto: Money, spent: Money): BudgetStatus =
      budgetStatus(
          Presupuesto(id, "cat_$id", monto, Instant.parse("2026-01-01T00:00:00Z")),
          nombre,
          null,
          spent,
          today,
      )

  private fun text(resource: StringResource, vararg args: Any) = runBlocking {
    getString(resource, *args)
  }

  private fun cop(units: Long) = Money(units * 100, Moneda.COP)

  private fun usd(units: Long) = Money(units * 100, Moneda.USD)
}

private class RecordingActions(private val open: (String) -> Unit) : BudgetsActions {
  override fun onCreate() = Unit

  override fun onOpen(id: String) = open(id)

  override fun onCategorySelected(id: String) = Unit

  override fun onMonedaSelected(moneda: Moneda) = Unit

  override fun onAmountChanged(text: String) = Unit

  override fun onUseSuggestion() = Unit

  override fun onSave() = Unit

  override fun onDismissEditor() = Unit

  override fun onEditAmount() = Unit

  override fun onRequestDelete() = Unit

  override fun onCancelDelete() = Unit

  override fun onConfirmDelete() = Unit

  override fun onDismissDetail() = Unit

  override fun onViewMovements() = Unit

  override fun onRetry() = Unit

  override fun onResume() = Unit
}
