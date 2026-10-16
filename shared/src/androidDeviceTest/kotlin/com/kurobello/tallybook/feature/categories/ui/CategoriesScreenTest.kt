package com.kurobello.tallybook.feature.categories.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoriaOrigen
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.component.TallybookViewportOverlayHost
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesActions
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesContent
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesUiState
import com.kurobello.tallybook.feature.categories.viewmodel.CategoryDetails
import com.kurobello.tallybook.feature.categories.viewmodel.CategoryDraftState
import com.kurobello.tallybook.feature.categories.viewmodel.CategoryEditor
import com.kurobello.tallybook.feature.categories.viewmodel.CategoryRow
import com.kurobello.tallybook.feature.categories.viewmodel.CategorySheetRequest
import kotlin.test.assertEquals
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
import tallybook.shared.generated.resources.categories_title
import tallybook.shared.generated.resources.category_chip_hidden_state
import tallybook.shared.generated.resources.category_delete
import tallybook.shared.generated.resources.category_hide_named
import tallybook.shared.generated.resources.category_movimiento_count
import tallybook.shared.generated.resources.category_new_title
import tallybook.shared.generated.resources.category_show_named
import tallybook.shared.generated.resources.category_view_movimientos_count

@OptIn(ExperimentalResourceApi::class)
class CategoriesScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Before
  fun setUp() {
    setResourceReaderAndroidContext(InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun chipsCarryTheirCountAndHiddenStateAndOpenTheirSheet() {
    val actions = RecordingActions()
    val hidden = cafe.copy(oculta = true)
    show(
        CategoriesUiState(
            content =
                CategoriesContent.Browse(
                    custom = listOf(CategoryRow(gym, 2L)),
                    families = listOf(listOf(CategoryRow(comida, 1L), CategoryRow(hidden, 3L))),
                )
        ),
        actions,
    )

    val one = plural(Res.plurals.category_movimiento_count, 1)
    val three = plural(Res.plurals.category_movimiento_count, 3)
    composeRule.onNode(hasState(one)).assertIsDisplayed()
    composeRule.onNode(hasState(text(Res.string.category_chip_hidden_state, three))).performClick()
    composeRule.onNodeWithText("Gym").performClick()

    composeRule.runOnIdle {
      assertEquals(
          listOf<CategorySheetRequest>(
              CategorySheetRequest.Open(hidden.id),
              CategorySheetRequest.Open(gym.id),
          ),
          actions.opened,
      )
    }
  }

  @Test
  fun headerControlsAreCenteredOnTheTitleAndStayTappable() {
    var backs = 0
    val actions = RecordingActions()
    composeRule.setContent {
      TallybookTheme {
        TallybookViewportOverlayHost {
          CategoriesScreen(
              CategoriesUiState(content = CategoriesContent.Browse(emptyList(), emptyList())),
              actions,
              onBack = { backs++ },
          )
        }
      }
    }

    val title = composeRule.onNodeWithText(text(Res.string.categories_title))
    val back = composeRule.onNodeWithContentDescription(text(Res.string.back))
    val create = composeRule.onNodeWithContentDescription(text(Res.string.category_new_title))
    val titleCenter = title.getUnclippedBoundsInRoot().centerY()
    assertEquals(titleCenter.value, back.getUnclippedBoundsInRoot().centerY().value, 1f)
    assertEquals(titleCenter.value, create.getUnclippedBoundsInRoot().centerY().value, 1f)
    back.performClick()
    create.performClick()

    composeRule.runOnIdle {
      assertEquals(1, backs)
      assertEquals(listOf<CategorySheetRequest>(CategorySheetRequest.New("")), actions.opened)
    }
  }

  @Test
  fun editorHeaderHidesAndDeletesAndViewsItsMovimientos() {
    val actions = RecordingActions()
    show(
        CategoriesUiState(
            content = CategoriesContent.Browse(emptyList(), emptyList()),
            sheet = CategoryEditor.of(gym, CategoryDraftState.forEdit(gym), count = 2L),
        ),
        actions,
    )

    composeRule
        .onNodeWithContentDescription(text(Res.string.category_hide_named, gym.nombre))
        .performClick()
    composeRule.onNodeWithContentDescription(text(Res.string.category_delete)).performClick()
    composeRule
        .onNodeWithText(plural(Res.plurals.category_view_movimientos_count, 2))
        .performClick()

    composeRule.runOnIdle {
      assertEquals(listOf("toggle:${gym.id}", "delete", "view"), actions.calls)
    }
  }

  @Test
  fun detailsOfAHiddenPredefinedCategoryOfferShowingIt() {
    val actions = RecordingActions()
    show(
        CategoriesUiState(
            content = CategoriesContent.Browse(emptyList(), emptyList()),
            sheet = CategoryDetails(cafe.copy(oculta = true), count = 0L),
        ),
        actions,
    )

    composeRule
        .onNodeWithContentDescription(text(Res.string.category_show_named, cafe.nombre))
        .performClick()
    composeRule.runOnIdle { assertEquals(listOf("toggle:${cafe.id}"), actions.calls) }
    composeRule
        .onNodeWithText(plural(Res.plurals.category_view_movimientos_count, 0))
        .assertDoesNotExist()
  }

  @Test
  fun otrosDetailsHaveNoVisibilityToggle() {
    show(
        CategoriesUiState(
            content = CategoriesContent.Browse(emptyList(), emptyList()),
            sheet = CategoryDetails(otros, count = 4L),
        ),
        RecordingActions(),
    )

    composeRule
        .onNodeWithContentDescription(text(Res.string.category_hide_named, otros.nombre))
        .assertDoesNotExist()
    composeRule
        .onNodeWithText(plural(Res.plurals.category_view_movimientos_count, 4))
        .assertIsDisplayed()
  }

  private fun show(state: CategoriesUiState, actions: CategoriesActions) {
    composeRule.setContent {
      TallybookTheme {
        TallybookViewportOverlayHost { CategoriesScreen(state, actions, onBack = {}) }
      }
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

  private class RecordingActions : CategoriesActions {
    val opened = mutableListOf<CategorySheetRequest>()
    val calls = mutableListOf<String>()

    override fun onOpenSheet(request: CategorySheetRequest): Boolean {
      opened += request
      return true
    }

    override fun onToggleHidden(id: String) {
      calls += "toggle:$id"
    }

    override fun onRequestDelete() {
      calls += "delete"
    }

    override fun onViewMovimientos() {
      calls += "view"
    }

    override fun onQueryChanged(query: String) = Unit

    override fun onClearQuery() = Unit

    override fun onRetry() = Unit

    override fun onSaveEditor() = Unit

    override fun onRequestCloseSheet() = Unit

    override fun onConfirmDiscard() = Unit

    override fun onContinueEditing() = Unit

    override fun onSheetHidden() = Unit

    override fun onOpenDuplicate() = Unit

    override fun onConfirmDelete() = Unit

    override fun onHideInstead() = Unit

    override fun onCancelDelete() = Unit

    override fun onNombreChanged(value: String) = Unit

    override fun onIconoSelected(icono: CategoryIcon) = Unit

    override fun onColorSelected(color: CategoryTint) = Unit

    override fun onDescripcionChanged(value: String) = Unit

    override fun onMoreOptions() = Unit
  }

  private companion object {
    val comida = Categoria("cat_comida", "Comida", null, CategoryIcon.UTENSILS, CategoryTint.AMBER)
    val cafe = Categoria("cat_cafe", "Café", "cat_comida", CategoryIcon.COFFEE, CategoryTint.AMBER)
    val otros =
        Categoria(
            CATEGORIA_OTROS_ID,
            "Otros",
            null,
            CategoryIcon.CIRCLE_DASHED,
            CategoryTint.NEUTRAL,
        )
    val gym =
        Categoria(
            "gym",
            "Gym",
            null,
            CategoryIcon.DUMBBELL,
            CategoryTint.ROSE,
            origen = CategoriaOrigen.USUARIO,
        )
  }
}

private fun DpRect.centerY(): Dp = (top + bottom) / 2
