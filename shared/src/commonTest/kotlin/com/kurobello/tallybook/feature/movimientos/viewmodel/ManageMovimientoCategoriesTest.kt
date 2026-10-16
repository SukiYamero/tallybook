package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Etiqueta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ManageMovimientoCategoriesTest {
  @Test
  fun theEditorOpensWithTheStoredCategoryAndTagsAndSavesWithoutChangesAsANoOp() = runTest {
    val f = fixture(stored = "food", tags = listOf("trip", "work"))

    assertEquals("food", f.form().categoriaId)
    assertEquals(listOf("trip", "work"), f.form().etiquetaIds)
    assertFalse(f.viewModel.uiState.value.session!!.hasUnsavedChanges)
    f.viewModel.onSubmit()
    runCurrent()

    assertEquals(0, f.repository.updates)
  }

  @Test
  fun choosingAnotherCategoryReplacesItAndKeepsTheTags() = runTest {
    val f = fixture(stored = "food", tags = listOf("trip"))

    f.viewModel.onCategoriaSelected("home")
    f.viewModel.onSubmit()
    runCurrent()

    assertEquals("home", f.repository.changes?.categoriaId)
    assertEquals(listOf("trip"), f.repository.changes?.etiquetaIds)
    assertEquals("home", f.repository.current?.categoriaId)
  }

  @Test
  fun aPendingMovimientoLeavesOtrosOnceARealCategoryIsChosen() = runTest {
    val f = fixture(stored = CATEGORIA_OTROS_ID, catalog = listOf("food", CATEGORIA_OTROS_ID))

    f.viewModel.onCategoriaSelected("food")
    f.viewModel.onSubmit()
    runCurrent()

    assertEquals("food", f.repository.changes?.categoriaId)
  }

  @Test
  fun anUnavailableCatalogKeepsTheStoredCategoryValidButBlocksNewOnes() = runTest {
    val f = fixture(stored = "food", catalog = emptyList())

    f.viewModel.onCategoriaSelected("home")
    assertEquals("food", f.form().categoriaId)
    f.viewModel.onAmountChanged("20.00")
    f.viewModel.onSubmit()
    runCurrent()

    assertEquals("food", f.repository.changes?.categoriaId)
  }

  @Test
  fun aTagDeletedWhileEditingIsLeftOutOfTheSave() = runTest {
    val f = fixture(stored = "food", tags = listOf("trip", "work"))
    f.etiquetas.etiquetas.value = listOf(etiqueta("work"))

    f.viewModel.onAmountChanged("20.00")
    f.viewModel.onSubmit()
    runCurrent()

    assertEquals(listOf("work"), f.repository.changes?.etiquetaIds)
  }

  @Test
  fun undoRestoresTheCategoryAndTagsOfADeletedMovimiento() = runTest {
    val f = fixture(stored = "home", tags = listOf("trip", "work"))

    f.viewModel.onRequestDelete()
    f.viewModel.onConfirmDelete()
    runCurrent()
    f.viewModel.onSheetHidden(assertNotNull(f.viewModel.uiState.value.session).token)
    val feedback = assertNotNull(f.viewModel.uiState.value.feedback)
    f.viewModel.onFeedbackAction(feedback.token, feedback.revision)
    runCurrent()

    assertEquals("home", f.repository.restored?.categoriaId)
    assertEquals(listOf("trip", "work"), f.repository.restored?.etiquetaIds)
    assertTrue(f.viewModel.uiState.value.feedback?.kind == MovimientoFeedbackKind.Restored)
  }

  private fun TestScope.fixture(
      stored: String,
      tags: List<String> = emptyList(),
      catalog: List<String> = listOf("food", "health", "home"),
  ): ManageMovimientoFixture {
    val f = ManageMovimientoFixture(backgroundScope)
    f.repository.current = managementMovimiento().copy(categoriaId = stored, etiquetaIds = tags)
    f.categorias.values.value = DataOutcome.Success(catalog.map { managementCategoria(it) })
    f.etiquetas.etiquetas.value = tags.map(::etiqueta)
    f.viewModel.open("movement")
    runCurrent()
    return f
  }

  private fun etiqueta(id: String) = Etiqueta(id, id, Instant.parse("2026-01-01T00:00:00Z"))

  private fun ManageMovimientoFixture.form(): EditMovimientoUiState =
      assertNotNull(viewModel.uiState.value.session?.form)
}
