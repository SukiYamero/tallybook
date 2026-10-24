package com.kurobello.tallybook.di

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.AppStartupViewModel
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategorySeedProvider
import com.kurobello.tallybook.core.data.DeviceLocaleProvider
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.data.LocalDataBootstrapper
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.PreferencesDataStoreFactory
import com.kurobello.tallybook.core.data.PreferencesFile
import com.kurobello.tallybook.core.data.PreferencesPathProvider
import com.kurobello.tallybook.core.data.entry.CategorySuggester
import com.kurobello.tallybook.core.data.entry.MovimientoPhraseParser
import com.kurobello.tallybook.core.database.DatabaseDriverFactory
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.feature.home.HomeViewModel
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoViewModel
import com.kurobello.tallybook.feature.movimientos.viewmodel.ManageMovimientoViewModel
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoEntryEngine
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class RepositoryModulesTest {

  @Test
  fun `app modules resolve repositories entry engine and form view models`() {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    val directory = createTempDirectory("tallybook-repositories-")
    TallybookDatabase.Schema.create(driver)
    val testDriverModule = module {
      single<DatabaseDriverFactory> { DatabaseDriverFactory { driver } }
      single<PreferencesPathProvider> {
        RepositoryTestPreferencesPathProvider(directory.toString())
      }
      single<DeviceLocaleProvider> { RepositoryTestDeviceLocaleProvider() }
      single { CategorySeedProvider { "Category" } }
    }
    val application = koinApplication {
      allowOverride(true)
      modules(appModules + testDriverModule)
    }

    try {
      assertNotNull(application.koin.get<MovimientoRepository>())
      assertNotNull(application.koin.get<CategoriaRepository>())
      assertNotNull(application.koin.get<EtiquetaRepository>())
      assertNotNull(application.koin.get<CategorySeedProvider>())
      assertNotNull(application.koin.get<LocalDataBootstrapper>())
      assertNotNull(application.koin.get<AppStartupViewModel>())
      assertNotNull(application.koin.get<HomeViewModel>())
      assertNotNull(application.koin.get<MovimientoPhraseParser>())
      assertNotNull(application.koin.get<CategorySuggester>())
      assertNotNull(application.koin.get<MovimientoEntryEngine>())
      assertNotNull(application.koin.get<AddMovimientoViewModel>())
      assertNotNull(application.koin.get<ManageMovimientoViewModel>())
    } finally {
      application.koin.get<PreferencesDataStoreFactory>().close()
      application.close()
      driver.close()
      directory.toFile().deleteRecursively()
    }
  }
}

private class RepositoryTestPreferencesPathProvider(
    private val directory: String,
) : PreferencesPathProvider {
  override fun path(file: PreferencesFile): String = "$directory/${file.fileName}"
}

private class RepositoryTestDeviceLocaleProvider : DeviceLocaleProvider {
  override fun currentLanguageTag(): String = "es-CO"
}
