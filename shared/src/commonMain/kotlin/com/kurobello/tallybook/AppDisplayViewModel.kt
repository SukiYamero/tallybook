package com.kurobello.tallybook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategorySeedProvider
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Display preferences resolved once at the root, before any screen composes. */
data class AppDisplaySettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLocale: AppLocale? = null,
)

class AppDisplayViewModel(
    preferencesRepository: AppPreferencesRepository,
    private val categoriaRepository: CategoriaRepository,
    private val categorySeedProvider: CategorySeedProvider,
    scope: CoroutineScope? = null,
) : ViewModel() {

  private val coroutineScope = scope ?: viewModelScope
  private var localization: Job? = null

  /**
   * `null` until the stored preferences are read, so the first frame never shows the wrong theme.
   */
  val settings: StateFlow<AppDisplaySettings?> =
      preferencesRepository.preferences
          .map { outcome ->
            val state = (outcome as? DataOutcome.Success)?.data
            val preferences = (state as? AppPreferencesState.Ready)?.value
            AppDisplaySettings(
                themeMode = preferences?.themeMode ?: ThemeMode.SYSTEM,
                appLocale = preferences?.appLocale,
            )
          }
          .stateIn(coroutineScope, SharingStarted.Eagerly, null)

  /** Call after the resolved locale is the process default, so seed names resolve in it. */
  fun onLocaleApplied() {
    localization?.cancel()
    localization = coroutineScope.launch {
      categoriaRepository.localizeSeeded(categorySeedProvider.getCategories())
    }
  }
}
