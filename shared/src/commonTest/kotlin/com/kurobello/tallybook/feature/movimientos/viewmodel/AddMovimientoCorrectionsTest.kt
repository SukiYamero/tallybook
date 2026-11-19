package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.FakeCorreccionFraseRepository
import com.kurobello.tallybook.core.data.FakeCuentaRepository
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.voice.DictationEvent
import com.kurobello.tallybook.core.data.voice.FakeVoiceDictation
import com.kurobello.tallybook.core.model.CampoCorreccion
import com.kurobello.tallybook.core.model.CorreccionFrase
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.OrigenFrase
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

private const val LUNCH = "almuerzo 20 mil con la visa"
private const val TWO_PARTS = "Compré 2 hamburguesas por 20mil y 3 gaseosas fueron 30mil"
private const val DEBOUNCE_FOR_TESTS = 300L

/** What Save records about the values the user corrected after Continue, and what it teaches. */
@OptIn(ExperimentalCoroutinesApi::class)
class AddMovimientoCorrectionsTest {

  private val correcciones = FakeCorreccionFraseRepository()
  private val clock = AddFixedClock(ADD_TEST_NOW)
  private var nextId = 0
  private val ids = IdGenerator { "id-${nextId++}" }

  private val records: List<CorreccionFrase>
    get() = correcciones.records.value

