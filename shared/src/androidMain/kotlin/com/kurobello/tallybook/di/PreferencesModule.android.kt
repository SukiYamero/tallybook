package com.kurobello.tallybook.di

import com.kurobello.tallybook.core.data.AndroidPreferencesPathProvider
import com.kurobello.tallybook.core.data.BackupExclusion
import com.kurobello.tallybook.core.data.PreferencesPathProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val preferencesModule: Module = module {
  includes(preferencesDataModule)
  single<PreferencesPathProvider> { AndroidPreferencesPathProvider(androidContext()) }
  // Android excludes the device file declaratively through androidApp's data_extraction_rules.xml.
  single<BackupExclusion> { BackupExclusion {} }
}
