package com.kurobello.tallybook.di

import com.kurobello.tallybook.core.data.AndroidDeviceLocaleProvider
import com.kurobello.tallybook.core.data.DeviceLocaleProvider
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val localeModule: Module = module {
  single<DeviceLocaleProvider> { AndroidDeviceLocaleProvider() }
}
