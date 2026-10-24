package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.DataOutcome

class LocalDataBootstrapper(
    private val localeProvider: DeviceLocaleProvider,
    private val preferencesRepository: AppPreferencesRepository,
    private val categoriaRepository: CategoriaRepository,
    private val categorySeedProvider: CategorySeedProvider,
    private val changeClock: ChangeClock,
    private val storedMaxChangeStamp: suspend () -> DataOutcome<ChangeStamp?>,
) {
  suspend fun initialize(): DataOutcome<Unit> {
    val deviceLocale = resolveDeviceLocale(localeProvider.currentLanguageTag())
    when (
        val preferenceResult =
            preferencesRepository.initialize(monedaForRegion(deviceLocale.region))
    ) {
      is DataOutcome.Failure -> return preferenceResult
      is DataOutcome.Success -> Unit
    }

    return when (
        val seedResult = categoriaRepository.ensureSeeded(categorySeedProvider.getCategories())
    ) {
      is DataOutcome.Failure -> seedResult
      is DataOutcome.Success -> changeClock.prepare(storedMaxChangeStamp)
    }
  }
}