  @Test
  fun aHandPickedMedioIsRecorded() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, LUNCH)
    viewModel.onPhraseContinue()
    viewModel.onMedioSelected(MedioPago.CREDITO)
    save(viewModel)

    val record = records.single()
    assertEquals(CampoCorreccion.MEDIO, record.campo)
    assertEquals(null, record.leido)
    assertEquals(null, record.propuesto)
    assertEquals(MedioPago.CREDITO.name, record.guardado)
    assertEquals(OrigenFrase.ESCRITO, record.origen)
    assertTrue("almuerzo" in record.palabras && "visa" in record.palabras, "${record.palabras}")
    assertEquals(ADD_TEST_NOW, record.createdAt)
  }

  @Test
  fun defaultsAreNotCorrections() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, LUNCH)
    viewModel.onPhraseContinue()
    save(viewModel)

    assertTrue(records.isEmpty())
    assertEquals(0, correcciones.insertCalls)
  }

  @Test
  fun aMedioAdaptedByACuentaChangeIsNotRecorded() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, LUNCH)
    viewModel.onPhraseContinue()
    viewModel.onCuentaSelected(NU.id)
    assertEquals(MedioPago.CREDITO, viewModel.uiState.value.cuentaField.medio)
    save(viewModel)

    val record = records.single()
    assertEquals(CampoCorreccion.CUENTA, record.campo)
    assertEquals(null, record.propuesto, "the default cuenta is not the sentence's proposal")
    assertEquals(NU.id, record.guardado)
  }

  @Test
  fun aHandPickEqualToTheProposalStillCounts() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, "almuerzo 20 mil con débito")
    viewModel.onPhraseContinue()
    viewModel.onMedioSelected(MedioPago.DEBITO)
    save(viewModel)

    val record = records.single()
    assertEquals(CampoCorreccion.MEDIO, record.campo)
    assertEquals(MedioPago.DEBITO.name, record.leido)
    assertEquals(MedioPago.DEBITO.name, record.propuesto)
    assertEquals(MedioPago.DEBITO.name, record.guardado)
  }

  @Test
  fun aFailedSaveRecordsNothing() = runTest {
    val movimientos =
        correctionsMovimientos().apply {
          insertResult = DataOutcome.Failure(DataError.Local(IllegalStateException("disk full")))
        }
    val viewModel = correctionsViewModel(movimientos)

    type(viewModel, LUNCH)
    viewModel.onPhraseContinue()
    viewModel.onMedioSelected(MedioPago.CREDITO)
    save(viewModel)

    assertEquals(MovimientoFormError.SAVE_FAILED, viewModel.uiState.value.error)
    assertTrue(records.isEmpty())
  }

  @Test
  fun aFailedRecordWriteStillSavesTheMovimiento() = runTest {
    correcciones.insertResult =
        DataOutcome.Failure(DataError.Local(IllegalStateException("disk full")))
    val movimientos = correctionsMovimientos()
    val viewModel = correctionsViewModel(movimientos)

    type(viewModel, LUNCH)
    viewModel.onPhraseContinue()
    viewModel.onMedioSelected(MedioPago.CREDITO)
    save(viewModel)

    assertEquals(1, movimientos.inserted.size)
    assertEquals(1, correcciones.insertCalls)
    assertEquals(AddMovimientoEffect.Saved(null), viewModel.effects.first())
  }

  @Test
  fun fillManuallyRecordsNothing() = runTest {
    val movimientos = correctionsMovimientos()
    val viewModel = correctionsViewModel(movimientos)

    type(viewModel, LUNCH)
    viewModel.onFillManually()
    viewModel.onAmountChanged("20000")
    viewModel.onCategoriaSelected("cat_cine")
    viewModel.onMedioSelected(MedioPago.CREDITO)
    viewModel.onTituloChanged("almuerzo")
    save(viewModel)

    assertEquals(1, movimientos.inserted.size)
    assertTrue(records.isEmpty())
  }

  @Test
  fun dictatedSentencesAreMarked() = runTest {
    val voice = FakeVoiceDictation()
    val viewModel = correctionsViewModel(voice = voice)

    viewModel.onVoiceStart()
    runCurrent()
    voice.events.send(DictationEvent.Final(LUNCH))
    testScheduler.advanceTimeBy(DEBOUNCE_FOR_TESTS)
    runCurrent()
    viewModel.onPhraseContinue()
    viewModel.onMedioSelected(MedioPago.CREDITO)
    save(viewModel)

    assertEquals(OrigenFrase.DICTADO, records.single().origen)
  }

  @Test
  fun discardedDictationLeavesATypedSentenceTyped() = runTest {
    val voice = FakeVoiceDictation()
    val viewModel = correctionsViewModel(voice = voice)

    type(viewModel, LUNCH)
    viewModel.onVoiceStart()
    runCurrent()
    voice.events.send(DictationEvent.Partial("con amigos"))
    runCurrent()
    viewModel.onVoiceDiscard()
    runCurrent()
    viewModel.onPhraseContinue()
    viewModel.onMedioSelected(MedioPago.CREDITO)
    save(viewModel)

    assertEquals(OrigenFrase.ESCRITO, records.single().origen)
  }

  @Test
  fun aRewrittenTituloRecordsNothing() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, LUNCH)
    viewModel.onPhraseContinue()
    viewModel.onEditMovimiento()
    viewModel.onTituloChanged("cena de cumpleaños en casa")
    save(viewModel)

    assertTrue(records.none { it.campo == CampoCorreccion.TITULO }, "$records")
  }

  @Test
  fun aCorrectedWordInTheTituloIsRecorded() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, LUNCH)
    viewModel.onPhraseContinue()
    viewModel.onEditMovimiento()
    viewModel.onTituloChanged("almuerzo 20 mil con la vista")
    save(viewModel)

    val record = records.single()
    assertEquals(CampoCorreccion.TITULO, record.campo)
    assertEquals(null, record.leido)
    assertEquals("visa", record.propuesto)
    assertEquals("vista", record.guardado)
  }

  @Test
  fun oneOfTwoTituloWordsCorrectedIsRecorded() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, "uber vissa 8000")
    viewModel.onPhraseContinue()
    viewModel.onEditMovimiento()
    viewModel.onTituloChanged("uber visa 8000")
    save(viewModel)

    val record = records.single()
    assertEquals(CampoCorreccion.TITULO, record.campo)
    assertEquals("vissa", record.propuesto)
    assertEquals("visa", record.guardado)
  }

  @Test
  fun eachSavedPartRecordsItsOwnWords() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, TWO_PARTS)
    viewModel.onPhraseContinue()
    assertEquals(AddMovimientoStep.REVIEW, viewModel.uiState.value.step)
    viewModel.onPartCategoriaSelected(0, "cat_cine")
    viewModel.onMedioSelected(MedioPago.CREDITO)
    save(viewModel)

    val categorias = records.filter { it.campo == CampoCorreccion.CATEGORIA }
    assertEquals(1, categorias.size)
    assertEquals("cat_restaurante", categorias.single().leido)
    assertEquals("cat_cine", categorias.single().guardado)
    assertTrue("hamburguesa" in categorias.single().palabras)
    assertFalse("gaseosa" in categorias.single().palabras)
    val medios = records.filter { it.campo == CampoCorreccion.MEDIO }
    assertEquals(2, medios.size)
    assertEquals(listOf(MedioPago.CREDITO.name, MedioPago.CREDITO.name), medios.map { it.guardado })
    assertTrue("hamburguesa" in medios[0].palabras && "gaseosa" !in medios[0].palabras)
    assertTrue("gaseosa" in medios[1].palabras && "hamburguesa" !in medios[1].palabras)
    assertEquals(3, records.size)
  }

  @Test
  fun aPartFormMedioPickCountsForThatPartOnly() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, TWO_PARTS)
    viewModel.onPhraseContinue()
    viewModel.onPartEdit(1)
    viewModel.onMedioSelected(MedioPago.CREDITO)
    viewModel.onPartDone()
    save(viewModel)

    val record = records.single()
    assertEquals(CampoCorreccion.MEDIO, record.campo)
    assertTrue("gaseosa" in record.palabras)
  }

  @Test
  fun aPartFormCuentaPickDoesNotRecordItsAdaptedMedio() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, TWO_PARTS)
    viewModel.onPhraseContinue()
    viewModel.onPartEdit(0)
    viewModel.onCuentaSelected(NU.id)
    viewModel.onPartDone()
    save(viewModel)

    assertEquals(listOf(CampoCorreccion.CUENTA), records.map { it.campo })
  }

  @Test
  fun joiningRecordsTheStructure() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, TWO_PARTS)
    viewModel.onPhraseContinue()
    viewModel.onJoinParts()
    save(viewModel)

    val record = records.single()
    assertEquals(CampoCorreccion.PARTES, record.campo)
    assertEquals("2", record.propuesto)
    assertEquals("JOINED", record.guardado)
    assertTrue("hamburguesa" in record.palabras && "gaseosa" in record.palabras)
  }

  @Test
  fun aJoinedMovimientoIsComparedWithTheWholeSentence() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, TWO_PARTS)
    viewModel.onPhraseContinue()
    viewModel.onJoinParts()
    viewModel.onMedioSelected(MedioPago.CREDITO)
    save(viewModel)

    assertEquals(
        listOf(CampoCorreccion.PARTES, CampoCorreccion.MEDIO),
        records.map { it.campo },
    )
    val medio = records.last()
    assertTrue("hamburguesa" in medio.palabras && "gaseosa" in medio.palabras)
  }

  @Test
  fun removingAPartRecordsTheSavedCount() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, TWO_PARTS)
    viewModel.onPhraseContinue()
    viewModel.onPartRemoved(1)
    save(viewModel)

    val record = records.single()
    assertEquals(CampoCorreccion.PARTES, record.campo)
    assertEquals("2", record.propuesto)
    assertEquals("1", record.guardado)
  }

  @Test
  fun theThirdSentencePreselectsWhatTheFirstTwoCorrected() = runTest {
    val first = lunchRound()
    assertEquals(MedioPago.EFECTIVO, first.medio)
    pickAndSave(first.viewModel, MedioPago.CREDITO)

    val second = lunchRound()
    assertEquals(MedioPago.EFECTIVO, second.medio)
    pickAndSave(second.viewModel, MedioPago.CREDITO)

    val third = lunchRound()
    assertEquals(MedioPago.CREDITO, third.medio)
    assertEquals(SelectionOwner.SENTENCE, third.viewModel.uiState.value.cuentaChoice.owner)
    pickAndSave(third.viewModel, MedioPago.DEBITO)

    val fourth = lunchRound()
    assertEquals(MedioPago.EFECTIVO, fourth.medio)
  }

  @Test
  fun aWordRepeatedAcrossPartsOfOneSaveIsOnlyOffered() = runTest {
    val first = correctionsViewModel()
    type(first, "uber 8 mil y uber 12 mil")
    first.onPhraseContinue()
    assertEquals(AddMovimientoStep.REVIEW, first.uiState.value.step)
    first.onCuentaSelected(NU.id)
    save(first)
    assertEquals(2, records.count { it.campo == CampoCorreccion.CUENTA && "uber" in it.palabras })

    clock.instant += 1.minutes
    val next = correctionsViewModel()
    type(next, "uber 10 mil")

    val cuentaField = next.uiState.value.cuentaField
    assertFalse(cuentaField.selected == NU, "$cuentaField")
    assertTrue(NU in cuentaField.suggestions, "$cuentaField")
  }

  @Test
  fun aMedioPickThatSwapsTheCuentaDropsTheCuentaPick() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, LUNCH)
    viewModel.onPhraseContinue()
    viewModel.onCuentaSelected(NU.id)
    viewModel.onMedioSelected(MedioPago.EFECTIVO)
    assertEquals(COP_CASH, viewModel.uiState.value.cuentaField.selected)
    save(viewModel)

    assertEquals(listOf(CampoCorreccion.MEDIO), records.map { it.campo })
  }

  @Test
  fun aMedioPickThatKeepsTheCuentaKeepsTheCuentaPick() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, LUNCH)
    viewModel.onPhraseContinue()
    viewModel.onCuentaSelected(NEQUI.id)
    viewModel.onMedioSelected(MedioPago.TRANSFERENCIA)
    assertEquals(NEQUI, viewModel.uiState.value.cuentaField.selected)
    save(viewModel)

    assertEquals(
        setOf(CampoCorreccion.CUENTA, CampoCorreccion.MEDIO),
        records.map { it.campo }.toSet(),
    )
  }

  @Test
  fun aCuentaPickThatAdaptsTheMedioDropsTheMedioPick() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, LUNCH)
    viewModel.onPhraseContinue()
    viewModel.onMedioSelected(MedioPago.CREDITO)
    viewModel.onCuentaSelected(NEQUI.id)
    assertEquals(MedioPago.DEBITO, viewModel.uiState.value.cuentaField.medio)
    save(viewModel)

    assertEquals(listOf(CampoCorreccion.CUENTA), records.map { it.campo })
  }

  @Test
  fun aTipoChangeThatAdaptsTheMedioDropsTheMedioPick() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, LUNCH)
    viewModel.onPhraseContinue()
    viewModel.onMedioSelected(MedioPago.DEBITO)
    viewModel.onEditMovimiento()
    viewModel.onTipoChanged(TipoMovimiento.INGRESO)
    assertEquals(MedioPago.TRANSFERENCIA, viewModel.uiState.value.cuentaField.medio)
    save(viewModel)

    assertEquals(listOf(CampoCorreccion.TIPO), records.map { it.campo })
  }

  @Test
  fun aPartFormMedioPickThatSwapsTheCuentaDropsTheCuentaPick() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, TWO_PARTS)
    viewModel.onPhraseContinue()
    viewModel.onPartEdit(0)
    viewModel.onCuentaSelected(NU.id)
    viewModel.onMedioSelected(MedioPago.EFECTIVO)
    viewModel.onPartDone()
    save(viewModel)

    val record = records.single()
    assertEquals(CampoCorreccion.MEDIO, record.campo)
    assertTrue("hamburguesa" in record.palabras)
  }

  @Test
  fun aPartCuentaPickThatAdaptsTheSharedMedioDropsItForThatPart() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, TWO_PARTS)
    viewModel.onPhraseContinue()
    viewModel.onMedioSelected(MedioPago.CREDITO)
    viewModel.onPartEdit(1)
    viewModel.onCuentaSelected(NEQUI.id)
    viewModel.onPartDone()
    save(viewModel)

    val medio = records.single { it.campo == CampoCorreccion.MEDIO }
    assertTrue("hamburguesa" in medio.palabras, "only the part that kept the shared pick")
    val cuenta = records.single { it.campo == CampoCorreccion.CUENTA }
    assertTrue("gaseosa" in cuenta.palabras)
    assertEquals(2, records.size)
  }

  @Test
  fun aConfirmingTipoOrFechaPickInAPartFormCounts() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, TWO_PARTS)
    viewModel.onPhraseContinue()
    val shared = viewModel.uiState.value
    viewModel.onPartEdit(0)
    viewModel.onTipoChanged(shared.tipo)
    viewModel.onFechaChanged(shared.fecha)
    viewModel.onPartDone()
    save(viewModel)

    assertEquals(
        setOf(CampoCorreccion.TIPO, CampoCorreccion.FECHA),
        records.map { it.campo }.toSet(),
    )
    assertTrue(records.all { "hamburguesa" in it.palabras })
    assertEquals(2, records.size)
  }

  @Test
  fun aCurrencyReformatAfterJoinIsNotAnAmountEdit() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, TWO_PARTS)
    viewModel.onPhraseContinue()
    viewModel.onJoinParts()
    val joinedAmount = viewModel.uiState.value.amountInput
    viewModel.onCuentaSelected(USD_CASH.id)
    assertFalse(joinedAmount == viewModel.uiState.value.amountInput, "the text is reformatted")
    save(viewModel)

    assertEquals(
        listOf(CampoCorreccion.PARTES, CampoCorreccion.CUENTA),
        records.map { it.campo },
    )
  }

  @Test
  fun anAmountEditAfterJoinIsRecorded() = runTest {
    val viewModel = correctionsViewModel()

    type(viewModel, TWO_PARTS)
    viewModel.onPhraseContinue()
    viewModel.onJoinParts()
    viewModel.onAmountChanged("45000")
    save(viewModel)

    assertEquals(
        listOf(CampoCorreccion.PARTES, CampoCorreccion.MONTO),
        records.map { it.campo },
    )
  }

  @Test
  fun rulesAreLearnedOnTheComputationDispatcher() = runTest {
    val computation = CountingDispatcher(StandardTestDispatcher(testScheduler))
    correctionsViewModel(computationDispatcher = computation)
    val before = computation.dispatches

    correcciones.records.value = listOf(record(CampoCorreccion.MEDIO, MedioPago.CREDITO.name))
    runCurrent()

    assertTrue(computation.dispatches > before)
  }

  private fun record(campo: CampoCorreccion, guardado: String) =
      CorreccionFrase(
          id = "seed",
          origen = OrigenFrase.ESCRITO,
          palabras = listOf("almuerzo"),
          campo = campo,
          leido = null,
          propuesto = null,
          guardado = guardado,
          createdAt = ADD_TEST_NOW,
      )

  private class CountingDispatcher(private val delegate: CoroutineDispatcher) :
      CoroutineDispatcher() {
    var dispatches = 0
      private set

    override fun dispatch(context: CoroutineContext, block: Runnable) {
      dispatches++
      delegate.dispatch(context, block)
    }
  }

  private class Round(val viewModel: AddMovimientoViewModel) {
    val medio
      get() = viewModel.uiState.value.sentence.cuenta.medio
  }

  private fun TestScope.lunchRound(): Round {
    clock.instant += 1.minutes
    val viewModel = correctionsViewModel()
    type(viewModel, LUNCH)
    return Round(viewModel)
  }

  private fun TestScope.pickAndSave(viewModel: AddMovimientoViewModel, medio: MedioPago) {
    viewModel.onPhraseContinue()
    viewModel.onMedioSelected(medio)
    save(viewModel)
  }

  private fun TestScope.save(viewModel: AddMovimientoViewModel) {
    viewModel.onSubmit()
    runCurrent()
  }

  private fun correctionsMovimientos() =
      AddFakeMovimientos().apply {
        medioSelections[MedioPago.DEBITO] = NEQUI.selection(MedioPago.DEBITO)
        medioSelections[MedioPago.TRANSFERENCIA] = NEQUI.selection(MedioPago.TRANSFERENCIA)
        medioSelections[MedioPago.CREDITO] = NU.selection(MedioPago.CREDITO)
      }

  private fun TestScope.correctionsViewModel(
      movimientos: AddFakeMovimientos = correctionsMovimientos(),
      voice: FakeVoiceDictation = FakeVoiceDictation(available = false),
      computationDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler),
  ): AddMovimientoViewModel =
      createAddViewModel(
              movimientoRepository = movimientos,
              categoriaRepository = AddFakeCategorias(DataOutcome.Success(addSeedCategorias())),
              preferencesRepository = addPreferences(Moneda.COP),
              cuentaRepository = FakeCuentaRepository(listOf(COP_CASH, NEQUI, NU, USD_CASH)),
              correccionRepository = correcciones,
              clock = clock,
              voice = voice,
              idGenerator = ids,
              computationDispatcher = computationDispatcher,
              scope = backgroundScope,
          )
          .also { runCurrent() }

  private fun TestScope.type(viewModel: AddMovimientoViewModel, text: String) {
    viewModel.onPhraseChanged(text)
    testScheduler.advanceTimeBy(DEBOUNCE_FOR_TESTS)
    runCurrent()
  }
}
