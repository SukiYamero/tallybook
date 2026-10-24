package com.kurobello.tallybook.feature.profile.viewmodel

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.kurobello.tallybook.core.data.AppInfo
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.AppPreferencesRepositoryImpl
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.data.FakeEtiquetaRepository
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.PreferencesDataStoreFactory
import com.kurobello.tallybook.core.data.PreferencesFile
import com.kurobello.tallybook.core.data.PreferencesPathProvider
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.data.export.MovimientoCsvLabels
import com.kurobello.tallybook.core.data.export.MovimientoExportSharer
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.ThemeMode
import com.kurobello.tallybook.di.DispatcherQualifier
import com.kurobello.tallybook.di.viewModelModule
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import org.junit.runner.RunWith
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.viewmodel.defaultExtras
import org.koin.viewmodel.resolveViewModel
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class, KoinInternalApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ProfileViewModelGraphHostTest {
  @Test
  fun `production graph persists currency theme and language`() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)
    val directory = createTempDirectory("tallybook-profile-vm-")
    val factory =
        PreferencesDataStoreFactory(
            object : PreferencesPathProvider {
              override fun path(file: PreferencesFile) = directory.resolve(file.fileName).toString()
            },
            dispatcher,
        )
    val preferences = AppPreferencesRepositoryImpl(factory.create())
    val sharer = RecordingSharer()
    val context = koinApplication {
      modules(
          viewModelModule,
          module {
            single<AppPreferencesRepository> { preferences }
            single<MovimientoRepository> { ProfileFakeMovimientos() }
            single<EtiquetaRepository> { FakeEtiquetaRepository() }
            single<CategoriaRepository> { ProfileFakeCategorias }
            single<MovimientoExportSharer> { sharer }
            single<Clock> {
              object : Clock {
                override fun now() = Instant.parse("2026-10-15T12:00:00Z")
              }
            }
            single<TimeZoneProvider> { TimeZoneProvider { TimeZone.UTC } }
            single<AppInfo> { AppInfo { "1.0" } }
            single<CoroutineDispatcher>(named(DispatcherQualifier.DEFAULT)) { dispatcher }
          },
      )
    }
    val owner =
        object : ViewModelStoreOwner {
          override val viewModelStore = ViewModelStore()
        }
    try {
      assertEquals(DataOutcome.Success(Unit), preferences.initialize(Moneda.COP))
      val vm =
          resolveViewModel(
              vmClass = ProfileViewModel::class,
              viewModelStore = owner.viewModelStore,
              extras = defaultExtras(owner),
              scope = context.koin.scopeRegistry.rootScope,
          )
      runCurrent()
      assertEquals(Moneda.COP, vm.uiState.value.stored?.monedaPrincipal)

      vm.onOpenSheet(ProfileSheetKind.CURRENCY)
      vm.onSelectCurrency(Moneda.ARS)
      vm.onSheetHidden()
      runCurrent()
      vm.onSelectTheme(ThemeMode.DARK)
      runCurrent()
      vm.onOpenSheet(ProfileSheetKind.LANGUAGE)
      vm.onSelectLanguage(AppLocale.PT_BR)
      vm.onSheetHidden()
      runCurrent()

      val expected = AppPreferences(Moneda.ARS, ThemeMode.DARK, AppLocale.PT_BR)
      assertEquals(expected, vm.uiState.value.stored)
      assertEquals(
          DataOutcome.Success(AppPreferencesState.Ready(expected)),
          preferences.preferences.first(),
      )

      vm.onExport(LABELS)
      runCurrent()
      assertEquals(ProfileExport.IDLE, vm.uiState.value.export)
      assertEquals(listOf("tallybook-2026-10-15.csv"), sharer.fileNames)
    } finally {
      owner.viewModelStore.clear()
      runCurrent()
      context.close()
      factory.close()
      directory.toFile().deleteRecursively()
      Dispatchers.resetMain()
    }
  }
}

private val LABELS =
    MovimientoCsvLabels(
        "Date",
        "Type",
        "Category",
        "Parent",
        "Tags",
        "Title",
        "Description",
        "Amount",
        "Currency",
        "Expense",
        "Income",
    )

private class RecordingSharer : MovimientoExportSharer {
  val fileNames = mutableListOf<String>()

  override suspend fun share(fileName: String, csv: String): DataOutcome<Unit> {
    fileNames += fileName
    return DataOutcome.Success(Unit)
  }
}
