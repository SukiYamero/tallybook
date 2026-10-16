package com.kurobello.tallybook.feature.history.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.history.viewmodel.HistoryFailure
import com.kurobello.tallybook.feature.history.viewmodel.HistoryProjection
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.history_overflow_error
import tallybook.shared.generated.resources.history_period_balance
import tallybook.shared.generated.resources.history_stale
import tallybook.shared.generated.resources.home_balance
import tallybook.shared.generated.resources.retry

class HistorySummaryTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun periodSummaryPreservesSignsRetainedTotalsAndRetryWithoutInventingMissingTotals() {
    val content =
        HistoryProjection.Content(
            MovimientoSummary(Money(1_000_000, Moneda.COP), Money(250_000, Moneda.COP)),
            emptyList(),
        )
    val period =
        mutableStateOf<RetainedLoadState<HistoryProjection.Content, HistoryFailure>>(
            RetainedLoadState.Ready(content)
        )
    var retries = 0
    val periodLabel = runBlocking { getString(Res.string.history_period_balance) }
    val homeLabel = runBlocking { getString(Res.string.home_balance) }
    val error = runBlocking { getString(Res.string.history_overflow_error) }
    val stale = runBlocking { getString(Res.string.history_stale) }
    val retry = runBlocking { getString(Res.string.retry) }
    composeRule.setContent { TallybookTheme { HistoryPeriodSummary(period.value, { retries++ }) } }

    composeRule.onNodeWithText(periodLabel).assertExists()
    composeRule.onNodeWithText(homeLabel).assertDoesNotExist()
    composeRule.onNodeWithText("+$\u00A010.000,00").assertExists()
    composeRule.onNodeWithText("-$\u00A02.500,00").assertExists()
    composeRule.onNodeWithText("$\u00A07.500,00").assertExists()
    composeRule.onNodeWithText("$\u00A010.000,00").assertDoesNotExist()
    composeRule.onNodeWithText("$\u00A02.500,00").assertDoesNotExist()

    composeRule.runOnIdle {
      period.value = RetainedLoadState.Failed(HistoryFailure.CalculationOverflow, content)
    }
    composeRule.onNodeWithText("+$\u00A010.000,00").assertExists()
    composeRule.onNodeWithText("-$\u00A02.500,00").assertExists()
    composeRule.onNodeWithText("$\u00A07.500,00").assertExists()
    composeRule.onNodeWithText(error).assertExists()
    composeRule.onNodeWithText(stale).assertExists()
    composeRule.onNodeWithText(retry).performClick()
    composeRule.runOnIdle { assertEquals(1, retries) }

    composeRule.runOnIdle {
      period.value = RetainedLoadState.Failed(HistoryFailure.CalculationOverflow)
    }
    composeRule.onNodeWithText(periodLabel).assertExists()
    composeRule.onNodeWithText(error).assertExists()
    composeRule.onNodeWithText("+$\u00A010.000,00").assertDoesNotExist()
    composeRule.onNodeWithText("-$\u00A02.500,00").assertDoesNotExist()
    composeRule.onNodeWithText("$\u00A07.500,00").assertDoesNotExist()
    composeRule.onNodeWithText(Money(0, Moneda.COP).format()).assertDoesNotExist()
    composeRule.onNodeWithText(stale).assertDoesNotExist()

    composeRule.runOnIdle { period.value = RetainedLoadState.Ready(content) }
    composeRule.onNodeWithText("$\u00A07.500,00").assertExists()
    composeRule.onNodeWithText(error).assertDoesNotExist()
    composeRule.onNodeWithText(stale).assertDoesNotExist()
    composeRule.onNodeWithText(retry).assertDoesNotExist()
  }
}
