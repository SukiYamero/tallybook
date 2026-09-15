package com.kurobello.tallybook.di

val appModules =
    listOf(
        dispatcherModule,
        timeModule,
        localeModule,
        preferencesModule,
        databaseDriverModule,
        databaseModule,
        repositoryModule,
        viewModelModule,
    )
