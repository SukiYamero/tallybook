package com.kurobello.tallybook.core.ui.navigation

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.savedState
import com.kurobello.tallybook.core.data.ActividadRepository
import com.kurobello.tallybook.core.data.AppInfo
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategoryHistoryRow
import com.kurobello.tallybook.core.data.CuentaRepository
import com.kurobello.tallybook.core.data.DeviceLocaleProvider
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.data.FakeCuentaRepository
import com.kurobello.tallybook.core.data.FakeEtiquetaRepository
import com.kurobello.tallybook.core.data.FakeRecurrenciaRepository
import com.kurobello.tallybook.core.data.FakeSaldoRepository
import com.kurobello.tallybook.core.data.FakeTransferenciaRepository
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.RecurrenciaRepository
import com.kurobello.tallybook.core.data.SaldoRepository
import com.kurobello.tallybook.core.data.StatisticsRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.data.TransferenciaRepository
import com.kurobello.tallybook.core.data.UnsupportedCategoriaRepository
import com.kurobello.tallybook.core.data.export.MovimientoExportSharer
import com.kurobello.tallybook.core.data.export.MovimientoExportSource
import com.kurobello.tallybook.core.data.export.RepositoryExportSource
import com.kurobello.tallybook.core.model.Actividad
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryStatisticsRequest
import com.kurobello.tallybook.core.model.CategoryStatisticsSnapshot
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaSaldo
import com.kurobello.tallybook.core.model.CuentaSelection
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.DayGasto
import com.kurobello.tallybook.core.model.DimensionTotal
import com.kurobello.tallybook.core.model.DimensionTotalOrder
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MonthTotals
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoPeriodRanges
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.MovimientoUpdateResult
import com.kurobello.tallybook.core.model.RestoreResult
import com.kurobello.tallybook.core.model.StatisticsKey
import com.kurobello.tallybook.core.model.StatisticsRequest
import com.kurobello.tallybook.core.model.StatisticsSnapshot
import com.kurobello.tallybook.core.model.ThemeMode
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.map
import com.kurobello.tallybook.di.DispatcherQualifier
import com.kurobello.tallybook.di.entryModule
import com.kurobello.tallybook.di.viewModelModule
import java.io.Serializable as JavaSerializable
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertNotSame
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.plus
import kotlinx.datetime.yearMonth
import org.jetbrains.compose.resources.getString
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.search_placeholder

internal val isPrimaryTab: SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

internal fun ComposeTestRule.assertNoTabSelected() {
  onAllNodes(isPrimaryTab and isSelected()).assertCountEquals(0)
}

/** Search has no tab, so while it is open the bar marks none. */
internal fun ComposeTestRule.assertSearchShownWithNoTabSelected() {
  onNodeWithContentDescription(runBlocking { getString(Res.string.search_placeholder) })
      .assertIsDisplayed()
  assertNoTabSelected()
}

internal class PrimaryDeviceShell(monedaPrincipal: Moneda = Moneda.USD) {
  val movimientos = NavigationMovimientos()
  val cuentas = FakeCuentaRepository(listOf(NAVIGATION_CASH))
  val saldos = FakeSaldoRepository(listOf(CuentaSaldo(NAVIGATION_CASH, Money(125_000, Moneda.COP))))
  val mounted = mutableStateOf(true)
  val owner = mutableStateOf(NavigationParent(null))
  val registry =
      mutableStateOf(SaveableStateRegistry(null) { it is Parcelable || it is JavaSerializable })
  private val ids = AtomicInteger()
  val context = koinApplication {
    modules(
        viewModelModule,
        entryModule,
        module {
          single<DeviceLocaleProvider> {
            object : DeviceLocaleProvider {
              override fun currentLanguageTag() = "es-CO"
            }
          }
          single<AppPreferencesRepository> { NavigationPreferences(monedaPrincipal) }
          single<MovimientoRepository> { movimientos }
          single<ActividadRepository> { movimientos }
          single<CategoriaRepository> { NavigationCategories() }
          single<EtiquetaRepository> { FakeEtiquetaRepository() }
          single<CuentaRepository> { cuentas }
          single<SaldoRepository> { saldos }
          single<RecurrenciaRepository> { FakeRecurrenciaRepository() }
          single<StatisticsRepository> { movimientos }
          single<TransferenciaRepository> { FakeTransferenciaRepository(cuentas) }
          single<IdGenerator> { IdGenerator { "new-${ids.incrementAndGet()}" } }
          single<Clock> {
            object : Clock {
              override fun now() = Instant.parse("2026-10-05T12:00:00Z")
            }
          }
          single<TimeZoneProvider> { TimeZoneProvider { TimeZone.UTC } }
          single<AppInfo> { AppInfo { "1.0" } }
          single<MovimientoExportSource> {
            RepositoryExportSource(get(), get(), get(), get(), get())
          }
          single<MovimientoExportSharer> {
            object : MovimientoExportSharer {
              override suspend fun share(fileName: String, csv: String) = DataOutcome.Success(Unit)
            }
          }
          single<CoroutineDispatcher>(named(DispatcherQualifier.DEFAULT)) { Dispatchers.Default }
        },
    )
  }

