package com.kurobello.tallybook.di

import com.kurobello.tallybook.AppDisplayViewModel
import com.kurobello.tallybook.AppStartupViewModel
import com.kurobello.tallybook.core.data.CivilToday
import com.kurobello.tallybook.core.ui.feedback.ManagementFeedbackViewModel
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountsViewModel
import com.kurobello.tallybook.feature.accounts.viewmodel.QuickCuentaViewModel
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesViewModel
import com.kurobello.tallybook.feature.categories.viewmodel.QuickCategoryViewModel
import com.kurobello.tallybook.feature.history.viewmodel.HistoryDateProvider
import com.kurobello.tallybook.feature.history.viewmodel.HistoryRepositories
import com.kurobello.tallybook.feature.history.viewmodel.HistoryViewModel
import com.kurobello.tallybook.feature.home.HomeRepositories
import com.kurobello.tallybook.feature.home.HomeViewModel
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoRepositories
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoViewModel
import com.kurobello.tallybook.feature.movimientos.viewmodel.ManageMovimientoRepositories
import com.kurobello.tallybook.feature.movimientos.viewmodel.ManageMovimientoViewModel
import com.kurobello.tallybook.feature.profile.viewmodel.ProfileExportDependencies
import com.kurobello.tallybook.feature.profile.viewmodel.ProfileViewModel
import com.kurobello.tallybook.feature.recurrentes.viewmodel.RecurrentesRepositories
import com.kurobello.tallybook.feature.recurrentes.viewmodel.RecurrentesViewModel
import com.kurobello.tallybook.feature.search.viewmodel.SearchDateProvider
import com.kurobello.tallybook.feature.search.viewmodel.SearchRepositories
import com.kurobello.tallybook.feature.search.viewmodel.SearchViewModel
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsRepositories
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsViewModel
import com.kurobello.tallybook.feature.tags.viewmodel.TagsViewModel
import com.kurobello.tallybook.feature.transfers.viewmodel.ManageTransferenciaRepositories
import com.kurobello.tallybook.feature.transfers.viewmodel.ManageTransferenciaViewModel
import com.kurobello.tallybook.feature.transfers.viewmodel.RecentTransferenciasViewModel
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
        repositories = HomeRepositories(get(), get(), get(), get(), get(), get()),
        today = CivilToday(get(), get()),
    )
  }
  viewModel { parameters ->
    HistoryViewModel(
        repositories = HistoryRepositories(get(), get(), get(), get()),
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
                cuentas = get(),
                recurrencias = get(),
            ),
        idGenerator = get(),
        clock = get(),
        timeZoneProvider = get(),
        entryEngine = get(),
    )
  }
  viewModel {
    ManagementFeedbackViewModel(
        movimientoRepository = get(),
        transferenciaRepository = get(),
        recurrenciaRepository = get(),
    )
  }
  viewModel { parameters ->
    ManageMovimientoViewModel(
        repositories = ManageMovimientoRepositories(get(), get(), get(), get()),
        clock = get(),
        timeZoneProvider = get(),
        feedback = parameters.get(),
    )
  }
  viewModel { QuickCategoryViewModel(categoriaRepository = get(), suggester = get()) }
  viewModel { parameters ->
    SearchViewModel(
        repositories = SearchRepositories(get(), get(), get(), get(), get()),
        savedStateHandle = get(),
        unknownCategoryLabel = parameters.get(),
        dateProvider = SearchDateProvider(get(), get()),
        computationDispatcher = get<CoroutineDispatcher>(named(DispatcherQualifier.DEFAULT)),
    )
  }
  viewModel {
    StatisticsViewModel(
        repositories =
            StatisticsRepositories(
                preferences = get(),
                statistics = get(),
                actividades = get(),
                categorias = get(),
                etiquetas = get(),
                cuentas = get(),
            ),
        savedStateHandle = get(),
        civilToday = CivilToday(get(), get()),
        computationDispatcher = get<CoroutineDispatcher>(named(DispatcherQualifier.DEFAULT)),
    )
  }
  viewModel {
    ProfileViewModel(
        preferencesRepository = get(),
        export =
            ProfileExportDependencies(
                source = get(),
                sharer = get(),
                clock = get(),
                timeZoneProvider = get(),
            ),
        appInfo = get(),
        computationDispatcher = get<CoroutineDispatcher>(named(DispatcherQualifier.DEFAULT)),
    )
  }
  viewModel { CategoriesViewModel(categorias = get(), preferences = get(), suggester = get()) }
  viewModel { TagsViewModel(etiquetas = get(), preferences = get()) }
  viewModel {
    AccountsViewModel(
        cuentas = get(),
        saldos = get(),
        preferences = get(),
        today = CivilToday(get(), get()),
    )
  }
  viewModel { QuickCuentaViewModel(cuentaRepository = get()) }
  viewModel {
    RecurrentesViewModel(
        repositories =
            RecurrentesRepositories(
                recurrencias = get(),
                categorias = get(),
                cuentas = get(),
                etiquetas = get(),
            ),
        today = CivilToday(get(), get()),
    )
  }
  viewModel { parameters ->
    ManageTransferenciaViewModel(
        repositories =
            ManageTransferenciaRepositories(
                transferencias = get(),
                cuentas = get(),
                preferences = get(),
            ),
        idGenerator = get(),
        clock = get(),
        timeZoneProvider = get(),
        feedback = parameters.get(),
    )
  }
  viewModel {
    RecentTransferenciasViewModel(transferencias = get(), cuentas = get(), preferences = get())
  }
}
