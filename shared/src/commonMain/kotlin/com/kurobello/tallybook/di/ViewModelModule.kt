package com.kurobello.tallybook.di

import com.kurobello.tallybook.feature.home.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val viewModelModule = module { viewModelOf(::HomeViewModel) }
