package com.kurobello.tallybook

import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategorySeedProvider
import com.kurobello.tallybook.core.data.DeviceLocaleProvider
import com.kurobello.tallybook.core.data.LocalDataBootstrapper
import com.kurobello.tallybook.core.data.TestChangeClock
import com.kurobello.tallybook.core.data.UnsupportedCategoriaRepository
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AppStartupViewModelTest {

  @Test
  fun initializesOnceAndExposesReadyAfterBootstrapSucceeds() = runTest {
    val preferences = FakePreferencesRepository(DataOutcome.Success(Unit))
    val viewModel = AppStartupViewModel(bootstrapper(preferences), backgroundScope)

    runCurrent()

    assertEquals(1, preferences.initializeCalls)
    assertEquals(AppStartupUiState.Ready, viewModel.uiState.value)
  }

  @Test
  fun exposesFailureUntilRetrySucceeds() = runTest {
    val preferences =
        FakePreferencesRepository(
            DataOutcome.Failure(DataError.Local(IllegalStateException())),
            DataOutcome.Success(Unit),
        )
    val viewModel = AppStartupViewModel(bootstrapper(preferences), backgroundScope)

    runCurrent()
    assertEquals(AppStartupUiState.Error, viewModel.uiState.value)

    viewModel.onRetry()
    runCurrent()

    assertEquals(2, preferences.initializeCalls)
    assertEquals(AppStartupUiState.Ready, viewModel.uiState.value)
  }

  @Test
  fun ignoresRetryWhileInitializationIsRunning() = runTest {
    val preferences = SuspendingPreferencesRepository()
    val viewModel = AppStartupViewModel(bootstrapper(preferences), backgroundScope)

    runCurrent()
    preferences.started.await()
    viewModel.onRetry()
    viewModel.onRetry()
    runCurrent()
    preferences.result.complete(DataOutcome.Success(Unit))
    runCurrent()

    assertEquals(1, preferences.initializeCalls)
    assertEquals(AppStartupUiState.Ready, viewModel.uiState.value)
  }

  private fun bootstrapper(preferences: AppPreferencesRepository) =
      LocalDataBootstrapper(
          localeProvider = TestDeviceLocaleProvider,
          preferencesRepository = preferences,
          categoriaRepository = SuccessfulCategoriaRepository(),
          categorySeedProvider = CategorySeedProvider { "Category" },
          changeClock = TestChangeClock(),
          storedMaxChangeStamp = { DataOutcome.Success(null) },
      )

  private open class FakePreferencesRepository(vararg outcomes: DataOutcome<Unit>) :
      AppPreferencesRepository {
    private val results = outcomes.iterator()
    var initializeCalls = 0
      private set

    override val preferences: Flow<DataOutcome<AppPreferencesState>> = emptyFlow()

    override suspend fun initialize(monedaPrincipal: Moneda): DataOutcome<Unit> {
      recordInitialize()
      return results.next()
    }

    protected fun recordInitialize() {
      initializeCalls += 1
    }

    override suspend fun setThemeMode(themeMode: ThemeMode): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    override suspend fun setAppLocale(appLocale: AppLocale?): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    override suspend fun setMonedaPrincipal(moneda: Moneda): DataOutcome<Unit> =
        DataOutcome.Success(Unit)
  }

  private class SuspendingPreferencesRepository : FakePreferencesRepository() {
    val started = CompletableDeferred<Unit>()
    val result = CompletableDeferred<DataOutcome<Unit>>()

    override suspend fun initialize(monedaPrincipal: Moneda): DataOutcome<Unit> {
      recordInitialize()
      started.complete(Unit)
      return result.await()
    }
  }

  private class SuccessfulCategoriaRepository :
      CategoriaRepository by UnsupportedCategoriaRepository {
    override fun observeAll(): Flow<DataOutcome<List<Categoria>>> = emptyFlow()

    override suspend fun getAll(): DataOutcome<List<Categoria>> = DataOutcome.Success(emptyList())

    override suspend fun localizeSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    override suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
        DataOutcome.Success(Unit)
  }

  private data object TestDeviceLocaleProvider : DeviceLocaleProvider {
    override fun currentLanguageTag(): String = "es-CO"
  }
}
