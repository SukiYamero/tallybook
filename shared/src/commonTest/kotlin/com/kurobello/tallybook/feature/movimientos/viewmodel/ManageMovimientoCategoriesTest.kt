package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.DataOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ManageMovimientoCategoriesTest {
  @Test
  fun theEditorOpensWithEveryStoredCategoryAndSavesWithoutChangesAsANoOp() = runTest {
    val f = fixture(stored = listOf("food", "health"))

    assertEquals(listOf("food", "health"), f.form().categoriaIds)
    assertFalse(f.viewModel.uiState.value.session!!.hasUnsavedChanges)
    f.viewModel.onSubmit()
    runCurrent()

    assertEquals(0, f.repository.updates)
  }

  @Test
  fun addingAndRemovingCategoriesPersistsTheNewSelection() = runTest {
    val f = fixture(stored = listOf("food", "health"))

    f.viewModel.onCategoriasChanged(listOf("food", "health", "home"))
    f.viewModel.onCategoriaRemoved("food")
    f.viewModel.onSubmit()
    runCurrent()

    assertEquals(listOf("health", "home"), f.repository.changes?.categoriaIds)
    assertEquals(listOf("health", "home"), f.repository.current?.categoriaIds)
  }

  @Test
  fun theLastCategoryCannotBeRemoved() = runTest {
    val f = fixture(stored = listOf("food"))

    f.viewModel.onCategoriaRemoved("food")

    assertEquals(listOf("food"), f.form().categoriaIds)
  }

  @Test
  fun aSeventhCategoryIsIgnored() = runTest {
    val six = listOf("food", "health", "home", "gifts", "pets", "leisure")
    val f = fixture(stored = six, catalog = (six + "transport").toList())

    f.viewModel.onCategoriasChanged(six + "transport")

    assertEquals(six, f.form().categoriaIds)
  }

  @Test
  fun aPendingMovimientoLeavesOtrosOnceARealCategoryIsChosen() = runTest {
    val f =
        fixture(stored = listOf(CATEGORIA_OTROS_ID), catalog = listOf("food", CATEGORIA_OTROS_ID))

    f.viewModel.onCategoriasChanged(listOf("food"))
    f.viewModel.onSubmit()
    runCurrent()

    assertEquals(listOf("food"), f.repository.changes?.categoriaIds)
  }

  @Test
  fun anUnavailableCatalogKeepsTheStoredCategoriesValidButBlocksNewOnes() = runTest {
    val f = fixture(stored = listOf("food", "health"), catalog = emptyList())

    f.viewModel.onCategoriasChanged(listOf("food", "health", "home"))
    assertEquals(listOf("food", "health"), f.form().categoriaIds)
    f.viewModel.onAmountChanged("20.00")
    f.viewModel.onSubmit()
    runCurrent()

    assertEquals(listOf("food", "health"), f.repository.changes?.categoriaIds)
  }

  @Test
  fun undoRestoresEveryCategoryOfADeletedMovimiento() = runTest {
    val f = fixture(stored = listOf("food", "health", "home"))

    f.viewModel.onRequestDelete()
    f.viewModel.onConfirmDelete()
    runCurrent()
    f.viewModel.onSheetHidden(assertNotNull(f.viewModel.uiState.value.session).token)
    val feedback = assertNotNull(f.viewModel.uiState.value.feedback)
    f.viewModel.onFeedbackAction(feedback.token, feedback.revision)
    runCurrent()

    assertEquals(listOf("food", "health", "home"), f.repository.restored?.categoriaIds)
    assertTrue(f.viewModel.uiState.value.feedback?.kind == MovimientoFeedbackKind.Restored)
  }

  private fun TestScope.fixture(
      stored: List<String>,
      catalog: List<String> =
          listOf("food", "health", "home", "gifts", "pets", "leisure", "transport"),
  ): ManageMovimientoFixture {
    val f = ManageMovimientoFixture(backgroundScope)
    f.repository.current = managementMovimiento().copy(categoriaIds = stored)
    f.categorias.values.value = DataOutcome.Success(catalog.map { managementCategoria(it) })
    f.viewModel.open("movement")
    runCurrent()
    return f
  }

  private fun ManageMovimientoFixture.form(): EditMovimientoUiState =
      assertNotNull(viewModel.uiState.value.session?.form)
}
