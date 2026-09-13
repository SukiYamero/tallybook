package com.kurobello.tallybook.di

import com.kurobello.tallybook.core.database.DatabaseDriverFactory
import com.kurobello.tallybook.core.database.IosDatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val databaseDriverModule: Module = module {
  single<DatabaseDriverFactory> { IosDatabaseDriverFactory() }
}
