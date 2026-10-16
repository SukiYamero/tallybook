package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.ui.theme.TallybookSpacing
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoSummaryUi
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.amount
import tallybook.shared.generated.resources.category_tag_add_description
import tallybook.shared.generated.resources.category_tag_field_label
import tallybook.shared.generated.resources.category_tag_remove
import tallybook.shared.generated.resources.date
import tallybook.shared.generated.resources.expense
import tallybook.shared.generated.resources.movement_type
import tallybook.shared.generated.resources.movimiento_pending_label
import tallybook.shared.generated.resources.movimiento_relative_yesterday
import tallybook.shared.generated.resources.movimiento_summary_amount_missing
import tallybook.shared.generated.resources.movimiento_summary_amount_review
import tallybook.shared.generated.resources.movimiento_summary_description_label
import tallybook.shared.generated.resources.movimiento_summary_description_quote
import tallybook.shared.generated.resources.movimiento_summary_edit
import tallybook.shared.generated.resources.movimiento_summary_save
import tallybook.shared.generated.resources.movimiento_summary_subtitle
import tallybook.shared.generated.resources.movimiento_summary_title

@OptIn(ExperimentalResourceApi::class)
class MovimientoSummaryStepTest {
  @get:Rule val composeRule = createComposeRule()

  private val samples = testEntrySamples()
  private val selected = mutableListOf<Categoria>()
  private val removed = mutableListOf<Categoria>()
  private var addRequests = 0
  private var edits = 0
  private var saves = 0

