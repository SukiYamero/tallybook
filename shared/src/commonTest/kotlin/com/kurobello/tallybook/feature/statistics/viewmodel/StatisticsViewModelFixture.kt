package com.kurobello.tallybook.feature.statistics.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.kurobello.tallybook.core.data.ActividadRepository
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CivilToday
import com.kurobello.tallybook.core.data.FakeCuentaRepository
import com.kurobello.tallybook.core.data.FakeEtiquetaRepository
import com.kurobello.tallybook.core.data.MutableClock
import com.kurobello.tallybook.core.data.StatisticsRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.data.UnsupportedCategoriaRepository
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
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.DimensionTotal
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.StatisticsKey
import com.kurobello.tallybook.core.model.StatisticsRequest
import com.kurobello.tallybook.core.model.StatisticsSnapshot
import com.kurobello.tallybook.core.model.ThemeMode
import com.kurobello.tallybook.core.model.TipoCuenta
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.datetime.TimeZone

internal class StatisticsFixture(private val scope: TestScope) {
  val clock = MutableClock(Instant.parse("2026-10-24T12:00:00Z"))
  val preferences = StatisticsPreferences()
  val statistics = FakeStatistics()
  val cuentas =
      FakeCuentaRepository(
          listOf(statisticsCuenta("usd", Moneda.USD), statisticsCuenta("cop", Moneda.COP))
      )

  fun create(handle: SavedStateHandle = SavedStateHandle()): StatisticsViewModel =
      StatisticsViewModel(
          StatisticsRepositories(
              preferences = preferences,
              statistics = statistics,
              actividades = StatisticsYears,
              categorias = StatisticsCategorias,
              etiquetas = FakeEtiquetaRepository(),
              cuentas = cuentas,
          ),
          handle,
          CivilToday(clock) { TimeZone.UTC },
          StandardTestDispatcher(scope.testScheduler),
          scope.backgroundScope,
      )
}

/**
 * Answers every request with [respond]; a null answer leaves that read pending, and replacing
 * [respond] re-emits every open read.
 */
internal class FakeStatistics : StatisticsRepository {
  val requests = mutableListOf<StatisticsRequest>()
  val categoryRequests = mutableListOf<CategoryStatisticsRequest>()
  val respond =
      MutableStateFlow<(StatisticsRequest) -> DataOutcome<StatisticsSnapshot>?>({
        DataOutcome.Success(statisticsSnapshot(it.moneda))
      })
  val respondCategory =
      MutableStateFlow<(CategoryStatisticsRequest) -> DataOutcome<CategoryStatisticsSnapshot>?>({
        DataOutcome.Success(categorySnapshot(it.moneda))
      })

  override fun observe(request: StatisticsRequest): Flow<DataOutcome<StatisticsSnapshot>> = flow {
    requests += request
    emitAll(respond.map { it(request) }.filterNotNull())
  }

  override fun observeCategory(
      request: CategoryStatisticsRequest
  ): Flow<DataOutcome<CategoryStatisticsSnapshot>> = flow {
    categoryRequests += request
    emitAll(respondCategory.map { it(request) }.filterNotNull())
  }
}

internal class StatisticsPreferences : AppPreferencesRepository {
  val state =
      MutableStateFlow<DataOutcome<AppPreferencesState>>(
          DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(Moneda.USD)))
      )

  override val preferences: Flow<DataOutcome<AppPreferencesState>>
    get() = state

  override suspend fun initialize(monedaPrincipal: Moneda) = DataOutcome.Success(Unit)

  override suspend fun setMonedaPrincipal(moneda: Moneda) = DataOutcome.Success(Unit)

  override suspend fun setThemeMode(themeMode: ThemeMode) = DataOutcome.Success(Unit)

  override suspend fun setAppLocale(appLocale: AppLocale?) = DataOutcome.Success(Unit)
}

private object StatisticsYears : ActividadRepository {
  override fun observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>> =
      flowOf(DataOutcome.Success(listOf(2026, 2025)))

  override fun observeActividadRange(
      moneda: Moneda,
      range: MovimientoDateRange,
  ): Flow<DataOutcome<List<Actividad>>> = error("Not used by Statistics")

  override fun observeFiltered(
      criteria: MovimientoReadCriteria
  ): Flow<DataOutcome<MovimientoSearchSnapshot>> = error("Not used by Statistics")
}

private object StatisticsCategorias : CategoriaRepository by UnsupportedCategoriaRepository {
  override fun observeAll(): Flow<DataOutcome<List<Categoria>>> =
      flowOf(
          DataOutcome.Success(
              listOf(Categoria("comida", "Comida", null, CategoryIcon.UTENSILS, CategoryTint.AMBER))
          )
      )
}

internal fun statisticsSnapshot(moneda: Moneda, gastos: Long = 1_000) =
    StatisticsSnapshot(
        current = MovimientoSummary(Money(0, moneda), Money(gastos, moneda)),
        previous = null,
        categorias =
            listOf(DimensionTotal(StatisticsKey.Categoria("comida"), Money(gastos, moneda), null)),
        etiquetas =
            listOf(DimensionTotal(StatisticsKey.Etiqueta("trip"), Money(gastos, moneda), null)),
        cuentas = emptyList(),
        medios = emptyList(),
        months = emptyList(),
        daily = emptyList(),
        hasAnyMovimiento = true,
    )

internal fun categorySnapshot(moneda: Moneda, count: Long = 3) =
    CategoryStatisticsSnapshot(
        current = Money(1_000, moneda),
        previous = null,
        count = count,
        subcategorias = emptyList(),
        months = emptyList(),
        largest = emptyList(),
    )

private fun statisticsCuenta(id: String, moneda: Moneda) =
    Cuenta(
        id,
        id,
        TipoCuenta.EFECTIVO,
        Money(0, moneda),
        null,
        false,
        Instant.parse("2026-01-01T00:00:00Z"),
    )
