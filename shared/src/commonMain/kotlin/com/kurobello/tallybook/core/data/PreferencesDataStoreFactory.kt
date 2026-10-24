package com.kurobello.tallybook.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import okio.Path.Companion.toPath

class PreferencesDataStoreFactory(
    private val pathProvider: PreferencesPathProvider,
    dispatcher: CoroutineDispatcher,
) {
  private val scope = CoroutineScope(SupervisorJob() + dispatcher)

  // DataStore allows only one active instance per file in a process.
  private val dataStores: Map<PreferencesFile, Lazy<DataStore<Preferences>>> =
      PreferencesFile.entries.associateWith { file ->
        lazy {
          PreferenceDataStoreFactory.createWithPath(
              scope = scope,
              produceFile = { pathProvider.path(file).toPath() },
          )
        }
      }

  fun create(file: PreferencesFile = PreferencesFile.APP): DataStore<Preferences> =
      dataStores.getValue(file).value

  fun close() {
    scope.cancel()
  }
}
