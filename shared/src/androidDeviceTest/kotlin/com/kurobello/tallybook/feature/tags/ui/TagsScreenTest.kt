package com.kurobello.tallybook.feature.tags.ui

import android.view.KeyEvent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.CategoriaRules
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.ui.component.TallybookViewportOverlayHost
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.tags.viewmodel.TagDeletion
import com.kurobello.tallybook.feature.tags.viewmodel.TagRow
import com.kurobello.tallybook.feature.tags.viewmodel.TagSheet
import com.kurobello.tallybook.feature.tags.viewmodel.TagSheetRequest
import com.kurobello.tallybook.feature.tags.viewmodel.TagsActions
import com.kurobello.tallybook.feature.tags.viewmodel.TagsContent
import com.kurobello.tallybook.feature.tags.viewmodel.TagsUiState
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.back
import tallybook.shared.generated.resources.retry
import tallybook.shared.generated.resources.tag_delete
import tallybook.shared.generated.resources.tag_delete_message
import tallybook.shared.generated.resources.tag_delete_message_unused
import tallybook.shared.generated.resources.tag_delete_title
import tallybook.shared.generated.resources.tag_duplicate_message
import tallybook.shared.generated.resources.tag_duplicate_open
import tallybook.shared.generated.resources.tag_edit_title
import tallybook.shared.generated.resources.tag_movimiento_count
import tallybook.shared.generated.resources.tag_name_too_long
import tallybook.shared.generated.resources.tag_new_title
import tallybook.shared.generated.resources.tag_no_movimientos
import tallybook.shared.generated.resources.tag_save
import tallybook.shared.generated.resources.tag_view_movimientos_count
import tallybook.shared.generated.resources.tags_empty
import tallybook.shared.generated.resources.tags_load_error
import tallybook.shared.generated.resources.tags_search_empty

