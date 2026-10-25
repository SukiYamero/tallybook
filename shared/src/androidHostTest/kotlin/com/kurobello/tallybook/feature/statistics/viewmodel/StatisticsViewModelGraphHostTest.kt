package com.kurobello.tallybook.feature.statistics.viewmodel

import android.os.Parcel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.savedState
import com.kurobello.tallybook.core.data.ActividadRepository
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CuentaRepository
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.data.FakeCuentaRepository
import com.kurobello.tallybook.core.data.FakeEtiquetaRepository
import com.kurobello.tallybook.core.data.StatisticsRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.data.UnsupportedCategoriaRepository
import com.kurobello.tallybook.core.model.Actividad
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.di.DispatcherQualifier
import com.kurobello.tallybook.di.viewModelModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.viewmodel.defaultExtras
import org.koin.viewmodel.resolveViewModel
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class, KoinInternalApi::class)
@RunWith(RobolectricTestRunner::class)
internal class StatisticsViewModelGraphHostTest {
  @Test
  fun `the production module builds the view model and restores its input in a new owner`() =
      runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val statistics = FakeStatistics()
        val context = koinApplication {
          modules(
              viewModelModule,
              module {
                single<AppPreferencesRepository> { StatisticsPreferences() }
                single<StatisticsRepository> { statistics }
                single<ActividadRepository> { NoYears }
                single<CategoriaRepository> { NoCategorias }
                single<EtiquetaRepository> { FakeEtiquetaRepository() }
                single<CuentaRepository> { FakeCuentaRepository() }
                single<Clock> {
                  object : Clock {
                    override fun now() = Instant.parse("2026-10-24T12:00:00Z")
                  }
                }
                single<TimeZoneProvider> { TimeZoneProvider { TimeZone.UTC } }
                single<CoroutineDispatcher>(named(DispatcherQualifier.DEFAULT)) { dispatcher }
              },
          )
        }
        val owners = mutableListOf<StatisticsRegistryOwner>()
        try {
          val firstOwner = StatisticsRegistryOwner(null).also(owners::add)
          val first = context.statistics(firstOwner)
          val firstCollector = backgroundScope.launch { first.uiState.collect {} }
          runCurrent()
          assertIs<RetainedLoadState.Ready<StatisticsContent>>(first.uiState.value.content)
          assertEquals(
              MovimientoDateRange(LocalDate(2026, 10, 1), LocalDate(2026, 10, 24)),
              statistics.requests.single().period,
          )
          assertEquals(Moneda.USD, statistics.requests.single().moneda)

          val september = MovimientoDateRange(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30))
          first.onPeriodSelected(september)
          first.onDimensionSelected(StatisticsDimension.Medios)
          runCurrent()
          val saved = firstOwner.saveAndParcel()
          firstCollector.cancel()
          firstOwner.close()
          runCurrent()

          val secondOwner = StatisticsRegistryOwner(saved).also(owners::add)
          val second = context.statistics(secondOwner)
          backgroundScope.launch { second.uiState.collect {} }
          runCurrent()
          assertEquals(LocalDate(2026, 9, 24), second.uiState.value.selection.anchor)
          assertEquals(StatisticsDimension.Medios, second.uiState.value.dimension)
          assertEquals(september, statistics.requests.last().period)
        } finally {
          owners.forEach { it.close() }
          runCurrent()
          context.close()
          Dispatchers.resetMain()
        }
      }
}

@OptIn(KoinInternalApi::class)
private fun KoinApplication.statistics(owner: StatisticsRegistryOwner): StatisticsViewModel =
    resolveViewModel(
        vmClass = StatisticsViewModel::class,
        viewModelStore = owner.viewModelStore,
        extras = defaultExtras(owner.viewModelOwner),
        scope = koin.scopeRegistry.rootScope,
    )

private class StatisticsRegistryOwner(restored: SavedState?) :
    SavedStateRegistryOwner, ViewModelStoreOwner {
  override val lifecycle = LifecycleRegistry.createUnsafe(this)
  override val viewModelStore = ViewModelStore()
  private val controller = SavedStateRegistryController.create(this)
  override val savedStateRegistry = controller.savedStateRegistry
  val viewModelOwner: ViewModelStoreOwner
  private var closed = false

  init {
    controller.performRestore(restored)
    viewModelOwner =
        androidx.lifecycle.viewmodel.ViewModelStoreOwner(
            viewModelStore,
            this,
            defaultArgs = savedState(),
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

private object NoYears : ActividadRepository {
  override fun observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>> =
      flowOf(DataOutcome.Success(emptyList()))

  override fun observeActividadRange(
      moneda: Moneda,
      range: MovimientoDateRange,
  ): Flow<DataOutcome<List<Actividad>>> = error("Not used by Statistics")

  override fun observeFiltered(
      criteria: MovimientoReadCriteria
  ): Flow<DataOutcome<MovimientoSearchSnapshot>> = error("Not used by Statistics")
}

private object NoCategorias : CategoriaRepository by UnsupportedCategoriaRepository {
  override fun observeAll(): Flow<DataOutcome<List<Categoria>>> =
      flowOf(DataOutcome.Success(emptyList()))
}
