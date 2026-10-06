package com.kurobello.tallybook.core.ui.navigation

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.savedState
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.di.DispatcherQualifier
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module

internal class PrimaryDeviceShell {
  val movimientos = NavigationMovimientos()
  val mounted = mutableStateOf(true)
  val owner = mutableStateOf(NavigationParent(null))
  val registry =
      mutableStateOf(SaveableStateRegistry(null) { it is Parcelable || it is JavaSerializable })
  private val ids = AtomicInteger()
  val context = koinApplication {
    modules(
        viewModelModule,
        module {
          single<AppPreferencesRepository> { NavigationPreferences() }
          single<MovimientoRepository> { movimientos }
          single<CategoriaRepository> { NavigationCategories() }
          single<IdGenerator> { IdGenerator { "new-${ids.incrementAndGet()}" } }
          single<Clock> {
            object : Clock {
              override fun now() = Instant.parse("2026-10-05T12:00:00Z")
            }
          }
          single<TimeZoneProvider> { TimeZoneProvider { TimeZone.UTC } }
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

internal class NavigationMovimientos : MovimientoRepository {
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
                Instant.parse("2026-10-05T12:00:00Z"),
            )
          }
      )
  val starts = AtomicInteger()
  val activeRanges = AtomicInteger()
  val activeRecent = AtomicInteger()

  override fun observeFiltered(
      criteria: MovimientoReadCriteria
  ): Flow<DataOutcome<MovimientoSearchSnapshot>> = flow {
    starts.incrementAndGet()
    emitAll(
        rows.map { values ->
          val currencyRows = values.filter { it.monto.moneda == criteria.moneda }
          DataOutcome.Success(
              MovimientoSearchSnapshot(
                  currencyRows
                      .filter { row ->
                        (criteria.range == null ||
                            row.fecha in criteria.range.start..criteria.range.end) &&
                            (criteria.tipo == null || row.tipo == criteria.tipo) &&
                            (criteria.categoryIds.isEmpty() ||
                                row.categoriaId in criteria.categoryIds)
                      }
                      .sortedWith(navigationStorageOrder),
                  currencyRows.map { it.fecha.year }.distinct().sortedDescending(),
              )
          )
        }
    )
  }

  override fun observeRange(
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

  override fun observeRecent(limit: Int): Flow<DataOutcome<List<Movimiento>>> = flow {
    activeRecent.incrementAndGet()
    try {
      emitAll(rows.map { DataOutcome.Success(it.sortedWith(navigationStorageOrder).take(limit)) })
    } finally {
      activeRecent.decrementAndGet()
    }
  }

  override fun observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>> = rows.map {
    DataOutcome.Success(it.map { row -> row.fecha.year }.distinct())
  }

  override fun observeSummary(moneda: Moneda): Flow<DataOutcome<MovimientoSummary>> = rows.map {
    DataOutcome.Success(
        MovimientoSummary(Money(0, moneda), Money(it.sumOf { row -> row.monto.minorUnits }, moneda))
    )
  }

  override suspend fun insert(movimiento: Movimiento): DataOutcome<Unit> {
    rows.value += movimiento
    return DataOutcome.Success(Unit)
  }

  override suspend fun delete(id: String): DataOutcome<Unit> {
    rows.value = rows.value.filter { it.id != id }
    return DataOutcome.Success(Unit)
  }

  override suspend fun getAll(): DataOutcome<List<Movimiento>> = DataOutcome.Success(rows.value)
}

private val navigationStorageOrder =
    compareByDescending<Movimiento> { it.fecha }
        .thenByDescending { it.createdAt }
        .thenByDescending { it.id }

private class NavigationPreferences : AppPreferencesRepository {
  override val preferences: Flow<DataOutcome<AppPreferencesState>> =
      MutableStateFlow(DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(Moneda.USD))))

  override suspend fun initialize(monedaPrincipal: Moneda): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun setMonedaPrincipal(moneda: Moneda): DataOutcome<Unit> =
      DataOutcome.Success(Unit)
}

private class NavigationCategories : CategoriaRepository {
  private val category = Categoria("food", "Food", null, CategoryIcon.UTENSILS, CategoryTint.AMBER)

  override fun observeAll(): Flow<DataOutcome<List<Categoria>>> =
      MutableStateFlow(DataOutcome.Success(listOf(category)))

  override suspend fun getAll(): DataOutcome<List<Categoria>> =
      DataOutcome.Success(listOf(category))

  override suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
      DataOutcome.Success(Unit)
}
