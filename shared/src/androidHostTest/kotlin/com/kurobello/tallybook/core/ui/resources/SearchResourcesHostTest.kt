package com.kurobello.tallybook.core.ui.resources

import java.util.Locale
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.search_count
import tallybook.shared.generated.resources.search_loading

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SearchResourcesHostTest {
  @Test
  fun pendingResultStatusUsesLoadingCopyForEverySupportedLocale() = runBlocking {
    val original = Locale.getDefault()
    try {
      val cases =
          mapOf(
              "es-CO" to "Cargando…",
              "es-AR" to "Cargando…",
              "en-US" to "Loading…",
              "pt-BR" to "Carregando…",
          )
      cases.forEach { (locale, expected) ->
        Locale.setDefault(Locale.forLanguageTag(locale))
        assertEquals(expected, getString(Res.string.search_loading), locale)
      }
    } finally {
      Locale.setDefault(original)
    }
  }

  @Test
  fun resultCountsRenderNumbersForEverySupportedLocale() = runBlocking {
    val original = Locale.getDefault()
    try {
      val cases =
          listOf(
              Triple("es-CO", "resultado", "resultados"),
              Triple("es-AR", "resultado", "resultados"),
              Triple("en-US", "result", "results"),
              Triple("pt-BR", "resultado", "resultados"),
          )
      cases.forEach { (locale, singular, plural) ->
        Locale.setDefault(Locale.forLanguageTag(locale))
        assertEquals("1 $singular", getPluralString(Res.plurals.search_count, 1, 1), locale)
        val zeroLabel = if (locale == "pt-BR") singular else plural
        assertEquals("0 $zeroLabel", getPluralString(Res.plurals.search_count, 0, 0), locale)
        listOf(2, 58).forEach { count ->
          assertEquals(
              "$count $plural",
              getPluralString(Res.plurals.search_count, count, count),
              locale,
          )
        }
      }
    } finally {
      Locale.setDefault(original)
    }
  }
}
