package com.kurobello.tallybook.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

// GlobalContext itself is declared per-platform in Koin, not in commonMain — KoinPlatform is the
// multiplatform-safe accessor for the default context.
internal fun initKoin(appDeclaration: KoinApplication.() -> Unit = {}) {
  if (KoinPlatform.getKoinOrNull() != null) return
  startKoin {
    appDeclaration()
    modules(appModules)
  }
}
