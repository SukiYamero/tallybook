package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategorySeedCatalog
import com.kurobello.tallybook.core.data.UnsupportedCategoriaRepository
import com.kurobello.tallybook.core.data.entry.CategoryAppearanceSuggester
import com.kurobello.tallybook.core.data.entry.CategoryKeywordDictionary
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoriaDraft
import com.kurobello.tallybook.core.model.CategoriaOrigen
import com.kurobello.tallybook.core.model.CategoriaWrite
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.categories.viewmodel.QuickCategoryViewModel
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_picker_create

class CategoryPickerQuickCreateTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun aSearchWithoutResultsCreatesTheTypedCategorySelectsItAndCloses() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val repository = PickerCategorias(listOf(food))
    val quickCategory =
        QuickCategoryViewModel(
            categoriaRepository = repository,
            suggester =
                CategoryAppearanceSuggester(
                    CategoryKeywordDictionary.builtIn,
                    CategorySeedCatalog.categories,
                ),
            scope = scope,
        )
    var selected by mutableStateOf<String?>(food.id)
    var dismissals = 0
    try {
      composeRule.setContent {
        TallybookTheme {
          val catalog by repository.catalog.collectAsState()
          val quickState by quickCategory.uiState.collectAsState()
          QuickCategorySelectionEffect(
              effects = quickCategory.effects,
              selection = CategoryPickerSelection(selected) { selected = it },
              catalog = catalog,
          )
          CategoryPickerSheet(
              visible = true,
              categorias = catalog,
              selectedId = selected,
              onSelect = { selected = it },
              onDismiss = { dismissals++ },
              creation = CategoryPickerCreation(quickState, quickCategory),
          )
        }
      }

      composeRule.onNode(hasSetTextAction()).performTextInput("  Gym ")
      composeRule.onNodeWithText(string("Gym")).performClick()
      composeRule.onNodeWithTag(CATEGORY_PICKER_CREATION_TAG).assertExists()
      composeRule.onNodeWithTag(CATEGORY_PICKER_CREATE_TAG).performClick()

      composeRule.waitUntil(5_000) { selected == GYM_ID }
      composeRule.onNodeWithTag(CATEGORY_PICKER_CREATION_TAG).assertDoesNotExist()
      composeRule.runOnIdle {
        assertEquals("Gym", repository.catalog.value.last().nombre)
        assertEquals(GYM_ID, selected)
        assertEquals(1, dismissals)
      }
    } finally {
      scope.cancel()
    }
  }

  private fun string(nombre: String) = runBlocking {
    getString(Res.string.category_picker_create, nombre)
  }

  private class PickerCategorias(initial: List<Categoria>) :
      CategoriaRepository by UnsupportedCategoriaRepository {
    val catalog = MutableStateFlow(initial)

    override suspend fun create(draft: CategoriaDraft): DataOutcome<CategoriaWrite> {
      val created =
          Categoria(
              id = GYM_ID,
              nombre = draft.nombre,
              padreId = null,
              icono = draft.icono,
              color = draft.color,
              origen = CategoriaOrigen.USUARIO,
          )
      catalog.update { it + created }
      return DataOutcome.Success(CategoriaWrite.Saved(created))
    }
  }

  private companion object {
    const val GYM_ID = "custom-gym"
    val food = Categoria("food", "Food", null, CategoryIcon.UTENSILS, CategoryTint.AMBER)
  }
}
