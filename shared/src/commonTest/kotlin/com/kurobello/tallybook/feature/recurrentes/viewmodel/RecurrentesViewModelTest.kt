package com.kurobello.tallybook.feature.recurrentes.viewmodel

import com.kurobello.tallybook.core.data.CivilToday
import com.kurobello.tallybook.core.data.FakeCuentaRepository
import com.kurobello.tallybook.core.data.FakeEtiquetaRepository
import com.kurobello.tallybook.core.data.FakePresupuestoRepository
import com.kurobello.tallybook.core.data.FakeRecurrenciaRepository
import com.kurobello.tallybook.core.data.FakeTransferenciaRepository
import com.kurobello.tallybook.core.data.MutableClock
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.model.BudgetNotice
import com.kurobello.tallybook.core.model.BudgetState
import com.kurobello.tallybook.core.model.BudgetTouch
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Presupuesto
import com.kurobello.tallybook.core.model.Recurrencia
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.budgetStatus
import com.kurobello.tallybook.core.ui.feedback.ManagementFeedbackKind
import com.kurobello.tallybook.core.ui.feedback.ManagementFeedbackViewModel
import com.kurobello.tallybook.core.ui.feedback.ManagementOutcome
import com.kurobello.tallybook.core.ui.feedback.ManagementSubject
import com.kurobello.tallybook.core.ui.feedback.UndoSnapshot
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddFakeCategorias
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddFakeMovimientos
import com.kurobello.tallybook.feature.movimientos.viewmodel.COP_CASH
import com.kurobello.tallybook.feature.movimientos.viewmodel.addTestCategoria
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class RecurrentesViewModelTest {

  @Test
  fun pendingOccurrencesComeFirstOldestDueDateFirstAcrossRecurrencias() = runTest {
    val f = fixture(rent(), gym(), salary())

    val ready = assertNotNull(f.viewModel.uiState.value.ready)

    assertEquals(
        listOf(
            "rent" to LocalDate(2026, 9, 5),
            "gym" to LocalDate(2026, 10, 1),
            "rent" to LocalDate(2026, 10, 5),
            "gym" to LocalDate(2026, 11, 1),
            "rent" to LocalDate(2026, 11, 5),
        ),
        ready.pending.map { it.recurrencia.id to it.dueDate },
    )
    assertEquals(listOf(true, true, false, false, false), ready.pending.map { it.isNext })
    assertEquals(5, f.viewModel.uiState.value.pendingCount)
    assertEquals(listOf("rent", "gym", "salary"), ready.scheduled.map { it.id })
  }

  @Test
  fun onlyTheOldestPendingDateOfARecurrenciaOpens() = runTest {
    val f = fixture(rent())

    assertTrue(f.viewModel.onOpenOccurrence("rent"))
    val sheet = assertIs<OccurrenceSheet>(f.viewModel.uiState.value.sheet)
    assertEquals(LocalDate(2026, 9, 5), sheet.dueDate)
    assertEquals(LocalDate(2026, 9, 5), sheet.fecha)
    assertEquals(Money(120_000_000, Moneda.COP), sheet.monto)
    assertFalse(f.viewModel.onOpenOccurrence("rent"))
  }

  @Test
  fun pagadoWithAChangedAmountCreatesTheMovimientoAndUpdatesTheTemplate() = runTest {
    val f = fixture(rent())
    f.viewModel.onOpenOccurrence("rent")
    f.viewModel.onOccurrenceAmountChanged("1250000")
    f.viewModel.onOccurrenceFechaChanged(LocalDate(2026, 9, 7))

    f.viewModel.onPaid()
    runCurrent()

    val confirmed = f.recurrencias.confirmed.single()
    assertEquals(Money(125_000_000, Moneda.COP), confirmed.monto)
    assertEquals(LocalDate(2026, 9, 7), confirmed.fecha)
    val template = f.recurrencias.recurrencias.value.getValue("rent")
    assertEquals(Money(125_000_000, Moneda.COP), template.monto)
    assertEquals(LocalDate(2026, 10, 5), template.proximaFecha)
    assertTrue(assertNotNull(f.viewModel.uiState.value.sheet).isClosing)

    f.viewModel.onSheetHidden()
    assertNull(f.viewModel.uiState.value.sheet)
    val outcome = f.feedbackOutcome()
    assertEquals(ManagementSubject.Recurrencia, outcome.subject)
    assertEquals(ManagementFeedbackKind.Confirmed, outcome.kind)
    assertIs<UndoSnapshot.OfOccurrence>(outcome.undo)
    assertEquals(2, f.viewModel.uiState.value.pendingCount)
  }

  @Test
  fun paidGastoMergesNoticeAndKeepsUndo() = runTest {
    val f = fixture(rent())
    f.presupuestos.statuses.value = DataOutcome.Success(listOf(hogarBudget()))
    f.viewModel.onOpenOccurrence("rent")
    f.viewModel.onOccurrenceFechaChanged(LocalDate(2026, 11, 10))

    f.viewModel.onPaid()
    runCurrent()
    f.viewModel.onSheetHidden()

    val outcome = f.feedbackOutcome()
    assertEquals(ManagementFeedbackKind.Confirmed, outcome.kind)
    assertIs<UndoSnapshot.OfOccurrence>(outcome.undo)
    assertEquals(
        BudgetNotice("Hogar", BudgetState.OVER, Money(30_000_000, Moneda.COP), null),
        outcome.budget,
    )
    assertEquals(
        listOf(listOf(BudgetTouch("cat_hogar", null, Moneda.COP, LocalDate(2026, 11, 10)))),
        f.presupuestos.noticeReads,
    )
  }

  @Test
  fun paidLastMonthSendsNone() = runTest {
    val f = fixture(rent())
    f.presupuestos.statuses.value = DataOutcome.Success(listOf(hogarBudget()))
    f.viewModel.onOpenOccurrence("rent")
    f.viewModel.onOccurrenceFechaChanged(LocalDate(2026, 10, 31))

    f.viewModel.onPaid()
    runCurrent()
    f.viewModel.onSheetHidden()

    val outcome = f.feedbackOutcome()
    assertEquals(ManagementFeedbackKind.Confirmed, outcome.kind)
    assertIs<UndoSnapshot.OfOccurrence>(outcome.undo)
    assertNull(outcome.budget)
  }

  @Test
  fun paidIngresoAndSkipReadNoNotice() = runTest {
    val f = fixture(rent(), salary())
    f.presupuestos.statuses.value = DataOutcome.Success(listOf(hogarBudget()))
    f.clock.now = Instant.parse("2026-11-30T12:00:00Z")
    f.viewModel.onResume()
    runCurrent()

    f.viewModel.onOpenOccurrence("salary")
    f.viewModel.onPaid()
    runCurrent()
    f.viewModel.onSheetHidden()
    assertNull(f.feedbackOutcome().budget)

    f.viewModel.onOpenOccurrence("rent")
    f.viewModel.onSkip()
    runCurrent()
    f.viewModel.onSheetHidden()
    assertNull(f.feedbackOutcome().budget)

    assertTrue(f.presupuestos.noticeReads.isEmpty())
  }

  @Test
  fun pagadoWithoutAValidAmountShowsTheErrorAndWritesNothing() = runTest {
    val f = fixture(rent())
    f.viewModel.onOpenOccurrence("rent")
    f.viewModel.onOccurrenceAmountChanged("")

    f.viewModel.onPaid()
    runCurrent()

    val sheet = assertIs<OccurrenceSheet>(f.viewModel.uiState.value.sheet)
    assertNotNull(sheet.amountError)
    assertNull(sheet.busy)
    assertEquals(emptyList(), f.recurrencias.confirmed)
  }

  @Test
  fun aFutureFechaIsIgnoredForPagado() = runTest {
    val f = fixture(rent())
    f.viewModel.onOpenOccurrence("rent")

    f.viewModel.onOccurrenceFechaChanged(LocalDate(2026, 11, 16))

    assertEquals(
        LocalDate(2026, 9, 5),
        assertIs<OccurrenceSheet>(f.viewModel.uiState.value.sheet).fecha,
    )
  }

  @Test
  fun saltarAdvancesTheScheduleWithoutAMovimiento() = runTest {
    val f = fixture(rent())
    f.viewModel.onOpenOccurrence("rent")

    f.viewModel.onSkip()
    runCurrent()
    f.viewModel.onSheetHidden()

    assertEquals(emptyList(), f.recurrencias.confirmed)
    assertEquals(
        LocalDate(2026, 10, 5),
        f.recurrencias.recurrencias.value.getValue("rent").proximaFecha,
    )
    val outcome = f.feedbackOutcome()
    assertEquals(ManagementFeedbackKind.Skipped, outcome.kind)
    assertNull(assertIs<UndoSnapshot.OfOccurrence>(outcome.undo).undo.movimientoId)
  }

  @Test
  fun todaviaNoClosesWithoutWritingOrFeedback() = runTest {
    val f = fixture(rent())
    f.viewModel.onOpenOccurrence("rent")

    f.viewModel.onNotYet()
    f.viewModel.onSheetHidden()

    assertNull(f.viewModel.uiState.value.sheet)
    assertEquals(
        LocalDate(2026, 9, 5),
        f.recurrencias.recurrencias.value.getValue("rent").proximaFecha,
    )
    assertEquals(3, f.viewModel.uiState.value.pendingCount)
  }

  @Test
  fun undoOfPagadoThroughTheShellQueueRestoresTheOccurrenceAndAmount() = runTest {
    val f = fixture(rent())
    f.viewModel.onOpenOccurrence("rent")
    f.viewModel.onOccurrenceAmountChanged("1250000")
    f.viewModel.onPaid()
    runCurrent()
    f.viewModel.onSheetHidden()
    val queue = f.queue()
    queue.enqueue(f.feedbackOutcome())
    val head = assertNotNull(queue.state.value.feedback)

    queue.onFeedbackAction(head.token, head.revision)
    runCurrent()

    assertEquals(ManagementFeedbackKind.Restored, queue.state.value.feedback?.kind)
    val template = f.recurrencias.recurrencias.value.getValue("rent")
    assertEquals(LocalDate(2026, 9, 5), template.proximaFecha)
    assertEquals(Money(120_000_000, Moneda.COP), template.monto)
    assertEquals(emptyList(), f.recurrencias.confirmed)
    assertEquals(3, f.viewModel.uiState.value.pendingCount)
  }

  @Test
  fun undoAfterTheRecurrenciaChangedIsAConflictAndKeepsTheChange() = runTest {
    val f = fixture(rent())
    f.viewModel.onOpenOccurrence("rent")
    f.viewModel.onSkip()
    runCurrent()
    f.viewModel.onSheetHidden()
    f.recurrencias.touch("rent")
    val queue = f.queue()
    queue.enqueue(f.feedbackOutcome())
    val head = assertNotNull(queue.state.value.feedback)

    queue.onFeedbackAction(head.token, head.revision)
    runCurrent()

    assertEquals(ManagementFeedbackKind.RestoreConflict, queue.state.value.feedback?.kind)
    assertEquals(
        LocalDate(2026, 10, 5),
        f.recurrencias.recurrencias.value.getValue("rent").proximaFecha,
    )
  }

  @Test
  fun aRefusedTemplateSaysSoAndOffersItsSheet() = runTest {
    val f = fixture(rent())
    f.recurrencias.confirmFailure = DataError.InvalidInput("Archived cuenta")
    f.viewModel.onOpenOccurrence("rent")

    f.viewModel.onPaid()
    runCurrent()

    val sheet = assertIs<OccurrenceSheet>(f.viewModel.uiState.value.sheet)
    assertEquals(OccurrenceError.TEMPLATE_INVALID, sheet.error)
    assertNull(sheet.busy)

    f.viewModel.onEditFromOccurrence()
    assertEquals("rent", assertIs<RecurrenciaSheet>(f.viewModel.uiState.value.sheet).recurrencia.id)
  }

  @Test
  fun aStorageFailureOnPagadoKeepsTheDraftForAnotherTry() = runTest {
    val f = fixture(rent())
    f.recurrencias.confirmFailure = DataError.Local(IllegalStateException("disk"))
    f.viewModel.onOpenOccurrence("rent")
    f.viewModel.onOccurrenceAmountChanged("1300000")

    f.viewModel.onPaid()
    runCurrent()

    val sheet = assertIs<OccurrenceSheet>(f.viewModel.uiState.value.sheet)
    assertEquals(OccurrenceError.CONFIRM_FAILED, sheet.error)
    assertEquals("1300000", sheet.amountInput)
  }

  @Test
  fun scheduleIsLockedWhileAnOccurrenceIsPendingButOtherFieldsSave() = runTest {
    val f = fixture(rent())
    f.viewModel.onOpenRecurrencia("rent")
    val state = f.viewModel.uiState.value
    assertTrue(state.isScheduleLocked(assertIs<RecurrenciaSheet>(state.sheet)))

    f.viewModel.onFrecuenciaChanged(Frecuencia.SEMANAL)
    f.viewModel.onFechaChanged(LocalDate(2026, 12, 1))
    f.viewModel.onTituloChanged("Arriendo depto")
    f.viewModel.onSubmit()
    runCurrent()

    val draft = f.recurrencias.updates.single()
    assertEquals("Arriendo depto", draft.titulo)
    assertEquals(Frecuencia.MENSUAL, draft.frecuencia)
    assertEquals(LocalDate(2026, 9, 5), draft.proximaFecha)
    f.viewModel.onSheetHidden()
    assertEquals(ManagementFeedbackKind.Saved, f.feedbackOutcome().kind)
  }

  @Test
  fun withNothingPendingTheScheduleChangesAndReanchors() = runTest {
    val f = fixture(salary())
    f.viewModel.onOpenRecurrencia("salary")

    f.viewModel.onFrecuenciaChanged(Frecuencia.SEMANAL)
    f.viewModel.onFechaChanged(LocalDate(2026, 11, 20))
    f.viewModel.onSubmit()
    runCurrent()

    val updated = f.recurrencias.recurrencias.value.getValue("salary")
    assertEquals(Frecuencia.SEMANAL, updated.frecuencia)
    assertEquals(LocalDate(2026, 11, 20), updated.proximaFecha)
    assertEquals(LocalDate(2026, 11, 20), updated.fechaAncla)
  }

  @Test
  fun aScheduleRefusedOnSaveLocksTheSheetAndRestoresTheSchedule() = runTest {
    val f = fixture(salary())
    f.viewModel.onOpenRecurrencia("salary")
    f.viewModel.onFrecuenciaChanged(Frecuencia.ANUAL)
    f.clock.now = Instant.parse("2026-12-01T12:00:00Z")

    f.viewModel.onSubmit()
    runCurrent()

    val sheet = assertIs<RecurrenciaSheet>(f.viewModel.uiState.value.sheet)
    assertTrue(sheet.scheduleRejected)
    assertFalse(sheet.isSubmitting)
    assertEquals(Frecuencia.MENSUAL, sheet.frecuencia)
    assertEquals(emptyList(), f.recurrencias.updates)
    f.viewModel.onFrecuenciaChanged(Frecuencia.SEMANAL)
    assertEquals(
        Frecuencia.MENSUAL,
        assertIs<RecurrenciaSheet>(f.viewModel.uiState.value.sheet).frecuencia,
    )
  }

  @Test
  fun anUnchangedSheetClosesWithoutWriting() = runTest {
    val f = fixture(salary())
    f.viewModel.onOpenRecurrencia("salary")

    f.viewModel.onSubmit()
    runCurrent()
    f.viewModel.onSheetHidden()

    assertEquals(emptyList(), f.recurrencias.updates)
    assertNull(f.viewModel.uiState.value.sheet)
  }

  @Test
  fun deleteAsksFirstThenRemovesTheRecurrenciaWithoutUndo() = runTest {
    val f = fixture(rent(), salary())
    f.viewModel.onOpenRecurrencia("rent")

    f.viewModel.onRequestDelete()
    assertTrue(
        assertIs<RecurrenciaSheet>(f.viewModel.uiState.value.sheet).isDeleteConfirmationVisible
    )
    f.viewModel.onConfirmDelete()
    runCurrent()
    f.viewModel.onSheetHidden()

    assertEquals(setOf("salary"), f.recurrencias.recurrencias.value.keys)
    val outcome = f.feedbackOutcome()
    assertEquals(ManagementFeedbackKind.Deleted, outcome.kind)
    assertNull(outcome.undo)
    assertEquals(0, f.viewModel.uiState.value.pendingCount)
  }

  @Test
  fun cancellingDeleteKeepsTheSheetEditable() = runTest {
    val f = fixture(rent())
    f.viewModel.onOpenRecurrencia("rent")
    f.viewModel.onRequestDelete()

    f.viewModel.onRequestCloseSheet()

    val sheet = assertIs<RecurrenciaSheet>(f.viewModel.uiState.value.sheet)
    assertFalse(sheet.isDeleteConfirmationVisible)
    assertFalse(sheet.isClosing)
    assertEquals(setOf("rent"), f.recurrencias.recurrencias.value.keys)
  }

  @Test
  fun aNewDayOnResumeSurfacesOccurrencesThatFellDue() = runTest {
    val f = fixture(salary())
    assertEquals(0, f.viewModel.uiState.value.pendingCount)

    f.clock.now = Instant.parse("2026-11-30T12:00:00Z")
    f.viewModel.onResume()
    runCurrent()

    assertEquals(1, f.viewModel.uiState.value.pendingCount)
    assertEquals(LocalDate(2026, 11, 30), f.viewModel.uiState.value.today)
  }

  @Test
  fun aFailedReadShowsTheErrorAndRetryReadsAgain() = runTest {
    val f = fixture(rent()) { failure = DataError.Local(IllegalStateException("disk")) }
    assertEquals(RecurrentesContent.Failed, f.viewModel.uiState.value.content)

    f.recurrencias.failure = null
    f.viewModel.onRetry()
    runCurrent()

    assertEquals(3, f.viewModel.uiState.value.pendingCount)
  }

  private class Fixture(
      val viewModel: RecurrentesViewModel,
      val recurrencias: FakeRecurrenciaRepository,
      val presupuestos: FakePresupuestoRepository,
      val clock: MutableClock,
      private val scope: TestScope,
  ) {
    suspend fun feedbackOutcome(): ManagementOutcome =
        assertIs<RecurrentesEffect.Feedback>(viewModel.effects.first()).outcome

    fun queue() =
        ManagementFeedbackViewModel(
                AddFakeMovimientos(),
                FakeTransferenciaRepository(),
                recurrencias,
                scope.backgroundScope,
            )
            .queue
  }

  private fun TestScope.fixture(
      vararg recurrencias: Recurrencia,
      configure: FakeRecurrenciaRepository.() -> Unit = {},
  ): Fixture {
    val repository = FakeRecurrenciaRepository(recurrencias.toList()).apply(configure)
    val clock = MutableClock(Instant.parse("2026-11-15T12:00:00Z"))
    val presupuestos = FakePresupuestoRepository()
    val viewModel =
        RecurrentesViewModel(
            repositories =
                RecurrentesRepositories(
                    recurrencias = repository,
                    categorias =
                        AddFakeCategorias(
                            DataOutcome.Success(
                                listOf(
                                    addTestCategoria("cat_hogar"),
                                    addTestCategoria(CATEGORIA_OTROS_ID),
                                )
                            )
                        ),
                    cuentas = FakeCuentaRepository(listOf(COP_CASH)),
                    etiquetas = FakeEtiquetaRepository(),
                    presupuestos = presupuestos,
                ),
            today = CivilToday(clock, TimeZoneProvider { TimeZone.UTC }),
            scope = backgroundScope,
        )
    runCurrent()
    return Fixture(viewModel, repository, presupuestos, clock, this)
  }
}

