package com.kurobello.tallybook.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.mp.KoinPlatform

// Each platform entry point calls this; guarding against a second startKoin() covers Android's
// MainActivity.onCreate() re-running on process-retaining recreation (e.g. rotation).
// GlobalContext itself is declared per-platform in Koin, not in commonMain — KoinPlatform is the
// multiplatform-safe accessor for the default context.
fun initKoin(extraModules: List<Module> = emptyList()) {
  if (KoinPlatform.getKoinOrNull() != null) return
  startKoin { modules(appModules + extraModules) }
}
