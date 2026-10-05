package com.kurobello.tallybook.core.ui.resources

import java.util.Locale
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.calendar_date_content_description
import tallybook.shared.generated.resources.date_picker_months
import tallybook.shared.generated.resources.history_movement_count
import tallybook.shared.generated.resources.history_week_title
import tallybook.shared.generated.resources.history_year_description

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HistoryResourcesHostTest {
  @Test
  fun countAndYearPresentationResolveEverySupportedLocale() = runBlocking {
    val original = Locale.getDefault()
    try {
      val locales =
          listOf(
              Triple("es-CO", "1 movimiento", "2 movimientos"),
              Triple("es-AR", "1 movimiento", "2 movimientos"),
              Triple("en-US", "1 movement", "2 movements"),
              Triple("pt-BR", "1 movimentação", "2 movimentações"),
          )
      locales.forEach { (locale, singular, plural) ->
        Locale.setDefault(Locale.forLanguageTag(locale))
        assertEquals(singular, getPluralString(Res.plurals.history_movement_count, 1, 1), locale)
        assertEquals(plural, getPluralString(Res.plurals.history_movement_count, 2, 2), locale)
        val prefix =
            when (locale) {
              "en-US" -> "Select year:"
              "pt-BR" -> "Selecionar ano:"
              "es-AR" -> "Elegir año:"
              else -> "Seleccionar año:"
            }
        assertEquals("$prefix 2026", getString(Res.string.history_year_description, 2026), locale)
      }
    } finally {
      Locale.setDefault(original)
    }
  }

  @Test
  fun civilDatesAndWeekRangesUseLocaleWordOrderAndFullYear() = runBlocking {
    val original = Locale.getDefault()
    try {
      val cases =
          listOf(
              Triple("es-CO", "15 de septiembre de 2026", "8–14 septiembre"),
              Triple("es-AR", "15 de septiembre de 2026", "8–14 septiembre"),
              Triple("en-US", "September 15, 2026", "September 8–14"),
              Triple("pt-BR", "15 de setembro de 2026", "8–14 setembro"),
          )
      cases.forEach { (locale, date, week) ->
        Locale.setDefault(Locale.forLanguageTag(locale))
        val month = getStringArray(Res.array.date_picker_months)[8]
        assertEquals(
            date,
            getString(Res.string.calendar_date_content_description, 15, month, 2026),
            locale,
        )
        assertEquals(week, getString(Res.string.history_week_title, 8, 14, month), locale)
      }
    } finally {
      Locale.setDefault(original)
    }
  }
}
