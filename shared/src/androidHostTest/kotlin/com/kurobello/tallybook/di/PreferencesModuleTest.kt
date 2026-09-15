package com.kurobello.tallybook.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.PREFERENCES_FILE_NAME
import com.kurobello.tallybook.core.data.PreferencesDataStoreFactory
import com.kurobello.tallybook.core.data.PreferencesPathProvider
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertSame
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class PreferencesModuleTest {

  @Test
  fun `preferences module overrides its path provider and resolves single instances`() {
    val directory = createTempDirectory("tallybook-preferences-")
    val application = koinApplication {
      allowOverride(true)
      modules(
          dispatcherModule,
          preferencesModule,
          module {
            single<PreferencesPathProvider> { TestPreferencesPathProvider(directory.toString()) }
          },
      )
    }

    try {
      val firstRepository = application.koin.get<AppPreferencesRepository>()
      val secondRepository = application.koin.get<AppPreferencesRepository>()
      val firstDataStore = application.koin.get<DataStore<Preferences>>()
      val secondDataStore = application.koin.get<DataStore<Preferences>>()

      assertSame(firstRepository, secondRepository)
      assertSame(firstDataStore, secondDataStore)
    } finally {
      application.koin.get<PreferencesDataStoreFactory>().close()
      application.close()
      directory.toFile().deleteRecursively()
    }
  }
}

private class TestPreferencesPathProvider(
    private val directory: String,
) : PreferencesPathProvider {
  override fun path(): String = "$directory/$PREFERENCES_FILE_NAME"
}
