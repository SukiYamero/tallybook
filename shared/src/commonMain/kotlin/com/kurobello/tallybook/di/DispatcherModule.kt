package com.kurobello.tallybook.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.qualifier.named
import org.koin.dsl.module

enum class DispatcherQualifier {
  IO,
  DEFAULT,
  MAIN,
}

// Dispatchers.IO isn't declared on every KMP target (Native only ships Default/Main/Unconfined),
// so the blocking-friendly dispatcher is resolved per platform via expect/actual.
expect val ioDispatcher: CoroutineDispatcher

// Explicit <CoroutineDispatcher>: Dispatchers.Main infers as MainCoroutineDispatcher, which would
// otherwise register under a different key and make it unresolvable via CoroutineDispatcher.
val dispatcherModule = module {
  single<CoroutineDispatcher>(named(DispatcherQualifier.IO)) { ioDispatcher }
  single<CoroutineDispatcher>(named(DispatcherQualifier.DEFAULT)) { Dispatchers.Default }
  single<CoroutineDispatcher>(named(DispatcherQualifier.MAIN)) { Dispatchers.Main }
}
