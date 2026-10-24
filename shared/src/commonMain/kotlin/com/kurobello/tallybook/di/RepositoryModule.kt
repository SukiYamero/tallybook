package com.kurobello.tallybook.di

import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategoriaRepositoryImpl
import com.kurobello.tallybook.core.data.CategorySeedProvider
import com.kurobello.tallybook.core.data.CuentaRepository
import com.kurobello.tallybook.core.data.CuentaRepositoryImpl
import com.kurobello.tallybook.core.data.CuentaSeedProvider
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.data.EtiquetaRepositoryImpl
import com.kurobello.tallybook.core.data.LocalDataBootstrapper
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.MovimientoRepositoryImpl
import com.kurobello.tallybook.core.data.SaldoRepository
import com.kurobello.tallybook.core.data.SaldoRepositoryImpl
import com.kurobello.tallybook.core.data.TransferenciaRepository
import com.kurobello.tallybook.core.data.TransferenciaRepositoryImpl
import com.kurobello.tallybook.core.data.maxStoredChangeStamp
import com.kurobello.tallybook.core.data.seedDefaultCuenta
import com.kurobello.tallybook.core.database.TallybookDatabase
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.qualifier.named
import org.koin.dsl.module

val repositoryModule = module {
  single<MovimientoRepository> {
    MovimientoRepositoryImpl(
        database = get(),
        dispatcher = get<CoroutineDispatcher>(named(DispatcherQualifier.IO)),
        changeClock = get(),
    )
  }
  single<CategoriaRepository> {
    CategoriaRepositoryImpl(
        database = get(),
        dispatcher = get<CoroutineDispatcher>(named(DispatcherQualifier.IO)),
        idGenerator = get(),
        changeClock = get(),
    )
  }
  single<EtiquetaRepository> {
    EtiquetaRepositoryImpl(
        database = get(),
        dispatcher = get<CoroutineDispatcher>(named(DispatcherQualifier.IO)),
        idGenerator = get(),
        clock = get(),
        changeClock = get(),
    )
  }
  single<CuentaRepository> {
    CuentaRepositoryImpl(
        database = get(),
        dispatcher = get<CoroutineDispatcher>(named(DispatcherQualifier.IO)),
        idGenerator = get(),
        clock = get(),
        changeClock = get(),
    )
  }
  single<TransferenciaRepository> {
    TransferenciaRepositoryImpl(
        database = get(),
        dispatcher = get<CoroutineDispatcher>(named(DispatcherQualifier.IO)),
        clock = get(),
        timeZoneProvider = get(),
        changeClock = get(),
    )
  }
  single { CategorySeedProvider() }
  single { CuentaSeedProvider(clock = get()) }
  single {
    val database = get<TallybookDatabase>()
    val dispatcher = get<CoroutineDispatcher>(named(DispatcherQualifier.IO))
    LocalDataBootstrapper(
        localeProvider = get(),
        preferencesRepository = get(),
        categoriaRepository = get(),
        categorySeedProvider = get(),
        cuentaSeedProvider = get(),
        seedDefaultCuenta = { database.seedDefaultCuenta(it, dispatcher) },
        changeClock = get(),
        storedMaxChangeStamp = { database.maxStoredChangeStamp(dispatcher) },
    )
  }
  single<SaldoRepository> {
    SaldoRepositoryImpl(
        database = get(),
        dispatcher = get<CoroutineDispatcher>(named(DispatcherQualifier.IO)),
    )
  }
}
