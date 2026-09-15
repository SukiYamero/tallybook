package com.kurobello.tallybook.di

import com.kurobello.tallybook.core.data.DeviceLocaleProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class LocaleModuleTest {
  @Test
  fun `locale module can be overridden and resolves its provider once`() {
    var creationCount = 0
    val fake = FakeDeviceLocaleProvider(languageTag = "en-US")
    val application = koinApplication {
      allowOverride(true)
      modules(
          localeModule,
          module {
            single<DeviceLocaleProvider> {
              creationCount += 1
              fake
            }
          },
      )
    }

    val firstProvider = application.koin.get<DeviceLocaleProvider>()
    val secondProvider = application.koin.get<DeviceLocaleProvider>()

    assertSame(fake, firstProvider)
    assertSame(firstProvider, secondProvider)
    assertEquals(1, creationCount)

    application.close()
  }
}

private class FakeDeviceLocaleProvider(
    private val languageTag: String,
) : DeviceLocaleProvider {
  override fun currentLanguageTag(): String = languageTag
}
