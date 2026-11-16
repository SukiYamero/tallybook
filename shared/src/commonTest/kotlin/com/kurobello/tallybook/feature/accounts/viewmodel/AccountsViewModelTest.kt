package com.kurobello.tallybook.feature.accounts.viewmodel

import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CivilToday
import com.kurobello.tallybook.core.data.FakeCuentaRepository
import com.kurobello.tallybook.core.data.FakeSaldoRepository
import com.kurobello.tallybook.core.data.MutableClock
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.CreditEstimate
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaSaldo
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.ThemeMode
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.ui.account.CuentaBalance
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferEntry
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferPurpose
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModelTest {

  @Test
  fun groupsLiveAccountsByTypeSortedByNameWithArchivedCollapsedLast() = runTest {
    val fixture =
        AccountsFixture(this, listOf(NU, NEQUI, EFECTIVO, AHORROS, VIEJA.copy(archivada = true)))
    val vm = fixture.create()
    runCurrent()

    val ready = assertIs<AccountsContent.Ready>(vm.uiState.value.content)
    assertEquals(
        listOf(
            TipoCuenta.EFECTIVO to listOf("Efectivo"),
            TipoCuenta.BANCO to listOf("ahorros", "Nequi"),
            TipoCuenta.CREDITO to listOf("NU"),
        ),
        ready.groups.map { group -> group.tipo to group.rows.map { it.cuenta.nombre } },
    )
    assertEquals(listOf("Vieja"), ready.archived.map { it.cuenta.nombre })
    assertFalse(vm.uiState.value.archivedExpanded)

    vm.onToggleArchived()
    assertTrue(vm.uiState.value.archivedExpanded)
  }

  @Test
  fun totalsListEveryCurrencyPrimaryFirstThenDeclarationOrder() = runTest {
    val fixture =
        AccountsFixture(
            this,
            listOf(
                EFECTIVO,
                cuenta("brl", "Real", TipoCuenta.EFECTIVO, Money(700, Moneda.BRL)),
                cuenta("usd", "Dólares", TipoCuenta.BANCO, Money(5_000, Moneda.USD)),
                VIEJA.copy(archivada = true),
            ),
        )
    fixture.preferences.moneda.value = Moneda.USD
    val vm = fixture.create()
    runCurrent()

    val ready = assertIs<AccountsContent.Ready>(vm.uiState.value.content)
    assertEquals(
        listOf(Money(5_000, Moneda.USD), Money(80_000, Moneda.COP), Money(700, Moneda.BRL)),
        ready.totals,
    )
    val rows = ready.groups.flatMap { it.rows } + ready.archived
    assertEquals(
        mapOf("Dólares" to false, "Efectivo" to true, "Real" to true, "Vieja" to true),
        rows.associate { it.cuenta.nombre to it.showsMoneda },
    )
  }

  @Test
  fun balancesFollowTheReadModel() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO, NEQUI))
    val vm = fixture.create()
    runCurrent()

    fixture.deltas.value = mapOf(NEQUI.id to Money(-2_500, Moneda.COP))
    runCurrent()

    val nequi = vm.rows().single { it.cuenta.id == NEQUI.id }
    assertEquals(Money(47_500, Moneda.COP), nequi.saldo)
  }

  @Test
  fun creditCardsShowTheirEstimateForTodaysCycle() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO, NU))
    fixture.deltas.value = mapOf(NU.id to Money(-4_000_000, Moneda.COP))
    fixture.saldos.cycleSpending.value = mapOf(NU.id to Money(1_500_000, Moneda.COP))
    val vm = fixture.create()
    runCurrent()

    val nu = vm.rows().single { it.cuenta.id == NU.id }
    assertEquals(
        CreditEstimate(
            deuda = Money(9_000_000, Moneda.COP),
            cycleSpending = Money(1_500_000, Moneda.COP),
            cycle = MovimientoDateRange(LocalDate(2026, 10, 16), LocalDate(2026, 11, 15)),
        ),
        nu.credit,
    )
    assertNull(vm.rows().single { it.cuenta.id == EFECTIVO.id }.credit)
  }

  @Test
  fun resumingOnALaterDayFollowsTheNewCycle() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO, NU))
    val vm = fixture.create()
    runCurrent()

    fixture.clock.now = Instant.parse("2026-11-20T12:00:00Z")
    vm.onResume()
    runCurrent()

    assertEquals(
        MovimientoDateRange(LocalDate(2026, 11, 16), LocalDate(2026, 12, 15)),
        vm.rows().single { it.cuenta.id == NU.id }.credit?.cycle,
    )
  }

  @Test
  fun searchFiltersEveryAccountByNormalizedNameIncludingArchived() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO, NEQUI, NU, VIEJA.copy(archivada = true)))
    val vm = fixture.create()
    runCurrent()

    vm.onSearchChanged(" VIÉ ")
    val archived = assertIs<AccountsContent.Ready>(vm.uiState.value.content)
    assertEquals("VIÉ", archived.query)
    assertTrue(archived.groups.isEmpty())
    assertEquals(listOf(VIEJA.id), archived.archived.map { it.cuenta.id })
    assertTrue(vm.uiState.value.showsArchived)
    assertEquals(listOf(Money(-4_870_000, Moneda.COP)), archived.totals)

    vm.onSearchChanged("nada")
    assertTrue(assertIs<AccountsContent.Ready>(vm.uiState.value.content).isSearchEmpty)

    vm.onSearchChanged("")
    assertEquals(4, vm.rows().size)
    assertFalse(vm.uiState.value.showsArchived)
  }

  @Test
  fun aFailedSourceShowsNoRowsRefusesSheetsAndRetryReloads() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO))
    fixture.saldos.failure.value = DataError.Local(IllegalStateException())
    val vm = fixture.create()
    runCurrent()
    assertEquals(AccountsContent.Failed, vm.uiState.value.content)
    assertFalse(vm.openSheet(AccountSheetRequest.New("")))

    fixture.saldos.failure.value = null
    vm.onRetry()
    runCurrent()
    assertIs<AccountsContent.Ready>(vm.uiState.value.content)
  }

  @Test
  fun aPrimaryCurrencyChangeReordersTotalsWithoutStaleRows() = runTest {
    val fixture =
        AccountsFixture(
            this,
            listOf(EFECTIVO, cuenta("usd", "Dólares", TipoCuenta.BANCO, Money(5_000, Moneda.USD))),
        )
    val vm = fixture.create()
    runCurrent()
    assertEquals(Moneda.COP, vm.ready().totals.first().moneda)

    fixture.preferences.moneda.value = Moneda.USD
    runCurrent()
    assertEquals(listOf(Moneda.USD, Moneda.COP), vm.ready().totals.map { it.moneda })
    assertEquals(false, vm.rows().single { it.cuenta.id == "usd" }.showsMoneda)
  }

  @Test
  fun creatingACreditCardStoresItsNegatedDebtAndClosesWithFeedback() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO))
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()

    vm.onSearchChanged("NU")
    vm.onCreate()
    val sheet = assertNotNull(vm.uiState.value.sheet)
    assertNull(sheet.editing)
    assertEquals("NU", sheet.draft.nombre)
    assertEquals(Moneda.COP, sheet.draft.moneda)

    vm.onDraftChanged { onTipo(TipoCuenta.CREDITO).onSaldo("50000").onDiaCorte("15") }
    vm.onSave()
    runCurrent()

    val created = fixture.cuentas.cuentas.value.single { it.nombre == "NU" }
    assertEquals(Money(-5_000_000, Moneda.COP), created.saldoInicial)
    assertEquals(15, created.diaCorte)
    assertEquals(true, vm.uiState.value.sheet?.isClosing)
    assertEquals(listOf<AccountsEffect>(AccountsEffect.Feedback(AccountsFeedback.Created)), effects)
    vm.onSheetHidden()
    assertNull(vm.uiState.value.sheet)
  }

  @Test
  fun anInvalidDraftCannotBeSaved() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO))
    val vm = fixture.create()
    runCurrent()

    vm.onCreate()
    assertFalse(vm.uiState.value.sheet?.canSave == true)
    vm.onSave()
    runCurrent()
    assertEquals(1, fixture.cuentas.cuentas.value.size)
  }

  @Test
  fun aDuplicateNameInTheSameCurrencyIsShownInlineUntilTheDraftChanges() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO, NEQUI))
    val vm = fixture.create()
    runCurrent()

    vm.onCreate()
    vm.onDraftChanged { onNombre("nequi") }
    vm.onSave()
    runCurrent()
    val sheet = assertNotNull(vm.uiState.value.sheet)
    assertEquals(AccountSheetError.Duplicate(NEQUI), sheet.error)
    assertFalse(sheet.canSave)
    assertFalse(sheet.isClosing)

    vm.onDraftChanged { onMoneda(Moneda.USD) }
    assertNull(vm.uiState.value.sheet?.error)
    vm.onSave()
    runCurrent()
    assertEquals(true, vm.uiState.value.sheet?.isClosing)
  }

  @Test
  fun aReferencedAccountLocksItsTypeAndCurrency() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO, NEQUI))
    fixture.cuentas.references.value = mapOf(NEQUI.id to 2L)
    val vm = fixture.create()
    runCurrent()

    vm.onOpenCuenta(NEQUI.id)

    val sheet = assertNotNull(vm.uiState.value.sheet)
    assertTrue(sheet.typeLocked)
    assertTrue(sheet.currencyLocked)
  }

  @Test
  fun anUnusedDefaultCashLocksOnlyItsCurrency() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO, NEQUI))
    val vm = fixture.create()
    runCurrent()

    vm.onOpenCuenta(EFECTIVO.id)

    val sheet = assertNotNull(vm.uiState.value.sheet)
    assertFalse(sheet.typeLocked)
    assertTrue(sheet.currencyLocked)
  }

  @Test
  fun unusedAndNewAccountsLockNothingUntilAReferenceArrives() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO, NEQUI))
    val vm = fixture.create()
    runCurrent()

    vm.onCreate()
    assertFalse(assertNotNull(vm.uiState.value.sheet).currencyLocked)
    vm.onDismissSheet()
    vm.onSheetHidden()

    vm.onOpenCuenta(NEQUI.id)
    val unused = assertNotNull(vm.uiState.value.sheet)
    assertFalse(unused.typeLocked)
    assertFalse(unused.currencyLocked)

    fixture.cuentas.references.value = mapOf(NEQUI.id to 1L)
    runCurrent()
    val referenced = assertNotNull(vm.uiState.value.sheet)
    assertTrue(referenced.typeLocked)
    assertTrue(referenced.currencyLocked)
  }

  @Test
  fun lockedCurrencyAndTypeRejectionsAreShownInline() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO, NEQUI))
    fixture.cuentas.references.value = mapOf(NEQUI.id to 2L)
    val vm = fixture.create()
    runCurrent()

    vm.onOpenCuenta(NEQUI.id)
    vm.onDraftChanged { onMoneda(Moneda.USD) }
    vm.onSave()
    runCurrent()
    assertEquals(AccountSheetError.CurrencyLocked, vm.uiState.value.sheet?.error)

    vm.onDraftChanged { onMoneda(Moneda.COP).onTipo(TipoCuenta.EFECTIVO) }
    vm.onSave()
    runCurrent()
    assertEquals(AccountSheetError.TypeLocked, vm.uiState.value.sheet?.error)
    assertEquals(NEQUI, fixture.cuentas.find(NEQUI.id))
  }

  @Test
  fun aFailedSaveKeepsTheDraftWithAnError() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO))
    val vm = fixture.create()
    runCurrent()

    vm.onCreate()
    vm.onDraftChanged { onNombre("Nequi") }
    fixture.cuentas.failure = DataError.Local(IllegalStateException())
    vm.onSave()
    runCurrent()

    val sheet = assertNotNull(vm.uiState.value.sheet)
    assertEquals(AccountSheetError.SaveFailed, sheet.error)
    assertEquals("Nequi", sheet.draft.nombre)
    assertFalse(sheet.pending)
    assertTrue(sheet.canSave)
  }

  @Test
  fun archivingAsksFirstAndTheLastActiveAccountStays() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO, NEQUI))
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()

    vm.onOpenCuenta(NEQUI.id)
    vm.onRequestArchive(archived = true)
    assertEquals(AccountConfirmation.Archive, vm.uiState.value.sheet?.confirmation)
    vm.onCancelConfirmation()
    assertNull(vm.uiState.value.sheet?.confirmation)
    assertFalse(fixture.cuentas.find(NEQUI.id)!!.archivada)

    vm.onRequestArchive(archived = true)
    vm.onConfirmArchive()
    runCurrent()
    assertTrue(fixture.cuentas.find(NEQUI.id)!!.archivada)
    assertEquals(true, vm.uiState.value.sheet?.isClosing)
    assertEquals(
        listOf<AccountsEffect>(AccountsEffect.Feedback(AccountsFeedback.Archived("Nequi"))),
        effects,
    )
    vm.onSheetHidden()

    vm.onOpenCuenta(EFECTIVO.id)
    vm.onRequestArchive(archived = true)
    vm.onConfirmArchive()
    runCurrent()
    val sheet = assertNotNull(vm.uiState.value.sheet)
    assertEquals(AccountSheetError.ArchiveLastActive, sheet.error)
    assertNull(sheet.confirmation)
    assertFalse(sheet.isClosing)
  }

  @Test
  fun unarchivingNeedsNoConfirmation() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO, VIEJA.copy(archivada = true)))
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()

    vm.onOpenCuenta(VIEJA.id)
    vm.onRequestArchive(archived = false)
    runCurrent()

    assertFalse(fixture.cuentas.find(VIEJA.id)!!.archivada)
    assertEquals(true, vm.uiState.value.sheet?.isClosing)
    assertEquals(
        listOf<AccountsEffect>(AccountsEffect.Feedback(AccountsFeedback.Unarchived("Vieja"))),
        effects,
    )
  }

  @Test
  fun deletingAnUnusedAccountAsksAndDeletesWithFeedback() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO, NEQUI))
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()

    vm.onOpenCuenta(NEQUI.id)
    vm.onRequestDelete()
    assertEquals(AccountConfirmation.Delete, vm.uiState.value.sheet?.confirmation)
    vm.onConfirmDelete()
    runCurrent()

    assertNull(fixture.cuentas.find(NEQUI.id))
    assertEquals(true, vm.uiState.value.sheet?.isClosing)
    assertEquals(listOf<AccountsEffect>(AccountsEffect.Feedback(AccountsFeedback.Deleted)), effects)
  }

  @Test
  fun anAccountInUseOffersArchivingInsteadOfDeleting() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO, NEQUI))
    fixture.cuentas.references.value = mapOf(NEQUI.id to 4L)
    val vm = fixture.create()
    runCurrent()

    vm.onOpenCuenta(NEQUI.id)
    vm.onRequestDelete()
    vm.onConfirmDelete()
    runCurrent()
    assertEquals(AccountConfirmation.DeleteInUse(4L), vm.uiState.value.sheet?.confirmation)
    assertNotNull(fixture.cuentas.find(NEQUI.id))

    vm.onConfirmArchive()
    runCurrent()
    assertTrue(fixture.cuentas.find(NEQUI.id)!!.archivada)
    assertEquals(true, vm.uiState.value.sheet?.isClosing)
  }

  @Test
  fun theLastActiveAccountCannotBeDeleted() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO, VIEJA.copy(archivada = true)))
    val vm = fixture.create()
    runCurrent()

    vm.onOpenCuenta(EFECTIVO.id)
    vm.onRequestDelete()
    vm.onConfirmDelete()
    runCurrent()

    val sheet = assertNotNull(vm.uiState.value.sheet)
    assertEquals(AccountSheetError.DeleteLastActive, sheet.error)
    assertNull(sheet.confirmation)
    assertNotNull(fixture.cuentas.find(EFECTIVO.id))
  }

  @Test
  fun dismissingAChangedDraftAsksBeforeDiscarding() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO, NEQUI))
    val vm = fixture.create()
    runCurrent()

    vm.onOpenCuenta(NEQUI.id)
    vm.onDraftChanged { onNombre("Nequi ") }
    assertTrue(vm.onDismissSheet())
    assertEquals(true, vm.uiState.value.sheet?.isClosing)
    vm.onSheetHidden()

    vm.onOpenCuenta(NEQUI.id)
    vm.onDraftChanged { onSaldo("1") }
    assertFalse(vm.onDismissSheet())
    assertEquals(AccountConfirmation.Discard, vm.uiState.value.sheet?.confirmation)
    vm.onContinueEditing()
    assertNull(vm.uiState.value.sheet?.confirmation)
    assertEquals("1", vm.uiState.value.sheet?.draft?.saldo)

    vm.onDismissSheet()
    vm.onConfirmDiscard()
    assertEquals(true, vm.uiState.value.sheet?.isClosing)
  }

  @Test
  fun transferAndPayCardOpenOnlyOnceTheSheetHid() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO, NU))
    fixture.deltas.value = mapOf(NU.id to Money(-1_000_000, Moneda.COP))
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()

    vm.onOpenCuenta(EFECTIVO.id)
    vm.onTransfer()
    assertEquals(true, vm.uiState.value.sheet?.isClosing)
    runCurrent()
    assertTrue(effects.isEmpty())
    vm.onSheetHidden()
    runCurrent()

    vm.onOpenCuenta(NU.id)
    vm.onPayCard()
    vm.onSheetHidden()
    runCurrent()

    assertEquals(
        listOf<AccountsEffect>(
            AccountsEffect.Transfer(
                TransferEntry.New(origenId = EFECTIVO.id, destinoId = null, prefill = null)
            ),
            AccountsEffect.Transfer(
                TransferEntry.New(
                    origenId = null,
                    destinoId = NU.id,
                    prefill = Money(6_000_000, Moneda.COP),
                    purpose = TransferPurpose.PayCard,
                )
            ),
        ),
        effects,
    )
  }

  @Test
  fun viewMovementsLeavesForSearchInTheAccountsCurrencyEvenWhenArchived() = runTest {
    val archived = EFECTIVO.copy(archivada = true)
    val fixture = AccountsFixture(this, listOf(archived, NEQUI))
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()

    vm.onOpenCuenta(archived.id)
    vm.onViewMovements()
    assertEquals(true, vm.uiState.value.sheet?.isClosing)
    runCurrent()
    assertTrue(effects.isEmpty())
    vm.onSheetHidden()
    runCurrent()

    assertEquals(
        listOf<AccountsEffect>(AccountsEffect.ViewMovements(archived.id, archived.moneda)),
        effects,
    )
  }

  @Test
  fun leavingForATransferWithUnsavedChangesAsksFirst() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO, NEQUI))
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()

    vm.onOpenCuenta(NEQUI.id)
    vm.onDraftChanged { onNombre("Nequi 2") }
    vm.onTransfer()
    assertEquals(AccountConfirmation.Discard, vm.uiState.value.sheet?.confirmation)
    vm.onContinueEditing()
    vm.onDismissSheet()
    vm.onConfirmDiscard()
    vm.onSheetHidden()
    runCurrent()
    assertTrue(effects.isEmpty())
  }

  @Test
  fun archivedAccountsAndOtherTypesOfferNoTransferOrPayment() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO, VIEJA.copy(archivada = true)))
    val vm = fixture.create()
    runCurrent()

    vm.onOpenCuenta(VIEJA.id)
    assertFalse(vm.uiState.value.sheet!!.canTransfer)
    vm.onTransfer()
    vm.onPayCard()
    assertEquals(false, vm.uiState.value.sheet?.isClosing)
    vm.onDismissSheet()
    vm.onSheetHidden()

    vm.onOpenCuenta(EFECTIVO.id)
    assertTrue(vm.uiState.value.sheet!!.canTransfer)
    assertFalse(vm.uiState.value.sheet!!.canPayCard)
  }

  @Test
  fun anOpenSheetFollowsTheStoredAccountAndOnlyOneOpensAtATime() = runTest {
    val fixture = AccountsFixture(this, listOf(EFECTIVO, NU))
    val vm = fixture.create()
    runCurrent()

    assertFalse(vm.openSheet(AccountSheetRequest.Open("missing")))
    assertTrue(vm.openSheet(AccountSheetRequest.Open(NU.id)))
    assertFalse(vm.openSheet(AccountSheetRequest.Open(EFECTIVO.id)))
    val opened = assertNotNull(vm.uiState.value.sheet)
    assertEquals("50000,00", opened.draft.saldo)

    fixture.deltas.value = mapOf(NU.id to Money(-200, Moneda.COP))
    fixture.cuentas.cuentas.value =
        fixture.cuentas.cuentas.value.map { if (it.id == NU.id) it.copy(diaCorte = 20) else it }
    runCurrent()
    val refreshed = assertNotNull(vm.uiState.value.sheet)
    assertEquals(20, refreshed.editing?.diaCorte)
    assertEquals(Money(5_000_200, Moneda.COP), refreshed.credit?.deuda)
    assertEquals(opened.draft, refreshed.draft)
  }
}

