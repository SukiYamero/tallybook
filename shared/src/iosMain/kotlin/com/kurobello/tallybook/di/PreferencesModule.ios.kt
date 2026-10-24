package com.kurobello.tallybook.di

import com.kurobello.tallybook.core.data.BackupExclusion
import com.kurobello.tallybook.core.data.IosBackupExclusion
import com.kurobello.tallybook.core.data.IosPreferencesPathProvider
import com.kurobello.tallybook.core.data.PreferencesPathProvider
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val preferencesModule: Module = module {
  includes(preferencesDataModule)
  single<PreferencesPathProvider> { IosPreferencesPathProvider() }
  single<BackupExclusion> { IosBackupExclusion(get()) }
}
