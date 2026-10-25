package com.kurobello.tallybook.core.ui.chart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class ChartComponentsTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun theDonutIsOneNodeThatReadsItsSummary() {
    composeRule.setContent {
      TallybookTheme {
        TallybookDonut(
            slices = listOf(DonutSlice(6_000, Color.Red), DonutSlice(4_000, Color.Blue)),
            contentDescription = SUMMARY,
        ) {
          BasicText(CENTER)
        }
      }
    }

    // The unmerged tree still lists cleared descendants; accessibility reads the merged one.
    composeRule.onNodeWithContentDescription(SUMMARY).assertIsDisplayed().assertHasNoChildren()
    composeRule.onNodeWithText(CENTER).assertDoesNotExist()
  }

  @Test
  fun theTrendChartExposesTheSelectedMonthAndSelectsByTap() {
    val selected = mutableListOf<Int>()
    composeRule.setContent {
      TallybookTheme {
        TallybookTrendChart(
            points = trendPoints,
            selectedIndex = trendPoints.lastIndex,
            onSelect = { selected += it },
            contentDescription = SUMMARY,
            modifier = Modifier.fillMaxWidth(),
        )
      }
    }

    composeRule
        .onNodeWithContentDescription(SUMMARY)
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, MONTHS.last()))
        .performTouchInput { click(Offset(width * TAPPED_SLOT_CENTER / MONTHS.size, centerY)) }

    composeRule.runOnIdle { assertEquals(listOf(TAPPED_SLOT), selected) }
  }

  @Test
  fun everyTrendMonthIsAnAccessibleActionThatSelectsIt() {
    val selected = mutableListOf<Int>()
    composeRule.setContent {
      TallybookTheme {
        TallybookTrendChart(
            points = trendPoints,
            selectedIndex = 0,
            onSelect = { selected += it },
            contentDescription = SUMMARY,
        )
      }
    }

    val actions =
        composeRule
            .onNodeWithContentDescription(SUMMARY)
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
    assertEquals(MONTHS, actions.map { it.label })
    composeRule.runOnIdle { actions[TAPPED_SLOT].action() }

    composeRule.runOnIdle { assertEquals(listOf(TAPPED_SLOT), selected) }
  }

  @Test
  fun thePaceAndCategoryChartsReadTheirSummaries() {
    composeRule.setContent {
      TallybookTheme {
        Column {
          TallybookPaceChart(
              cumulative = listOf(10.0, 25.0, 40.0),
              averageMonth = 300.0,
              projection = 400.0,
              daysInMonth = 30,
              todayLabel = CENTER,
              contentDescription = PACE_SUMMARY,
          )
          TallybookCategoryTrendChart(
              values = listOf(0.0, 0.0, 0.0),
              labels = MONTHS.take(3),
              average = 0.0,
              partialLast = true,
              color = Color.Red,
              contentDescription = CATEGORY_SUMMARY,
          )
        }
      }
    }

    composeRule.onNodeWithContentDescription(PACE_SUMMARY).assertIsDisplayed().assertHasNoChildren()
    composeRule
        .onNodeWithContentDescription(CATEGORY_SUMMARY)
        .assertIsDisplayed()
        .assertHasNoChildren()
    composeRule.onNodeWithText(CENTER).assertDoesNotExist()
    composeRule.onNodeWithText(MONTHS.first()).assertDoesNotExist()
  }

  @Test
  fun aDistributionRowReportsItsClick() {
    var clicks = 0
    composeRule.setContent {
      TallybookTheme {
        TallybookDistributionRow(
            label = LABEL,
            amount = AMOUNT,
            share = SHARE,
            fraction = HALF,
            color = Color.Red,
            change = ChangeIndicator(CHANGE, ChangeTone.Unfavorable),
            onClick = { clicks++ },
        )
      }
    }

    composeRule.onNodeWithText(LABEL).performClick()

    composeRule.runOnIdle { assertEquals(1, clicks) }
  }

  @Test
  fun emptyAndFullFractionsKeepTheRowLayout() {
    composeRule.setContent {
      TallybookTheme {
        Column(Modifier.fillMaxWidth()) {
          listOf(0f, 1f, Float.NaN).forEachIndexed { index, fraction ->
            TallybookDistributionRow(
                label = LABEL,
                amount = AMOUNT,
                share = SHARE,
                fraction = fraction,
                color = Color.Red,
                modifier = Modifier.fillMaxWidth().testTag(rowTag(index)),
            )
          }
        }
      }
    }

    val rootWidth = composeRule.onRoot().getBoundsInRoot().width
    val bounds = List(ROW_COUNT) { composeRule.onNodeWithTag(rowTag(it)).getBoundsInRoot() }
    bounds.forEach { row ->
      assertEquals(rootWidth.value, row.width.value, LAYOUT_TOLERANCE_DP)
      assertEquals(bounds.first().height.value, row.height.value, LAYOUT_TOLERANCE_DP)
    }
  }

  private fun rowTag(index: Int) = "distribution-row-$index"

  private fun SemanticsNodeInteraction.assertHasNoChildren() {
    onChildren().assertCountEquals(0)
  }

  private companion object {
    const val SUMMARY = "Gastos e ingresos de mayo a octubre"
    const val PACE_SUMMARY = "Gasto acumulado contra el promedio"
    const val CATEGORY_SUMMARY = "Gasto de la categoría por mes"
    const val CENTER = "Hoy"
    const val LABEL = "Comida"
    const val AMOUNT = "$ 180.700"
    const val SHARE = "30%"
    const val CHANGE = "▲ 18%"
    const val HALF = 0.5f
    /** Pixel bounds converted to dp differ in the last float digits between rows. */
    const val LAYOUT_TOLERANCE_DP = 0.5f
    const val ROW_COUNT = 3
    const val TAPPED_SLOT = 2
    const val TAPPED_SLOT_CENTER = TAPPED_SLOT + 0.5f
    val MONTHS = listOf("May", "Jun", "Jul", "Ago", "Sep", "Oct")
    val trendPoints = MONTHS.mapIndexed { index, label ->
      TrendPoint(
          label = label,
          gastos = 600.0 + index,
          ingresos = 1_000.0,
          partial = index == MONTHS.lastIndex,
      )
    }
  }
}
