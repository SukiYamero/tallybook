package com.kurobello.tallybook.feature.history.viewmodel

import android.os.Parcel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.savedState
import androidx.savedstate.serialization.encodeToSavedState
import androidx.savedstate.write
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.data.ActividadRepository
import com.kurobello.tallybook.core.data.ActividadRepositoryImpl
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.AppPreferencesRepositoryImpl
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategoriaRepositoryImpl
import com.kurobello.tallybook.core.data.CuentaRepository
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.data.FakeCuentaRepository
import com.kurobello.tallybook.core.data.FakeEtiquetaRepository
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.MovimientoRepositoryImpl
import com.kurobello.tallybook.core.data.PreferencesDataStoreFactory
import com.kurobello.tallybook.core.data.PreferencesFile
import com.kurobello.tallybook.core.data.PreferencesPathProvider
import com.kurobello.tallybook.core.data.TestChangeClock
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.data.cashCuenta
import com.kurobello.tallybook.core.data.insertLegacy
import com.kurobello.tallybook.core.data.seedCashCuentas
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.di.DispatcherQualifier
import com.kurobello.tallybook.di.viewModelModule
import com.kurobello.tallybook.feature.history.ui.HistoryMovimientoRowUi
import java.sql.Driver
import java.sql.DriverManager
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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
internal class HistoryViewModelGraphHostTest {
  @Test
  fun `production Koin binding observes real SQL and restores typed selection in a new owner`() =
      runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        // DriverManager may have initialized under a plain JVM test's different class loader.
        val jdbc =
            Class.forName("org.sqlite.JDBC")
                .asSubclass(Driver::class.java)
                .getDeclaredConstructor()
                .newInstance()
        DriverManager.registerDriver(jdbc)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TallybookDatabase.Schema.create(driver)
        val database = TallybookDatabase(driver).apply { seedCashCuentas() }
        val directory = createTempDirectory("tallybook-history-vm-")
        val factory =
            PreferencesDataStoreFactory(
                object : PreferencesPathProvider {
                  override fun path(file: PreferencesFile) =
                      directory.resolve(file.fileName).toString()
                },
                dispatcher,
            )
        val preferences = AppPreferencesRepositoryImpl(factory.create())
        val movimientos = MovimientoRepositoryImpl(database, dispatcher, TestChangeClock())
        val categorias =
            CategoriaRepositoryImpl(
                database,
                dispatcher,
                IdGenerator { error("Not used by this test") },
                TestChangeClock(),
            )
        val owners = mutableListOf<HistoryRegistryOwner>()
        val context = koinApplication {
          modules(
              viewModelModule,
              module {
                single<AppPreferencesRepository> { preferences }
                single<MovimientoRepository> { movimientos }
                single<ActividadRepository> { ActividadRepositoryImpl(database, dispatcher) }
                single<CuentaRepository> { FakeCuentaRepository(listOf(cashCuenta(Moneda.USD))) }
                single<EtiquetaRepository> { FakeEtiquetaRepository() }
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
          val typed =
              encodeToSavedState(
                  HistorySavedInput.serializer(),
                  HistorySavedInput(1, "Week", "2024-02-22"),
              )
          val seededOwner = HistoryRegistryOwner(null, typed).also(owners::add)
          val seeded = context.history(seededOwner)
          assertEquals(
              HistorySelection(HistoryScope.Week, LocalDate(2024, 2, 22)),
              seeded.uiState.value.selection,
          )
          assertEquals(
              listOf(2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019),
              seeded.uiState.value.availableYears,
          )
          val malformedOwner = HistoryRegistryOwner(null, savedState()).also(owners::add)
          assertEquals(
              HistorySelection(HistoryScope.Day, LocalDate(2026, 10, 5)),
              context.history(malformedOwner).uiState.value.selection,
          )
          val unsupported =
              encodeToSavedState(
                  HistorySavedInput.serializer(),
                  HistorySavedInput(2, "Year", "2024-02-22"),
              )
          val unsupportedOwner = HistoryRegistryOwner(null, unsupported).also(owners::add)
          assertEquals(
              HistorySelection(HistoryScope.Day, LocalDate(2026, 10, 5)),
              context.history(unsupportedOwner).uiState.value.selection,
          )
          val firstOwner = HistoryRegistryOwner(null).also(owners::add)
          val first = context.history(firstOwner)
          val collector = backgroundScope.launch { first.uiState.collect {} }
          runCurrent()
          assertEquals(
              0,
              assertIs<RetainedLoadState.Ready<HistoryProjection.Content>>(
                      first.uiState.value.period
                  )
                  .value
                  .movementCount,
          )
          database.insertLegacy(row("inside", LocalDate(2026, 10, 5), Moneda.USD))
          runCurrent()
          assertEquals(
              1,
              assertIs<RetainedLoadState.Ready<HistoryProjection.Content>>(
                      first.uiState.value.period
                  )
                  .value
                  .movementCount,
          )
          assertEquals(
              "Sin categoría",
              assertIs<RetainedLoadState.Ready<HistoryProjection.Content>>(
                      first.uiState.value.period
                  )
                  .value
                  .groups
                  .single()
                  .rows
                  .single()
                  .let { assertIs<HistoryMovimientoRowUi>(it) }
                  .title,
          )
          database.insertLegacy(row("outside", LocalDate(2026, 10, 4), Moneda.USD))
          database.insertLegacy(row("foreign", LocalDate(2026, 10, 5), Moneda.COP))
          runCurrent()
          assertEquals(
              1,
              assertIs<RetainedLoadState.Ready<HistoryProjection.Content>>(
                      first.uiState.value.period
                  )
                  .value
                  .movementCount,
          )
          assertEquals(
              Money(-250, Moneda.USD),
              assertIs<DataOutcome.Success<MovimientoSummary>>(
                      movimientos.observeSummary(Moneda.USD, allDates).first()
                  )
                  .data
                  .balance,
          )
          first.onScopeChanged(HistoryScope.Week)
          runCurrent()
          assertEquals(
              2,
              assertIs<RetainedLoadState.Ready<HistoryProjection.Content>>(
                      first.uiState.value.period
                  )
                  .value
                  .movementCount,
          )
          val saved = firstOwner.saveAndParcel()
          collector.cancel()
          firstOwner.close()
          runCurrent()
          val secondOwner = HistoryRegistryOwner(saved).also(owners::add)
          val second = context.history(secondOwner)
          assertNotSame(first, second)
          assertEquals(
              HistorySelection(HistoryScope.Week, LocalDate(2026, 10, 5)),
              second.uiState.value.selection,
          )
          backgroundScope.launch { second.uiState.collect {} }
          runCurrent()
          assertEquals(
              2,
              assertIs<RetainedLoadState.Ready<HistoryProjection.Content>>(
                      second.uiState.value.period
                  )
                  .value
                  .movementCount,
          )
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
private fun KoinApplication.history(owner: HistoryRegistryOwner): HistoryViewModel =
    resolveViewModel(
        vmClass = HistoryViewModel::class,
        viewModelStore = owner.viewModelStore,
        extras = defaultExtras(owner.viewModelOwner),
        scope = koin.scopeRegistry.rootScope,
        parameters = { parametersOf("Sin categoría") },
    )

private class HistoryRegistryOwner(restored: SavedState?, input: SavedState? = null) :
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
    if (input != null) defaults.write { putSavedState(HISTORY_SAVED_INPUT_KEY, input) }
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
        "missing",
        null,
        null,
        Instant.parse("2026-10-05T00:00:00Z"),
        Cuenta.defaultCashId(moneda),
        MedioPago.EFECTIVO,
    )

private val allDates = MovimientoDateRange(LocalDate(2000, 1, 1), LocalDate(2100, 12, 31))
