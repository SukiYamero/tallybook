package com.kurobello.tallybook.feature.history.ui

import android.graphics.Bitmap
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.history.viewmodel.HistoryActions
import com.kurobello.tallybook.feature.history.viewmodel.HistoryFailure
import com.kurobello.tallybook.feature.history.viewmodel.HistoryProjection
import com.kurobello.tallybook.feature.history.viewmodel.HistoryScope
import com.kurobello.tallybook.feature.history.viewmodel.HistorySelection
import com.kurobello.tallybook.feature.history.viewmodel.HistoryUiState
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.calendar_date_content_description
import tallybook.shared.generated.resources.date_picker_months
import tallybook.shared.generated.resources.date_picker_weekday_names
import tallybook.shared.generated.resources.expense
import tallybook.shared.generated.resources.history_day_title
import tallybook.shared.generated.resources.history_empty_title
import tallybook.shared.generated.resources.history_movement_count
import tallybook.shared.generated.resources.history_overflow_error
import tallybook.shared.generated.resources.history_period_balance
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
  fun realMovementRowEmitsItsStoredId() {
    var opened: String? = null
    composeRule.setContent {
      TallybookTheme {
        HistoryScreen(
            contentState,
            NoActions,
            HistoryNavigationCallbacks({}, onOpenMovimiento = { opened = it }),
            0.dp,
        )
      }
    }
    composeRule
        .onNodeWithContentDescription(localized(Res.string.navigation_history))
        .performScrollToNode(hasText("Lunch with Ana"))
    composeRule.onNodeWithText("Lunch with Ana").performClick()
    composeRule.runOnIdle { assertEquals(movement.id, opened) }
  }

  @Test
  fun contentShowsPeriodSummaryAndReadOnlyDescriptionRowsWithoutOverallHeader() {
    composeRule.setContent {
      TallybookTheme {
        HistoryScreen(contentState, NoActions, HistoryNavigationCallbacks({}), 0.dp)
      }
    }

    composeRule.onNodeWithText("Overall balance").assertDoesNotExist()
    composeRule.onNodeWithText("Balance general").assertDoesNotExist()
    composeRule.onNodeWithText("Saldo geral").assertDoesNotExist()
    composeRule.onNodeWithText(localized(Res.string.history_period_balance)).assertExists()
    composeRule
        .onNodeWithContentDescription(localized(Res.string.navigation_history))
        .performScrollToNode(hasText("Lunch with Ana"))
    composeRule.onNodeWithText("Lunch with Ana").assertIsDisplayed().assertHasNoClickAction()
    val count = runBlocking { getPluralString(Res.plurals.history_movement_count, 1, 1) }
    composeRule.onNodeWithText(count).performScrollTo().assertIsDisplayed()
  }

  @Test
  fun loadingKeepsPeriodControlsWithoutInventingZero() {
    val state = contentState.copy(period = RetainedLoadState.Loading())
    composeRule.setContent {
      TallybookTheme { HistoryScreen(state, NoActions, HistoryNavigationCallbacks({}), 0.dp) }
    }
    composeRule.mainClock.autoAdvance = false
    composeRule.onNodeWithContentDescription(localized(Res.string.loading)).assertDoesNotExist()
    composeRule.mainClock.advanceTimeBy(240)
    composeRule.onNodeWithContentDescription(localized(Res.string.loading)).assertIsDisplayed()
    composeRule.onNodeWithText(localized(Res.string.history_scope_month)).assertExists()
    composeRule.onNodeWithText("Lunch with Ana").assertDoesNotExist()
    composeRule.onNodeWithText(summary.balance.format()).assertDoesNotExist()
  }

  @Test
  fun quickPeriodReadKeepsItsOriginalDateAndContentUntilNewResultArrives() {
    val state = mutableStateOf(contentState)
    composeRule.setContent {
      TallybookTheme { HistoryScreen(state.value, NoActions, HistoryNavigationCallbacks({}), 0.dp) }
    }
    composeRule.mainClock.autoAdvance = false
    val nextSelection = HistorySelection(HistoryScope.Day, LocalDate(2026, 9, 14))
    composeRule.runOnIdle {
      state.value =
          contentState.copy(selection = nextSelection, period = RetainedLoadState.Loading())
    }
    composeRule.mainClock.advanceTimeBy(96)
    val weekdays = runBlocking { getStringArray(Res.array.date_picker_weekday_names) }
    val originalTitle = runBlocking {
      getString(Res.string.history_day_title, weekdays[today.dayOfWeek.ordinal], today.day)
          .replaceFirstChar(Char::titlecase)
    }
    val pendingTitle = runBlocking {
      getString(
              Res.string.history_day_title,
              weekdays[nextSelection.anchor.dayOfWeek.ordinal],
              nextSelection.anchor.day,
          )
          .replaceFirstChar(Char::titlecase)
    }
    composeRule.onNodeWithText(originalTitle).assertExists()
    composeRule.onNodeWithText(pendingTitle).assertDoesNotExist()
    composeRule.onNodeWithText(summary.balance.format()).assertExists()
    composeRule.onNodeWithText("Lunch with Ana").assertExists()
    composeRule.onNodeWithContentDescription(localized(Res.string.loading)).assertDoesNotExist()
    val empty =
        HistoryProjection.Content(
            MovimientoSummary(Money(0, Moneda.COP), Money(0, Moneda.COP)),
            emptyList(),
        )
    composeRule.runOnIdle {
      state.value = state.value.copy(period = RetainedLoadState.Ready(empty))
    }
    composeRule.mainClock.advanceTimeBy(32)
    composeRule.onNodeWithText("Lunch with Ana").assertDoesNotExist()
    composeRule.onNodeWithText(localized(Res.string.history_empty_title)).assertExists()
    composeRule.mainClock.advanceTimeBy(240)
    composeRule.onNodeWithContentDescription(localized(Res.string.loading)).assertDoesNotExist()
  }

  @Test
  fun slowPeriodReadShowsSkeletonAndErrorsInterruptTheWait() {
    val state = mutableStateOf(contentState)
    composeRule.setContent {
      TallybookTheme { HistoryScreen(state.value, NoActions, HistoryNavigationCallbacks({}), 0.dp) }
    }
    composeRule.mainClock.autoAdvance = false
    composeRule.runOnIdle {
      state.value =
          contentState.copy(
              selection = HistorySelection(HistoryScope.Week, today),
              period = RetainedLoadState.Loading(),
          )
    }
    composeRule.mainClock.advanceTimeBy(240)
    composeRule.onNodeWithContentDescription(localized(Res.string.loading)).assertIsDisplayed()
    composeRule.onNodeWithText(summary.balance.format()).assertDoesNotExist()
    composeRule.onNodeWithText("Lunch with Ana").assertDoesNotExist()
    composeRule.onNodeWithText("$ 000.000").assertDoesNotExist()
    val screenshot =
        File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "history-skeleton.png",
        )
    screenshot.outputStream().use {
      composeRule
          .onNodeWithContentDescription(localized(Res.string.navigation_history))
          .captureToImage()
          .asAndroidBitmap()
          .compress(Bitmap.CompressFormat.PNG, 100, it)
    }
    composeRule.runOnIdle {
      state.value =
          state.value.copy(period = RetainedLoadState.Failed(HistoryFailure.CalculationOverflow))
    }
    composeRule.mainClock.advanceTimeBy(32)
    composeRule.onNodeWithText(localized(Res.string.history_overflow_error)).assertExists()
    composeRule.onNodeWithContentDescription(localized(Res.string.loading)).assertDoesNotExist()
    composeRule.runOnIdle { state.value = state.value.copy(period = RetainedLoadState.Loading()) }
    composeRule.mainClock.advanceTimeBy(32)
    composeRule.runOnIdle {
      state.value =
          state.value.copy(period = RetainedLoadState.Failed(HistoryFailure.CalculationOverflow))
    }
    composeRule.mainClock.advanceTimeBy(240)
    composeRule.onNodeWithText(localized(Res.string.history_overflow_error)).assertExists()
    composeRule.onNodeWithContentDescription(localized(Res.string.loading)).assertDoesNotExist()
  }

  @Test
  fun emptyPeriodAddsUsingCallerCallback() {
    var additions = 0
    val empty =
        HistoryProjection.Content(
            MovimientoSummary(Money(0, Moneda.COP), Money(0, Moneda.COP)),
            emptyList(),
        )
    composeRule.setContent {
      TallybookTheme {
        HistoryScreen(
            contentState.copy(period = RetainedLoadState.Ready(empty)),
            NoActions,
            HistoryNavigationCallbacks({ additions++ }),
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
            contentState.copy(period = RetainedLoadState.Failed(failure)),
            actions,
            HistoryNavigationCallbacks({}),
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
            contentState.copy(
                period = RetainedLoadState.Failed(HistoryFailure.CalculationOverflow)
            ),
            NoActions,
            HistoryNavigationCallbacks({}),
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
    composeRule.setContent {
      TallybookTheme { HistoryScreen(state, actions, HistoryNavigationCallbacks({}), 0.dp) }
    }
    composeRule.onNodeWithText(localized(Res.string.history_scope_month)).performClick()
    composeRule.onNodeWithText("2026").performClick()
    composeRule.onNodeWithText("2025").performClick()
    composeRule.runOnIdle {
      assertEquals(HistoryScope.Month, selectedScope)
      assertEquals(2025, selectedYear)
    }
  }

  @Test
  fun longListKeepsLastRowAbovePublishedClearance() {
    val rows =
        (1..30).map { index ->
          HistoryMovimientoRowUi(
              movement.copy(id = "row-$index", titulo = "Item $index"),
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
            contentState.copy(period = RetainedLoadState.Ready(content)),
            NoActions,
            HistoryNavigationCallbacks({}),
            96.dp,
        )
      }
    }
    val list = composeRule.onNodeWithContentDescription(localized(Res.string.navigation_history))
    list.performScrollToNode(hasText("Item 30")).performTouchInput { swipeUp() }
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
    composeRule.setContent {
      TallybookTheme { HistoryScreen(contentState, actions, HistoryNavigationCallbacks({}), 0.dp) }
    }
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
            period = RetainedLoadState.Failed(failure),
        )
    var selectedScope: HistoryScope? = null
    val actions =
        object : HistoryActions by NoActions {
          override fun onScopeChanged(scope: HistoryScope) {
            selectedScope = scope
          }
        }
    var pixelsPerDp = 1f
    composeRule.setContent {
      val density = LocalDensity.current
      pixelsPerDp = density.density
      CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
        TallybookTheme {
          Box(Modifier.height(320.dp).testTag("history-compact-viewport")) {
            HistoryScreen(
                state,
                actions,
                HistoryNavigationCallbacks({}),
                96.dp,
                Modifier.fillMaxSize(),
            )
          }
        }
      }
    }
    composeRule
        .onNodeWithContentDescription(localized(Res.string.navigation_history))
        .performScrollToNode(hasText(localized(Res.string.history_scope_month)))
    val scope = composeRule.onNodeWithText(localized(Res.string.history_scope_month))
    val list = composeRule.onNodeWithContentDescription(localized(Res.string.navigation_history))
    val targetBounds = scope.getUnclippedBoundsInRoot()
    val listBounds = list.getUnclippedBoundsInRoot()
    val screenshot =
        File(
            requireNotNull(
                InstrumentationRegistry.getInstrumentation()
                    .targetContext
                    .getExternalFilesDir("history-runtime")
            ),
            "history-compact-before.png",
        )
    screenshot.outputStream().use {
      composeRule
          .onNodeWithTag("history-compact-viewport")
          .captureToImage()
          .asAndroidBitmap()
          .compress(Bitmap.CompressFormat.PNG, 100, it)
    }
    val diagnostic =
        JSONObject()
            .put("phase", "before")
            .put("targetTop", targetBounds.top.value * pixelsPerDp)
            .put("targetBottom", targetBounds.bottom.value * pixelsPerDp)
            .put("targetHeight", (targetBounds.bottom.value - targetBounds.top.value) * pixelsPerDp)
            .put("listTop", listBounds.top.value * pixelsPerDp)
            .put("listBottom", listBounds.bottom.value * pixelsPerDp)
            .put("listHeight", (listBounds.bottom.value - listBounds.top.value) * pixelsPerDp)
            .put("clearancePixels", 96f * pixelsPerDp)
            .put(
                "scroll",
                list
                    .fetchSemanticsNode()
                    .config[SemanticsProperties.VerticalScrollAxisRange]
                    .value(),
            )
            .put("screenshot", screenshot.absolutePath)
    InstrumentationRegistry.getInstrumentation()
        .sendStatus(2, Bundle().apply { putString("history_diagnostic", diagnostic.toString()) })
    val usableHeight = listBounds.bottom.value - listBounds.top.value - 96f
    assertTrue(
        targetBounds.bottom.value - targetBounds.top.value <= usableHeight,
        diagnostic.toString(),
    )
    val occludedPixels =
        (targetBounds.bottom.value - listBounds.bottom.value + 96f).coerceAtLeast(0f) * pixelsPerDp
    assertTrue(occludedPixels <= (listBounds.bottom.value - listBounds.top.value) * pixelsPerDp)
    list.performSemanticsAction(SemanticsActions.ScrollBy) { scroll ->
      assertTrue(scroll(0f, occludedPixels))
    }
    composeRule.waitForIdle()
    val revealedTarget = scope.getUnclippedBoundsInRoot()
    val revealedList = list.getUnclippedBoundsInRoot()
    val afterScreenshot = File(screenshot.parentFile, "history-compact-after.png")
    afterScreenshot.outputStream().use {
      composeRule
          .onNodeWithTag("history-compact-viewport")
          .captureToImage()
          .asAndroidBitmap()
          .compress(Bitmap.CompressFormat.PNG, 100, it)
    }
    val afterDiagnostic =
        JSONObject(diagnostic.toString())
            .put("phase", "after")
            .put("requestedScrollPixels", occludedPixels)
            .put("targetTop", revealedTarget.top.value * pixelsPerDp)
            .put("targetBottom", revealedTarget.bottom.value * pixelsPerDp)
            .put(
                "targetHeight",
                (revealedTarget.bottom.value - revealedTarget.top.value) * pixelsPerDp,
            )
            .put("listTop", revealedList.top.value * pixelsPerDp)
            .put("listBottom", revealedList.bottom.value * pixelsPerDp)
            .put(
                "scroll",
                list
                    .fetchSemanticsNode()
                    .config[SemanticsProperties.VerticalScrollAxisRange]
                    .value(),
            )
            .put(
                "scrollMax",
                list
                    .fetchSemanticsNode()
                    .config[SemanticsProperties.VerticalScrollAxisRange]
                    .maxValue()
                    .toString(),
            )
            .put("screenshot", afterScreenshot.absolutePath)
    InstrumentationRegistry.getInstrumentation()
        .sendStatus(
            2,
            Bundle().apply { putString("history_diagnostic", afterDiagnostic.toString()) },
        )
    assertTrue(
        revealedTarget.top >= revealedList.top &&
            revealedTarget.bottom.value <= revealedList.bottom.value - 96f,
        afterDiagnostic.toString(),
    )
    scope.assertIsDisplayed().performClick()
    composeRule.runOnIdle { assertEquals(HistoryScope.Month, selectedScope) }
  }

  @Test
  fun retainedPeriodRowsRemainVisibleWithoutInitialFeedbackDuringReload() {
    composeRule.setContent {
      TallybookTheme {
        HistoryScreen(
            contentState.copy(period = RetainedLoadState.Loading(projection)),
            NoActions,
            HistoryNavigationCallbacks({}),
            0.dp,
        )
      }
    }
    composeRule.mainClock.advanceTimeBy(240)
    val list = composeRule.onNodeWithContentDescription(localized(Res.string.navigation_history))
    composeRule.onNodeWithText(localized(Res.string.history_stale)).assertDoesNotExist()
    composeRule.onNodeWithContentDescription(localized(Res.string.loading)).assertDoesNotExist()
    list.performScrollToNode(hasText("Lunch with Ana"))
    composeRule.onNodeWithText("Lunch with Ana").assertIsDisplayed()
  }

  @Test
  fun fallbackHistoryRowPreservesTypeSignedAmountAndCurrencySemantics() {
    val fallback = localized(Res.string.unknown_category)
    val type = localized(Res.string.expense)
    val row = HistoryMovimientoRowUi(movement.copy(titulo = null), null, fallback, fallback)
    composeRule.setContent { TallybookTheme { HistoryRow(row) } }
    composeRule
        .onNodeWithContentDescription("$fallback, $type, -$\u00A01,00, COP")
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
            null,
            Instant.parse("2026-09-15T12:00:00Z"),
            Cuenta.defaultCashId(Moneda.COP),
            MedioPago.EFECTIVO,
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
            RetainedLoadState.Ready(projection),
        )
  }
}