  fun saveAndUnmount(): PrimaryNavigationSnapshot {
    val parcel = Parcel.obtain()
    try {
      parcel.writeBundle(owner.value.save())
      parcel.writeValue(registry.value.performSave())
      parcel.setDataPosition(0)
      val saved = requireNotNull(parcel.readBundle(javaClass.classLoader))
      val raw = parcel.readValue(javaClass.classLoader)
      require(raw is Map<*, *>)
      val compose =
          raw.entries.associate { (key, value) ->
            require(key is String && value is List<*>)
            key to value.toList()
          }
      mounted.value = false
      return PrimaryNavigationSnapshot(saved, compose)
    } finally {
      parcel.recycle()
    }
  }

  fun restoreNewOwners(snapshot: PrimaryNavigationSnapshot) {
    check(!mounted.value)
    val original = owner.value
    original.close()
    val restored = NavigationParent(snapshot.owner)
    assertNotSame(original.viewModelStore, restored.viewModelStore)
    owner.value = restored
    registry.value =
        SaveableStateRegistry(snapshot.compose) { it is Parcelable || it is JavaSerializable }
    mounted.value = true
  }

  fun close() {
    owner.value.close()
    context.close()
  }
}

private val NAVIGATION_CASH =
    Cuenta(
        id = Cuenta.defaultCashId(Moneda.COP),
        nombre = "Efectivo",
        tipo = TipoCuenta.EFECTIVO,
        saldoInicial = Money(0, Moneda.COP),
        diaCorte = null,
        archivada = false,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        color = TipoCuenta.EFECTIVO.defaultColor,
    )

internal data class PrimaryNavigationSnapshot(
    val owner: SavedState,
    val compose: Map<String, List<Any?>>,
)

internal class NavigationParent(restored: SavedState?) :
    SavedStateRegistryOwner, ViewModelStoreOwner {
  override val viewModelStore = ViewModelStore()
  override val lifecycle = LifecycleRegistry.createUnsafe(this)
  private val controller = SavedStateRegistryController.create(this)
  override val savedStateRegistry = controller.savedStateRegistry
  val viewModelOwner: ViewModelStoreOwner

  init {
    controller.performRestore(restored)
    viewModelOwner = androidx.lifecycle.viewmodel.ViewModelStoreOwner(viewModelStore, this)
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
  }

  fun save(): SavedState = savedState().also(controller::performSave)

  fun close() {
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    viewModelStore.clear()
  }
}

