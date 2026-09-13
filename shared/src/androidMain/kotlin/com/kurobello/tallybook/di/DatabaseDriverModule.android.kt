package com.kurobello.tallybook.di

import com.kurobello.tallybook.core.database.AndroidDatabaseDriverFactory
import com.kurobello.tallybook.core.database.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val databaseDriverModule: Module = module {
  single<DatabaseDriverFactory> { AndroidDatabaseDriverFactory(androidContext()) }
}
