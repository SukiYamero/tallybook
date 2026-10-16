package com.kurobello.tallybook.core.ui.catalog

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category
import tallybook.shared.generated.resources.category_comida
import tallybook.shared.generated.resources.category_required
import tallybook.shared.generated.resources.category_transporte
import tallybook.shared.generated.resources.expense
import tallybook.shared.generated.resources.home_search_placeholder
import tallybook.shared.generated.resources.income
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.navigation_add_movement
import tallybook.shared.generated.resources.navigation_history
import tallybook.shared.generated.resources.navigation_home
import tallybook.shared.generated.resources.navigation_profile
import tallybook.shared.generated.resources.navigation_search
import tallybook.shared.generated.resources.tag_picker_title
import tallybook.shared.generated.resources.ui_catalog_actions
import tallybook.shared.generated.resources.ui_catalog_amount_inputs
import tallybook.shared.generated.resources.ui_catalog_category_field
import tallybook.shared.generated.resources.ui_catalog_choice_pills
import tallybook.shared.generated.resources.ui_catalog_clear_search
import tallybook.shared.generated.resources.ui_catalog_dark
import tallybook.shared.generated.resources.ui_catalog_data_display
import tallybook.shared.generated.resources.ui_catalog_date_pickers
import tallybook.shared.generated.resources.ui_catalog_disabled_icon_button
import tallybook.shared.generated.resources.ui_catalog_filter_tags
import tallybook.shared.generated.resources.ui_catalog_foundations
import tallybook.shared.generated.resources.ui_catalog_icon_buttons
import tallybook.shared.generated.resources.ui_catalog_loading_states
import tallybook.shared.generated.resources.ui_catalog_open_picker
import tallybook.shared.generated.resources.ui_catalog_period_day
import tallybook.shared.generated.resources.ui_catalog_period_pickers
import tallybook.shared.generated.resources.ui_catalog_sample_body_emphasis
import tallybook.shared.generated.resources.ui_catalog_sample_body_small
import tallybook.shared.generated.resources.ui_catalog_sample_caption
import tallybook.shared.generated.resources.ui_catalog_sample_label
import tallybook.shared.generated.resources.ui_catalog_sample_text
import tallybook.shared.generated.resources.ui_catalog_search_fields
import tallybook.shared.generated.resources.ui_catalog_segmented_controls
import tallybook.shared.generated.resources.ui_catalog_selection_fields
import tallybook.shared.generated.resources.ui_catalog_tag_chips
import tallybook.shared.generated.resources.ui_catalog_text_field_label
import tallybook.shared.generated.resources.ui_catalog_text_fields

class UiCatalogScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun catalogDisplaysCurrentComponentFamilies() {
    composeRule.setContent { UiCatalogRoute() }

