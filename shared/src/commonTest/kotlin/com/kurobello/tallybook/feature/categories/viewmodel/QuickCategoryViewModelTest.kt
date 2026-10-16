package com.kurobello.tallybook.feature.categories.viewmodel

import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategorySeedCatalog
import com.kurobello.tallybook.core.data.UnsupportedCategoriaRepository
import com.kurobello.tallybook.core.data.entry.CategoryAppearanceSuggester
import com.kurobello.tallybook.core.data.entry.CategoryKeywordDictionary
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoriaDraft
import com.kurobello.tallybook.core.model.CategoriaOrigen
import com.kurobello.tallybook.core.model.CategoriaWrite
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class QuickCategoryViewModelTest {

  private val suggester =
      CategoryAppearanceSuggester(CategoryKeywordDictionary.builtIn, CategorySeedCatalog.categories)

  @Test
  fun startTrimsTheQueryAndSuggestsItsLook() = runTest {
    val (viewModel, _) = quickCategory(QuickCategoryRepository())

    viewModel.onStart("  GYM ")

    val draft = assertNotNull(viewModel.uiState.value.draft)
    assertEquals("GYM", draft.nombre)
    assertEquals(suggester.suggest("GYM").icono, draft.icono)
    assertFalse(viewModel.uiState.value.saveFailed)
  }

  @Test
  fun createEmitsCreatedAndReturnsToTheResults() = runTest {
    val repository = QuickCategoryRepository()
    val (viewModel, effects) = quickCategory(repository)
    viewModel.onStart("GYM")

    viewModel.onSave()
    runCurrent()

    assertEquals("GYM", repository.created.single().nombre)
    assertEquals(listOf(QuickCategoryEffect.Created("gym-id")), effects)
    assertEquals(QuickCategoryUiState(), viewModel.uiState.value)
  }

  @Test
  fun savingTwiceWritesOnce() = runTest {
    val repository = QuickCategoryRepository(gate = CompletableDeferred())
    val (viewModel, effects) = quickCategory(repository)
    viewModel.onStart("GYM")

    viewModel.onSave()
    viewModel.onSave()
    runCurrent()
    assertTrue(assertNotNull(viewModel.uiState.value.draft).saving)
    repository.gate?.complete(Unit)
    runCurrent()

    assertEquals(1, repository.created.size)
    assertEquals(1, effects.size)
  }

  @Test
  fun aDuplicateKeepsThePaneAndUseSelectsTheExistingCategory() = runTest {
    val existing = categoria("cat_gym", "Gym")
    val repository = QuickCategoryRepository(write = CategoriaWrite.Duplicate(existing))
    val (viewModel, effects) = quickCategory(repository)
    viewModel.onStart("gym")

    viewModel.onSave()
    runCurrent()

    val draft = assertNotNull(viewModel.uiState.value.draft)
    assertEquals(existing, draft.duplicate)
    assertFalse(draft.saving)
    assertTrue(effects.isEmpty())

    viewModel.onUseDuplicate()
    runCurrent()

    assertTrue(repository.unhidden.isEmpty())
    assertEquals(listOf(QuickCategoryEffect.Used(existing.id)), effects)
    assertNull(viewModel.uiState.value.draft)
  }

  @Test
  fun aHiddenDuplicateIsShownAgainBeforeItIsUsed() = runTest {
    val hidden = categoria("custom-gym", "Gym", oculta = true)
    val repository = QuickCategoryRepository(write = CategoriaWrite.Duplicate(hidden))
    val (viewModel, effects) = quickCategory(repository)
    viewModel.onStart("Gym")
    viewModel.onSave()
    runCurrent()

    viewModel.onUseDuplicate()
    runCurrent()

    assertEquals(listOf(hidden.id), repository.unhidden)
    assertEquals(listOf(QuickCategoryEffect.Used(hidden.id)), effects)
  }

  @Test
  fun anOtrosDuplicateOffersNoAction() = runTest {
    val otros = categoria(CATEGORIA_OTROS_ID, "Otros", origen = CategoriaOrigen.SISTEMA)
    val repository = QuickCategoryRepository(write = CategoriaWrite.Duplicate(otros))
    val (viewModel, effects) = quickCategory(repository)
    viewModel.onStart("otros")
    viewModel.onSave()
    runCurrent()

    viewModel.onUseDuplicate()
    runCurrent()

    assertEquals(otros, assertNotNull(viewModel.uiState.value.draft).duplicate)
    assertTrue(effects.isEmpty())
  }

  @Test
  fun aFailedWriteKeepsTheDraftForAnotherTry() = runTest {
    val repository =
        QuickCategoryRepository(failure = DataError.Local(IllegalStateException("disk")))
    val (viewModel, effects) = quickCategory(repository)
    viewModel.onStart("GYM")
    viewModel.onIconoSelected(CategoryIcon.TAG)

    viewModel.onSave()
    runCurrent()

    val state = viewModel.uiState.value
    val draft = assertNotNull(state.draft)
    assertTrue(state.saveFailed)
    assertEquals("GYM", draft.nombre)
    assertEquals(CategoryIcon.TAG, draft.icono)
    assertFalse(draft.saving)
    assertTrue(draft.canSave)
    assertTrue(effects.isEmpty())
  }

  @Test
  fun closingMidSaveCreatesTheCategoryWithoutSelectingIt() = runTest {
    val repository = QuickCategoryRepository(gate = CompletableDeferred())
    val (viewModel, effects) = quickCategory(repository)
    viewModel.onStart("GYM")
    viewModel.onSave()
    runCurrent()

    viewModel.onClose()
    viewModel.onStart("Otra")
    repository.gate?.complete(Unit)
    runCurrent()

    assertEquals(1, repository.created.size)
    assertTrue(effects.isEmpty())
    assertEquals("Otra", assertNotNull(viewModel.uiState.value.draft).nombre)
  }

  @Test
  fun editsAreIgnoredWhileSaving() = runTest {
    val repository = QuickCategoryRepository(gate = CompletableDeferred())
    val (viewModel, _) = quickCategory(repository)
    viewModel.onStart("GYM")
    viewModel.onSave()
    runCurrent()

    viewModel.onNombreChanged("Other")

    assertEquals("GYM", assertNotNull(viewModel.uiState.value.draft).nombre)
  }

  private fun TestScope.quickCategory(
      repository: QuickCategoryRepository
  ): Pair<QuickCategoryViewModel, List<QuickCategoryEffect>> {
    val viewModel = QuickCategoryViewModel(repository, suggester, backgroundScope)
    val effects = mutableListOf<QuickCategoryEffect>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.effects.collect(effects::add)
    }
    return viewModel to effects
  }

  private fun categoria(
      id: String,
      nombre: String,
      oculta: Boolean = false,
      origen: CategoriaOrigen = CategoriaOrigen.USUARIO,
  ) =
      Categoria(
          id = id,
          nombre = nombre,
          padreId = null,
          icono = CategoryIcon.TAG,
          color = CategoryTint.NEUTRAL,
          origen = origen,
          oculta = oculta,
      )
}

private class QuickCategoryRepository(
    private val write: CategoriaWrite? = null,
    private val failure: DataError? = null,
    val gate: CompletableDeferred<Unit>? = null,
) : CategoriaRepository by UnsupportedCategoriaRepository {
  val created = mutableListOf<CategoriaDraft>()
  val unhidden = mutableListOf<String>()

  override suspend fun create(draft: CategoriaDraft): DataOutcome<CategoriaWrite> {
    gate?.await()
    if (failure != null) return DataOutcome.Failure(failure)
    created += draft
    return DataOutcome.Success(
        write
            ?: CategoriaWrite.Saved(
                Categoria(
                    id = "${draft.nombre.lowercase()}-id",
                    nombre = draft.nombre,
                    padreId = null,
                    icono = draft.icono,
                    color = draft.color,
                    origen = CategoriaOrigen.USUARIO,
                )
            )
    )
  }

  override suspend fun setOculta(id: String, oculta: Boolean): DataOutcome<Unit> {
    if (!oculta) unhidden += id
    return DataOutcome.Success(Unit)
  }
}