/** Statistics read the same rows as Search, so a category's count matches its filtered results. */
internal class NavigationMovimientos :
    MovimientoRepository, ActividadRepository, StatisticsRepository {
  override suspend fun defaultSelection(
      primary: Moneda,
      tipo: TipoMovimiento,
      medio: MedioPago?,
  ): DataOutcome<CuentaSelection?> =
      DataOutcome.Success(
          CuentaSelection(Cuenta.defaultCashId(primary), medio ?: MedioPago.EFECTIVO, primary)
      )

  private val rows =
      MutableStateFlow(
          (1..60).map { index ->
            Movimiento(
                "row-$index",
                TipoMovimiento.GASTO,
                Money(125, Moneda.USD),
                LocalDate(2026, 10, 5),
                "food",
                "Item $index",
                null,
                Instant.parse("2026-10-05T12:00:00Z"),
                Cuenta.defaultCashId(Moneda.USD),
                MedioPago.EFECTIVO,
            )
          }
      )
  val starts = AtomicInteger()

  /** While false, Search reads stay pending, as a slow first query would. */
  val searchReadsReleased = MutableStateFlow(true)
  val activeRanges = AtomicInteger()
  val activeRecent = AtomicInteger()

  override fun observeFiltered(
      criteria: MovimientoReadCriteria
  ): Flow<DataOutcome<MovimientoSearchSnapshot>> = flow {
    starts.incrementAndGet()
    searchReadsReleased.first { it }
    emitAll(
        rows.map { values ->
          val currencyRows = values.filter { it.monto.moneda == criteria.moneda }
          DataOutcome.Success(
              MovimientoSearchSnapshot(
                  currencyRows
                      .filter { row ->
                        (criteria.range == null ||
                            row.fecha in criteria.range.start..criteria.range.end) &&
                            (criteria.tipo == null || row.tipo == criteria.tipo.tipoMovimiento) &&
                            (criteria.categoryIds.isEmpty() ||
                                row.categoriaId in criteria.categoryIds)
                      }
                      .sortedWith(navigationStorageOrder)
                      .map(Actividad::DeMovimiento),
                  currencyRows.map { it.fecha.year }.distinct().sortedDescending(),
              )
          )
        }
    )
  }

  override fun observeActividadRange(
      moneda: Moneda,
      range: MovimientoDateRange,
  ): Flow<DataOutcome<List<Actividad>>> =
      observeRange(moneda, range).map { outcome -> outcome.map { it.map(Actividad::DeMovimiento) } }

  private fun observeRange(
      moneda: Moneda,
      range: MovimientoDateRange,
  ): Flow<DataOutcome<List<Movimiento>>> = flow {
    starts.incrementAndGet()
    activeRanges.incrementAndGet()
    try {
      emitAll(
          rows.map { values ->
            DataOutcome.Success(
                values
                    .filter { it.fecha in range.start..range.end && it.monto.moneda == moneda }
                    .sortedWith(navigationStorageOrder)
            )
          }
      )
    } finally {
      activeRanges.decrementAndGet()
    }
  }

  override fun observeRecent(moneda: Moneda, limit: Int): Flow<DataOutcome<List<Movimiento>>> =
      flow {
        activeRecent.incrementAndGet()
        try {
          emitAll(
              rows.map { values ->
                DataOutcome.Success(
                    values
                        .filter { it.monto.moneda == moneda }
                        .sortedWith(navigationStorageOrder)
                        .take(limit)
                )
              }
          )
        } finally {
          activeRecent.decrementAndGet()
        }
      }

  override fun observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>> = rows.map {
    DataOutcome.Success(it.map { row -> row.fecha.year }.distinct())
  }

  override fun observeSummary(
      moneda: Moneda,
      range: MovimientoDateRange,
  ): Flow<DataOutcome<MovimientoSummary>> = rows.map {
    val inRange = it.filter { row -> row.fecha in range.start..range.end }
    DataOutcome.Success(
        MovimientoSummary(
            Money(0, moneda),
            Money(inRange.sumOf { row -> row.monto.minorUnits }, moneda),
        )
    )
  }

  override fun observeCategoryHistory(limit: Int): Flow<DataOutcome<List<CategoryHistoryRow>>> =
      MutableStateFlow(DataOutcome.Success(emptyList()))

  override suspend fun insert(movimiento: Movimiento): DataOutcome<Unit> {
    rows.value += movimiento
    return DataOutcome.Success(Unit)
  }

  override suspend fun insertAll(movimientos: List<Movimiento>): DataOutcome<Unit> {
    rows.value += movimientos
    return DataOutcome.Success(Unit)
  }

  override suspend fun getById(id: String): DataOutcome<Movimiento?> =
      DataOutcome.Success(rows.value.firstOrNull { it.id == id })

  override suspend fun update(
      id: String,
      changes: MovimientoChanges,
  ): DataOutcome<MovimientoUpdateResult> {
    val current =
        rows.value.firstOrNull { it.id == id }
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
    val values = rows.value.map { if (it.id == id) updated else it }
    rows.value = values
    return DataOutcome.Success(MovimientoUpdateResult.Updated)
  }

  override suspend fun deleteAndCapture(id: String): DataOutcome<Movimiento?> {
    val captured = rows.value.firstOrNull { it.id == id }
    val values = rows.value.filter { it.id != id }
    rows.value = values
    return DataOutcome.Success(captured)
  }

  override suspend fun restoreDeleted(movimiento: Movimiento): DataOutcome<RestoreResult> {
    val current = rows.value.firstOrNull { it.id == movimiento.id }
    if (current != null)
        return DataOutcome.Success(
            if (current == movimiento) RestoreResult.AlreadyPresent else RestoreResult.Conflict
        )
    val values = rows.value + movimiento
    rows.value = values
    return DataOutcome.Success(RestoreResult.Restored)
  }

  override suspend fun getAll(): DataOutcome<List<Movimiento>> = DataOutcome.Success(rows.value)

  override fun observe(request: StatisticsRequest): Flow<DataOutcome<StatisticsSnapshot>> =
      rows.map { values ->
        val currency = values.filter { it.monto.moneda == request.moneda }
        val period = currency.within(request.period)
        val previous = request.comparison?.let { currency.within(it) }
        DataOutcome.Success(
            StatisticsSnapshot(
                current = period.summary(request.moneda),
                previous = previous?.summary(request.moneda),
                categorias =
                    period.totals(previous, request.moneda) {
                      StatisticsKey.Categoria(it.categoriaId)
                    },
                etiquetas = emptyList(),
                cuentas =
                    period.totals(previous, request.moneda) { StatisticsKey.Cuenta(it.cuentaId) },
                medios = period.totals(previous, request.moneda) { StatisticsKey.Medio(it.medio) },
                months =
                    request.trend.months().map { month ->
                      MonthTotals(month, currency.within(month.range()).summary(request.moneda))
                    },
                daily =
                    request.daily.days().map { day ->
                      DayGasto(
                          day,
                          currency.within(MovimientoDateRange(day, day)).gastos(request.moneda),
                      )
                    },
                hasAnyMovimiento = currency.isNotEmpty(),
            )
        )
      }

  override fun observeCategory(
      request: CategoryStatisticsRequest
  ): Flow<DataOutcome<CategoryStatisticsSnapshot>> = rows.map { values ->
    val gastos = values.filter {
      it.monto.moneda == request.moneda &&
          it.tipo == TipoMovimiento.GASTO &&
          it.categoriaId == request.rootId
    }
    val period = gastos.within(request.period)
    val current = period.gastos(request.moneda)
    val previous = request.comparison?.let { gastos.within(it).gastos(request.moneda) }
    DataOutcome.Success(
        CategoryStatisticsSnapshot(
            current = current,
            previous = previous,
            count = period.size.toLong(),
            subcategorias =
                listOf(DimensionTotal(StatisticsKey.Categoria(request.rootId), current, previous)),
            months =
                request.trend.months().map { month ->
                  month to gastos.within(month.range()).gastos(request.moneda)
                },
            largest =
                period
                    .sortedWith(
                        compareByDescending<Movimiento> { it.monto }
                            .thenByDescending { it.fecha }
                            .thenBy { it.id }
                    )
                    .take(LARGEST_COUNT),
        )
    )
  }
}

