package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class LocalDataBootstrapperTest {

  @Test
  fun `initialize resolves locale once then initializes preferences and categories`() = runTest {
    val localeProvider = FakeDeviceLocaleProvider("en-US")
    val preferencesRepository = FakePreferencesRepository(DataOutcome.Success(Unit))
    val categoriaRepository = FakeCategoriaRepository(DataOutcome.Success(Unit))
    var resolvedResourceCount = 0
    val bootstrapper =
        LocalDataBootstrapper(
            localeProvider = localeProvider,
            preferencesRepository = preferencesRepository,
            categoriaRepository = categoriaRepository,
            categorySeedProvider =
                CategorySeedProvider { resource ->
                  resolvedResourceCount += 1
                  resource.key
                },
        )

    assertEquals(DataOutcome.Success(Unit), bootstrapper.initialize())
    assertEquals(1, localeProvider.calls)
    assertEquals(listOf(Moneda.USD), preferencesRepository.initializedWith)
    assertEquals(1, categoriaRepository.seedCalls)
    assertEquals(59, categoriaRepository.seededCategorias.single().size)
    assertEquals(59, resolvedResourceCount)
  }

  @Test
  fun `initialize stops before category seeding when preferences initialization fails`() = runTest {
    val failure = DataOutcome.Failure(DataError.Local(IllegalStateException("preferences failed")))
    val categoriaRepository = FakeCategoriaRepository(DataOutcome.Success(Unit))
    val bootstrapper =
        LocalDataBootstrapper(
            localeProvider = FakeDeviceLocaleProvider("es-CO"),
            preferencesRepository = FakePreferencesRepository(failure),
            categoriaRepository = categoriaRepository,
            categorySeedProvider = CategorySeedProvider { resource -> resource.key },
        )

    assertEquals(failure, bootstrapper.initialize())
    assertEquals(0, categoriaRepository.seedCalls)
  }

  @Test
  fun `initialize returns a category seed failure`() = runTest {
    val failure = DataOutcome.Failure(DataError.Local(IllegalStateException("categories failed")))
    val categoriaRepository = FakeCategoriaRepository(failure)
    val bootstrapper =
        LocalDataBootstrapper(
            localeProvider = FakeDeviceLocaleProvider("es-CO"),
            preferencesRepository = FakePreferencesRepository(DataOutcome.Success(Unit)),
            categoriaRepository = categoriaRepository,
            categorySeedProvider = CategorySeedProvider { resource -> resource.key },
        )

    assertEquals(failure, bootstrapper.initialize())
    assertEquals(1, categoriaRepository.seedCalls)
  }

  private class FakeDeviceLocaleProvider(
      private val languageTag: String,
  ) : DeviceLocaleProvider {
    var calls = 0

    override fun currentLanguageTag(): String {
      calls += 1
      return languageTag
    }
  }

  private class FakePreferencesRepository(
      private val initializeResult: DataOutcome<Unit>,
  ) : AppPreferencesRepository {
    override val preferences: Flow<DataOutcome<AppPreferencesState>> = emptyFlow()
    val initializedWith = mutableListOf<Moneda>()

    override suspend fun initialize(monedaPrincipal: Moneda): DataOutcome<Unit> {
      initializedWith += monedaPrincipal
      return initializeResult
    }

    override suspend fun setThemeMode(themeMode: ThemeMode): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    override suspend fun setAppLocale(appLocale: AppLocale?): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    override suspend fun setMonedaPrincipal(moneda: Moneda): DataOutcome<Unit> =
        DataOutcome.Success(Unit)
  }

  private class FakeCategoriaRepository(
      private val seedResult: DataOutcome<Unit>,
  ) : CategoriaRepository by UnsupportedCategoriaRepository {
    var seedCalls = 0
    val seededCategorias = mutableListOf<List<Categoria>>()

    override fun observeAll(): Flow<DataOutcome<List<Categoria>>> =
        flowOf(DataOutcome.Success(emptyList()))

    override suspend fun getAll(): DataOutcome<List<Categoria>> = DataOutcome.Success(emptyList())

    override suspend fun localizeSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
        DataOutcome.Success(Unit)

    override suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit> {
      seedCalls += 1
      seededCategorias += categorias
      return seedResult
    }
  }
}
