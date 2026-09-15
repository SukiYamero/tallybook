package com.kurobello.tallybook.di

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategorySeedProvider
import com.kurobello.tallybook.core.data.LocalDataBootstrapper
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.PREFERENCES_FILE_NAME
import com.kurobello.tallybook.core.data.PreferencesDataStoreFactory
import com.kurobello.tallybook.core.data.PreferencesPathProvider
import com.kurobello.tallybook.core.database.DatabaseDriverFactory
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.feature.home.HomeViewModel
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoViewModel
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class RepositoryModulesTest {

  @Test
  fun `app modules resolve repositories and form view models`() {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    val directory = createTempDirectory("tallybook-repositories-")
    TallybookDatabase.Schema.create(driver)
    val testDriverModule = module {
      single<DatabaseDriverFactory> { DatabaseDriverFactory { driver } }
      single<PreferencesPathProvider> {
        RepositoryTestPreferencesPathProvider(directory.toString())
      }
    }
    val application = koinApplication {
      allowOverride(true)
      modules(appModules + testDriverModule)
    }

    try {
      assertNotNull(application.koin.get<MovimientoRepository>())
      assertNotNull(application.koin.get<CategoriaRepository>())
      assertNotNull(application.koin.get<CategorySeedProvider>())
      assertNotNull(application.koin.get<LocalDataBootstrapper>())
      assertNotNull(application.koin.get<HomeViewModel>())
      assertNotNull(application.koin.get<AddMovimientoViewModel>())
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
  override fun path(): String = "$directory/$PREFERENCES_FILE_NAME"
}
