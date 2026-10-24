package com.kurobello.tallybook.core.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategoriaRepositoryImpl
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.data.FakeEtiquetaRepository
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.MovimientoRepositoryImpl
import com.kurobello.tallybook.core.data.TestChangeClock
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.data.seedCashCuentas
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.MovimientoRestoreResult
import com.kurobello.tallybook.core.model.MovimientoUpdateResult
import com.kurobello.tallybook.di.viewModelModule
import com.kurobello.tallybook.feature.movimientos.viewmodel.ManageMovimientoViewModel
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFeedbackKind
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoManagementPhase
import com.kurobello.tallybook.feature.movimientos.viewmodel.managementCategoria
import com.kurobello.tallybook.feature.movimientos.viewmodel.managementMovimiento
import java.sql.Driver
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import org.junit.runner.RunWith
import org.koin.compose.KoinIsolatedContext
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ManageMovimientoOwnerHostTest {
  @Test
  fun `actual Activity recreation retains edit and one suspended database update`() = runTest {
    withManagementOwner {
      val first = attach()
      first.viewModel.open("movement")
      settle()
      first.viewModel.onAmountChanged("20.00")
      repository.writeGate = CompletableDeferred()
      first.viewModel.onSubmit()
      settle()
      val retained = first.viewModel
      val next = recreate(first)
      assertSame(retained, next.viewModel)
      assertNotSame(activity.get(), next.owner)
      assertEquals("20.00", next.viewModel.uiState.value.session?.form?.amountInput)
      repository.writeGate?.complete(Unit)
      settle()
      val origin = assertNotNull(next.viewModel.uiState.value.session).token
      assertEquals(MovimientoManagementPhase.Closing, next.viewModel.uiState.value.session?.phase)
      next.viewModel.onSheetHidden(origin)
      next.viewModel.onSheetHidden(origin)
      assertEquals(1, repository.updates)
      assertEquals(MovimientoFeedbackKind.Saved, next.viewModel.uiState.value.feedback?.kind)
      assertEquals(
          2000L,
          assertNotNull((repository.getById("movement") as DataOutcome.Success).data)
              .monto
              .minorUnits,
      )
    }
  }

  @Test
  fun `actual Activity recreation retains claimed Undo and commits one restore`() = runTest {
    withManagementOwner {
      val first = attach()
      first.viewModel.open("movement")
      settle()
      first.viewModel.onRequestDelete()
      first.viewModel.onConfirmDelete()
      settle()
      first.viewModel.onSheetHidden(assertNotNull(first.viewModel.uiState.value.session).token)
      val undo = assertNotNull(first.viewModel.uiState.value.feedback)
      repository.restoreGate = CompletableDeferred()
      first.viewModel.onFeedbackAction(undo.token, undo.revision)
      settle()
      val next = recreate(first)
      assertSame(first.viewModel, next.viewModel)
      next.viewModel.onFeedbackDismiss(undo.token, undo.revision)
      next.viewModel.onFeedbackAction(undo.token, undo.revision)
      repository.restoreGate?.complete(Unit)
      settle()
      assertEquals(1, repository.restores)
      assertEquals(MovimientoFeedbackKind.Restored, next.viewModel.uiState.value.feedback?.kind)
      assertEquals(DataOutcome.Success(managementMovimiento()), repository.getById("movement"))
    }
  }

  @Test
  fun `active scene disposal clears child jobs and new scene starts closed`() = runTest {
    withManagementOwner {
      val first = attach()
      first.viewModel.open("movement")
      settle()
      repository.writeGate = CompletableDeferred()
      first.viewModel.onAmountChanged("20.00")
      first.viewModel.onSubmit()
      settle()
      first.close()
      settle()
      assertNull(first.viewModel.uiState.value.session)
      val next = attach()
      assertNotSame(first.viewModel, next.viewModel)
      assertNull(next.viewModel.uiState.value.session)
      assertNull(next.viewModel.uiState.value.feedback)
      repository.writeGate?.complete(Unit)
      settle()
      assertEquals(DataOutcome.Success(managementMovimiento()), repository.getById("movement"))
    }
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun TestScope.withManagementOwner(block: suspend ManagementOwnerSuite.() -> Unit) {
  val suite = ManagementOwnerSuite(this)
  try {
    suite.prepare()
    suite.block()
  } finally {
    suite.close()
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class ManagementOwnerSuite(val scope: TestScope) {
  private val dispatcher = StandardTestDispatcher(scope.testScheduler)
  // DriverManager can already be initialized by tests using a different Robolectric loader.
  private val jdbc =
      Class.forName("org.sqlite.JDBC")
          .asSubclass(Driver::class.java)
          .getDeclaredConstructor()
          .newInstance()
          .also(DriverManager::registerDriver)
  private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
  private val database = TallybookDatabase(driver)
  val repository: OwnerMutationRepository
  val activity: ActivityController<ComponentActivity>
  private val koin: KoinApplication
  private val hosts = mutableListOf<ManagementOwnerHarness>()

  init {
    TallybookDatabase.Schema.create(driver)
    database.seedCashCuentas()
    Dispatchers.setMain(dispatcher)
    repository =
        OwnerMutationRepository(MovimientoRepositoryImpl(database, dispatcher, TestChangeClock()))
    koin = koinApplication {
      modules(
          viewModelModule,
          module {
            single<MovimientoRepository> { repository }
            single<EtiquetaRepository> { FakeEtiquetaRepository() }
            single<CategoriaRepository> {
              CategoriaRepositoryImpl(
                  database,
                  dispatcher,
                  IdGenerator { error("Not used by this test") },
                  TestChangeClock(),
              )
            }
            single<Clock> {
              object : Clock {
                override fun now() = Instant.parse("2026-09-15T12:00:00Z")
              }
            }
            single<TimeZoneProvider> { TimeZoneProvider { TimeZone.UTC } }
          },
      )
    }
    activity = Robolectric.buildActivity(ComponentActivity::class.java).setup()
  }

  suspend fun prepare() {
    koin.koin.get<CategoriaRepository>().ensureSeeded(listOf(managementCategoria()))
    repository.insert(managementMovimiento())
  }

  fun attach() =
      ManagementOwnerHarness(scope, activity.get(), koin).also {
        hosts += it
        settle()
      }

  fun recreate(previous: ManagementOwnerHarness): ManagementOwnerHarness {
    activity.recreate()
    previous.close()
    settle()
    return attach()
  }

  fun settle() {
    repeat(3) {
      Snapshot.sendApplyNotifications()
      scope.runCurrent()
    }
  }

  suspend fun close() {
    try {
      hosts.forEach { it.close() }
      activity.pause().stop().destroy()
      hosts.forEach { it.finish() }
    } finally {
      try {
        koin.close()
      } finally {
        try {
          driver.close()
        } finally {
          try {
            DriverManager.deregisterDriver(jdbc)
          } finally {
            Dispatchers.resetMain()
          }
        }
      }
    }
  }
}

private class OwnerMutationRepository(private val delegate: MovimientoRepository) :
    MovimientoRepository by delegate {
  var writeGate: CompletableDeferred<Unit>? = null
  var restoreGate: CompletableDeferred<Unit>? = null
  var updates = 0
  var restores = 0

  override suspend fun update(
      id: String,
      changes: MovimientoChanges,
  ): DataOutcome<MovimientoUpdateResult> {
    updates++
    writeGate?.await()
    return delegate.update(id, changes)
  }

  override suspend fun restoreDeleted(
      movimiento: Movimiento
  ): DataOutcome<MovimientoRestoreResult> {
    restores++
    restoreGate?.await()
    return delegate.restoreDeleted(movimiento)
  }
}

private class ManagementOwnerHarness(
    scope: TestScope,
    activity: ComponentActivity,
    koin: KoinApplication,
) {
  lateinit var viewModel: ManageMovimientoViewModel
  lateinit var owner: ViewModelStoreOwner
  private val clock = ManagementOwnerFrameClock()
  private val recomposer = Recomposer(scope.coroutineContext + clock)
  private val composition = Composition(ManagementOwnerApplier(), recomposer)
  private val job: Job = scope.launch(clock) { recomposer.runRecomposeAndApplyChanges() }

  init {
    composition.setContent {
      KoinIsolatedContext(koin) {
        CompositionLocalProvider(
            LocalLifecycleOwner provides activity,
            LocalViewModelStoreOwner provides activity,
            LocalSavedStateRegistryOwner provides activity,
        ) {
          val actualOwner = rememberManageMovimientoOwner()
          val actualViewModel =
              org.koin.compose.viewmodel.koinViewModel<ManageMovimientoViewModel>(
                  viewModelStoreOwner = actualOwner
              )
          SideEffect {
            owner = actualOwner
            viewModel = actualViewModel
          }
        }
      }
    }
  }

  fun close() {
    composition.dispose()
    recomposer.close()
  }

  suspend fun finish() {
    job.join()
  }
}

private class ManagementOwnerApplier : AbstractApplier<Unit>(Unit) {
  override fun insertTopDown(index: Int, instance: Unit) = Unit

  override fun insertBottomUp(index: Int, instance: Unit) = Unit

  override fun remove(index: Int, count: Int) = Unit

  override fun move(from: Int, to: Int, count: Int) = Unit

  override fun onClear() = Unit
}

private class ManagementOwnerFrameClock : MonotonicFrameClock {
  override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(0)
}
