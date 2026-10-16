package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.home_expenses
import tallybook.shared.generated.resources.home_income

class BalanceCardLayoutTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun stackedSignedCopAmountsKeepFullLinesAtDefaultAndLargeFont() {
    val fontScale = mutableStateOf(1f)
    val incomeLabel = runBlocking { getString(Res.string.home_income) }
    val expenseLabel = runBlocking { getString(Res.string.home_expenses) }
    composeRule.setContent {
      val density = LocalDensity.current
      CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale.value)) {
        TallybookTheme {
          BalanceCard(
              summary =
                  MovimientoSummary(Money(5_000_000, Moneda.COP), Money(200_000_000, Moneda.COP)),
              modifier = Modifier.width(320.dp).testTag("balance-card"),
              secondaryAmountFormatter = ::signedMovimientoAmountText,
          )
        }
      }
    }

    listOf(1f, 1.3f).forEach { scale ->
      composeRule.runOnIdle { fontScale.value = scale }
      val income = composeRule.onNodeWithText("+$\u00A050.000,00")
      val expense = composeRule.onNodeWithText("-$\u00A02.000.000,00")
      val incomeTitle = composeRule.onNodeWithText(incomeLabel)
      val expenseTitle = composeRule.onNodeWithText(expenseLabel)
      assertFullLine(income)
      assertFullLine(expense)
      assertFullLine(incomeTitle)
      assertFullLine(expenseTitle)
      val card = composeRule.onNodeWithTag("balance-card").fetchSemanticsNode().boundsInRoot
      val incomeBounds = income.fetchSemanticsNode().boundsInRoot
      val expenseBounds = expense.fetchSemanticsNode().boundsInRoot
      assertTrue(incomeBounds.bottom < expenseBounds.top)
      assertEquals(incomeBounds.right, expenseBounds.right, absoluteTolerance = 1f)
      val incomeLabelBounds = incomeTitle.fetchSemanticsNode().boundsInRoot
      val expenseLabelBounds = expenseTitle.fetchSemanticsNode().boundsInRoot
      listOf(incomeBounds, expenseBounds, incomeLabelBounds, expenseLabelBounds).forEach { bounds ->
        assertTrue(bounds.left >= card.left && bounds.right <= card.right)
        assertTrue(bounds.top >= card.top && bounds.bottom <= card.bottom)
      }
      assertTrue(
          incomeLabelBounds.right < incomeBounds.left || incomeLabelBounds.bottom < incomeBounds.top
      )
      assertTrue(
          expenseLabelBounds.right < expenseBounds.left ||
              expenseLabelBounds.bottom < expenseBounds.top
      )
    }
  }

  private fun assertFullLine(node: SemanticsNodeInteraction) {
    val results = mutableListOf<TextLayoutResult>()
    node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(results) }
    val layout = results.single()
    assertEquals(1, layout.lineCount)
    assertFalse(layout.didOverflowWidth)
    assertFalse(layout.didOverflowHeight)
  }
}