    listOf(
            CatalogSection("foundations", Res.string.ui_catalog_foundations),
            CatalogSection("actions", Res.string.ui_catalog_actions),
            CatalogSection("data-display", Res.string.ui_catalog_data_display),
        )
        .forEach { section ->
          scrollTo(section.key)
          composeRule.onNodeWithText(localizedString(section.title)).assertIsDisplayed()
        }
    scrollTo("loading-states")
    composeRule
        .onNodeWithContentDescription(localizedString(Res.string.loading))
        .assertIsDisplayed()
  }

  @Test
  fun catalogChoicePillsShowSelectedUnselectedAndDisabledStates() {
    composeRule.setContent { UiCatalogRoute() }
    scrollTo("choice-pills")
    composeRule
        .onNodeWithText(localizedString(Res.string.ui_catalog_choice_pills))
        .assertIsDisplayed()
    val pills =
        composeRule.onAllNodes(
            hasText(localizedString(Res.string.ui_catalog_period_day)) and
                hasAnyAncestor(hasTestTag(catalogSectionAutomationTag("choice-pills")))
        )
    val selected = SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)
    val unselected = SemanticsMatcher.expectValue(SemanticsProperties.Selected, false)
    pills.filter(selected and isEnabled()).assertCountEquals(1)
    pills.filter(unselected and isEnabled()).assertCountEquals(1)
    pills.filter(!isEnabled()).assertCountEquals(1)
  }

  @Test
  fun catalogFilterTagsShowNormalTintedAndDisabledButtonsWithoutSelectedState() {
    composeRule.setContent { UiCatalogRoute() }
    scrollTo("filter-tags")
    composeRule
        .onNodeWithText(localizedString(Res.string.ui_catalog_filter_tags))
        .assertIsDisplayed()
    val tags =
        composeRule.onAllNodes(
            hasText(localizedString(Res.string.category_comida)) and
                hasAnyAncestor(hasTestTag(catalogSectionAutomationTag("filter-tags")))
        )
    tags
        .filter(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        .assertCountEquals(3)
    tags.filter(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected)).assertCountEquals(3)
    tags.filter(isEnabled()).assertCountEquals(2)
    tags.filter(!isEnabled()).assertCountEquals(1)
  }

  @Test
  fun catalogCategoryFieldSuggestionSwitchesTheOneSelectedCategory() {
    composeRule.setContent { UiCatalogRoute() }
    scrollTo("category-single-field")
    composeRule
        .onNodeWithText(localizedString(Res.string.ui_catalog_category_field))
        .assertIsDisplayed()
    val transport =
        composeRule.onAllNodes(
            hasText(localizedString(Res.string.category_transporte)) and
                hasAnyAncestor(hasTestTag(catalogSectionAutomationTag("category-single-field")))
        )
    val selected = SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)
    transport.filter(selected).assertCountEquals(0)

    transport[0].performClick()

    transport.filter(selected).assertCountEquals(1)
  }

  @Test
  fun catalogTagSectionsShowTheTagFamiliesAndOpenThePicker() {
    composeRule.setContent { UiCatalogRoute() }
    scrollTo("etiqueta-tags")
    composeRule.onNodeWithText(localizedString(Res.string.ui_catalog_tag_chips)).assertIsDisplayed()

    scrollTo("etiqueta-tag-field")
    composeRule
        .onNode(
            hasText(localizedString(Res.string.ui_catalog_open_picker)) and
                hasAnyAncestor(hasTestTag(catalogSectionAutomationTag("etiqueta-tag-field")))
        )
        .performClick()

    composeRule.onNodeWithText(localizedString(Res.string.tag_picker_title)).assertIsDisplayed()
  }

  @Test
  fun catalogDisplaysCompleteTypographyContract() {
    composeRule.setContent { UiCatalogRoute() }

    listOf(
            Res.string.ui_catalog_sample_text,
            Res.string.ui_catalog_sample_caption,
            Res.string.ui_catalog_sample_label,
            Res.string.ui_catalog_sample_body_small,
            Res.string.ui_catalog_sample_body_emphasis,
        )
        .forEach { resource ->
          composeRule
              .onNodeWithText(localizedString(resource))
              .performScrollTo()
              .assertIsDisplayed()
        }
  }

  @Test
  fun catalogBottomNavigationHasNoSelectedProductDestinationAndPreservesActions() {
    val unavailableDestinations =
        listOf(
            Res.string.navigation_home,
            Res.string.navigation_history,
            Res.string.navigation_search,
            Res.string.navigation_profile,
        )
    composeRule.setContent { UiCatalogRoute() }

    unavailableDestinations.forEach { destination ->
      composeRule
          .onNodeWithContentDescription(localizedString(destination))
          .assertHasNoClickAction()
          .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
    }
    composeRule
        .onNodeWithContentDescription(localizedString(Res.string.navigation_add_movement))
        .assertHasClickAction()
  }

  @Test
  fun catalogKeepsThemeControlsWithoutTemporaryPaletteOrTextScaleSelectors() {
    composeRule.setContent { UiCatalogRoute() }

    val dark = localizedString(Res.string.ui_catalog_dark)
    composeRule.onNodeWithText(dark).performClick().assertIsSelected()

    composeRule.onNodeWithText("Large").assertDoesNotExist()
    composeRule.onNodeWithText("Navy · #1E3A5F").assertDoesNotExist()
    composeRule.onNodeWithText("Soft sage · #A8C9B0").assertDoesNotExist()
  }

  @Test
  fun catalogExposesLocalizedControlFamiliesAndRepresentativeStaticStates() {
    composeRule.setContent { UiCatalogRoute() }

    controlFamilyHeadings.forEach { section ->
      scrollTo(section.key)
      composeRule.onNodeWithText(localizedString(section.title)).assertIsDisplayed()
    }

    scrollTo("icon-buttons")
    composeRule
        .onNodeWithContentDescription(localizedString(Res.string.ui_catalog_disabled_icon_button))
        .assertIsNotEnabled()
    scrollTo("selection-fields")
    composeRule
        .onNode(
            hasText(localizedString(Res.string.category)) and
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Error,
                    localizedString(Res.string.category_required),
                )
        )
        .assertIsDisplayed()
  }

  @Test
  fun catalogExposesStableAutomationTagsForDatePickerSectionAndDefaultControl() {
    composeRule.setContent { UiCatalogRoute() }

    scrollTo("date-pickers")
    composeRule.onNodeWithTag("catalog.section.date-pickers").assertIsDisplayed()
    composeRule.onNodeWithTag("catalog.control.date-picker.default").assertIsDisplayed()
  }

  @Test
  fun catalogExposesInteractivePeriodPickerControls() {
    composeRule.setContent { UiCatalogRoute() }

    scrollTo("period-pickers")
    composeRule.onNodeWithTag("catalog.section.period-pickers").assertIsDisplayed()
    composeRule.onNodeWithTag(CATALOG_PERIOD_PICKER_SCOPE_TAG).assertIsDisplayed()
    composeRule.onNodeWithTag(CATALOG_PERIOD_PICKER_ROW_TAG).assertIsDisplayed()
    composeRule.onNodeWithTag(CATALOG_PERIOD_PICKER_YEAR_MENU_TAG).assertIsDisplayed()
  }

  @Test
  fun catalogEditableTextAndClearableSearchUpdateTheirRenderedState() {
    composeRule.setContent { UiCatalogRoute() }

    scrollTo("text-fields")
    composeRule
        .onAllNodes(
            hasContentDescription(localizedString(Res.string.ui_catalog_text_field_label)) and
                isEnabled()
        )[0]
        .performTextInput("Trip")
    composeRule.onNodeWithText("Trip").assertIsDisplayed()

    scrollTo("search-fields")
    composeRule
        .onNode(
            hasContentDescription(localizedString(Res.string.ui_catalog_clear_search)) and
                isEnabled()
        )
        .performClick()
    composeRule
        .onAllNodes(hasText(localizedString(Res.string.home_search_placeholder)))
        .assertCountEquals(2)
    composeRule
        .onNode(
            hasContentDescription(localizedString(Res.string.ui_catalog_clear_search)) and
                isEnabled()
        )
        .assertDoesNotExist()
  }

  @Test
  fun catalogSegmentSelectionAndLoadingRegionRemainConnectedToSampleState() {
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent { UiCatalogRoute() }

    scrollTo("segmented-controls")
    composeRule
        .onNode(hasText(localizedString(Res.string.expense)) and isEnabled())
        .assertIsSelected()
    composeRule.onNode(hasText(localizedString(Res.string.income)) and isEnabled()).performClick()
    composeRule.mainClock.advanceTimeByFrame()
    composeRule
        .onNode(hasText(localizedString(Res.string.income)) and isEnabled())
        .assertIsSelected()

    scrollTo("loading-states")
    val loadingRegion =
        composeRule.onNodeWithTag(CATALOG_LOADING_REGION_TAG).fetchSemanticsNode().boundsInRoot
    val loadingIndicator =
        composeRule
            .onNodeWithContentDescription(localizedString(Res.string.loading))
            .fetchSemanticsNode()
            .boundsInRoot

    assertEquals(loadingRegion.center.x, loadingIndicator.center.x, absoluteTolerance = 1f)
    assertEquals(loadingRegion.center.y, loadingIndicator.center.y, absoluteTolerance = 1f)
  }

  private fun localizedString(resource: StringResource): String = runBlocking {
    getString(resource)
  }

  private fun scrollTo(key: String) {
    composeRule.onNode(hasScrollAction()).performScrollToKey(key)
  }

  private companion object {
    val controlFamilyHeadings =
        listOf(
            CatalogSection("icon-buttons", Res.string.ui_catalog_icon_buttons),
            CatalogSection("text-fields", Res.string.ui_catalog_text_fields),
            CatalogSection("search-fields", Res.string.ui_catalog_search_fields),
            CatalogSection("amount-inputs", Res.string.ui_catalog_amount_inputs),
            CatalogSection("date-pickers", Res.string.ui_catalog_date_pickers),
            CatalogSection("choice-pills", Res.string.ui_catalog_choice_pills),
            CatalogSection("filter-tags", Res.string.ui_catalog_filter_tags),
            CatalogSection("period-pickers", Res.string.ui_catalog_period_pickers),
            CatalogSection("segmented-controls", Res.string.ui_catalog_segmented_controls),
            CatalogSection("selection-fields", Res.string.ui_catalog_selection_fields),
            CatalogSection("loading-states", Res.string.ui_catalog_loading_states),
        )
  }

  private data class CatalogSection(val key: String, val title: StringResource)
}
