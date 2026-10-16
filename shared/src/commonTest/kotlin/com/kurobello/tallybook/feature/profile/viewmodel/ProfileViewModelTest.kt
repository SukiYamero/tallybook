package com.kurobello.tallybook.feature.profile.viewmodel

import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.export.MovimientoCsvLabels
import com.kurobello.tallybook.core.data.export.MovimientoExportSharer
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.ThemeMode
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
  @Test
  fun selectingAnotherCurrencyPersistsItAndRendersTheStoredValue() = runTest {
    val preferences = FakePreferences(AppPreferences(Moneda.COP))
    val vm = profileViewModel(preferences)
    runCurrent()
    assertEquals(Moneda.COP, vm.uiState.value.stored?.monedaPrincipal)
    assertEquals("2.1.0", vm.uiState.value.appVersion)

    assertTrue(vm.onOpenSheet(ProfileSheetKind.CURRENCY))
    assertFalse(vm.onOpenSheet(ProfileSheetKind.LANGUAGE))
    vm.onSelectLanguage(AppLocale.EN)
    vm.onSelectCurrency(Moneda.USD)
    assertEquals(
        ProfileSheet(ProfileSheetKind.CURRENCY, AppPreferences(Moneda.COP), isClosing = true),
        vm.uiState.value.sheet,
    )
    assertEquals(ProfileSetting.CURRENCY, vm.uiState.value.pendingWrite)
    assertFalse(vm.uiState.value.canEdit)
    runCurrent()

    assertEquals(listOf<Any?>(Moneda.USD), preferences.writes)
    assertEquals(Moneda.USD, vm.uiState.value.stored?.monedaPrincipal)
    assertNull(vm.uiState.value.pendingWrite)
    vm.onSheetHidden()
    assertNull(vm.uiState.value.sheet)
  }

  @Test
  fun languageAndThemeSelectionsPersistOnlyRealChanges() = runTest {
    val preferences = FakePreferences(AppPreferences(Moneda.MXN, ThemeMode.SYSTEM, AppLocale.ES))
    val vm = profileViewModel(preferences)
    runCurrent()
    vm.onSelectTheme(ThemeMode.SYSTEM)
    vm.onOpenSheet(ProfileSheetKind.LANGUAGE)
    vm.onSelectLanguage(AppLocale.ES)
    vm.onSheetHidden()
    runCurrent()
    assertTrue(preferences.writes.isEmpty())

    vm.onSelectTheme(ThemeMode.DARK)
    runCurrent()
    vm.onOpenSheet(ProfileSheetKind.LANGUAGE)
    vm.onSelectLanguage(null)
    runCurrent()
    assertEquals(listOf<Any?>(ThemeMode.DARK, null), preferences.writes)
    assertEquals(AppPreferences(Moneda.MXN, ThemeMode.DARK, null), vm.uiState.value.stored)
  }

  @Test
  fun pendingWriteBlocksEditsAndFailureIsShownOnTheFailedSettingOnly() = runTest {
    val preferences = FakePreferences(AppPreferences(Moneda.COP))
    val pending = CompletableDeferred<DataOutcome<Unit>>()
    preferences.nextWrite = pending
    val vm = profileViewModel(preferences)
    runCurrent()
    vm.onSelectTheme(ThemeMode.LIGHT)
    assertFalse(vm.onOpenSheet(ProfileSheetKind.CURRENCY))
    vm.onSelectTheme(ThemeMode.DARK)
    runCurrent()
    assertEquals(listOf<Any?>(ThemeMode.LIGHT), preferences.writes)

    pending.complete(DataOutcome.Failure(DataError.Local(IllegalStateException())))
    runCurrent()
    assertEquals(ProfileSetting.THEME, vm.uiState.value.failedWrite)
    assertEquals(ThemeMode.SYSTEM, vm.uiState.value.stored?.themeMode)

    assertTrue(vm.onOpenSheet(ProfileSheetKind.CURRENCY))
    vm.onSelectCurrency(Moneda.COP)
    assertNull(vm.uiState.value.failedWrite)
  }

  @Test
  fun readFailureClosesTheSheetAndBlocksEditsUntilRetrySucceeds() = runTest {
    val preferences = FakePreferences(AppPreferences(Moneda.BRL))
    val vm = profileViewModel(preferences)
    runCurrent()
    assertTrue(vm.onOpenSheet(ProfileSheetKind.CURRENCY))
    preferences.failNextRead = true
    vm.onRetry()
    runCurrent()
    assertEquals(ProfilePreferences.Unavailable, vm.uiState.value.preferences)
    assertEquals(
        ProfileSheet(ProfileSheetKind.CURRENCY, AppPreferences(Moneda.BRL), isClosing = true),
        vm.uiState.value.sheet,
    )
    vm.onSheetHidden()
    assertFalse(vm.onOpenSheet(ProfileSheetKind.CURRENCY))
    vm.onSelectTheme(ThemeMode.DARK)
    assertTrue(preferences.writes.isEmpty())

    vm.onRetry()
    runCurrent()
    assertEquals(Moneda.BRL, vm.uiState.value.stored?.monedaPrincipal)
    assertTrue(vm.uiState.value.canEdit)
  }

  @Test
  fun uninitializedPreferencesAreUnavailable() = runTest {
    val vm = profileViewModel(FakePreferences(null))
    runCurrent()
    assertEquals(ProfilePreferences.Unavailable, vm.uiState.value.preferences)
    assertFalse(vm.uiState.value.canEdit)
  }

  @Test
  fun exportSharesADatedCsvAndReportsReadOrShareFailures() = runTest {
    val movimientos = ProfileFakeMovimientos()
    val sharer = FakeSharer()
    val vm = profileViewModel(FakePreferences(AppPreferences(Moneda.COP)), movimientos, sharer)
    runCurrent()

    vm.onExport(LABELS)
    assertEquals(ProfileExport.EXPORTING, vm.uiState.value.export)
    vm.onExport(LABELS)
    runCurrent()
    assertEquals(ProfileExport.IDLE, vm.uiState.value.export)
    val (fileName, csv) = sharer.shared.single()
    assertEquals("tallybook-2026-10-15.csv", fileName)
    assertTrue(csv.contains("2026-10-14,Gasto,Comida,Comida,café,12.50,COP"), csv)

    sharer.result = DataOutcome.Failure(DataError.Unknown(IllegalStateException()))
    vm.onExport(LABELS)
    runCurrent()
    assertEquals(ProfileExport.FAILED, vm.uiState.value.export)

    movimientos.result = DataOutcome.Failure(DataError.Local(IllegalStateException()))
    sharer.result = DataOutcome.Success(Unit)
    vm.onExport(LABELS)
    runCurrent()
    assertEquals(ProfileExport.FAILED, vm.uiState.value.export)
    assertEquals(1, sharer.shared.size)
  }
}

