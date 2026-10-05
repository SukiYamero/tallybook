package com.kurobello.tallybook.feature.history.ui

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.history.viewmodel.HistoryActions
import com.kurobello.tallybook.feature.history.viewmodel.HistoryFailure
import com.kurobello.tallybook.feature.history.viewmodel.HistoryLoadState
import com.kurobello.tallybook.feature.history.viewmodel.HistoryProjection
import com.kurobello.tallybook.feature.history.viewmodel.HistoryScope
import com.kurobello.tallybook.feature.history.viewmodel.HistorySelection
import com.kurobello.tallybook.feature.history.viewmodel.HistoryUiState
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.calendar_date_content_description
import tallybook.shared.generated.resources.date_chip_value
import tallybook.shared.generated.resources.date_picker_months
import tallybook.shared.generated.resources.expense
import tallybook.shared.generated.resources.history_empty_title
import tallybook.shared.generated.resources.history_movement_count
import tallybook.shared.generated.resources.history_overall_balance
import tallybook.shared.generated.resources.history_overall_error
import tallybook.shared.generated.resources.history_overflow_error
import tallybook.shared.generated.resources.history_period_error
import tallybook.shared.generated.resources.history_scope_month
import tallybook.shared.generated.resources.history_stale
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.navigation_add_movement
import tallybook.shared.generated.resources.navigation_history
import tallybook.shared.generated.resources.retry
import tallybook.shared.generated.resources.unknown_category

class HistoryScreenTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun contentShowsOverallBalanceAndReadOnlyDescriptionRows() {
    val label = runBlocking { getString(Res.string.history_overall_balance) }
    composeRule.setContent { TallybookTheme { HistoryScreen(contentState, NoActions, {}, 0.dp) } }

