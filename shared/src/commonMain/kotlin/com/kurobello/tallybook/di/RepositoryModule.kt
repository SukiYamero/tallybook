package com.kurobello.tallybook.di

import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategoriaRepositoryImpl
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.MovimientoRepositoryImpl
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.qualifier.named
import org.koin.dsl.module

val repositoryModule = module {
  single<MovimientoRepository> {
    MovimientoRepositoryImpl(get(), get<CoroutineDispatcher>(named(DispatcherQualifier.IO)))
  }
  single<CategoriaRepository> {
    CategoriaRepositoryImpl(get(), get<CoroutineDispatcher>(named(DispatcherQualifier.IO)))
  }
}