private val LABELS =
    MovimientoCsvLabels(
        dateHeader = "Fecha",
        typeHeader = "Tipo",
        categoryHeader = "Categoría",
        parentCategoryHeader = "Categoría principal",
        descriptionHeader = "Descripción",
        amountHeader = "Monto",
        currencyHeader = "Moneda",
        expenseLabel = "Gasto",
        incomeLabel = "Ingreso",
    )

private fun TestScope.profileViewModel(
    preferences: AppPreferencesRepository,
    movimientos: ProfileFakeMovimientos = ProfileFakeMovimientos(),
    sharer: FakeSharer = FakeSharer(),
): ProfileViewModel =
    ProfileViewModel(
        preferencesRepository = preferences,
        export =
            ProfileExportDependencies(
                movimientos = movimientos,
                categorias = ProfileFakeCategorias,
                sharer = sharer,
                clock =
                    object : Clock {
                      override fun now() = Instant.parse("2026-10-15T15:00:00Z")
                    },
                timeZoneProvider = { TimeZone.UTC },
            ),
        appInfo = { "2.1.0" },
        computationDispatcher = StandardTestDispatcher(testScheduler),
        scope = backgroundScope,
    )

private class FakePreferences(initial: AppPreferences?) : AppPreferencesRepository {
  private val stored = MutableStateFlow(initial)
  val writes = mutableListOf<Any?>()
  var nextWrite: CompletableDeferred<DataOutcome<Unit>>? = null
  var failNextRead = false