@OptIn(ExperimentalResourceApi::class)
class TagsScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Before
  fun setUp() {
    setResourceReaderAndroidContext(InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun chipsCarryTheirCountAndOpenTheirSheet() {
    val actions = RecordingActions()
    show(
        TagsUiState(content = TagsContent.Browse(listOf(TagRow(empresa, 2L), TagRow(viaje, 0L)))),
        actions,
    )

    composeRule
        .onNode(hasState(plural(Res.plurals.tag_movimiento_count, 2)) and hasText("empresa"))
        .assertIsDisplayed()
    composeRule
        .onNode(hasState(text(Res.string.tag_no_movimientos)) and hasText("Viaje Chile"))
        .performClick()
    composeRule.onNodeWithText("empresa").performClick()

    composeRule.runOnIdle {
      assertEquals(
          listOf<TagSheetRequest>(
              TagSheetRequest.Open(viaje.id),
              TagSheetRequest.Open(empresa.id),
          ),
          actions.opened,
      )
    }
  }

  @Test
  fun anEmptyCatalogAndAnEmptySearchExplainThemselves() {
    val content = mutableStateOf<TagsContent>(TagsContent.Browse(emptyList()))
    composeRule.setContent {
      TallybookTheme {
        TallybookViewportOverlayHost {
          TagsScreen(TagsUiState(content = content.value), RecordingActions(), onBack = {})
        }
      }
    }

    composeRule.onNodeWithText(text(Res.string.tags_empty)).assertIsDisplayed()
    content.value = TagsContent.Search("viaje", emptyList())
    composeRule.onNodeWithText(text(Res.string.tags_search_empty, "viaje")).assertIsDisplayed()
  }

  @Test
  fun aLoadFailureOffersRetry() {
    val actions = RecordingActions()
    show(TagsUiState(content = TagsContent.Failed), actions)

    composeRule.onNodeWithText(text(Res.string.tags_load_error)).assertIsDisplayed()
    composeRule.onNodeWithText(text(Res.string.retry)).performClick()
    composeRule.runOnIdle { assertEquals(listOf("retry"), actions.calls) }
  }

  @Test
  fun headerGoesBackAndCreatesFromTheQuery() {
    var backs = 0
    val actions = RecordingActions()
    composeRule.setContent {
      TallybookTheme {
        TallybookViewportOverlayHost {
          TagsScreen(
              TagsUiState(query = "Viaje", content = TagsContent.Search("Viaje", emptyList())),
              actions,
              onBack = { backs++ },
          )
        }
      }
    }

    composeRule.onNodeWithContentDescription(text(Res.string.back)).performClick()
    composeRule.onNodeWithContentDescription(text(Res.string.tag_new_title)).performClick()

    composeRule.runOnIdle {
      assertEquals(1, backs)
      assertEquals(listOf<TagSheetRequest>(TagSheetRequest.New("Viaje")), actions.opened)
    }
  }

  @Test
  fun anEditSheetSavesViewsItsMovimientosAndDeletes() {
    val actions = RecordingActions()
    show(
        TagsUiState(
            content = TagsContent.Browse(listOf(TagRow(empresa, 2L))),
            sheet = TagSheet.forEdit(empresa, count = 2L),
        ),
        actions,
    )

    composeRule.onNodeWithText(text(Res.string.tag_edit_title)).assertIsDisplayed()
    composeRule.onNodeWithText(text(Res.string.tag_save)).performClick()
    composeRule.onNodeWithText(plural(Res.plurals.tag_view_movimientos_count, 2)).performClick()
    composeRule.onNodeWithContentDescription(text(Res.string.tag_delete)).performClick()

    composeRule.runOnIdle { assertEquals(listOf("save", "view", "delete"), actions.calls) }
  }

  @Test
  fun aTagWithoutMovimientosOffersNoViewAction() {
    show(
        TagsUiState(
            content = TagsContent.Browse(listOf(TagRow(viaje, 0L))),
            sheet = TagSheet.forEdit(viaje, count = 0L),
        ),
        RecordingActions(),
    )

    composeRule
        .onNodeWithText(plural(Res.plurals.tag_view_movimientos_count, 0))
        .assertDoesNotExist()
  }

  @Test
  fun aNewDuplicateOffersOpeningTheExistingTagAndBlocksSaving() {
    val actions = RecordingActions()
    show(
        TagsUiState(
            content = TagsContent.Browse(listOf(TagRow(empresa, 2L))),
            sheet = TagSheet.forNew("EMPRESA").copy(duplicate = empresa),
        ),
        actions,
    )

    composeRule.onNodeWithText(text(Res.string.tag_new_title)).assertIsDisplayed()
    composeRule
        .onNodeWithText(text(Res.string.tag_duplicate_message, empresa.nombre))
        .assertIsDisplayed()
    composeRule.onNode(hasText(text(Res.string.tag_save)) and hasClickAction()).assertIsNotEnabled()
    composeRule.onNodeWithText(text(Res.string.tag_duplicate_open, empresa.nombre)).performClick()

    composeRule.runOnIdle { assertEquals(listOf("open-duplicate"), actions.calls) }
  }

  @Test
  fun aRenameCollisionIsAnInlineErrorWithoutAnOpenAction() {
    show(
        TagsUiState(
            content = TagsContent.Browse(listOf(TagRow(empresa, 2L), TagRow(viaje, 0L))),
            sheet =
                TagSheet.forEdit(empresa, count = 2L)
                    .copy(nombre = "viaje chile", duplicate = viaje),
        ),
        RecordingActions(),
    )

    composeRule
        .onNodeWithText(text(Res.string.tag_duplicate_message, viaje.nombre))
        .assertIsDisplayed()
    composeRule
        .onNodeWithText(text(Res.string.tag_duplicate_open, viaje.nombre))
        .assertDoesNotExist()
  }

  @Test
  fun aTooLongNameShowsItsLimitAndBlocksSaving() {
    show(
        TagsUiState(
            content = TagsContent.Browse(emptyList()),
            sheet =
                TagSheet.forNew("")
                    .copy(
                        nombre = "x".repeat(CategoriaRules.NOMBRE_MAX + 1),
                        nombreEdited = true,
                    ),
        ),
        RecordingActions(),
    )

    composeRule
        .onNodeWithText(text(Res.string.tag_name_too_long, CategoriaRules.NOMBRE_MAX))
        .assertIsDisplayed()
    composeRule.onNode(hasText(text(Res.string.tag_save)) and hasClickAction()).assertIsNotEnabled()
  }

  @Test
  fun deletingAUsedTagConfirmsWithItsCount() {
    val actions = RecordingActions()
    show(
        TagsUiState(
            content = TagsContent.Browse(listOf(TagRow(empresa, 2L))),
            sheet = TagSheet.forEdit(empresa, count = 2L).copy(deletion = TagDeletion.Confirm(3L)),
        ),
        actions,
    )

    composeRule
        .onNodeWithText(text(Res.string.tag_delete_title, empresa.nombre))
        .assertIsDisplayed()
    composeRule.onNodeWithText(plural(Res.plurals.tag_delete_message, 3)).assertIsDisplayed()
    composeRule
        .onNode(hasText(text(Res.string.tag_delete)) and hasClickAction())
        .assertIsEnabled()
        .performClick()

    composeRule.runOnIdle { assertEquals(listOf("confirm-delete"), actions.calls) }
  }

  @Test
  fun deletingAnUnusedTagSaysNothingChanges() {
    show(
        TagsUiState(
            content = TagsContent.Browse(listOf(TagRow(viaje, 0L))),
            sheet = TagSheet.forEdit(viaje, count = 0L).copy(deletion = TagDeletion.Confirm(0L)),
        ),
        RecordingActions(),
    )

    composeRule.onNodeWithText(text(Res.string.tag_delete_message_unused)).assertIsDisplayed()
  }

  @Test
  fun backAsksTheOpenSheetToCloseFirst() {
    val actions = RecordingActions()
    show(
        TagsUiState(
            content = TagsContent.Browse(listOf(TagRow(empresa, 2L))),
            sheet = TagSheet.forEdit(empresa, count = 2L),
        ),
        actions,
    )

    InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)

    composeRule.runOnIdle { assertEquals(listOf("close"), actions.calls) }
  }

  private fun show(state: TagsUiState, actions: TagsActions) {
    composeRule.setContent {
      TallybookTheme { TallybookViewportOverlayHost { TagsScreen(state, actions, onBack = {}) } }
    }
  }

  private fun hasState(value: String) =
      SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

  private fun text(resource: StringResource, vararg args: Any): String = runBlocking {
    getString(resource, *args)
  }

  private fun plural(resource: PluralStringResource, count: Int): String = runBlocking {
    getPluralString(resource, count, count)
  }

  private class RecordingActions : TagsActions {
    val opened = mutableListOf<TagSheetRequest>()
    val calls = mutableListOf<String>()

    override fun onOpenSheet(request: TagSheetRequest): Boolean {
      opened += request
      return true
    }

    override fun onRetry() {
      calls += "retry"
    }

    override fun onSaveSheet() {
      calls += "save"
    }

    override fun onViewMovimientos() {
      calls += "view"
    }

    override fun onOpenDuplicate() {
      calls += "open-duplicate"
    }

    override fun onRequestDelete() {
      calls += "delete"
    }

    override fun onConfirmDelete() {
      calls += "confirm-delete"
    }

    override fun onRequestCloseSheet() {
      calls += "close"
    }

    override fun onQueryChanged(query: String) = Unit

    override fun onNombreChanged(value: String) = Unit

    override fun onCancelDelete() = Unit

    override fun onSheetHidden() = Unit
  }

  private companion object {
    val createdAt = Instant.parse("2026-01-01T00:00:00Z")
    val empresa = Etiqueta("t_empresa", "empresa", createdAt)
    val viaje = Etiqueta("t_viaje", "Viaje Chile", createdAt)
  }
}
