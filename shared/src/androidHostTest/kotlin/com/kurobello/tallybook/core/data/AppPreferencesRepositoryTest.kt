package com.kurobello.tallybook.core.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.ThemeMode
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class AppPreferencesRepositoryTest {

  @Test
  fun `preferences emits uninitialized before initialization`() = runTest {
    val directory = createTempDirectory("tallybook-preferences-")
    val factory =
        PreferencesDataStoreFactory(
            pathProvider = TestPreferencesPathProvider(directory.toString()),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
    val repository = AppPreferencesRepositoryImpl(factory.create())

    try {
      assertEquals(
          DataOutcome.Success(AppPreferencesState.Uninitialized),
          repository.preferences.first(),
      )
    } finally {
      factory.close()
      directory.toFile().deleteRecursively()
    }
  }

  @Test
  fun `initialize preserves the first moneda principal`() = runTest {
    val directory = createTempDirectory("tallybook-preferences-")
    val factory =
        PreferencesDataStoreFactory(
            pathProvider = TestPreferencesPathProvider(directory.toString()),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
    val repository = AppPreferencesRepositoryImpl(factory.create())

    try {
      assertEquals(DataOutcome.Success(Unit), repository.initialize(Moneda.COP))
      assertEquals(DataOutcome.Success(Unit), repository.initialize(Moneda.USD))
      assertEquals(
          DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(Moneda.COP))),
          repository.preferences.first(),
      )
    } finally {
      factory.close()
      directory.toFile().deleteRecursively()
    }
  }

  @Test
  fun `set moneda principal persists across a new DataStore instance`() = runTest {
    val directory = createTempDirectory("tallybook-preferences-")
    val pathProvider = TestPreferencesPathProvider(directory.toString())
    val dispatcher = StandardTestDispatcher(testScheduler)
    val firstFactory = PreferencesDataStoreFactory(pathProvider, dispatcher)
    val firstRepository = AppPreferencesRepositoryImpl(firstFactory.create())

    try {
      assertEquals(DataOutcome.Success(Unit), firstRepository.setMonedaPrincipal(Moneda.USD))
      firstFactory.close()

      val secondFactory = PreferencesDataStoreFactory(pathProvider, dispatcher)
      try {
        val secondRepository = AppPreferencesRepositoryImpl(secondFactory.create())
        assertEquals(
            DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(Moneda.USD))),
            secondRepository.preferences.first(),
        )
      } finally {
        secondFactory.close()
      }
    } finally {
      firstFactory.close()
      directory.toFile().deleteRecursively()
    }
  }

  @Test
  fun `preferences maps a real storage read failure to a local error`() = runTest {
    val directory = createTempDirectory("tallybook-preferences-")
    Files.createDirectory(directory.resolve(PreferencesFile.APP.fileName))
    val factory =
        PreferencesDataStoreFactory(
            pathProvider = TestPreferencesPathProvider(directory.toString()),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
    val repository = AppPreferencesRepositoryImpl(factory.create())

    try {
      val failure = assertIs<DataOutcome.Failure>(repository.preferences.first())
      assertIs<DataError.Local>(failure.error)
    } finally {
      factory.close()
      directory.toFile().deleteRecursively()
    }
  }

  @Test
  fun `preferences maps an unknown stored moneda to a local error`() = runTest {
    val directory = createTempDirectory("tallybook-preferences-")
    val factory =
        PreferencesDataStoreFactory(
            pathProvider = TestPreferencesPathProvider(directory.toString()),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
    val dataStore = factory.create()
    val repository = AppPreferencesRepositoryImpl(dataStore)

    try {
      dataStore.edit { preferences ->
        preferences[stringPreferencesKey("moneda_principal")] = "UNKNOWN"
      }

      val failure = assertIs<DataOutcome.Failure>(repository.preferences.first())
      assertIs<DataError.Local>(failure.error)
    } finally {
      factory.close()
      directory.toFile().deleteRecursively()
    }
  }

  @Test
  fun `theme and app locale round trip and a null locale follows the device again`() = runTest {
    val directory = createTempDirectory("tallybook-preferences-")
    val factory =
        PreferencesDataStoreFactory(
            pathProvider = TestPreferencesPathProvider(directory.toString()),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
    val repository = AppPreferencesRepositoryImpl(factory.create())

    try {
      repository.initialize(Moneda.COP)
      assertEquals(DataOutcome.Success(Unit), repository.setThemeMode(ThemeMode.DARK))
      assertEquals(DataOutcome.Success(Unit), repository.setAppLocale(AppLocale.ES_AR))
      assertEquals(
          DataOutcome.Success(
              AppPreferencesState.Ready(AppPreferences(Moneda.COP, ThemeMode.DARK, AppLocale.ES_AR))
          ),
          repository.preferences.first(),
      )
      repository.setAppLocale(null)
      assertEquals(
          DataOutcome.Success(
              AppPreferencesState.Ready(AppPreferences(Moneda.COP, ThemeMode.DARK, null))
          ),
          repository.preferences.first(),
      )
    } finally {
      factory.close()
      directory.toFile().deleteRecursively()
    }
  }

  @Test
  fun `unknown display values fall back without hiding the currency`() = runTest {
    val directory = createTempDirectory("tallybook-preferences-")
    val factory =
        PreferencesDataStoreFactory(
            pathProvider = TestPreferencesPathProvider(directory.toString()),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
    val dataStore = factory.create()
    val repository = AppPreferencesRepositoryImpl(dataStore)

    try {
      repository.initialize(Moneda.COP)
      dataStore.edit {
        it[stringPreferencesKey("theme_mode")] = "SEPIA"
        it[stringPreferencesKey("app_locale")] = "fr-FR"
      }
      assertEquals(
          DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(Moneda.COP))),
          repository.preferences.first(),
      )
    } finally {
      factory.close()
      directory.toFile().deleteRecursively()
    }
  }
}

private class TestPreferencesPathProvider(
    private val directory: String,
) : PreferencesPathProvider {
  override fun path(file: PreferencesFile): String = "$directory/${file.fileName}"
}
