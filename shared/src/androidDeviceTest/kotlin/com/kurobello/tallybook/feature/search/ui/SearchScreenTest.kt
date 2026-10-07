package com.kurobello.tallybook.feature.search.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.search.viewmodel.SearchDateFailure
import com.kurobello.tallybook.feature.search.viewmodel.SearchDatePreset
import com.kurobello.tallybook.feature.search.viewmodel.SearchFailure
import com.kurobello.tallybook.feature.search.viewmodel.SearchWork
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.retry
import tallybook.shared.generated.resources.search_adjust_filters
import tallybook.shared.generated.resources.search_clear_query
import tallybook.shared.generated.resources.search_count
import tallybook.shared.generated.resources.search_expenses
import tallybook.shared.generated.resources.search_filters
import tallybook.shared.generated.resources.search_invalid_dates
import tallybook.shared.generated.resources.search_last_7_days
import tallybook.shared.generated.resources.search_loading
import tallybook.shared.generated.resources.search_no_data_title
import tallybook.shared.generated.resources.search_no_results_title
import tallybook.shared.generated.resources.search_placeholder
import tallybook.shared.generated.resources.search_remove_filter
import tallybook.shared.generated.resources.search_searching
import tallybook.shared.generated.resources.search_updating
import tallybook.shared.generated.resources.unknown_category

class SearchScreenTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun realMovementRowEmitsItsStoredId() {
    var opened: String? = null
    composeRule.setContent {
      TallybookTheme {
        SearchScreen(
            searchTestState,
            SearchTestActions(),
            {},
            {},
            0.dp,
            onOpenMovimiento = { opened = it },
        )
      }
    }
    composeRule.onNodeWithText("Morning coffee").performScrollTo().performClick()
    composeRule.runOnIdle { assertEquals(searchTestMovement.id, opened) }
  }

  @Test
  fun appliedFilterTagsRemoveOnlyTheirOwnDateTypeOrCategoryDimension() {
    val actions = SearchTestActions()
    val state =
        searchTestState.copy(
            filters =
                searchTestState.filters.copy(
                    preset = SearchDatePreset.Last7Days,
                    tipo = TipoMovimiento.GASTO,
                    categoryIds = setOf("food", "missing"),
                )
        )
    composeRule.setContent { TallybookTheme { SearchScreen(state, actions, {}, {}, 0.dp) } }
    val labels =
        listOf(
            runBlocking { getString(Res.string.search_last_7_days) },
            runBlocking { getString(Res.string.search_expenses) },
            "Food",
            runBlocking { getString(Res.string.unknown_category) },
        )
    labels.forEach { label ->
      val remove = runBlocking { getString(Res.string.search_remove_filter, label) }
      composeRule.onNodeWithContentDescription(remove).performScrollTo().performClick()
    }
    composeRule.runOnIdle {
      assertEquals(
          listOf("remove-date", "remove-type", "remove-category:food", "remove-category:missing"),
          actions.events,
      )
    }
  }

  @Test
  fun pendingResultsShowLoadingWithoutCountOrSearchUpdateWording() {
    val state = mutableStateOf(searchTestState)
    composeRule.setContent {
      TallybookTheme { SearchScreen(state.value, SearchTestActions(), {}, {}, 0.dp) }
    }
    val count = runBlocking { getPluralString(Res.plurals.search_count, 1, 1) }
    val loading = runBlocking { getString(Res.string.search_loading) }
    val pending =
        listOf(
            searchTestState.copy(results = RetainedLoadState.Loading(), work = SearchWork.Reading),
            searchTestState.copy(
                results = RetainedLoadState.Loading(searchTestResults),
                work = SearchWork.Revalidating,
            ),
            searchTestState.copy(
                rawQuery = "tea",
                results = RetainedLoadState.Loading(searchTestResults),
                work = SearchWork.WaitingText,
            ),
        )
    pending.forEach { frame ->
      composeRule.runOnIdle { state.value = frame }
      composeRule.onNodeWithText(loading).assertIsDisplayed()
      composeRule.onNodeWithText(count).assertDoesNotExist()
      composeRule
          .onNodeWithText(runBlocking { getString(Res.string.search_searching) })
          .assertDoesNotExist()
      composeRule
          .onNodeWithText(runBlocking { getString(Res.string.search_updating) })
          .assertDoesNotExist()
    }
    composeRule.runOnIdle { state.value = searchTestState }
    composeRule.onNodeWithText(count).assertIsDisplayed()
    composeRule.onNodeWithText(loading).assertDoesNotExist()
  }

  @Test
  fun settledCountAndRowsAreReadOnlyWhileControlsEmitIsolatedCallbacks() {
    val actions = SearchTestActions()
    var filterRequests = 0
    composeRule.setContent {
      TallybookTheme { SearchScreen(searchTestState, actions, {}, { filterRequests++ }, 0.dp) }
    }
    composeRule
        .onNodeWithText(runBlocking { getPluralString(Res.plurals.search_count, 1, 1) })
        .assertIsDisplayed()
    composeRule.onNodeWithText("Morning coffee").assertHasNoClickAction()
    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.search_placeholder) })
        .performTextInput("tea")
    composeRule
        .onNodeWithContentDescription(runBlocking { getString(Res.string.search_filters) })
        .performClick()
    composeRule.runOnIdle {
      assertEquals(listOf("query:tea"), actions.events)
      assertEquals(1, filterRequests)
    }
  }

  @Test
  fun newerDebouncedTextSuppressesOldCountAndEmptyAssertionsImmediately() {
    val empty =
        searchTestState.copy(
            results = RetainedLoadState.Ready(searchTestResults.copy(rows = emptyList()))
        )
    val state = mutableStateOf(empty)
    composeRule.setContent {
      TallybookTheme { SearchScreen(state.value, SearchTestActions(), {}, {}, 0.dp) }
    }
    val title = runBlocking { getString(Res.string.search_no_results_title) }
    composeRule.onNodeWithText(title).assertIsDisplayed()
    composeRule.runOnIdle {
      state.value = empty.copy(rawQuery = "coffee", work = SearchWork.WaitingText)
    }
    composeRule.onNodeWithText(title).assertDoesNotExist()
    composeRule
        .onNodeWithText(runBlocking { getPluralString(Res.plurals.search_count, 0, 0) })
        .assertDoesNotExist()
    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.search_loading) })
        .assertIsDisplayed()
    composeRule
        .onNodeWithContentDescription(runBlocking { getString(Res.string.search_clear_query) })
        .assertIsDisplayed()
  }

  @Test
  fun preparedQueryWithoutPreviousRowsShowsLoadingWithoutAnEmptyAssertion() {
    val state =
        searchTestState.copy(
            rawQuery = "coffee",
            results = RetainedLoadState.Loading(),
            work = SearchWork.WaitingText,
        )
    composeRule.setContent {
      TallybookTheme { SearchScreen(state, SearchTestActions(), {}, {}, 0.dp) }
    }
    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.search_loading) })
        .assertIsDisplayed()
    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.search_no_results_title) })
        .assertDoesNotExist()
    composeRule.onNodeWithText("Morning coffee").assertDoesNotExist()
  }

  @Test
  fun actualInvalidDateFailureOffersCorrectionInsteadOfGenericRetry() {
    var corrections = 0
    val invalid =
        searchTestState.copy(
            filters =
                searchTestState.filters.copy(
                    preset = SearchDatePreset.Custom,
                    customStart = LocalDate(2027, 1, 1),
                ),
            results =
                RetainedLoadState.Failed(SearchFailure.InvalidDates(SearchDateFailure.FutureDate)),
        )
    composeRule.setContent {
      TallybookTheme { SearchScreen(invalid, SearchTestActions(), {}, { corrections++ }, 0.dp) }
    }
    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.search_invalid_dates) })
        .assertIsDisplayed()
    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.search_adjust_filters) })
        .performClick()
    composeRule.runOnIdle { assertEquals(1, corrections) }
  }

  @Test
  fun invalidDatesKeepSourceRetrySeparateAndEnableCorrectionOnlyAfterRecovery() {
    val failure = DataError.Local(IllegalStateException("Unavailable"))
    val invalid =
        searchTestState.copy(
            filters =
                searchTestState.filters.copy(
                    preset = SearchDatePreset.Custom,
                    customStart = LocalDate(2027, 1, 1),
                ),
            results =
                RetainedLoadState.Failed(SearchFailure.InvalidDates(SearchDateFailure.FutureDate)),
        )
    val state = mutableStateOf(invalid)
    val actions = SearchTestActions()
    var corrections = 0
    composeRule.setContent {
      TallybookTheme { SearchScreen(state.value, actions, {}, { corrections++ }, 0.dp) }
    }
    val retry = runBlocking { getString(Res.string.retry) }
    val adjust = runBlocking { getString(Res.string.search_adjust_filters) }
    composeRule.onNodeWithText(retry).assertDoesNotExist()
    composeRule.onNodeWithText(adjust).assertIsEnabled()

    val failures =
        listOf(
            invalid.copy(preferenceFailure = failure),
            invalid.copy(categories = invalid.categories.copy(failure = failure)),
        )
    failures.forEach { failed ->
      composeRule.runOnIdle { state.value = failed }
      composeRule.onNodeWithText(adjust).assertIsNotEnabled().performClick()
      composeRule.onNodeWithText(retry).assertIsDisplayed().performClick()
      composeRule.runOnIdle { assertEquals(0, corrections) }
    }

    composeRule.runOnIdle { state.value = invalid }
    composeRule.onNodeWithText(retry).assertDoesNotExist()
    composeRule.onNodeWithText(adjust).assertIsEnabled().performClick()
    composeRule.runOnIdle {
      assertEquals(listOf("retry", "retry"), actions.events)
      assertEquals(1, corrections)
    }
  }

  @Test
  fun currencyWithoutAnyHistoryUsesTheDistinctNoDataState() {
    val noData =
        searchTestResults.copy(
            rows = emptyList(),
            years = SearchCurrencyYears(searchTestResults.years.moneda, emptyList()),
        )
    composeRule.setContent {
      TallybookTheme {
        SearchScreen(
            searchTestState.copy(results = RetainedLoadState.Ready(noData)),
            SearchTestActions(),
            {},
            {},
            0.dp,
        )
      }
    }
    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.search_no_data_title) })
        .assertIsDisplayed()
    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.search_no_results_title) })
        .assertDoesNotExist()
  }
}
