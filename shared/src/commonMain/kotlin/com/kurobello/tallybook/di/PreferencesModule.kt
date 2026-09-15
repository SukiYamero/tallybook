package com.kurobello.tallybook.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.AppPreferencesRepositoryImpl
import com.kurobello.tallybook.core.data.PreferencesDataStoreFactory
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal expect val preferencesModule: Module

val preferencesDataModule = module {
  single {
    PreferencesDataStoreFactory(
        pathProvider = get(),
        dispatcher = get<CoroutineDispatcher>(named(DispatcherQualifier.IO)),
    )
  }
  single<DataStore<Preferences>> { get<PreferencesDataStoreFactory>().create() }
  single<AppPreferencesRepository> { AppPreferencesRepositoryImpl(get()) }
}
