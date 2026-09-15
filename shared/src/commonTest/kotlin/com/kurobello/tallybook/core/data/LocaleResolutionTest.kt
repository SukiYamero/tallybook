package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.DeviceLocale
import com.kurobello.tallybook.core.model.Moneda
import kotlin.test.Test
import kotlin.test.assertEquals

class LocaleResolutionTest {
  @Test
  fun `resolveDeviceLocale normalizes supported and unsupported language tags`() {
    val cases =
        listOf(
            "es-CO" to DeviceLocale(AppLocale.ES, "CO"),
            "es-AR" to DeviceLocale(AppLocale.ES_AR, "AR"),
            "en-US" to DeviceLocale(AppLocale.EN, "US"),
            "pt-BR" to DeviceLocale(AppLocale.PT_BR, "BR"),
            "es-Latn-CO" to DeviceLocale(AppLocale.ES, "CO"),
            "en-US-u-hc-h12" to DeviceLocale(AppLocale.EN, "US"),
            "es_AR" to DeviceLocale(AppLocale.ES_AR, "AR"),
            "ES_co" to DeviceLocale(AppLocale.ES, "CO"),
            "pt-PT" to DeviceLocale(AppLocale.ES, "PT"),
            "fr-CA" to DeviceLocale(AppLocale.ES, "CA"),
            "" to DeviceLocale(AppLocale.ES, null),
        )

    cases.forEach { (languageTag, expected) ->
      assertEquals(expected, resolveDeviceLocale(languageTag), languageTag)
    }
  }

  @Test
  fun `monedaForRegion returns mapped moneda or COP fallback`() {
    val cases =
        listOf(
            "CO" to Moneda.COP,
            "MX" to Moneda.MXN,
            "AR" to Moneda.ARS,
            "CL" to Moneda.CLP,
            "BR" to Moneda.BRL,
            "US" to Moneda.USD,
            "DO" to Moneda.DOP,
            "PE" to Moneda.PEN,
            "ZZ" to Moneda.COP,
            null to Moneda.COP,
        )

    cases.forEach { (region, expected) -> assertEquals(expected, monedaForRegion(region), region) }
  }

  @Test
  fun `monedaForRegion follows the region instead of the app locale`() {
    val cases =
        listOf(
            "es-US" to Moneda.USD,
            "en-CO" to Moneda.COP,
        )

    cases.forEach { (languageTag, expected) ->
      val region = resolveDeviceLocale(languageTag).region

      assertEquals(expected, monedaForRegion(region), languageTag)
    }
  }
}
