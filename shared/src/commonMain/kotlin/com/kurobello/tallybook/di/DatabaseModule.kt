package com.kurobello.tallybook.di

import com.kurobello.tallybook.core.database.DatabaseDriverFactory
import com.kurobello.tallybook.core.database.TallybookDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

internal expect val databaseDriverModule: Module

val databaseModule = module {
  single { TallybookDatabase(get<DatabaseDriverFactory>().createDriver()) }
}