    composeRule.onNodeWithText(label).assertIsDisplayed()
    composeRule
        .onNodeWithContentDescription(localized(Res.string.navigation_history))
        .performScrollToNode(hasText("Lunch with Ana"))
    composeRule.onNodeWithText("Lunch with Ana").assertIsDisplayed().assertHasNoClickAction()
    val count = runBlocking { getPluralString(Res.plurals.history_movement_count, 1, 1) }
    composeRule.onNodeWithText(count).performScrollTo().assertIsDisplayed()
  }

  @Test
  fun loadingKeepsGlobalStatusAndPeriodControlsWithoutInventingZero() {
    val state =
        contentState.copy(period = HistoryLoadState.Loading(), overall = HistoryLoadState.Loading())
    composeRule.setContent { TallybookTheme { HistoryScreen(state, NoActions, {}, 0.dp) } }
    composeRule.onAllNodesWithText(localized(Res.string.loading))[0].assertIsDisplayed()
    composeRule.onNodeWithText(localized(Res.string.history_scope_month)).assertExists()
    composeRule.onNodeWithText("Lunch with Ana").assertDoesNotExist()
    composeRule.onNodeWithText(summary.balance.format()).assertDoesNotExist()
  }

  @Test
  fun emptyPeriodAddsUsingCallerCallbackAndKeepsOverallBalance() {
    var additions = 0
    val empty =
        HistoryProjection.Content(
            MovimientoSummary(Money(0, Moneda.COP), Money(0, Moneda.COP)),
            emptyList(),
        )
    composeRule.setContent {
      TallybookTheme {
        HistoryScreen(
            contentState.copy(period = HistoryLoadState.Ready(empty)),
            NoActions,
            { additions++ },
            0.dp,
        )
      }
    }
    composeRule
        .onNodeWithText(localized(Res.string.history_empty_title))
        .performScrollTo()
        .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    composeRule
        .onNodeWithText(localized(Res.string.navigation_add_movement))
        .performScrollTo()
        .performClick()
    composeRule.onNodeWithText(localized(Res.string.history_overall_balance)).assertIsDisplayed()
    composeRule.runOnIdle { assertEquals(1, additions) }
  }

  @Test
  fun readFailureOffersRetryWhileRetainingPeriodControls() {
    var retries = 0
    val actions =
        object : HistoryActions by NoActions {
          override fun onRetry() {
            retries++
          }
        }
    val failure = HistoryFailure.Repository(DataError.Local(IllegalStateException()))
    composeRule.setContent {
      TallybookTheme {
        HistoryScreen(
            contentState.copy(period = HistoryLoadState.Failed(failure)),
            actions,
            {},
            0.dp,
        )
      }
    }
    composeRule
        .onNodeWithText(localized(Res.string.history_period_error))
        .performScrollTo()
        .assertIsDisplayed()
    composeRule.onNodeWithText(localized(Res.string.retry)).performScrollTo().performClick()
    composeRule.runOnIdle { assertEquals(1, retries) }
    composeRule.onNodeWithText("Lunch with Ana").assertDoesNotExist()
  }

  @Test
  fun calculationFailureDoesNotPresentEmptyOrZeroTotals() {
    composeRule.setContent {
      TallybookTheme {
        HistoryScreen(
            contentState.copy(period = HistoryLoadState.Failed(HistoryFailure.CalculationOverflow)),
            NoActions,
            {},
            0.dp,
        )
      }
    }
    composeRule
        .onNodeWithText(localized(Res.string.history_overflow_error))
        .performScrollTo()
        .assertIsDisplayed()
    composeRule.onNodeWithText(localized(Res.string.history_empty_title)).assertDoesNotExist()
  }

  @Test
  fun globalFailureRetainsSeparatePeriodContentAndMarksPreviousBalanceStale() {
    val failure = HistoryFailure.Repository(DataError.Local(IllegalStateException()))
    composeRule.setContent {
      TallybookTheme {
        HistoryScreen(
            contentState.copy(overall = HistoryLoadState.Failed(failure, summary)),
            NoActions,
            {},
            0.dp,
        )
      }
    }
    composeRule
        .onNodeWithContentDescription(localized(Res.string.history_overall_error), substring = true)
        .assertIsDisplayed()
    composeRule.onNodeWithText(localized(Res.string.history_stale)).assertIsDisplayed()
    composeRule
        .onNodeWithContentDescription(localized(Res.string.navigation_history))
        .performScrollToNode(hasText("Lunch with Ana"))
    composeRule.onNodeWithText("Lunch with Ana").assertIsDisplayed()
  }

  @Test
  fun scopesAndYearReturnTypedArguments() {
    var selectedScope: HistoryScope? = null
    var selectedYear: Int? = null
    val actions =
        object : HistoryActions by NoActions {
          override fun onScopeChanged(scope: HistoryScope) {
            selectedScope = scope
          }

          override fun onYearSelected(year: Int) {
            selectedYear = year
          }
        }
    val state = contentState.copy(years = contentState.years.copy(persisted = listOf(2025)))
    composeRule.setContent { TallybookTheme { HistoryScreen(state, actions, {}, 0.dp) } }
    composeRule.onNodeWithText(localized(Res.string.history_scope_month)).performClick()
    composeRule.onNodeWithText("2026").performClick()
    composeRule.onNodeWithText("2025").performClick()
    composeRule.runOnIdle {
      assertEquals(HistoryScope.Month, selectedScope)
      assertEquals(2025, selectedYear)
    }
  }

  @Test
  fun longListKeepsGlobalSummaryVisibleAndLastRowAbovePublishedClearance() {
    val rows =
        (1..30).map { index ->
          HistoryMovimientoRowUi(
              movement.copy(id = "row-$index", descripcion = "Item $index"),
              null,
              "Item $index",
              "Unknown category",
          )
        }
    val content = HistoryProjection.Content(summary, listOf(HistoryDayGroupUi(today, rows)))
    var density = 1f
    composeRule.setContent {
      density = LocalDensity.current.density
      TallybookTheme {
        HistoryScreen(
            contentState.copy(period = HistoryLoadState.Ready(content)),
            NoActions,
            {},
            96.dp,
        )
      }
    }
    val list = composeRule.onNodeWithContentDescription(localized(Res.string.navigation_history))
    list.performScrollToNode(hasText("Item 30")).performTouchInput { swipeUp() }
    composeRule.onNodeWithText(localized(Res.string.history_overall_balance)).assertIsDisplayed()
    val rowBottom = composeRule.onNodeWithText("Item 30").fetchSemanticsNode().boundsInRoot.bottom
    val listBottom = list.fetchSemanticsNode().boundsInRoot.bottom
    assertTrue(listBottom - rowBottom >= 96f * density)
  }

  @Test
  fun periodChipReturnsItsCivilRangeAndDateGroupIsAHeading() {
    var selected: MovimientoDateRange? = null
    val actions =
        object : HistoryActions by NoActions {
          override fun onPeriodSelected(period: MovimientoDateRange) {
            selected = period
          }
        }
    val month = runBlocking { getStringArray(Res.array.date_picker_months)[8] }
    val dayLabel = runBlocking {
      getString(Res.string.calendar_date_content_description, 14, month, 2026)
    }
    val groupLabel = runBlocking {
      getString(Res.string.calendar_date_content_description, 15, month, 2026)
    }
    composeRule.setContent { TallybookTheme { HistoryScreen(contentState, actions, {}, 0.dp) } }
    composeRule.onNodeWithContentDescription(dayLabel).performClick()
    composeRule.runOnIdle {
      assertEquals(MovimientoDateRange(LocalDate(2026, 9, 14), LocalDate(2026, 9, 14)), selected)
    }
    composeRule
        .onNodeWithContentDescription(localized(Res.string.navigation_history))
        .performScrollToNode(hasText(groupLabel))
    composeRule
        .onNodeWithText(groupLabel)
        .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
  }

  @Test
  fun compactHeightAndLargeTextKeepFailedPeriodControlsReachable() {
    val failure = HistoryFailure.Repository(DataError.Local(IllegalStateException()))
    val state =
        contentState.copy(
            period = HistoryLoadState.Failed(failure),
            overall = HistoryLoadState.Failed(failure),
        )
    var pixelsPerDp = 1f
    composeRule.setContent {
      val density = LocalDensity.current
      pixelsPerDp = density.density
      CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
        TallybookTheme { HistoryScreen(state, NoActions, {}, 96.dp, Modifier.height(320.dp)) }
      }
    }
    composeRule
        .onNodeWithContentDescription(localized(Res.string.navigation_history))
        .performScrollToNode(hasText(localized(Res.string.history_scope_month)))
    val scope = composeRule.onNodeWithText(localized(Res.string.history_scope_month))
    scope.assertIsDisplayed().performClick()
    val list = composeRule.onNodeWithContentDescription(localized(Res.string.navigation_history))
    assertTrue(
        scope.fetchSemanticsNode().boundsInRoot.bottom <=
            list.fetchSemanticsNode().boundsInRoot.bottom - 96f * pixelsPerDp
    )
  }

  @Test
  fun retainedPeriodRowsRemainVisibleAndExplicitlyStaleDuringReload() {
    composeRule.setContent {
      TallybookTheme {
        HistoryScreen(
            contentState.copy(period = HistoryLoadState.Loading(projection)),
            NoActions,
            {},
            0.dp,
        )
      }
    }
    val list = composeRule.onNodeWithContentDescription(localized(Res.string.navigation_history))
    list.performScrollToNode(hasText(localized(Res.string.history_stale)))
    composeRule.onNodeWithText(localized(Res.string.history_stale)).assertIsDisplayed()
    list.performScrollToNode(hasText("Lunch with Ana"))
    composeRule.onNodeWithText("Lunch with Ana").assertIsDisplayed()
  }

  @Test
  fun fallbackHistoryRowPreservesCivilDateTypeSignedAmountAndCurrencySemantics() {
    val fallback = localized(Res.string.unknown_category)
    val month = runBlocking { getStringArray(Res.array.date_picker_months)[8] }
    val date = runBlocking { getString(Res.string.date_chip_value, 15, month) }
    val type = localized(Res.string.expense)
    val row = HistoryMovimientoRowUi(movement.copy(descripcion = null), null, fallback, fallback)
    composeRule.setContent { TallybookTheme { HistoryRow(row) } }
    composeRule
        .onNodeWithContentDescription("$fallback, $date, $type, $\u00A0-1,00, COP")
        .assertIsDisplayed()
        .assertHasNoClickAction()
  }

  private fun localized(resource: StringResource) = runBlocking { getString(resource) }

  private object NoActions : HistoryActions {
    override fun onScopeChanged(scope: HistoryScope) = Unit

    override fun onYearSelected(year: Int) = Unit

    override fun onPeriodSelected(period: MovimientoDateRange) = Unit

    override fun onRetry() = Unit

    override fun onResume() = Unit

    override fun onPresentationChanged(unknownCategoryLabel: String) = Unit
  }

  private companion object {
    val today = LocalDate(2026, 9, 15)
    val movement =
        Movimiento(
            "row",
            TipoMovimiento.GASTO,
            Money(100, Moneda.COP),
            today,
            "food",
            "Lunch with Ana",
            Instant.parse("2026-09-15T12:00:00Z"),
        )
    val summary = MovimientoSummary(Money(0, Moneda.COP), Money(100, Moneda.COP))
    val projection =
        HistoryProjection.Content(
            summary,
            listOf(
                HistoryDayGroupUi(
                    today,
                    listOf(
                        HistoryMovimientoRowUi(movement, null, "Lunch with Ana", "Unknown category")
                    ),
                )
            ),
        )
    val contentState =
        HistoryUiState(
            HistorySelection(HistoryScope.Day, today),
            today,
            Moneda.COP,
            HistoryLoadState.Ready(projection),
            HistoryLoadState.Ready(summary),
        )
  }
}
