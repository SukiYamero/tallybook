package com.kurobello.tallybook.core.ui.tag

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
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
import tallybook.shared.generated.resources.tag_picker_limit_reached
import tallybook.shared.generated.resources.tag_picker_title
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
  fun aSearchWithoutResultsOffersCreatingTheTrimmedName() {
    show()

    composeRule.onNode(hasSetTextAction()).performTextInput("  Gym ")
    composeRule.onNodeWithText(string(Res.string.tags_search_empty, "Gym")).assertExists()
    composeRule.onNodeWithText(string(Res.string.tag_picker_create, "Gym")).performClick()

    composeRule.runOnIdle { assertEquals(listOf("Gym"), created) }
  }

  private fun show(initial: List<String> = emptyList(), max: Int = 6) {
    selection = initial
    composeRule.setContent {
      TallybookTheme {
        EtiquetaPickerSheet(
            visible = true,
            etiquetas = listOf(trip, work),
            selectedIds = selection,
            onSelectionChange = { selection = it },
            onCreate = { created += it },
            onDismiss = { dismissals++ },
            max = max,
        )
      }
    }
  }

  private fun SemanticsNodeInteractionsProvider.onTagOption(text: String) =
      onNode(hasText(text) and hasClickAction() and isSelectable())

  private fun string(resource: StringResource, vararg args: Any): String = runBlocking {
    getString(resource, *args)
  }

  private companion object {
    val createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z")
    val trip = Etiqueta("trip", "Viaje Chile", createdAt)
    val work = Etiqueta("work", "Empresa", createdAt)
  }
}
