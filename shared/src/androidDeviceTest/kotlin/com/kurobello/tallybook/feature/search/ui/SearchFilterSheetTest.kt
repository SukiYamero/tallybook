package com.kurobello.tallybook.feature.search.ui

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.SearchTipo
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.search.viewmodel.SearchCuentaState
import com.kurobello.tallybook.feature.search.viewmodel.SearchDatePreset
import com.kurobello.tallybook.feature.search.viewmodel.SearchEtiquetaState
import com.kurobello.tallybook.feature.search.viewmodel.SearchFilterSession
import com.kurobello.tallybook.feature.search.viewmodel.SearchFilterSessionId
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.activity_missing_account
import tallybook.shared.generated.resources.calendar_date_content_description
import tallybook.shared.generated.resources.date_picker_months
import tallybook.shared.generated.resources.payment_method_credit
import tallybook.shared.generated.resources.search_accounts_label
import tallybook.shared.generated.resources.search_apply
import tallybook.shared.generated.resources.search_category_label
import tallybook.shared.generated.resources.search_clear_filters
import tallybook.shared.generated.resources.search_last_7_days
import tallybook.shared.generated.resources.search_remove_filter
import tallybook.shared.generated.resources.search_start_date
import tallybook.shared.generated.resources.search_tags_label
import tallybook.shared.generated.resources.search_type_transfers
import tallybook.shared.generated.resources.unknown_category

class SearchFilterSheetTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun modalUsesSeventyTwoPercentOfItsWindowAndKeepsFooterReachable() {
    composeRule.setContent {
      TallybookTheme {
        SearchFilterSheet(
            searchTestState.copy(
                filterSession =
                    SearchFilterSession.Editing(SearchFilterSessionId(1), searchTestState.filters)
            ),
            SearchTestActions(),
            Modifier.testTag("filter-sheet"),
        )
      }
    }
    val window = composeRule.onNode(isDialog()).getUnclippedBoundsInRoot()
    val sheet = composeRule.onNodeWithTag("filter-sheet").getUnclippedBoundsInRoot()
    val windowHeight = window.bottom - window.top
    assertEquals(0.72f, (sheet.bottom - sheet.top) / windowHeight, absoluteTolerance = 0.01f)
    assertEquals(window.bottom.value, sheet.bottom.value, absoluteTolerance = 1f)
    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.search_apply) })
        .assertIsDisplayed()
  }

  @Test
  fun medioAccountAndTransferTypeEditTheDraftAndAMissingAccountCanBeRemoved() {
    val nequi =
        Cuenta(
            "nequi",
            "Nequi",
            TipoCuenta.BANCO,
            Money(0, Moneda.COP),
            null,
            true,
            Instant.parse("2026-01-01T00:00:00Z"),
            color = TipoCuenta.BANCO.defaultColor,
        )
    val savings =
        Cuenta(
            "savings",
            "Savings",
            TipoCuenta.BANCO,
            Money(0, Moneda.USD),
            null,
            false,
            Instant.parse("2026-01-01T00:00:00Z"),
            color = TipoCuenta.BANCO.defaultColor,
        )
    val actions = SearchTestActions()
    composeRule.setContent {
      TallybookTheme {
        SearchFilterSheetContent(
            searchTestState.copy(
                primary = Moneda.COP,
                cuentas = SearchCuentaState(listOf(nequi, savings)),
                filterSession =
                    SearchFilterSession.Editing(
                        SearchFilterSessionId(1),
                        searchTestState.filters.copy(cuentaIds = setOf("gone")),
                    ),
            ),
            actions,
        )
      }
    }
    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.search_type_transfers) })
        .performScrollTo()
        .performClick()
    composeRule.runOnIdle { assertEquals(SearchTipo.TRANSFERENCIA, actions.lastDraft?.tipo) }
    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.payment_method_credit) })
        .performScrollTo()
        .performClick()
    composeRule.runOnIdle { assertEquals(setOf(MedioPago.CREDITO), actions.lastDraft?.medios) }
    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.search_accounts_label) })
        .performScrollTo()
        .assertIsDisplayed()
    composeRule.onNodeWithText("Savings (USD)").performScrollTo().performClick()
    composeRule.runOnIdle { assertEquals(setOf("gone", "savings"), actions.lastDraft?.cuentaIds) }
    composeRule.onNodeWithText("Nequi").assertDoesNotExist()
    val missing = runBlocking { getString(Res.string.activity_missing_account) }
    composeRule
        .onNodeWithContentDescription(
            runBlocking { getString(Res.string.search_remove_filter, missing) }
        )
        .performScrollTo()
        .assertIsSelected()
        .performClick()
    composeRule.runOnIdle { assertEquals(emptySet(), actions.lastDraft?.cuentaIds) }
  }

  @Test
  fun unchangedDraftStillAllowsCountFreeApplyAndClearHasAnIndependentCallback() {
    val actions = SearchTestActions()
    composeRule.setContent {
      TallybookTheme {
        SearchFilterSheetContent(
            searchTestState.copy(
                filterSession =
                    SearchFilterSession.Editing(SearchFilterSessionId(1), searchTestState.filters)
            ),
            actions,
        )
      }
    }
    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.search_apply) })
        .assertIsEnabled()
        .performClick()
    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.search_clear_filters) })
        .performClick()
    composeRule.runOnIdle { assertEquals(listOf("apply-filters", "clear-filters"), actions.events) }
  }

  @Test
  fun closingDraftRejectsPresetCategoryClearAndApplyInteraction() {
    val actions = SearchTestActions()
    composeRule.setContent {
      TallybookTheme {
        SearchFilterSheetContent(
            searchTestState.copy(
                filterSession =
                    SearchFilterSession.Closing(SearchFilterSessionId(1), searchTestState.filters)
            ),
            actions,
        )
      }
    }
    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.search_apply) })
        .assertIsNotEnabled()
    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.search_clear_filters) })
        .assertIsNotEnabled()
    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.search_last_7_days) })
        .assertIsNotEnabled()
    composeRule.onNodeWithText("Food").assertIsNotEnabled()
    composeRule.runOnIdle { assertEquals(emptyList(), actions.events) }
  }

  @Test
  fun tagsSectionFollowsCategoriesAndTogglesOnlyTheDraftTag() {
    val actions = SearchTestActions()
    composeRule.setContent {
      TallybookTheme {
        SearchFilterSheetContent(
            searchTestState.copy(
                filterSession =
                    SearchFilterSession.Editing(SearchFilterSessionId(1), searchTestState.filters)
            ),
            actions,
        )
      }
    }
    val categoriesHeading =
        composeRule
            .onNodeWithText(runBlocking { getString(Res.string.search_category_label) })
            .getUnclippedBoundsInRoot()
    val tagsHeading =
        composeRule
            .onNodeWithText(runBlocking { getString(Res.string.search_tags_label) })
            .performScrollTo()
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
    assertTrue(tagsHeading.top > categoriesHeading.top)

    composeRule.onNodeWithText(searchTestEtiqueta.nombre).performScrollTo().performClick()

    composeRule.runOnIdle {
      assertEquals(setOf(searchTestEtiqueta.id), actions.lastDraft?.etiquetaIds)
      assertEquals(searchTestState.filters.categoryIds, actions.lastDraft?.categoryIds)
    }
  }

  @Test
  fun selectedDraftTagIsSelectedAndHiddenWithoutAnyTag() {
    val selected = searchTestState.filters.copy(etiquetaIds = setOf(searchTestEtiqueta.id))
    val editing =
        mutableStateOf(
            searchTestState.copy(
                filterSession = SearchFilterSession.Editing(SearchFilterSessionId(1), selected)
            )
        )
    composeRule.setContent {
      TallybookTheme { SearchFilterSheetContent(editing.value, SearchTestActions()) }
    }
    composeRule.onNodeWithText(searchTestEtiqueta.nombre).performScrollTo().assertIsSelected()

    editing.value = editing.value.copy(etiquetas = SearchEtiquetaState(emptyList()))

    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.search_tags_label) })
        .assertDoesNotExist()
    composeRule.onNodeWithText(searchTestEtiqueta.nombre).assertDoesNotExist()
  }

  @Test
  fun blankKnownAndMissingSelectedCategoriesUseTheSameFallbackWithoutRawIds() {
    val blank = searchTestCategory.copy(nombre = "")
    val filters = searchTestState.filters.copy(categoryIds = setOf(blank.id, "private-missing-id"))
    composeRule.setContent {
      TallybookTheme {
        SearchFilterSheetContent(
            searchTestState.copy(
                categories = searchTestState.categories.copy(values = listOf(blank)),
                filterSession = SearchFilterSession.Editing(SearchFilterSessionId(1), filters),
            ),
            SearchTestActions(),
        )
      }
    }
    val unknown = runBlocking { getString(Res.string.unknown_category) }
    composeRule.onAllNodesWithText(unknown).assertCountEquals(2)
    composeRule
        .onNodeWithContentDescription(
            runBlocking { getString(Res.string.search_remove_filter, unknown) }
        )
        .assertIsDisplayed()
    composeRule.onNodeWithText("private-missing-id").assertDoesNotExist()
  }

  @Test
  fun popupBackClosesCalendarBeforeCancellingTheOwningFilterSession() {
    val state =
        mutableStateOf(
            searchTestState.copy(
                filterSession =
                    SearchFilterSession.Editing(
                        SearchFilterSessionId(1),
                        searchTestState.filters.copy(preset = SearchDatePreset.Custom),
                    )
            )
        )
    val actions =
        object : SearchTestActions() {
          override fun onCancelFilters(id: SearchFilterSessionId) {
            super.onCancelFilters(id)
            state.value =
                state.value.copy(
                    filterSession = SearchFilterSession.Closing(id, searchTestState.filters)
                )
          }
        }
    composeRule.setContent { TallybookTheme { SearchFilterSheet(state.value, actions) } }
    val fullDate = runBlocking {
      getString(
          Res.string.calendar_date_content_description,
          searchTestToday.day,
          getStringArray(Res.array.date_picker_months)[searchTestToday.month.ordinal],
          searchTestToday.year,
      )
    }
    val fieldLabel = runBlocking { getString(Res.string.search_start_date) } + ": " + fullDate
    composeRule.onNodeWithContentDescription(fieldLabel).performScrollTo().performClick()
    composeRule.onNodeWithContentDescription("2026").assertIsDisplayed()
    InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    composeRule.onNodeWithContentDescription("2026").assertDoesNotExist()
    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.search_apply) })
        .assertIsDisplayed()
    composeRule.runOnIdle { assertEquals(emptyList(), actions.events) }
    InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    composeRule.runOnIdle {
      assertEquals(listOf("cancel-filters"), actions.events.filter { it == "cancel-filters" })
    }
  }

  @Test
  fun compactLargeTextKeepsFooterFixedAndEveryCatalogEntryReachable() {
    val categories =
        List(58) { index ->
          searchTestCategory.copy(
              id = "cat-$index",
              nombre = "Category $index",
              padreId = if (index % 3 == 0) null else "cat-${index - index % 3}",
          )
        }
    composeRule.setContent {
      val density = LocalDensity.current
      CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
        TallybookTheme {
          Box(Modifier.height(360.dp)) {
            SearchFilterSheetContent(
                searchTestState.copy(
                    categories = searchTestState.categories.copy(values = categories),
                    filterSession =
                        SearchFilterSession.Editing(
                            SearchFilterSessionId(1),
                            searchTestState.filters.copy(preset = SearchDatePreset.Last7Days),
                        ),
                ),
                SearchTestActions(),
            )
          }
        }
      }
    }
    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.search_apply) })
        .assertIsDisplayed()
    composeRule.onNodeWithText("Category 57").performScrollTo()
    composeRule.onNodeWithText("Category 57").assertIsDisplayed()
    composeRule
        .onNodeWithText(runBlocking { getString(Res.string.search_apply) })
        .assertIsDisplayed()
  }
}
