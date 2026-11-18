package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategoryHistoryRow
import com.kurobello.tallybook.core.data.CategorySeedCatalog
import com.kurobello.tallybook.core.data.CorreccionFraseRepository
import com.kurobello.tallybook.core.data.CuentaRepository
import com.kurobello.tallybook.core.data.DeviceLocaleProvider
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.data.FakeCorreccionFraseRepository
import com.kurobello.tallybook.core.data.FakeCuentaRepository
import com.kurobello.tallybook.core.data.FakeEtiquetaRepository
import com.kurobello.tallybook.core.data.FakeRecurrenciaRepository
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.RecurrenciaRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.data.UnsupportedCategoriaRepository
import com.kurobello.tallybook.core.data.entry.CategoryKeywordDictionary
import com.kurobello.tallybook.core.data.entry.CategorySuggester
import com.kurobello.tallybook.core.data.entry.CuentaMatcher
import com.kurobello.tallybook.core.data.entry.EtiquetaMatcher
import com.kurobello.tallybook.core.data.entry.MovimientoPhraseParser
import com.kurobello.tallybook.core.data.voice.FakeVoiceDictation
import com.kurobello.tallybook.core.data.voice.VoiceDictation
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaSelection
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.MovimientoUpdateResult
import com.kurobello.tallybook.core.model.RestoreResult
import com.kurobello.tallybook.core.model.ThemeMode
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.datetime.TimeZone

internal val ADD_TEST_NOW: Instant = Instant.parse("2026-09-15T02:30:00Z")

@OptIn(ExperimentalCoroutinesApi::class)
internal fun createAddViewModel(
    movimientoRepository: MovimientoRepository = AddFakeMovimientos(),
    categoriaRepository: CategoriaRepository = AddFakeCategorias(DataOutcome.Success(emptyList())),
    preferencesRepository: AppPreferencesRepository = AddFakePreferences(),
    etiquetaRepository: EtiquetaRepository = FakeEtiquetaRepository(),
    cuentaRepository: CuentaRepository = FakeCuentaRepository(addCashCuentas()),
    recurrenciaRepository: RecurrenciaRepository = FakeRecurrenciaRepository(),
    correccionRepository: CorreccionFraseRepository = FakeCorreccionFraseRepository(),
    clock: Clock = AddFixedClock(ADD_TEST_NOW),
    timeZone: TimeZone = TimeZone.UTC,
    computationDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
    voice: VoiceDictation = FakeVoiceDictation(available = false),
    idGenerator: IdGenerator = IdGenerator { "movement-id" },
    scope: CoroutineScope,
): AddMovimientoViewModel =
    AddMovimientoViewModel(
        repositories =
            AddMovimientoRepositories(
                preferences = preferencesRepository,
                categorias = categoriaRepository,
                movimientos = movimientoRepository,
                etiquetas = etiquetaRepository,
                cuentas = cuentaRepository,
                recurrencias = recurrenciaRepository,
                voice = voice,
                correcciones = correccionRepository,
            ),
        idGenerator = idGenerator,
        clock = clock,
        timeZoneProvider = TimeZoneProvider { timeZone },
        entryEngine =
            MovimientoEntryEngine(
                parser = MovimientoPhraseParser(),
                suggester = CategorySuggester(CategoryKeywordDictionary.builtIn),
                etiquetaMatcher = EtiquetaMatcher(),
                cuentaMatcher = CuentaMatcher(),
                localeProvider = AddFixedLocale,
                computationDispatcher = computationDispatcher,
            ),
        scope = scope,
    )

internal fun addPreferences(moneda: Moneda) =
    AddFakePreferences(DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(moneda))))

/** One default cash cuenta per moneda, which is what any primary currency resolves to. */
internal fun addCashCuentas(): List<Cuenta> = Moneda.entries.map(::cashCuenta)

internal fun addSeedCategorias(): List<Categoria> =
    CategorySeedCatalog.categories.map { Categoria(it.id, it.id, it.padreId, it.icono, it.color) }

internal fun addTestCategoria(id: String): Categoria =
    Categoria(
        id = id,
        nombre = "Food",
        padreId = null,
        icono = CategoryIcon.UTENSILS,
        color = CategoryTint.AMBER,
    )

private object AddFixedLocale : DeviceLocaleProvider {
  override fun currentLanguageTag(): String = "es-CO"
}

internal class AddFakePreferences(
    initial: DataOutcome<AppPreferencesState> =
        DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(Moneda.USD)))
) : AppPreferencesRepository {
  override val preferences = MutableStateFlow<DataOutcome<AppPreferencesState>>(initial)

  override suspend fun initialize(monedaPrincipal: Moneda): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun setThemeMode(themeMode: ThemeMode): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun setAppLocale(appLocale: AppLocale?): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun setMonedaPrincipal(moneda: Moneda): DataOutcome<Unit> =
      DataOutcome.Success(Unit)
}