private class AccountsFixture(private val scope: TestScope, initial: List<Cuenta>) {
  val cuentas = FakeCuentaRepository(initial)
  val saldos = FakeSaldoRepository()
  val deltas = MutableStateFlow<Map<String, Money>>(emptyMap())
  val preferences = AccountsPreferences()
  val clock = MutableClock(Instant.parse("2026-10-24T12:00:00Z"))

  init {
    scope.backgroundScope.launch {
      combine(cuentas.cuentas, deltas) { all, moved ->
            all.map { cuenta ->
              CuentaSaldo(
                  cuenta,
                  moved[cuenta.id]?.let { cuenta.saldoInicial + it } ?: cuenta.saldoInicial,
              )
            }
          }
          .collect { saldos.saldos.value = it }
    }
  }

  fun create(): AccountsViewModel =
      AccountsViewModel(
          cuentas = cuentas,
          saldos = saldos,
          preferences = preferences,
          today = CivilToday(clock, TimeZoneProvider { TimeZone.UTC }),
          scope = scope.backgroundScope,
      )

  fun collectEffects(vm: AccountsViewModel): List<AccountsEffect> {
    val effects = mutableListOf<AccountsEffect>()
    scope.backgroundScope.launch { vm.effects.collect { effects += it } }
    return effects
  }
}

