package com.kurobello.tallybook.feature.accounts.viewmodel

import com.kurobello.tallybook.core.data.FakeCuentaRepository
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class QuickCuentaViewModelTest {

  @Test
  fun startPresetsTheTypeAndDefaultCurrencyWithTheTrimmedName() = runTest {
    val (vm, _) = quickCuenta(FakeCuentaRepository())

    vm.onStart(TipoCuenta.CREDITO, Moneda.COP, "  NU ")

    val draft = assertNotNull(vm.uiState.value.draft)
    assertEquals("NU", draft.nombre)
    assertEquals(TipoCuenta.CREDITO, draft.tipo)
    assertEquals(Moneda.COP, draft.moneda)
  }

  @Test
  fun theTypeStaysPresetWhileOtherFieldsChange() = runTest {
    val (vm, _) = quickCuenta(FakeCuentaRepository())
    vm.onStart(TipoCuenta.BANCO, Moneda.COP)

    vm.onDraftChanged { onTipo(TipoCuenta.CREDITO).onNombre("Nequi").onMoneda(Moneda.USD) }

    val draft = assertNotNull(vm.uiState.value.draft)
    assertEquals(TipoCuenta.BANCO, draft.tipo)
    assertEquals("Nequi", draft.nombre)
    assertEquals(Moneda.USD, draft.moneda)
  }

  @Test
  fun aPinnedCurrencyStaysSoTheCreatedAccountFitsTheMovimiento() = runTest {
    val repository = FakeCuentaRepository()
    val (vm, effects) = quickCuenta(repository)
    vm.onStart(TipoCuenta.BANCO, Moneda.USD, "Chase", monedaPinned = true)

    vm.onDraftChanged { onSaldo("12.50").onMoneda(Moneda.COP) }
    val draft = assertNotNull(vm.uiState.value.draft)
    assertEquals(Moneda.USD, draft.moneda)
    assertEquals("12.50", draft.saldo)
    assertTrue(vm.uiState.value.monedaPinned)

    vm.onSave()
    runCurrent()
    val created = repository.cuentas.value.single()
    assertEquals(Money(1_250, Moneda.USD), created.saldoInicial)
    assertEquals(listOf<QuickCuentaEffect>(QuickCuentaEffect.Created(created)), effects)
  }

  @Test
  fun savingCreatesTheAccountReportsItAndCloses() = runTest {
    val repository = FakeCuentaRepository()
    val (vm, effects) = quickCuenta(repository)
    vm.onStart(TipoCuenta.CREDITO, Moneda.COP, "NU")
    vm.onDraftChanged { onSaldo("1000") }

    vm.onSave()
    runCurrent()

    val created = repository.cuentas.value.single()
    assertEquals(Money(-100_000, Moneda.COP), created.saldoInicial)
    assertEquals(listOf<QuickCuentaEffect>(QuickCuentaEffect.Created(created)), effects)
    assertEquals(QuickCuentaUiState(), vm.uiState.value)
  }

  @Test
  fun aDuplicateOfTheSameActiveTypeCanBeUsedInstead() = runTest {
    val nequi = nequi()
    val repository = FakeCuentaRepository(listOf(nequi))
    val (vm, effects) = quickCuenta(repository)
    vm.onStart(TipoCuenta.BANCO, Moneda.COP, "NEQUI")

    vm.onSave()
    runCurrent()
    assertEquals(nequi, vm.uiState.value.duplicate)
    assertEquals(true, vm.uiState.value.canUseDuplicate)

    vm.onUseDuplicate()
    runCurrent()
    assertEquals(listOf<QuickCuentaEffect>(QuickCuentaEffect.Used(nequi)), effects)
    assertNull(vm.uiState.value.draft)
  }

  @Test
  fun aDuplicateOfAnotherTypeOrArchivedCannotBeUsed() = runTest {
    val repository = FakeCuentaRepository(listOf(nequi(), nequi("archived", "Vieja", true)))
    val (vm, effects) = quickCuenta(repository)
    vm.onStart(TipoCuenta.CREDITO, Moneda.COP, "Nequi")
    vm.onSave()
    runCurrent()
    assertEquals(false, vm.uiState.value.canUseDuplicate)

    vm.onDraftChanged { onNombre("vieja") }
    assertNull(vm.uiState.value.duplicate)
    vm.onSave()
    runCurrent()
    assertEquals(false, vm.uiState.value.canUseDuplicate)
    vm.onUseDuplicate()
    runCurrent()
    assertTrue(effects.isEmpty())
  }

  @Test
  fun aFailedSaveKeepsTheDraftAndAWriteThatOutlivesItsPaneIsIgnored() = runTest {
    val repository = FakeCuentaRepository()
    val (vm, effects) = quickCuenta(repository)
    vm.onStart(TipoCuenta.BANCO, Moneda.COP, "Nequi")
    repository.failure = DataError.Local(IllegalStateException())

    vm.onSave()
    runCurrent()
    assertTrue(vm.uiState.value.saveFailed)
    assertEquals("Nequi", vm.uiState.value.draft?.nombre)

    repository.failure = null
    vm.onSave()
    vm.onClose()
    runCurrent()
    assertTrue(effects.isEmpty())
    assertEquals(QuickCuentaUiState(), vm.uiState.value)
  }

  private fun TestScope.quickCuenta(
      repository: FakeCuentaRepository
  ): Pair<QuickCuentaViewModel, List<QuickCuentaEffect>> {
    val vm = QuickCuentaViewModel(repository, scope = backgroundScope)
    val effects = mutableListOf<QuickCuentaEffect>()
    backgroundScope.launch { vm.effects.collect { effects += it } }
    return vm to effects
  }
}

private fun nequi(id: String = "nequi", nombre: String = "Nequi", archivada: Boolean = false) =
    Cuenta(
        id = id,
        nombre = nombre,
        tipo = TipoCuenta.BANCO,
        saldoInicial = Money(0, Moneda.COP),
        diaCorte = null,
        archivada = archivada,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
