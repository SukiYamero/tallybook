package com.kurobello.tallybook.feature.search.viewmodel

import android.os.Parcel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.savedState
import androidx.savedstate.write
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.AppPreferencesRepositoryImpl
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategoriaRepositoryImpl
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.MovimientoRepositoryImpl
import com.kurobello.tallybook.core.data.PreferencesDataStoreFactory
import com.kurobello.tallybook.core.data.PreferencesPathProvider
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.di.DispatcherQualifier
import com.kurobello.tallybook.di.viewModelModule
import java.sql.Driver
import java.sql.DriverManager
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.runner.RunWith
import org.koin.core.KoinApplication
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.viewmodel.defaultExtras
import org.koin.viewmodel.resolveViewModel
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class, KoinInternalApi::class)
@RunWith(RobolectricTestRunner::class)
internal class SearchViewModelGraphHostTest {
  @Test
  fun `production graph observes SQL and restores only applied input in new owner`() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)
    val jdbc =
        Class.forName("org.sqlite.JDBC")
            .asSubclass(Driver::class.java)
            .getDeclaredConstructor()
            .newInstance()
    DriverManager.registerDriver(jdbc)
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    val database = TallybookDatabase(driver)
    val directory = createTempDirectory("tallybook-search-vm-")
    val factory =
        PreferencesDataStoreFactory(
            object : PreferencesPathProvider {
              override fun path() = directory.resolve("preferences.preferences_pb").toString()
            },
            dispatcher,
        )
    val preferences = AppPreferencesRepositoryImpl(factory.create())
    val movimientos = MovimientoRepositoryImpl(database, dispatcher)
    val categorias =
        CategoriaRepositoryImpl(
            database,
            dispatcher,
            IdGenerator { error("Not used by this test") },
        )
    val owners = mutableListOf<SearchRegistryOwner>()
    val context = koinApplication {
      modules(
          viewModelModule,
          module {
            single<AppPreferencesRepository> { preferences }
            single<MovimientoRepository> { movimientos }
            single<CategoriaRepository> { categorias }
            single<Clock> {
              object : Clock {
                override fun now() = Instant.parse("2026-10-05T12:00:00Z")
              }
            }
            single<TimeZoneProvider> { TimeZoneProvider { TimeZone.UTC } }
            single<CoroutineDispatcher>(named(DispatcherQualifier.DEFAULT)) { dispatcher }
          },
      )
    }
    try {
      assertEquals(DataOutcome.Success(Unit), preferences.initialize(Moneda.USD))
      val firstOwner = SearchRegistryOwner(null).also(owners::add)
      val first = context.search(firstOwner)
      val collector = backgroundScope.launch { first.uiState.collect {} }
      runCurrent()
      assertEquals(0, first.uiState.value.currentCount)
      movimientos.insert(row("inside", LocalDate(2026, 10, 5), Moneda.USD))
      movimientos.insert(row("foreign", LocalDate(2026, 10, 5), Moneda.COP))
      runCurrent()
      assertEquals(1, first.uiState.value.currentCount)
      val session = requireNotNull(first.onOpenFilters())
      first.onFilterDraftChanged(
          session,
          first.uiState.value.filters.copy(tipo = TipoMovimiento.INGRESO),
      )
      assertTrue(first.hasActiveFilterSession)
      first.onQueryChanged("café %_")
      val saved = firstOwner.saveAndParcel()
      collector.cancel()
      firstOwner.close()
      runCurrent()
      val secondOwner = SearchRegistryOwner(saved).also(owners::add)
      val second = context.search(secondOwner)
      assertNotSame(first, second)
      assertEquals("café %_", second.uiState.value.rawQuery)
      assertEquals(null, second.uiState.value.filters.tipo)
      assertFalse(second.hasActiveFilterSession)
      backgroundScope.launch { second.uiState.collect {} }
      runCurrent()
      assertEquals(0, second.uiState.value.currentCount)
      second.onClearQuery()
      runCurrent()
      assertEquals(1, second.uiState.value.currentCount)
    } finally {
      owners.forEach { it.close() }
      runCurrent()
      context.close()
      factory.close()
      driver.close()
      DriverManager.deregisterDriver(jdbc)
      directory.toFile().deleteRecursively()
      Dispatchers.resetMain()
    }
  }
}

@OptIn(KoinInternalApi::class)
private fun KoinApplication.search(owner: SearchRegistryOwner): SearchViewModel =
    resolveViewModel(
        vmClass = SearchViewModel::class,
        viewModelStore = owner.viewModelStore,
        extras = defaultExtras(owner.viewModelOwner),
        scope = koin.scopeRegistry.rootScope,
        parameters = { parametersOf("Sin categoría") },
    )

private class SearchRegistryOwner(restored: SavedState?, input: SavedState? = null) :
    SavedStateRegistryOwner, ViewModelStoreOwner {
  override val lifecycle = LifecycleRegistry.createUnsafe(this)
  override val viewModelStore = ViewModelStore()
  private val controller = SavedStateRegistryController.create(this)
  override val savedStateRegistry = controller.savedStateRegistry
  val viewModelOwner: ViewModelStoreOwner
  private var closed = false

  init {
    controller.performRestore(restored)
    val defaults = savedState()
    if (input != null) defaults.write { putSavedState(SEARCH_SAVED_INPUT_KEY, input) }
    viewModelOwner =
        androidx.lifecycle.viewmodel.ViewModelStoreOwner(
            viewModelStore,
            this,
            defaultArgs = defaults,
        )
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
  }

  fun saveAndParcel(): SavedState {
    val saved = savedState().also(controller::performSave)
    val parcel = Parcel.obtain()
    try {
      parcel.writeBundle(saved)
      parcel.setDataPosition(0)
      return requireNotNull(parcel.readBundle(javaClass.classLoader))
    } finally {
      parcel.recycle()
    }
  }

  fun close() {
    if (closed) return
    closed = true
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    viewModelStore.clear()
  }
}

private fun row(id: String, date: LocalDate, moneda: Moneda) =
    Movimiento(
        id,
        TipoMovimiento.GASTO,
        Money(125, moneda),
        date,
        listOf("missing"),
        null,
        Instant.parse("2026-10-05T00:00:00Z"),
    )