internal class AddFakeCategorias(initial: DataOutcome<List<Categoria>>) :
    CategoriaRepository by UnsupportedCategoriaRepository {
  private val values = MutableStateFlow(initial)

  override fun observeAll(): Flow<DataOutcome<List<Categoria>>> = values

  override suspend fun getAll(): DataOutcome<List<Categoria>> = values.value

  override suspend fun localizeSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  fun emit(value: DataOutcome<List<Categoria>>) {
    values.value = value
  }
}

internal open class AddFakeMovimientos(
    var insertResult: DataOutcome<Unit> = DataOutcome.Success(Unit),
) : MovimientoRepository {
  val inserted = mutableListOf<Movimiento>()

  /** Null resolves the primary cash cuenta, as a fresh install does. */
  var selection: DataOutcome<CuentaSelection?>? = null

  /** Per-medio answers; a medio without one resolves the primary cash cuenta only for EFECTIVO. */
  val medioSelections = mutableMapOf<MedioPago, CuentaSelection?>()
  val selectionRequests = mutableListOf<Triple<Moneda, TipoMovimiento, MedioPago?>>()

  override suspend fun defaultSelection(
      primary: Moneda,
      tipo: TipoMovimiento,
      medio: MedioPago?,
  ): DataOutcome<CuentaSelection?> {
    selectionRequests += Triple(primary, tipo, medio)
    val cash = CuentaSelection(Cuenta.defaultCashId(primary), MedioPago.EFECTIVO, primary)
    return when {
      medio == null -> selection ?: DataOutcome.Success(cash)
      medio in medioSelections -> DataOutcome.Success(medioSelections[medio])
      else -> DataOutcome.Success(cash.takeIf { medio == MedioPago.EFECTIVO })
    }
  }

  override fun observeRecent(moneda: Moneda, limit: Int): Flow<DataOutcome<List<Movimiento>>> =
      error("Scoped recent reads are not expected in this fixture")

  override fun observeSummary(
      moneda: Moneda,
      range: MovimientoDateRange,
  ): Flow<DataOutcome<MovimientoSummary>> = emptyFlow()

  val history =
      MutableStateFlow<DataOutcome<List<CategoryHistoryRow>>>(DataOutcome.Success(emptyList()))

  override fun observeCategoryHistory(limit: Int): Flow<DataOutcome<List<CategoryHistoryRow>>> =
      history

  override suspend fun insert(movimiento: Movimiento): DataOutcome<Unit> {
    inserted += movimiento
    return insertResult
  }

  override suspend fun insertAll(movimientos: List<Movimiento>): DataOutcome<Unit> {
    if (insertResult is DataOutcome.Success) inserted += movimientos
    return insertResult
  }

  override suspend fun getAll(): DataOutcome<List<Movimiento>> = DataOutcome.Success(inserted)

  override suspend fun getById(id: String): DataOutcome<Movimiento?> =
      DataOutcome.Success(inserted.firstOrNull { it.id == id })

  override suspend fun update(
      id: String,
      changes: MovimientoChanges,
  ): DataOutcome<MovimientoUpdateResult> {
    val current =
        inserted.firstOrNull { it.id == id }
            ?: return DataOutcome.Success(MovimientoUpdateResult.NotFound)
    if (current.monto.moneda != changes.monto.moneda)
        return DataOutcome.Failure(DataError.InvalidInput("Currency cannot change"))
    val updated =
        current.copy(
            tipo = changes.tipo,
            monto = changes.monto,
            fecha = changes.fecha,
            categoriaId = changes.categoriaId,
            etiquetaIds = changes.etiquetaIds,
            titulo = changes.titulo,
            descripcion = changes.descripcion,
            cuentaId = changes.cuentaId,
            medio = changes.medio,
        )
    val values = inserted.map { if (it.id == id) updated else it }
    inserted.clear()
    inserted.addAll(values)
    return DataOutcome.Success(MovimientoUpdateResult.Updated)
  }

  override suspend fun deleteAndCapture(id: String): DataOutcome<Movimiento?> {
    val captured = inserted.firstOrNull { it.id == id }
    val values = inserted.filter { it.id != id }
    inserted.clear()
    inserted.addAll(values)
    return DataOutcome.Success(captured)
  }

  override suspend fun restoreDeleted(movimiento: Movimiento): DataOutcome<RestoreResult> {
    val current = inserted.firstOrNull { it.id == movimiento.id }
    if (current != null)
        return DataOutcome.Success(
            if (current == movimiento) RestoreResult.AlreadyPresent else RestoreResult.Conflict
        )
    val values = inserted + movimiento
    inserted.clear()
    inserted.addAll(values)
    return DataOutcome.Success(RestoreResult.Restored)
  }
}

internal class AddSuspendedMovimientos : AddFakeMovimientos() {
  val started = CompletableDeferred<Unit>()
  val result = CompletableDeferred<DataOutcome<Unit>>()
  var insertCalls = 0
    private set

  override suspend fun insert(movimiento: Movimiento): DataOutcome<Unit> {
    insertCalls++
    started.complete(Unit)
    return result.await()
  }

  override suspend fun insertAll(movimientos: List<Movimiento>): DataOutcome<Unit> {
    insertCalls++
    started.complete(Unit)
    return result.await()
  }
}

internal class AddFixedClock(var instant: Instant) : Clock {
  override fun now(): Instant = instant
}
