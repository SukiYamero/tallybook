package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.andThen
import com.kurobello.tallybook.core.model.primaryMoneda
import kotlinx.coroutines.flow.first

@Suppress("LongParameterList")
class LocalDataBootstrapper(
    private val localeProvider: DeviceLocaleProvider,
    private val preferencesRepository: AppPreferencesRepository,
    private val categoriaRepository: CategoriaRepository,
    private val categorySeedProvider: CategorySeedProvider,
    private val cuentaSeedProvider: CuentaSeedProvider,
    private val seedDefaultCuenta: suspend (Cuenta) -> DataOutcome<Unit>,
    private val changeClock: ChangeClock,
    private val storedMaxChangeStamp: suspend () -> DataOutcome<ChangeStamp?>,
) {
  suspend fun initialize(): DataOutcome<Unit> {
    val deviceLocale = resolveDeviceLocale(localeProvider.currentLanguageTag())
    return preferencesRepository
        .initialize(monedaForRegion(deviceLocale.region))
        .andThen { categoriaRepository.ensureSeeded(categorySeedProvider.getCategories()) }
        .andThen { seedPrimaryCash() }
        .andThen { changeClock.prepare(storedMaxChangeStamp) }
  }

  // The stored primary currency, not the device region: an existing install may differ from it.
  private suspend fun seedPrimaryCash(): DataOutcome<Unit> =
      preferencesRepository.preferences.first().primaryMoneda().andThen { primary ->
        seedDefaultCuenta(cuentaSeedProvider.defaultCash(primary))
      }
}
