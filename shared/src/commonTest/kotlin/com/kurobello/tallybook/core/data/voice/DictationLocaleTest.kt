package com.kurobello.tallybook.core.data.voice

import com.kurobello.tallybook.core.model.AppLocale
import kotlin.test.Test
import kotlin.test.assertEquals

class DictationLocaleTest {
  @Test
  fun everyAppLanguageDictatesInTheVariantMostDevicesCanInstall() {
    val expected =
        mapOf(
            AppLocale.ES to "es-ES",
            AppLocale.ES_AR to "es-ES",
            AppLocale.EN to "en-US",
            AppLocale.PT_BR to "pt-BR",
        )

    AppLocale.entries.forEach { locale ->
      assertEquals(expected.getValue(locale), dictationLanguageTag(locale), "$locale")
    }
  }
}
