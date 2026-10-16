package com.kurobello.tallybook.di

import com.kurobello.tallybook.AppDisplayViewModel
import com.kurobello.tallybook.AppStartupViewModel
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesViewModel
import com.kurobello.tallybook.feature.categories.viewmodel.QuickCategoryViewModel
import com.kurobello.tallybook.feature.history.viewmodel.HistoryDateProvider
import com.kurobello.tallybook.feature.history.viewmodel.HistoryRepositories
import com.kurobello.tallybook.feature.history.viewmodel.HistoryViewModel
import com.kurobello.tallybook.feature.home.HomeViewModel
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoRepositories
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoViewModel
import com.kurobello.tallybook.feature.movimientos.viewmodel.ManageMovimientoViewModel
import com.kurobello.tallybook.feature.profile.viewmodel.ProfileExportDependencies
import com.kurobello.tallybook.feature.profile.viewmodel.ProfileViewModel
import com.kurobello.tallybook.feature.search.viewmodel.SearchDateProvider
import com.kurobello.tallybook.feature.search.viewmodel.SearchRepositories
import com.kurobello.tallybook.feature.search.viewmodel.SearchViewModel
import com.kurobello.tallybook.feature.tags.viewmodel.TagsViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val viewModelModule = module {
  viewModel { AppStartupViewModel(bootstrapper = get()) }
  viewModel {
    AppDisplayViewModel(
        preferencesRepository = get(),
        categoriaRepository = get(),
        categorySeedProvider = get(),
    )
  }
  viewModel {
    HomeViewModel(
        preferencesRepository = get(),
        movimientoRepository = get(),
        categoriaRepository = get(),
    )
  }
  viewModel { parameters ->
    HistoryViewModel(
        repositories = HistoryRepositories(get(), get(), get()),
        savedStateHandle = get(),
        unknownCategoryLabel = parameters.get(),
        dateProvider = HistoryDateProvider(get(), get()),
        computationDispatcher = get<CoroutineDispatcher>(named(DispatcherQualifier.DEFAULT)),
    )
  }
  viewModel {
    AddMovimientoViewModel(
        repositories =
            AddMovimientoRepositories(
                preferences = get(),
                categorias = get(),
                movimientos = get(),
                etiquetas = get(),
            ),
        idGenerator = get(),
        clock = get(),
        timeZoneProvider = get(),
        entryEngine = get(),
    )
  }
  viewModel {
    ManageMovimientoViewModel(
        movimientoRepository = get(),
        categoriaRepository = get(),
        etiquetaRepository = get(),
        clock = get(),
        timeZoneProvider = get(),
    )
  }
  viewModel { QuickCategoryViewModel(categoriaRepository = get(), suggester = get()) }
  viewModel { parameters ->
    SearchViewModel(
        repositories = SearchRepositories(get(), get(), get(), get()),
        savedStateHandle = get(),
        unknownCategoryLabel = parameters.get(),
        dateProvider = SearchDateProvider(get(), get()),
        computationDispatcher = get<CoroutineDispatcher>(named(DispatcherQualifier.DEFAULT)),
    )
  }
  viewModel {
    ProfileViewModel(
        preferencesRepository = get(),
        export = ProfileExportDependencies(get(), get(), get(), get(), get(), get()),
        appInfo = get(),
        computationDispatcher = get<CoroutineDispatcher>(named(DispatcherQualifier.DEFAULT)),
    )
  }
  viewModel { CategoriesViewModel(categorias = get(), preferences = get(), suggester = get()) }
  viewModel { TagsViewModel(etiquetas = get(), preferences = get()) }
}
