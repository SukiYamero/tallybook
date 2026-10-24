package com.kurobello.tallybook.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.ChangeClock
import com.kurobello.tallybook.core.data.InstallationIdRepository
import com.kurobello.tallybook.core.data.PreferencesDataStoreFactory
import com.kurobello.tallybook.core.data.PreferencesFile
import com.kurobello.tallybook.core.data.PreferencesPathProvider
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import org.koin.core.qualifier.named
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

  @Test
  fun `device DataStore is a separate single instance behind the node repository and clock`() {
    val directory = createTempDirectory("tallybook-preferences-")
    val application = koinApplication {
      allowOverride(true)
      modules(
          dispatcherModule,
          preferencesModule,
          timeModule,
          module {
            single<PreferencesPathProvider> { TestPreferencesPathProvider(directory.toString()) }
          },
      )
    }

    try {
      val koin = application.koin
      val factory = koin.get<PreferencesDataStoreFactory>()
      val appDataStore = koin.get<DataStore<Preferences>>()
      val deviceDataStore = koin.get<DataStore<Preferences>>(named(PreferencesFile.DEVICE))

      assertNotSame(appDataStore, deviceDataStore)
      assertSame(factory.create(PreferencesFile.APP), appDataStore)
      assertSame(factory.create(PreferencesFile.DEVICE), deviceDataStore)
      assertSame(deviceDataStore, koin.get<DataStore<Preferences>>(named(PreferencesFile.DEVICE)))
      assertSame(koin.get<InstallationIdRepository>(), koin.get<InstallationIdRepository>())
      assertSame(koin.get<ChangeClock>(), koin.get<ChangeClock>())
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
  override fun path(file: PreferencesFile): String = "$directory/${file.fileName}"
}
