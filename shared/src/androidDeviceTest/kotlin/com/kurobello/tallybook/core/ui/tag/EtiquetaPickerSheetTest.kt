package com.kurobello.tallybook.core.ui.tag

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.ui.theme.TallybookDimensions
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.tag_picker_create
import tallybook.shared.generated.resources.tag_picker_done
import tallybook.shared.generated.resources.tag_picker_empty_hint
import tallybook.shared.generated.resources.tag_picker_limit_reached
import tallybook.shared.generated.resources.tag_picker_title
import tallybook.shared.generated.resources.tag_save_error
import tallybook.shared.generated.resources.tags_empty
import tallybook.shared.generated.resources.tags_search_empty

@OptIn(ExperimentalResourceApi::class)
class EtiquetaPickerSheetTest {
  @get:Rule val composeRule = createComposeRule()

  private var selection by mutableStateOf(listOf<String>())
  private val created = mutableListOf<String>()
  private var dismissals = 0

  @Before
  fun setUp() {
    setResourceReaderAndroidContext(InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun tappingTagsTogglesThemAndDoneReportsTheCount() {
    show(initial = listOf(trip.id))

    composeRule.onNodeWithText(string(Res.string.tag_picker_title)).assertExists()
    composeRule.onTagOption(work.nombre).performClick()
    composeRule.runOnIdle { assertEquals(listOf(trip.id, work.id), selection) }
    composeRule.onTagOption(work.nombre).assertIsSelected()

    composeRule.onTagOption(trip.nombre).performClick()
    composeRule.runOnIdle { assertEquals(listOf(work.id), selection) }

    composeRule.onNodeWithText(string(Res.string.tag_picker_done, 1)).performClick()
    composeRule.runOnIdle { assertEquals(1, dismissals) }
  }

  @Test
  fun doneSitsBesideTheTitleAboveTheSearchAndTheResultsKeepTheirHeight() {
    show(initial = listOf(trip.id), etiquetas = listOf(trip))

    val done = composeRule.onNodeWithText(string(Res.string.tag_picker_done, 1))
    val title = composeRule.onNodeWithText(string(Res.string.tag_picker_title))
    val search = composeRule.onNode(hasSetTextAction())
    assertEquals(
        title.boundsInRoot().center.y,
        done.boundsInRoot().center.y,
        absoluteTolerance = 1f,
    )
    assertTrue(done.boundsInRoot().bottom <= search.boundsInRoot().top)
    composeRule
        .onNodeWithTag(ETIQUETA_PICKER_RESULTS_TAG)
        .assertHeightIsEqualTo(TallybookDimensions.pickerResultsHeight)
  }

  @Test
  fun anEmptyCatalogInvitesTypingTheFirstName() {
    show(etiquetas = emptyList())

    composeRule.onNodeWithText(string(Res.string.tags_empty)).assertIsDisplayed()
    composeRule.onNodeWithText(string(Res.string.tag_picker_empty_hint)).assertIsDisplayed()
  }

  @Test
  fun atTheLimitUnselectedTagsAreDisabledAndTheHintExplainsWhy() {
    show(initial = listOf(trip.id), max = 1)

    composeRule.onNodeWithText(string(Res.string.tag_picker_limit_reached, 1)).assertExists()
    composeRule.onTagOption(work.nombre).assertIsNotEnabled()
    composeRule.onTagOption(trip.nombre).assertIsEnabled().performClick()
    composeRule.runOnIdle { assertEquals(emptyList(), selection) }
  }

  @Test
  fun searchFiltersTheTagsByName() {
    show()

    composeRule.onNode(hasSetTextAction()).performTextInput("CHILE")

    composeRule.onTagOption(trip.nombre).assertExists()
    composeRule.onTagOption(work.nombre).assertDoesNotExist()
  }

  @Test
  fun aSearchWithoutResultsOffersACenteredCreateActionForTheTrimmedName() {
    show()

    composeRule.onNode(hasSetTextAction()).performTextInput("  Gym ")
    composeRule.onNodeWithText(string(Res.string.tags_search_empty, "Gym")).assertExists()
    val results = composeRule.onNodeWithTag(ETIQUETA_PICKER_RESULTS_TAG).boundsInRoot()
    val create = composeRule.onNodeWithTag(ETIQUETA_PICKER_OFFER_CREATE_TAG)
    create.assert(hasText(string(Res.string.tag_picker_create, "Gym")))
    val createBounds = create.boundsInRoot()
    assertTrue(createBounds.width < results.width)
    assertEquals(results.center.x, createBounds.center.x, absoluteTolerance = 1f)
    create.performClick()

    composeRule.runOnIdle { assertEquals(listOf("Gym"), created) }
  }

  @Test
  fun aFailedCreateIsExplainedOnlyWhileTheQueryStillNamesIt() {
    show(failedCreateNombre = "Gym")
    val error = string(Res.string.tag_save_error)

    composeRule.onNode(hasSetTextAction()).performTextInput(" Gym")
    composeRule.onNodeWithText(error).assertExists()

    composeRule.onNode(hasSetTextAction()).performTextInput("nasio")
    composeRule.onNodeWithText(error).assertDoesNotExist()
  }

  private fun show(
      initial: List<String> = emptyList(),
      max: Int = 6,
      failedCreateNombre: String? = null,
      etiquetas: List<Etiqueta> = listOf(trip, work),
  ) {
    selection = initial
    composeRule.setContent {
      TallybookTheme {
        EtiquetaPickerSheet(
            visible = true,
            etiquetas = etiquetas,
            selectedIds = selection,
            onSelectionChange = { selection = it },
            onCreate = { created += it },
            onDismiss = { dismissals++ },
            max = max,
            failedCreateNombre = failedCreateNombre,
        )
      }
    }
  }

  private fun SemanticsNodeInteractionsProvider.onTagOption(text: String) =
      onNode(hasText(text) and hasClickAction() and isSelectable())

  private fun SemanticsNodeInteraction.boundsInRoot() = fetchSemanticsNode().boundsInRoot

  private fun string(resource: StringResource, vararg args: Any): String = runBlocking {
    getString(resource, *args)
  }

  private companion object {
    val createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z")
    val trip = Etiqueta("trip", "Viaje Chile", createdAt)
    val work = Etiqueta("work", "Empresa", createdAt)
  }
}
