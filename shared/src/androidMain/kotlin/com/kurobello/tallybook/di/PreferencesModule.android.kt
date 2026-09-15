package com.kurobello.tallybook.di

import com.kurobello.tallybook.core.data.AndroidPreferencesPathProvider
import com.kurobello.tallybook.core.data.PreferencesPathProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val preferencesModule: Module = module {
  includes(preferencesDataModule)
  single<PreferencesPathProvider> { AndroidPreferencesPathProvider(androidContext()) }
}
