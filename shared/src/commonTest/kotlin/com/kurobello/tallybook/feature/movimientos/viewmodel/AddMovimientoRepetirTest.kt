package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.FakeRecurrenciaRepository
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AddMovimientoRepetirTest {

  @Test
  fun savingWithoutRepetirInsertsOnlyTheMovimiento() = runTest {
    val f = fixture()
    f.viewModel.onFillManually()
    f.viewModel.onAmountChanged("5000")

    f.viewModel.onSubmit()
    runCurrent()

    assertEquals(1, f.movimientos.inserted.size)
    assertEquals(emptyList(), f.recurrencias.created)
    assertEquals(AddMovimientoEffect.Saved, f.viewModel.effects.first())
  }

  @Test
  fun savingWithRepetirCreatesTheRecurrenciaWithTheMovimientoInsteadOfInsertingIt() = runTest {
    val f = fixture()
    f.viewModel.onFillManually()
    f.viewModel.onAmountChanged("5000")
    f.viewModel.onRepetirChanged(true)
    f.viewModel.onFrecuenciaChanged(Frecuencia.SEMANAL)

    f.viewModel.onSubmit()
    runCurrent()

    assertEquals(emptyList(), f.movimientos.inserted)
    val (movimiento, frecuencia) = f.recurrencias.created.single()
    assertEquals(Frecuencia.SEMANAL, frecuencia)
    assertEquals(Money(500_000, Moneda.COP), movimiento.monto)
    val recurrencia = f.recurrencias.recurrencias.value.values.single()
    assertEquals(LocalDate(2026, 9, 22), recurrencia.proximaFecha)
    assertEquals(AddMovimientoEffect.Saved, f.viewModel.effects.first())
  }

  @Test
  fun repetirFromTheSentenceSummaryStartsMonthlyAndSavesFromThere() = runTest {
    val f = fixture()
    f.viewModel.onPhraseChanged("Pagué el arriendo 1.200.000")
    f.viewModel.onPhraseContinue()
    assertEquals(AddMovimientoStep.SUMMARY, f.viewModel.uiState.value.step)
    assertNull(f.viewModel.uiState.value.frecuencia)

    f.viewModel.onRepetirChanged(true)
    assertEquals(Frecuencia.MENSUAL, f.viewModel.uiState.value.frecuencia)
    f.viewModel.onSubmit()
    runCurrent()

    assertEquals(Frecuencia.MENSUAL, f.recurrencias.created.single().second)
    assertEquals(emptyList(), f.movimientos.inserted)
  }

  @Test
  fun turningRepetirOffSavesAOneOffMovimientoAndIgnoresAFrecuenciaWhileOff() = runTest {
    val f = fixture()
    f.viewModel.onFillManually()
    f.viewModel.onAmountChanged("5000")
    f.viewModel.onRepetirChanged(true)
    f.viewModel.onRepetirChanged(false)
    f.viewModel.onFrecuenciaChanged(Frecuencia.ANUAL)
    assertNull(f.viewModel.uiState.value.frecuencia)

    f.viewModel.onSubmit()
    runCurrent()

    assertEquals(1, f.movimientos.inserted.size)
    assertEquals(emptyList(), f.recurrencias.created)
  }

  @Test
  fun repetirMarksTheDraftUnsavedAndDiscardResetsIt() = runTest {
    val f = fixture()
    f.viewModel.onFillManually()
    assertFalse(f.viewModel.uiState.value.hasUnsavedChanges)

    f.viewModel.onRepetirChanged(true)
    assertTrue(f.viewModel.uiState.value.hasUnsavedChanges)

    f.viewModel.onDiscardChanges()
    assertNull(f.viewModel.uiState.value.frecuencia)
  }

  @Test
  fun aFailedCreateKeepsTheDraftWithItsRepetirAndShowsTheSaveError() = runTest {
    val f = fixture()
    f.recurrencias.failure = DataError.Local(IllegalStateException("disk"))
    f.viewModel.onFillManually()
    f.viewModel.onAmountChanged("5000")
    f.viewModel.onRepetirChanged(true)

    f.viewModel.onSubmit()
    runCurrent()

    val state = f.viewModel.uiState.value
    assertEquals(MovimientoFormError.SAVE_FAILED, state.error)
    assertEquals(Frecuencia.MENSUAL, state.frecuencia)
    assertFalse(state.isSubmitting)
  }

  private class Fixture(
      val viewModel: AddMovimientoViewModel,
      val movimientos: AddFakeMovimientos,
      val recurrencias: FakeRecurrenciaRepository,
  )

  private fun TestScope.fixture(): Fixture {
    val movimientos = AddFakeMovimientos()
    val recurrencias = FakeRecurrenciaRepository()
    val viewModel =
        createAddViewModel(
            movimientoRepository = movimientos,
            categoriaRepository = AddFakeCategorias(DataOutcome.Success(addSeedCategorias())),
            preferencesRepository = addPreferences(Moneda.COP),
            recurrenciaRepository = recurrencias,
            scope = backgroundScope,
        )
    runCurrent()
    return Fixture(viewModel, movimientos, recurrencias)
  }
}
