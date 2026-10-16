package com.kurobello.tallybook.feature.categories.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.categories.viewmodel.CategoryDraftState
import com.kurobello.tallybook.feature.categories.viewmodel.onColor
import com.kurobello.tallybook.feature.categories.viewmodel.onIcono
import com.kurobello.tallybook.feature.categories.viewmodel.onMoreOptions
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_duplicate_message
import tallybook.shared.generated.resources.category_duplicate_otros
import tallybook.shared.generated.resources.category_form_icon_expand
import tallybook.shared.generated.resources.category_form_more_options
import tallybook.shared.generated.resources.description_optional

@OptIn(ExperimentalResourceApi::class)
class CategoryFormTest {

  @get:Rule val composeRule = createComposeRule()

  @Before
  fun setUp() {
    setResourceReaderAndroidContext(InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun compactHidesTintAndDescriptionUntilMoreOptions() {
    val description = localizedString(Res.string.description_optional)
    val purple = localizedString(CategoryTint.PURPLE.labelResource())
    composeRule.setContent {
      StatefulForm(CategoryDraftState(nombre = "Gym"), CategoryFormMode.COMPACT)
    }

    composeRule.onNodeWithContentDescription(description).assertDoesNotExist()
    composeRule.onNodeWithContentDescription(purple).assertDoesNotExist()

    composeRule
        .onNodeWithText(localizedString(Res.string.category_form_more_options))
        .performClick()

    composeRule.onNodeWithContentDescription(purple).performScrollTo().assertIsNotSelected()
    composeRule.onNodeWithContentDescription(description).performScrollTo().assertIsDisplayed()
    composeRule
        .onNodeWithText(localizedString(Res.string.category_form_more_options))
        .assertDoesNotExist()
  }

  @Test
  fun fullShowsTintAndDescriptionWithTheSelectedTint() {
    composeRule.setContent {
      StatefulForm(
          CategoryDraftState(nombre = "Gym", color = CategoryTint.ROSE),
          CategoryFormMode.FULL,
      )
    }

    composeRule
        .onNodeWithContentDescription(localizedString(CategoryTint.ROSE.labelResource()))
        .performScrollTo()
        .assertIsSelected()
    composeRule
        .onNodeWithContentDescription(localizedString(Res.string.description_optional))
        .performScrollTo()
        .assertIsDisplayed()
  }

  @Test
  fun iconGridMarksOnlyTheChosenIconAsSelected() {
    val dumbbell = localizedString(CategoryIcon.DUMBBELL.labelResource())
    val book = localizedString(CategoryIcon.BOOK.labelResource())
    var chosen: CategoryIcon? = null
    composeRule.setContent {
      StatefulForm(
          CategoryDraftState(nombre = "Gym", icono = CategoryIcon.DUMBBELL),
          CategoryFormMode.FULL,
          onIcono = { chosen = it },
      )
    }

    composeRule.onNodeWithContentDescription(book).assertDoesNotExist()
    composeRule.onNodeWithText(localizedString(Res.string.category_form_icon_expand)).performClick()
    composeRule.onNodeWithContentDescription(dumbbell).performScrollTo().assertIsSelected()
    composeRule.onNodeWithContentDescription(book).performScrollTo().performClick()

    composeRule.onNodeWithContentDescription(book).assertIsSelected()
    composeRule.onNodeWithContentDescription(dumbbell).assertIsNotSelected()
    composeRule.runOnIdle { assertEquals(CategoryIcon.BOOK, chosen) }
  }

  @Test
  fun duplicateOffersTheCallerActionButNeverForOtros() {
    val existing =
        Categoria("cat_gimnasio", "Gimnasio", null, CategoryIcon.DUMBBELL, CategoryTint.ROSE)
    val otros =
        Categoria(
            CATEGORIA_OTROS_ID,
            "Otros",
            null,
            CategoryIcon.CIRCLE_DASHED,
            CategoryTint.NEUTRAL,
        )
    var duplicate by mutableStateOf(existing)
    var used = 0
    composeRule.setContent {
      TallybookTheme {
        CategoryForm(
            state = CategoryDraftState(nombre = duplicate.nombre, duplicate = duplicate),
            actions = RecordingActions(),
            mode = CategoryFormMode.COMPACT,
            duplicateAction = CategoryDuplicateAction("Use ${duplicate.nombre}") { used++ },
        )
      }
    }

    composeRule
        .onNodeWithText(localizedString(Res.string.category_duplicate_message, existing.nombre))
        .assertIsDisplayed()
    composeRule.onNodeWithText("Use Gimnasio").performClick()
    composeRule.runOnIdle {
      assertEquals(1, used)
      duplicate = otros
    }

    composeRule
        .onNodeWithText(localizedString(Res.string.category_duplicate_otros, otros.nombre))
        .assertIsDisplayed()
    composeRule.onNodeWithText("Use Otros").assertDoesNotExist()
  }

  @Composable
  private fun StatefulForm(
      initial: CategoryDraftState,
      mode: CategoryFormMode,
      onIcono: (CategoryIcon) -> Unit = {},
  ) {
    var state by remember { mutableStateOf(initial) }
    TallybookTheme {
      Column(Modifier.verticalScroll(rememberScrollState())) {
        CategoryForm(
            state = state,
            actions =
                RecordingActions(
                    onIcono = {
                      onIcono(it)
                      state = state.onIcono(it)
                    },
                    onColor = { state = state.onColor(it) },
                    onMore = { state = state.onMoreOptions() },
                ),
            mode = mode,
        )
      }
    }
  }

  private class RecordingActions(
      private val onIcono: (CategoryIcon) -> Unit = {},
      private val onColor: (CategoryTint) -> Unit = {},
      private val onMore: () -> Unit = {},
  ) : CategoryFormActions {
    override fun onNombreChanged(value: String) = Unit

    override fun onIconoSelected(icono: CategoryIcon) = onIcono(icono)

    override fun onColorSelected(color: CategoryTint) = onColor(color)

    override fun onDescripcionChanged(value: String) = Unit

    override fun onMoreOptions() = onMore()
  }

  private fun localizedString(resource: StringResource, vararg args: Any): String = runBlocking {
    getString(resource, *args)
  }
}