  override val preferences: Flow<DataOutcome<AppPreferencesState>> = flow {
    if (failNextRead) {
      failNextRead = false
      emit(DataOutcome.Failure(DataError.Local(IllegalStateException())))
      return@flow
    }
    stored.collect { value ->
      emit(
          DataOutcome.Success(
              if (value == null) AppPreferencesState.Uninitialized
              else AppPreferencesState.Ready(value)
          )
      )
    }
  }

  override suspend fun initialize(monedaPrincipal: Moneda): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun setMonedaPrincipal(moneda: Moneda) =
      write(moneda) { it.copy(monedaPrincipal = moneda) }

  override suspend fun setThemeMode(themeMode: ThemeMode) =
      write(themeMode) { it.copy(themeMode = themeMode) }

  override suspend fun setAppLocale(appLocale: AppLocale?) =
      write(appLocale) { it.copy(appLocale = appLocale) }

  private suspend fun write(
      value: Any?,
      change: (AppPreferences) -> AppPreferences,
  ): DataOutcome<Unit> {
    writes += value
    val outcome = nextWrite?.also { nextWrite = null }?.await() ?: DataOutcome.Success(Unit)
    if (outcome is DataOutcome.Success) stored.value = stored.value?.let(change)
    return outcome
  }
}

internal class ProfileFakeMovimientos : MovimientoRepository {
  var result: DataOutcome<List<Movimiento>> =
      DataOutcome.Success(
          listOf(
              Movimiento(
                  id = "m1",
                  tipo = TipoMovimiento.GASTO,
                  monto = Money(1250, Moneda.COP),
                  fecha = LocalDate(2026, 10, 14),
                  categoriaIds = listOf("cat_comida"),
                  descripcion = "café",
                  createdAt = Instant.parse("2026-10-14T12:00:00Z"),
              )
          )
      )

  override suspend fun getAll(): DataOutcome<List<Movimiento>> = result

  override fun observeRecent(limit: Int) = unused()

  override fun observeRecent(moneda: Moneda, limit: Int) = unused()

  override fun observeSummary(moneda: Moneda) = unused()

  override fun observeRange(moneda: Moneda, range: MovimientoDateRange) = unused()

  override fun observeYears(moneda: Moneda) = unused()

  override fun observeFiltered(criteria: MovimientoReadCriteria) = unused()

  override fun observeCategoryHistory(limit: Int) = unused()

  override suspend fun insert(movimiento: Movimiento) = unused()

  override suspend fun getById(id: String) = unused()

  override suspend fun update(id: String, changes: MovimientoChanges) = unused()

  override suspend fun deleteAndCapture(id: String) = unused()

  override suspend fun restoreDeleted(movimiento: Movimiento) = unused()

  override suspend fun delete(id: String) = unused()
}

internal object ProfileFakeCategorias : CategoriaRepository {
  override suspend fun getAll(): DataOutcome<List<Categoria>> =
      DataOutcome.Success(
          listOf(
              Categoria(
                  id = "cat_comida",
                  nombre = "Comida",
                  padreId = null,
                  icono = CategoryIcon.entries.first(),
                  color = CategoryTint.entries.first(),
              )
          )
      )

  override fun observeAll() = unused()

  override suspend fun ensureSeeded(categorias: List<Categoria>) = unused()

  override suspend fun localizeSeeded(categorias: List<Categoria>) = unused()
}

private fun unused(): Nothing = throw UnsupportedOperationException("Not used by Profile")

private class FakeSharer : MovimientoExportSharer {
  val shared = mutableListOf<Pair<String, String>>()
  var result: DataOutcome<Unit> = DataOutcome.Success(Unit)

  override suspend fun share(fileName: String, csv: String): DataOutcome<Unit> {
    if (result is DataOutcome.Success) shared += fileName to csv
    return result
  }
}
