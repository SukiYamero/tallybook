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

  private val dataStore: DataStore<Preferences> by lazy {
    PreferenceDataStoreFactory.createWithPath(
        scope = scope,
        produceFile = { pathProvider.path().toPath() },
    )
  }

  fun create(): DataStore<Preferences> = dataStore

  fun close() {
    scope.cancel()
  }
}
