package com.kurobello.tallybook

import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategorySeedProvider
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AppDisplayViewModelTest {
  @Test
  fun settingsStayUnresolvedUntilReadAndFallBackToDeviceDefaults() = runTest {
    val preferences = DisplayPreferences()
    val vm = AppDisplayViewModel(preferences, DisplayCategorias(), seedProvider(), backgroundScope)
    runCurrent()
    assertNull(vm.settings.value)

    preferences.emit(DataOutcome.Success(AppPreferencesState.Uninitialized))
    runCurrent()
    assertEquals(AppDisplaySettings(), vm.settings.value)

    preferences.emit(
        DataOutcome.Success(
            AppPreferencesState.Ready(AppPreferences(Moneda.COP, ThemeMode.DARK, AppLocale.EN))
        )
    )
    runCurrent()
    assertEquals(AppDisplaySettings(ThemeMode.DARK, AppLocale.EN), vm.settings.value)

    preferences.emit(DataOutcome.Failure(DataError.Local(IllegalStateException())))
    runCurrent()
    assertEquals(AppDisplaySettings(), vm.settings.value)
  }

  @Test
  fun appliedLocaleRelocalizesSeededCategoryNames() = runTest {
    val categorias = DisplayCategorias()
    val vm = AppDisplayViewModel(DisplayPreferences(), categorias, seedProvider(), backgroundScope)
    vm.onLocaleApplied()
    runCurrent()
    val localized = categorias.localized.single()
    assertEquals(59, localized.size)
    assertEquals("category_comida", localized.first { it.id == "cat_comida" }.nombre)
  }
}

private fun seedProvider() = CategorySeedProvider { it.key.removePrefix("string:") }

private class DisplayPreferences : AppPreferencesRepository {
  private val values = MutableSharedFlow<DataOutcome<AppPreferencesState>>()

  override val preferences: Flow<DataOutcome<AppPreferencesState>> = values

  suspend fun emit(value: DataOutcome<AppPreferencesState>) = values.emit(value)

  override suspend fun initialize(monedaPrincipal: Moneda) = unused()

  override suspend fun setMonedaPrincipal(moneda: Moneda) = unused()

  override suspend fun setThemeMode(themeMode: ThemeMode) = unused()

  override suspend fun setAppLocale(appLocale: AppLocale?) = unused()
}

private class DisplayCategorias : CategoriaRepository {
  val localized = mutableListOf<List<Categoria>>()

  override suspend fun localizeSeeded(categorias: List<Categoria>): DataOutcome<Unit> {
    localized += categorias
    return DataOutcome.Success(Unit)
  }

  override fun observeAll() = unused()

  override suspend fun getAll() = unused()

  override suspend fun ensureSeeded(categorias: List<Categoria>) = unused()
}

private fun unused(): Nothing = throw UnsupportedOperationException("Not used by display settings")
