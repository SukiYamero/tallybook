package com.kurobello.tallybook.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.AppInfo
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.data.export.MovimientoCsvLabels
import com.kurobello.tallybook.core.data.export.MovimientoExportSharer
import com.kurobello.tallybook.core.data.export.buildMovimientoCsv
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.ThemeMode
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.toLocalDateTime

internal interface ProfileActions {
  fun onOpenSheet(kind: ProfileSheetKind): Boolean

  fun onDismissSheet()

  fun onSheetHidden()

  fun onSelectCurrency(moneda: Moneda)

  fun onSelectLanguage(appLocale: AppLocale?)

  fun onSelectTheme(themeMode: ThemeMode)

  fun onExport(labels: MovimientoCsvLabels)

  fun onRetry()
}

internal class ProfileExportDependencies(
    val movimientos: MovimientoRepository,
    val categorias: CategoriaRepository,
    val sharer: MovimientoExportSharer,
    val clock: Clock,
    val timeZoneProvider: TimeZoneProvider,
)

@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
internal class ProfileViewModel(
    private val preferencesRepository: AppPreferencesRepository,
    private val export: ProfileExportDependencies,
    appInfo: AppInfo,
    private val computationDispatcher: CoroutineDispatcher,
    scope: CoroutineScope? = null,
) : ViewModel(), ProfileActions {

  private val coroutineScope = scope ?: viewModelScope
  private val retryRequests = MutableStateFlow(0L)

  val uiState: StateFlow<ProfileUiState>
    field = MutableStateFlow(ProfileUiState(appVersion = appInfo.versionName()))

  init {
    coroutineScope.launch {
      retryRequests
          .flatMapLatest {
            preferencesRepository.preferences
                .map { it.toProfilePreferences() }
                .onStart { emit(ProfilePreferences.Loading) }
          }
          .collect { preferences ->
            uiState.update {
              it.copy(
                  preferences = preferences,
                  sheet =
                      if (preferences is ProfilePreferences.Ready) it.sheet
                      else it.sheet?.copy(isClosing = true),
              )
            }
          }
    }
  }

  override fun onOpenSheet(kind: ProfileSheetKind): Boolean {
    val current = uiState.value
    val stored = current.stored
    if (!current.canEdit || current.sheet != null || stored == null) return false
    uiState.update { it.copy(sheet = ProfileSheet(kind, stored)) }
    return true
  }

  override fun onDismissSheet() {
    uiState.update { it.copy(sheet = it.sheet?.copy(isClosing = true)) }
  }

  override fun onSheetHidden() {
    uiState.update { if (it.sheet?.isClosing == true) it.copy(sheet = null) else it }
  }

  override fun onSelectCurrency(moneda: Moneda) {
    val stored = selectFromSheet(ProfileSheetKind.CURRENCY) ?: return
    if (moneda == stored.monedaPrincipal) return
    write(ProfileSetting.CURRENCY) { preferencesRepository.setMonedaPrincipal(moneda) }
  }

  override fun onSelectLanguage(appLocale: AppLocale?) {
    val stored = selectFromSheet(ProfileSheetKind.LANGUAGE) ?: return
    if (appLocale == stored.appLocale) return
    write(ProfileSetting.LANGUAGE) { preferencesRepository.setAppLocale(appLocale) }
  }

  override fun onSelectTheme(themeMode: ThemeMode) {
    val current = uiState.value
    val stored = current.stored
    if (!current.canEdit || current.sheet != null || stored == null) return
    if (themeMode == stored.themeMode) return
    write(ProfileSetting.THEME) { preferencesRepository.setThemeMode(themeMode) }
  }

  override fun onExport(labels: MovimientoCsvLabels) {
    if (uiState.value.export == ProfileExport.EXPORTING) return
    uiState.update { it.copy(export = ProfileExport.EXPORTING) }
    coroutineScope.launch {
      val shared = shareCsv(labels)
      uiState.update {
        it.copy(
            export = if (shared is DataOutcome.Success) ProfileExport.IDLE else ProfileExport.FAILED
        )
      }
    }
  }

  override fun onRetry() {
    retryRequests.update { it + 1 }
  }

  /** Closes the open sheet of [kind] and returns the stored values the selection compares with. */
  private fun selectFromSheet(kind: ProfileSheetKind): AppPreferences? {
    val current = uiState.value
    val sheet = current.sheet?.takeIf { it.kind == kind && !it.isClosing }
    if (sheet == null || !current.canEdit) return null
    uiState.update { it.copy(sheet = sheet.copy(isClosing = true), failedWrite = null) }
    return current.stored
  }

  private fun write(setting: ProfileSetting, operation: suspend () -> DataOutcome<Unit>) {
    uiState.update { it.copy(pendingWrite = setting, failedWrite = null) }
    coroutineScope.launch {
      val failed = operation() is DataOutcome.Failure
      uiState.update { it.copy(pendingWrite = null, failedWrite = if (failed) setting else null) }
    }
  }

  private suspend fun shareCsv(labels: MovimientoCsvLabels): DataOutcome<Unit> {
    val movimientos = export.movimientos.getAll()
    val categorias = export.categorias.getAll()
    return when (movimientos) {
      is DataOutcome.Failure -> movimientos
      is DataOutcome.Success ->
          when (categorias) {
            is DataOutcome.Failure -> categorias
            is DataOutcome.Success -> shareCsv(movimientos.data, categorias.data, labels)
          }
    }
  }

  private suspend fun shareCsv(
      movimientos: List<Movimiento>,
      categorias: List<Categoria>,
      labels: MovimientoCsvLabels,
  ): DataOutcome<Unit> {
    val csv =
        withContext(computationDispatcher) { buildMovimientoCsv(movimientos, categorias, labels) }
    val today = export.clock.now().toLocalDateTime(export.timeZoneProvider.current()).date
    return export.sharer.share("tallybook-$today.csv", csv)
  }
}

private fun DataOutcome<AppPreferencesState>.toProfilePreferences(): ProfilePreferences =
    when (this) {
      is DataOutcome.Success ->
          when (val state = data) {
            is AppPreferencesState.Ready -> ProfilePreferences.Ready(state.value)
            AppPreferencesState.Uninitialized -> ProfilePreferences.Unavailable
          }
      is DataOutcome.Failure -> ProfilePreferences.Unavailable
    }
