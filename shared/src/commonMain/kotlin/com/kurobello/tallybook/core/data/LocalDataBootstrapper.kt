package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.DataOutcome

class LocalDataBootstrapper(
    private val localeProvider: DeviceLocaleProvider,
    private val preferencesRepository: AppPreferencesRepository,
    private val categoriaRepository: CategoriaRepository,
    private val categorySeedProvider: CategorySeedProvider,
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

    return categoriaRepository.ensureSeeded(categorySeedProvider.getCategories())
  }
}
