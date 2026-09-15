package com.kurobello.tallybook.di

import com.kurobello.tallybook.AppStartupViewModel
import com.kurobello.tallybook.feature.home.HomeViewModel
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoRepositories
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val viewModelModule = module {
  viewModel { AppStartupViewModel(bootstrapper = get()) }
  viewModel {
    HomeViewModel(
        preferencesRepository = get(),
        movimientoRepository = get(),
        categoriaRepository = get(),
    )
  }
  viewModel {
    AddMovimientoViewModel(
        repositories = AddMovimientoRepositories(preferences = get(), categorias = get()),
        movimientoRepository = get(),
        idGenerator = get(),
        clock = get(),
        timeZoneProvider = get(),
    )
  }
}
