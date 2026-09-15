package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.DeviceLocale
import com.kurobello.tallybook.core.model.Moneda

private const val ALPHA_REGION_SUBTAG_LENGTH = 2
private const val NUMERIC_REGION_SUBTAG_LENGTH = 3

private val monedaByRegion =
    mapOf(
        "CO" to Moneda.COP,
        "MX" to Moneda.MXN,
        "AR" to Moneda.ARS,
        "CL" to Moneda.CLP,
        "BR" to Moneda.BRL,
        "US" to Moneda.USD,
        "DO" to Moneda.DOP,
        "PE" to Moneda.PEN,
    )

internal fun resolveDeviceLocale(languageTag: String): DeviceLocale {
  val subtags = languageTag.replace('_', '-').split('-').filter(String::isNotEmpty)
  val language = subtags.firstOrNull()?.lowercase()
  val region =
      subtags.drop(1).takeWhile { it.length != 1 }.firstOrNull(::isRegionSubtag)?.uppercase()

  val appLocale =
      when (language) {
        "es" -> if (region == "AR") AppLocale.ES_AR else AppLocale.ES
        "en" -> AppLocale.EN
        "pt" -> if (region == "BR") AppLocale.PT_BR else AppLocale.ES
        else -> AppLocale.ES
      }

  return DeviceLocale(appLocale, region)
}

internal fun monedaForRegion(region: String?): Moneda = monedaByRegion[region] ?: Moneda.COP

private fun isRegionSubtag(subtag: String): Boolean =
    (subtag.length == ALPHA_REGION_SUBTAG_LENGTH && subtag.all(Char::isLetter)) ||
        (subtag.length == NUMERIC_REGION_SUBTAG_LENGTH && subtag.all(Char::isDigit))