private fun recurrencia(
    id: String,
    tipo: TipoMovimiento,
    monto: Money,
    fechaAncla: LocalDate,
    proximaFecha: LocalDate,
    createdAt: String,
) =
    Recurrencia(
        id = id,
        tipo = tipo,
        monto = monto,
        categoriaId = "cat_hogar",
        titulo = id,
        descripcion = null,
        cuentaId = Cuenta.defaultCashId(Moneda.COP),
        medio = MedioPago.EFECTIVO,
        etiquetaIds = emptyList(),
        frecuencia = Frecuencia.MENSUAL,
        fechaAncla = fechaAncla,
        proximaFecha = proximaFecha,
        createdAt = Instant.parse(createdAt),
    )

private fun rent() =
    recurrencia(
        "rent",
        TipoMovimiento.GASTO,
        Money(120_000_000, Moneda.COP),
        LocalDate(2026, 8, 5),
        LocalDate(2026, 9, 5),
        "2026-08-05T10:00:00Z",
    )

private fun gym() =
    recurrencia(
        "gym",
        TipoMovimiento.GASTO,
        Money(9_000_000, Moneda.COP),
        LocalDate(2026, 9, 1),
        LocalDate(2026, 10, 1),
        "2026-09-01T10:00:00Z",
    )

private fun salary() =
    recurrencia(
        "salary",
        TipoMovimiento.INGRESO,
        Money(450_000_000, Moneda.COP),
        LocalDate(2026, 10, 30),
        LocalDate(2026, 11, 30),
        "2026-10-30T10:00:00Z",
    )

private fun hogarBudget() =
    budgetStatus(
        Presupuesto(
            "hogar-budget",
            "cat_hogar",
            Money(100_000_000, Moneda.COP),
            Instant.parse("2026-01-01T00:00:00Z"),
        ),
        "Hogar",
        null,
        Money(130_000_000, Moneda.COP),
        LocalDate(2026, 11, 15),
    )
