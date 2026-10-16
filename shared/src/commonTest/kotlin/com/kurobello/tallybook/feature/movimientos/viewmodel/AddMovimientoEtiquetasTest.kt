package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.FakeEtiquetaRepository
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.MAX_ETIQUETAS_POR_MOVIMIENTO
import com.kurobello.tallybook.core.model.Moneda
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AddMovimientoEtiquetasTest {

  @Test
  fun theSentenceSelectsExistingTagsAndContinueSavesThem() = runTest {
    val movimientos = AddFakeMovimientos()
    val viewModel = entryViewModel(movimientos = movimientos)

    type(viewModel, "Gasté 20.000 en cine con la EMPRESA")
    assertEquals(listOf("empresa"), viewModel.uiState.value.sentence.etiquetas.map { it.id })

    viewModel.onPhraseContinue()
    assertEquals(listOf("empresa"), viewModel.uiState.value.summary.etiquetas.map { it.id })
    viewModel.onSubmit()
    runCurrent()

    assertEquals(listOf("empresa"), movimientos.inserted.single().etiquetaIds)
    assertEquals("cat_cine", movimientos.inserted.single().categoriaId)
  }

  @Test
  fun detectionNeverCreatesTagsAndStopsAtTheLimit() = runTest {
    val names = List(MAX_ETIQUETAS_POR_MOVIMIENTO + 1) { "uno$it" }
    val repository = FakeEtiquetaRepository(names.map(::etiqueta))
    val viewModel = entryViewModel(repository)

    type(viewModel, names.joinToString(" ") + " y nueva")

    assertEquals(names.take(MAX_ETIQUETAS_POR_MOVIMIENTO), viewModel.uiState.value.etiquetaIds)
    assertEquals(names, repository.etiquetas.value.map { it.id })
  }

  @Test
  fun theSentenceOwnsTheTagsUntilTheUserAddsOrRemovesOne() = runTest {
    val viewModel = entryViewModel()
    type(viewModel, "Almuerzo empresa")
    type(viewModel, "Hotel viaje chile")
    assertEquals(listOf("viaje-chile"), viewModel.uiState.value.etiquetaIds)

    viewModel.onEtiquetaRemoved("viaje-chile")
    type(viewModel, "Almuerzo empresa")
    viewModel.onPhraseContinue()

    assertEquals(emptyList(), viewModel.uiState.value.etiquetaIds)
    assertTrue(viewModel.uiState.value.etiquetasEdited)
  }

  @Test
  fun clearingTheSentenceDropsDetectedTagsButKeepsHandPickedOnes() = runTest {
    val viewModel = entryViewModel()
    type(viewModel, "Almuerzo empresa")
    viewModel.onPhraseChanged("")
    assertEquals(emptyList(), viewModel.uiState.value.etiquetaIds)

    viewModel.onEtiquetasChanged(listOf("casa"))
    type(viewModel, "Almuerzo empresa")
    assertEquals(listOf("casa"), viewModel.uiState.value.etiquetaIds)
    viewModel.onPhraseChanged("  ")

    assertEquals(listOf("casa"), viewModel.uiState.value.etiquetaIds)
    assertFalse(viewModel.uiState.value.hasUnsavedChanges)
  }

  @Test
  fun tagsPickedInTheDialogDoNotMakeItDirty() = runTest {
    val viewModel = entryViewModel()

    viewModel.onEtiquetasChanged(listOf("casa", "empresa"))

    assertEquals(listOf("casa", "empresa"), viewModel.uiState.value.etiquetaIds)
    assertFalse(viewModel.uiState.value.hasUnsavedChanges)
  }

  @Test
  fun fillingManuallyKeepsHandPickedTagsAndDropsDetectedOnes() = runTest {
    val detected = entryViewModel()
    type(detected, "Almuerzo empresa")
    detected.onFillManually()
    assertEquals(emptyList(), detected.uiState.value.etiquetaIds)

    val picked = entryViewModel()
    picked.onEtiquetasChanged(listOf("casa"))
    type(picked, "Almuerzo empresa")
    picked.onFillManually()

    assertEquals(AddMovimientoStep.FORM, picked.uiState.value.step)
    assertEquals(listOf("casa"), picked.uiState.value.etiquetaIds)
    assertTrue(picked.uiState.value.etiquetasEdited)
  }

  @Test
  fun aPickerSelectionIsDistinctAndCappedAtTheLimit() = runTest {
    val names = List(MAX_ETIQUETAS_POR_MOVIMIENTO + 1) { "tag$it" }
    val viewModel = entryViewModel(FakeEtiquetaRepository(names.map(::etiqueta)))

    viewModel.onEtiquetasChanged(listOf(names[0], names[0]) + names.drop(1))

    assertEquals(names.take(MAX_ETIQUETAS_POR_MOVIMIENTO), viewModel.uiState.value.etiquetaIds)
  }

  @Test
  fun creatingATagSelectsItOrTheExistingDuplicateWhileUnderTheLimit() = runTest {
    val repository = FakeEtiquetaRepository(listOf(etiqueta("casa")))
    val viewModel = entryViewModel(repository)

    viewModel.onCreateEtiqueta("  Viaje Chile ")
    runCurrent()
    val created = repository.etiquetas.value.single { it.nombre == "Viaje Chile" }
    assertEquals(listOf(created.id), viewModel.uiState.value.etiquetaIds)
    assertEquals(listOf(created), viewModel.uiState.value.sentence.etiquetas)

    viewModel.onCreateEtiqueta("CASA")
    runCurrent()
    assertEquals(listOf(created.id, "casa"), viewModel.uiState.value.etiquetaIds)
    assertEquals(2, repository.etiquetas.value.size)
    assertTrue(viewModel.uiState.value.etiquetasEdited)
  }

  @Test
  fun aTagCreatedAtTheLimitIsNotSelected() = runTest {
    val names = List(MAX_ETIQUETAS_POR_MOVIMIENTO) { "tag$it" }
    val repository = FakeEtiquetaRepository(names.map(::etiqueta))
    val viewModel = entryViewModel(repository)
    viewModel.onEtiquetasChanged(names)

    viewModel.onCreateEtiqueta("Otra")
    runCurrent()

    assertEquals(names, viewModel.uiState.value.etiquetaIds)
    assertTrue(repository.etiquetas.value.any { it.nombre == "Otra" })
  }

  @Test
  fun anUnavailableTagsCatalogStillLetsTheMovimientoBeSavedWithoutTags() = runTest {
    val repository = FakeEtiquetaRepository(defaultEtiquetas())
    repository.failure = DataError.Local(IllegalStateException())
    val movimientos = AddFakeMovimientos()
    val viewModel = entryViewModel(repository, movimientos)

    val state = viewModel.uiState.value
    assertEquals(EtiquetasLoad.UNAVAILABLE, state.etiquetasLoad)
    assertNull(state.loadError)
    type(viewModel, "Gasté 20.000 con la empresa")
    viewModel.onEtiquetasChanged(listOf("casa"))
    viewModel.onPhraseContinue()
    viewModel.onSubmit()
    runCurrent()

    assertEquals(emptyList(), movimientos.inserted.single().etiquetaIds)
  }

  @Test
  fun retryingTheTagsCatalogMakesTagsAvailableAgain() = runTest {
    val repository = FakeEtiquetaRepository(defaultEtiquetas())
    repository.failure = DataError.Local(IllegalStateException())
    val viewModel = entryViewModel(repository)

    repository.failure = null
    viewModel.onRetryEtiquetas()
    runCurrent()

    assertEquals(EtiquetasLoad.READY, viewModel.uiState.value.etiquetasLoad)
    assertEquals(defaultEtiquetas().size, viewModel.uiState.value.etiquetas.size)
  }

  @Test
  fun discardingTheEntryClearsEveryTag() = runTest {
    val viewModel = entryViewModel()
    viewModel.onEtiquetasChanged(listOf("casa"))
    type(viewModel, "Almuerzo empresa")
    viewModel.onPhraseContinue()

    viewModel.onDiscardChanges()

    assertEquals(emptyList(), viewModel.uiState.value.etiquetaIds)
    assertFalse(viewModel.uiState.value.etiquetasEdited)
  }

  private fun TestScope.type(viewModel: AddMovimientoViewModel, text: String) {
    viewModel.onPhraseChanged(text)
    advanceTimeBy(PREVIEW_DEBOUNCE)
    runCurrent()
  }

  private fun TestScope.entryViewModel(
      etiquetas: FakeEtiquetaRepository = FakeEtiquetaRepository(defaultEtiquetas()),
      movimientos: AddFakeMovimientos = AddFakeMovimientos(),
  ): AddMovimientoViewModel =
      createAddViewModel(
              movimientoRepository = movimientos,
              categoriaRepository = AddFakeCategorias(DataOutcome.Success(addSeedCategorias())),
              preferencesRepository = addPreferences(Moneda.COP),
              etiquetaRepository = etiquetas,
              scope = backgroundScope,
          )
          .also { runCurrent() }

  private fun defaultEtiquetas() =
      listOf(
          etiqueta("empresa", "Empresa"),
          etiqueta("viaje-chile", "Viaje Chile"),
          etiqueta("casa"),
      )

  private fun etiqueta(id: String, nombre: String = id) =
      Etiqueta(id, nombre, Instant.parse("2026-01-01T00:00:00Z"))

  private companion object {
    const val PREVIEW_DEBOUNCE = 250L
  }
}