  @Before
  fun setUp() {
    setResourceReaderAndroidContext(InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun completeSummaryShowsDescriptionInQuotesAndCategoryStates() {
    show(samples.completeSummary)

    composeRule
        .onNodeWithText(
            string(
                Res.string.movimiento_summary_description_quote,
                samples.completeSummary.descripcion,
            )
        )
        .assertIsDisplayed()
    composeRule.onNodeWithText(samples.cinema.nombre).assertIsSelected().assertHasNoClickAction()
    composeRule.onNodeWithText(samples.food.nombre).assertHasClickAction().assertIsEnabled()
    composeRule.onNodeWithText(samples.transport.nombre).assertHasClickAction().assertIsEnabled()
  }

  @Test
  fun tappingASuggestionAddsThatCategory() {
    show(samples.completeSummary)

    composeRule.onNodeWithText(samples.transport.nombre).performClick()

    composeRule.runOnIdle { assertEquals(listOf(samples.transport), selected) }
  }

  @Test
  fun theReviewHasTitleSubtitleAndLabeledSections() {
    show(samples.completeSummary)

    composeRule.onNodeWithText(string(Res.string.movimiento_summary_title)).assertIsDisplayed()
    composeRule.onNodeWithText(string(Res.string.movimiento_summary_subtitle)).assertIsDisplayed()
    composeRule.onNodeWithText(string(Res.string.movement_type)).assertIsDisplayed()
    composeRule.onNodeWithText(string(Res.string.date)).assertIsDisplayed()
    composeRule.onNodeWithText(string(Res.string.amount)).assertIsDisplayed()
    composeRule
        .onNodeWithText(string(Res.string.movimiento_summary_description_label))
        .assertIsDisplayed()
    composeRule.onNodeWithText(string(Res.string.category_tag_field_label)).assertIsDisplayed()
  }

  @Test
  fun typeAndDateSitSideBySideAsLargeBadges() {
    show(samples.completeSummary)

    val type = bounds(composeRule.onNodeWithText(string(Res.string.expense)))
    val date = bounds(composeRule.onNodeWithText(string(Res.string.movimiento_relative_yesterday)))
    assertTrue(type.right <= date.left, "Type must sit to the left of Date")
    assertEquals(type.center.y, date.center.y, absoluteTolerance = 1f)
  }

  @Test
  fun actionsSitAtLeastSpace32BelowTheContent() {
    show(samples.completeSummary)

    val lastContent = bounds(composeRule.onNodeWithText(samples.transport.nombre))
    val edit = bounds(composeRule.onNodeWithText(string(Res.string.movimiento_summary_edit)))
    val minimum = with(composeRule.density) { TallybookSpacing.space32.toPx() }
    assertTrue(
        edit.top - lastContent.bottom >= minimum,
        "Actions start ${edit.top - lastContent.bottom}px below the content, expected $minimum",
    )
  }

  @Test
  fun removingAndAddingTagsReportsTheCallbacks() {
    show(samples.completeSummary.copy(categorias = listOf(samples.cinema, samples.food)))

    composeRule
        .onNodeWithContentDescription(string(Res.string.category_tag_remove, samples.food.nombre))
        .performClick()
    composeRule
        .onNodeWithContentDescription(string(Res.string.category_tag_add_description))
        .performClick()
    composeRule.runOnIdle {
      assertEquals(listOf(samples.food), removed)
      assertEquals(1, addRequests)
    }
  }

  @Test
  fun sixTagsHideAddAndSuggestions() {
    show(samples.fullSummary)

    composeRule
        .onNodeWithContentDescription(string(Res.string.category_tag_add_description))
        .assertDoesNotExist()
  }

  @Test
  fun footerPlacesEditBeforeSaveInOneRowWithEqualWidths() {
    show(samples.completeSummary)

    val edit = bounds(composeRule.onNodeWithText(string(Res.string.movimiento_summary_edit)))
    val save =
        bounds(composeRule.onNodeWithContentDescription(string(Res.string.movimiento_summary_save)))
    assertTrue(edit.right <= save.left, "Edit must sit to the left of Save")
    assertEquals(edit.top, save.top)
    assertEquals(edit.width, save.width, absoluteTolerance = 1f)

    composeRule.onNodeWithText(string(Res.string.movimiento_summary_edit)).performClick()
    composeRule
        .onNodeWithContentDescription(string(Res.string.movimiento_summary_save))
        .performClick()
    composeRule.runOnIdle {
      assertEquals(1, edits)
      assertEquals(1, saves)
    }
  }

  @Test
  fun missingAmountShowsTheRequiredMessageInsteadOfAnAmount() {
    show(samples.missingAmountSummary)

    composeRule
        .onNodeWithText(string(Res.string.movimiento_summary_amount_missing))
        .assertIsDisplayed()
  }

  @Test
  fun ambiguousAmountShowsTheReviewHintOnlyWhenFlagged() {
    show(samples.ambiguousAmountSummary)
    composeRule
        .onNodeWithText(string(Res.string.movimiento_summary_amount_review))
        .assertIsDisplayed()
  }

  @Test
  fun unflaggedAmountHasNoReviewHint() {
    show(samples.completeSummary)
    composeRule
        .onNodeWithText(string(Res.string.movimiento_summary_amount_review))
        .assertDoesNotExist()
  }

  @Test
  fun pendingCategoryShowsTheToCompleteLabel() {
    show(samples.pendingSummary)

    composeRule.onNodeWithText(string(Res.string.movimiento_pending_label)).assertIsDisplayed()
    composeRule.onNodeWithText(samples.other.nombre).assertIsSelected()
  }

  @Test
  fun submittingDisablesEveryAction() {
    show(samples.submittingSummary)

    composeRule.onNodeWithText(string(Res.string.movimiento_summary_edit)).assertIsNotEnabled()
    composeRule
        .onNodeWithContentDescription(string(Res.string.movimiento_summary_save))
        .assertIsNotEnabled()
    composeRule.onNodeWithText(samples.food.nombre).assertIsNotEnabled()
  }

  private fun show(summary: MovimientoSummaryUi) {
    composeRule.setContent {
      TallybookTheme {
        MovimientoSummaryStep(
            summary = summary,
            callbacks =
                MovimientoSummaryCallbacks(
                    onRemoveCategoria = { removed += it },
                    onAddCategoria = { addRequests++ },
                    onSuggestion = { selected += it },
                    onEdit = { edits++ },
                    onSave = { saves++ },
                ),
            today = MovimientoEntrySamples.today,
        )
      }
    }
  }

  private fun bounds(node: androidx.compose.ui.test.SemanticsNodeInteraction): Rect =
      node.fetchSemanticsNode().boundsInRoot

  private fun string(resource: StringResource, vararg args: Any): String = runBlocking {
    getString(resource, *args)
  }
}
