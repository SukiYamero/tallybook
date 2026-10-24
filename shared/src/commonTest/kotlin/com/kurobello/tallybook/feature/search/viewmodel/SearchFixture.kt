package com.kurobello.tallybook.feature.search.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategoryHistoryRow
import com.kurobello.tallybook.core.data.FakeEtiquetaRepository
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.data.UnsupportedCategoriaRepository
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaSelection
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.MovimientoRestoreResult
import com.kurobello.tallybook.core.model.MovimientoSearchSnapshot
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.MovimientoUpdateResult
import com.kurobello.tallybook.core.model.ThemeMode
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.map
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

internal val TODAY = LocalDate(2026, 10, 6)
internal val EMPRESA = Etiqueta("t_empresa", "Empresa", Instant.parse("2026-01-01T00:00:00Z"))
internal val VIAJE = Etiqueta("t_viaje", "Viaje Chile", Instant.parse("2026-01-01T00:00:00Z"))

internal class SearchFixture(private val scope: TestScope) {
  val preferences = SearchPreferences()
  val categorias = SearchCategorias()
  val movimientos = SearchMovimientos()
  val etiquetas = FakeEtiquetaRepository(listOf(EMPRESA, VIAJE))
  val clock = SearchClock()

  fun create(dispatcher: CoroutineDispatcher = StandardTestDispatcher(scope.testScheduler)) =
      SearchViewModel(
          SearchRepositories(preferences, movimientos, categorias, etiquetas),
          SavedStateHandle(),
          "Unknown",
          SearchDateProvider(clock, TimeZoneProvider { TimeZone.UTC }),
          dispatcher,
          scope.backgroundScope,
      )
}

internal class SearchClock(var instant: Instant = Instant.parse("2026-10-06T12:00:00Z")) : Clock {
  override fun now() = instant
}

internal class SearchPreferences : AppPreferencesRepository {
  val source =
      MutableStateFlow<DataOutcome<AppPreferencesState>?>(
          DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(Moneda.USD)))
      )
  override val preferences = source.filterNotNull()

  override suspend fun initialize(monedaPrincipal: Moneda) = DataOutcome.Success(Unit)

  override suspend fun setThemeMode(themeMode: ThemeMode): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun setAppLocale(appLocale: AppLocale?): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun setMonedaPrincipal(moneda: Moneda) = DataOutcome.Success(Unit)
}

internal class SearchCategorias : CategoriaRepository by UnsupportedCategoriaRepository {
  val source = MutableStateFlow<DataOutcome<List<Categoria>>?>(DataOutcome.Success(emptyList()))

  override fun observeAll() = source.filterNotNull()

  override suspend fun getAll() = DataOutcome.Success(emptyList<Categoria>())

  override suspend fun localizeSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun ensureSeeded(categorias: List<Categoria>) = DataOutcome.Success(Unit)
}

internal class SearchMovimientos : MovimientoRepository {
  override suspend fun defaultSelection(
      primary: Moneda,
      tipo: TipoMovimiento,
      medio: MedioPago?,
  ): DataOutcome<CuentaSelection?> = error("Default selections are not expected in this fixture")

  var starts = 0
  var active = 0
  private var rows =
      listOf("Café", "Lunch").mapIndexed { index, text ->
        Movimiento(
            "$index",
            TipoMovimiento.GASTO,
            Money(100, Moneda.USD),
            TODAY,
            "food",
            text,
            null,
            Instant.parse("2026-10-06T12:00:00Z"),
            Cuenta.defaultCashId(Moneda.USD),
            MedioPago.EFECTIVO,
            if (index == 0) listOf("t_empresa") else emptyList(),
        )
      }
  val source =
      MutableStateFlow<DataOutcome<MovimientoSearchSnapshot>?>(
          DataOutcome.Success(MovimientoSearchSnapshot(rows, listOf(2026)))
      )

  fun restore() {
    source.value = DataOutcome.Success(MovimientoSearchSnapshot(rows, listOf(2026)))
  }

  override fun observeFiltered(
      criteria: MovimientoReadCriteria
  ): Flow<DataOutcome<MovimientoSearchSnapshot>> = flow {
    starts += 1
    active += 1
    try {
      emitAll(
          source.filterNotNull().map { result ->
            result.map { snapshot ->
              snapshot.copy(
                  movimientos =
                      snapshot.movimientos.filter {
                        it.monto.moneda == criteria.moneda &&
                            (criteria.range?.let { range ->
                              it.fecha >= range.start && it.fecha <= range.end
                            } != false) &&
                            (criteria.tipo == null || it.tipo == criteria.tipo) &&
                            (criteria.categoryIds.isEmpty() ||
                                it.categoriaId in criteria.categoryIds) &&
                            (criteria.etiquetaIds.isEmpty() ||
                                it.etiquetaIds.any { id -> id in criteria.etiquetaIds })
                      }
              )
            }
          }
      )
    } finally {
      active -= 1
    }
  }

  override fun observeRecent(limit: Int): Flow<DataOutcome<List<Movimiento>>> =
      error("Unexpected read")

  override fun observeRecent(moneda: Moneda, limit: Int): Flow<DataOutcome<List<Movimiento>>> =
      error("Unexpected read")

  override fun observeSummary(moneda: Moneda): Flow<DataOutcome<MovimientoSummary>> =
      error("Unexpected read")

  override fun observeRange(
      moneda: Moneda,
      range: MovimientoDateRange,
  ): Flow<DataOutcome<List<Movimiento>>> = error("Unexpected read")

  override fun observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>> = error("Unexpected read")

  override fun observeCategoryHistory(limit: Int): Flow<DataOutcome<List<CategoryHistoryRow>>> =
      error("Unexpected read")

  override suspend fun insert(movimiento: Movimiento) = DataOutcome.Success(Unit)

  override suspend fun getAll() = DataOutcome.Success(emptyList<Movimiento>())

  override suspend fun getById(id: String): DataOutcome<Movimiento?> =
      DataOutcome.Success(rows.firstOrNull { it.id == id })

  override suspend fun update(
      id: String,
      changes: MovimientoChanges,
  ): DataOutcome<MovimientoUpdateResult> {
    val current =
        rows.firstOrNull { it.id == id }
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
    val values = rows.map { if (it.id == id) updated else it }
    rows = values
    restore()
    return DataOutcome.Success(MovimientoUpdateResult.Updated)
  }

  override suspend fun deleteAndCapture(id: String): DataOutcome<Movimiento?> {
    val captured = rows.firstOrNull { it.id == id }
    val values = rows.filter { it.id != id }
    rows = values
    restore()
    return DataOutcome.Success(captured)
  }

  override suspend fun restoreDeleted(
      movimiento: Movimiento
  ): DataOutcome<MovimientoRestoreResult> {
    val current = rows.firstOrNull { it.id == movimiento.id }
    if (current != null)
        return DataOutcome.Success(
            if (current == movimiento) MovimientoRestoreResult.AlreadyPresent
            else MovimientoRestoreResult.Conflict
        )
    val values = rows + movimiento
    rows = values
    restore()
    return DataOutcome.Success(MovimientoRestoreResult.Restored)
  }

  override suspend fun delete(id: String) = DataOutcome.Success(Unit)
}