private fun List<Movimiento>.within(range: MovimientoDateRange) = filter {
  it.fecha in range.start..range.end
}

private fun List<Movimiento>.gastos(moneda: Moneda) =
    Money(filter { it.tipo == TipoMovimiento.GASTO }.sumOf { it.monto.minorUnits }, moneda)

private fun List<Movimiento>.summary(moneda: Moneda) =
    MovimientoSummary(
        Money(filter { it.tipo == TipoMovimiento.INGRESO }.sumOf { it.monto.minorUnits }, moneda),
        gastos(moneda),
    )

private fun List<Movimiento>.totals(
    previous: List<Movimiento>?,
    moneda: Moneda,
    key: (Movimiento) -> StatisticsKey,
): List<DimensionTotal> {
  val current = filter { it.tipo == TipoMovimiento.GASTO }.groupBy(key)
  val before = previous?.filter { it.tipo == TipoMovimiento.GASTO }?.groupBy(key)
  return (current.keys + before?.keys.orEmpty())
      .map { group ->
        DimensionTotal(
            group,
            current[group].orEmpty().gastos(moneda),
            before?.let { it[group].orEmpty().gastos(moneda) },
        )
      }
      .sortedWith(DimensionTotalOrder)
}

private fun MovimientoDateRange.months(): List<YearMonth> =
    generateSequence(start.yearMonth) { it.plus(1, DateTimeUnit.MONTH) }
        .takeWhile { it <= end.yearMonth }
        .toList()

private fun MovimientoDateRange.days(): List<LocalDate> =
    generateSequence(start) { it.plus(1, DateTimeUnit.DAY) }.takeWhile { it <= end }.toList()

private fun YearMonth.range() = MovimientoPeriodRanges.calendarMonth(this)

private const val LARGEST_COUNT = 3

private val navigationStorageOrder =
    compareByDescending<Movimiento> { it.fecha }
        .thenByDescending { it.createdAt }
        .thenByDescending { it.id }

private class NavigationPreferences(monedaPrincipal: Moneda) : AppPreferencesRepository {
  override val preferences: Flow<DataOutcome<AppPreferencesState>> =
      MutableStateFlow(
          DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(monedaPrincipal)))
      )

  override suspend fun initialize(monedaPrincipal: Moneda): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun setThemeMode(themeMode: ThemeMode): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun setAppLocale(appLocale: AppLocale?): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun setMonedaPrincipal(moneda: Moneda): DataOutcome<Unit> =
      DataOutcome.Success(Unit)
}

private class NavigationCategories : CategoriaRepository by UnsupportedCategoriaRepository {
  private val category = Categoria("food", "Food", null, CategoryIcon.UTENSILS, CategoryTint.AMBER)

  override fun observeAll(): Flow<DataOutcome<List<Categoria>>> =
      MutableStateFlow(DataOutcome.Success(listOf(category)))

  override suspend fun getAll(): DataOutcome<List<Categoria>> =
      DataOutcome.Success(listOf(category))

  override suspend fun localizeSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override fun observeMovimientoCounts(moneda: Moneda): Flow<DataOutcome<Map<String, Long>>> =
      MutableStateFlow(DataOutcome.Success(mapOf(category.id to 60L)))
}