internal class AccountsPreferences : AppPreferencesRepository {
  val moneda = MutableStateFlow(Moneda.COP)

  override val preferences: Flow<DataOutcome<AppPreferencesState>> = moneda.map {
    DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(it)))
  }

  override suspend fun initialize(monedaPrincipal: Moneda) = DataOutcome.Success(Unit)

  override suspend fun setMonedaPrincipal(moneda: Moneda) = DataOutcome.Success(Unit)

  override suspend fun setThemeMode(themeMode: ThemeMode) = DataOutcome.Success(Unit)

  override suspend fun setAppLocale(appLocale: AppLocale?) = DataOutcome.Success(Unit)
}

private fun AccountsViewModel.ready(): AccountsContent.Ready =
    assertIs<AccountsContent.Ready>(uiState.value.content)

private fun AccountsViewModel.rows(): List<CuentaBalance> =
    ready().let { ready -> ready.groups.flatMap { it.rows } + ready.archived }

private val CREATED_AT = Instant.parse("2026-01-01T00:00:00Z")

private fun cuenta(
    id: String,
    nombre: String,
    tipo: TipoCuenta,
    saldoInicial: Money,
    diaCorte: Int? = null,
) =
    Cuenta(
        id,
        nombre,
        tipo,
        saldoInicial,
        diaCorte,
        archivada = false,
        createdAt = CREATED_AT,
        color = tipo.defaultColor,
    )

private val EFECTIVO =
    cuenta(Cuenta.defaultCashId(Moneda.COP), "Efectivo", TipoCuenta.EFECTIVO, Money(0, Moneda.COP))
private val NEQUI = cuenta("nequi", "Nequi", TipoCuenta.BANCO, Money(50_000, Moneda.COP))
private val AHORROS = cuenta("ahorros", "ahorros", TipoCuenta.BANCO, Money(0, Moneda.COP))
private val NU =
    cuenta("nu", "NU", TipoCuenta.CREDITO, Money(-5_000_000, Moneda.COP), diaCorte = 15)
private val VIEJA = cuenta("vieja", "Vieja", TipoCuenta.BANCO, Money(80_000, Moneda.COP))
