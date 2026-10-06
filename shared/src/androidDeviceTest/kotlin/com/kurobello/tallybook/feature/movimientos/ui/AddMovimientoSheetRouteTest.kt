package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoRepositories
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoViewModel
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.TimeZone
import org.junit.Rule
import org.junit.Test

class AddMovimientoSheetRouteTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun successfulSubmissionDismissesThroughTheRouteSavedEffect() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val movementRepository = FakeMovimientoRepository()
    val viewModel = createViewModel(movementRepository, scope)
    var dismissCount by mutableIntStateOf(0)

    try {
      composeRule.setContent {
        TallybookTheme {
          AddMovimientoSheetRoute(
              visible = true,
              onDismiss = { dismissCount += 1 },
              viewModel = viewModel,
          )
        }
      }
      composeRule.waitUntil(5_000) { !viewModel.uiState.value.isLoading }

      composeRule.runOnIdle {
        viewModel.onAmountChanged("1234")
        viewModel.onCategoriaSelected(category.id)
      }
      composeRule.waitUntil(5_000) { viewModel.uiState.value.canSubmit }
      composeRule.runOnIdle {
        assertTrue(viewModel.uiState.value.canSubmit)
        viewModel.onSubmit()
      }
      composeRule.waitUntil(5_000) { movementRepository.inserted.size == 1 }
      composeRule.waitUntil(5_000) { dismissCount == 1 }

      assertEquals(1, movementRepository.inserted.size)
      assertEquals(1, dismissCount)
    } finally {
      scope.cancel()
    }
  }

  @Test
  fun hiddenCompletionIsDeliveredOnceAfterClosingTheActualRoute() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val viewModel = createViewModel(FakeMovimientoRepository(), scope)
    var visible by mutableStateOf(true)
    var hidden by mutableIntStateOf(0)
    try {
      composeRule.setContent {
        TallybookTheme {
          AddMovimientoSheetRoute(
              visible = visible,
              onDismiss = { visible = false },
              onHidden = { hidden += 1 },
              viewModel = viewModel,
          )
        }
      }
      composeRule.waitForIdle()
      composeRule.runOnIdle { visible = false }
      composeRule.waitUntil(5_000) { hidden == 1 }
      composeRule.waitForIdle()
      composeRule.runOnIdle { assertEquals(1, hidden) }
    } finally {
      scope.cancel()
    }
  }

  private fun createViewModel(
      movementRepository: MovimientoRepository,
      scope: CoroutineScope,
  ): AddMovimientoViewModel =
      AddMovimientoViewModel(
          repositories =
              AddMovimientoRepositories(
                  preferences = FakePreferencesRepository(),
                  categorias = FakeCategoriaRepository(),
              ),
          movimientoRepository = movementRepository,
          idGenerator = IdGenerator { "movement-id" },
          clock = FixedClock,
          timeZoneProvider = TimeZoneProvider { TimeZone.UTC },
          scope = scope,
      )

  private class FakePreferencesRepository : AppPreferencesRepository {
    override val preferences: Flow<DataOutcome<AppPreferencesState>> =
        MutableStateFlow(DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(Moneda.COP))))

    override suspend fun initialize(monedaPrincipal: Moneda): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    override suspend fun setMonedaPrincipal(moneda: Moneda): DataOutcome<Unit> =
        DataOutcome.Success(Unit)
  }

  private class FakeCategoriaRepository : CategoriaRepository {
    override fun observeAll(): Flow<DataOutcome<List<Categoria>>> =
        MutableStateFlow(DataOutcome.Success(listOf(category)))

    override suspend fun getAll(): DataOutcome<List<Categoria>> =
        DataOutcome.Success(listOf(category))

    override suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
        DataOutcome.Success(Unit)
  }

  private class FakeMovimientoRepository : MovimientoRepository {
    val inserted = mutableStateListOf<Movimiento>()

    override fun observeRecent(limit: Int): Flow<DataOutcome<List<Movimiento>>> = emptyFlow()

    override fun observeSummary(moneda: Moneda): Flow<DataOutcome<MovimientoSummary>> = emptyFlow()

    override fun observeRange(
        moneda: Moneda,
        range: MovimientoDateRange,
    ): Flow<DataOutcome<List<Movimiento>>> =
        error("History range reads are not expected in this fixture")

    override fun observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>> =
        error("History year reads are not expected in this fixture")

    override suspend fun insert(movimiento: Movimiento): DataOutcome<Unit> {
      inserted += movimiento
      return DataOutcome.Success(Unit)
    }

    override suspend fun getAll(): DataOutcome<List<Movimiento>> = DataOutcome.Success(inserted)

    override suspend fun delete(id: String): DataOutcome<Unit> = DataOutcome.Success(Unit)
  }

  private object FixedClock : Clock {
    override fun now(): Instant = Instant.parse("2026-09-15T00:00:00Z")
  }

  private companion object {
    val category =
        Categoria(
            id = "food",
            nombre = "Food",
            padreId = null,
            icono = CategoryIcon.UTENSILS,
            color = CategoryTint.AMBER,
        )
  }
}
