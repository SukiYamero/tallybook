package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategoryHistoryRow
import com.kurobello.tallybook.core.data.CategorySeedCatalog
import com.kurobello.tallybook.core.data.DeviceLocaleProvider
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.data.entry.CategoryKeywordDictionary
import com.kurobello.tallybook.core.data.entry.CategorySuggester
import com.kurobello.tallybook.core.data.entry.MovimientoPhraseParser
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoRestoreResult
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.MovimientoUpdateResult
import com.kurobello.tallybook.core.model.ThemeMode
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
    clock: Clock = AddFixedClock(ADD_TEST_NOW),
    timeZone: TimeZone = TimeZone.UTC,
    computationDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
    scope: CoroutineScope,
): AddMovimientoViewModel =
    AddMovimientoViewModel(
        repositories =
            AddMovimientoRepositories(
                preferences = preferencesRepository,
                categorias = categoriaRepository,
                movimientos = movimientoRepository,
            ),
        idGenerator = IdGenerator { "movement-id" },
        clock = clock,
        timeZoneProvider = TimeZoneProvider { timeZone },
        entryEngine =
            MovimientoEntryEngine(
                parser = MovimientoPhraseParser(),
                suggester = CategorySuggester(CategoryKeywordDictionary.builtIn),
                localeProvider = AddFixedLocale,
                computationDispatcher = computationDispatcher,
            ),
        scope = scope,
    )

internal fun addPreferences(moneda: Moneda) =
    AddFakePreferences(DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(moneda))))

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

internal class AddFakeCategorias(initial: DataOutcome<List<Categoria>>) : CategoriaRepository {
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

  override fun observeRecent(moneda: Moneda, limit: Int): Flow<DataOutcome<List<Movimiento>>> =
      error("Scoped recent reads are not expected in this fixture")

  override fun observeRecent(limit: Int): Flow<DataOutcome<List<Movimiento>>> = emptyFlow()

  override fun observeSummary(moneda: Moneda): Flow<DataOutcome<MovimientoSummary>> = emptyFlow()

  override fun observeFiltered(
      criteria: MovimientoReadCriteria
  ): Flow<DataOutcome<MovimientoSearchSnapshot>> =
      error("Search reads are not expected in this fixture")

  override fun observeRange(
      moneda: Moneda,
      range: MovimientoDateRange,
  ): Flow<DataOutcome<List<Movimiento>>> =
      error("History range reads are not expected in this fixture")

  override fun observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>> =
      error("History year reads are not expected in this fixture")

  val history =
      MutableStateFlow<DataOutcome<List<CategoryHistoryRow>>>(DataOutcome.Success(emptyList()))

  override fun observeCategoryHistory(limit: Int): Flow<DataOutcome<List<CategoryHistoryRow>>> =
      history

  override suspend fun insert(movimiento: Movimiento): DataOutcome<Unit> {
    inserted += movimiento
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
            descripcion = changes.descripcion,
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

  override suspend fun restoreDeleted(
      movimiento: Movimiento
  ): DataOutcome<MovimientoRestoreResult> {
    val current = inserted.firstOrNull { it.id == movimiento.id }
    if (current != null)
        return DataOutcome.Success(
            if (current == movimiento) MovimientoRestoreResult.AlreadyPresent
            else MovimientoRestoreResult.Conflict
        )
    val values = inserted + movimiento
    inserted.clear()
    inserted.addAll(values)
    return DataOutcome.Success(MovimientoRestoreResult.Restored)
  }

  override suspend fun delete(id: String): DataOutcome<Unit> = DataOutcome.Success(Unit)
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
}

internal class AddFixedClock(var instant: Instant) : Clock {
  override fun now(): Instant = instant
}
