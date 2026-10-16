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
import tallybook.shared.generated.resources.movimiento_detected_label
import tallybook.shared.generated.resources.movimiento_row_extra_categories
import tallybook.shared.generated.resources.movimiento_summary_title

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EntryResourcesHostTest {
  @Test
  fun reviewTitleAndDetectedLabelFollowEachLocaleVoice() = runBlocking {
    val original = Locale.getDefault()
    try {
      val cases =
          listOf(
              Triple("es-CO", "Revisa antes de guardar", "Detectado"),
              Triple("es-AR", "Revisá antes de guardar", "Detectado"),
              Triple("en-US", "Review before saving", "Detected"),
              Triple("pt-BR", "Revise antes de salvar", "Detectado"),
          )
      cases.forEach { (locale, title, detected) ->
        Locale.setDefault(Locale.forLanguageTag(locale))
        assertEquals(title, getString(Res.string.movimiento_summary_title), locale)
        assertEquals(detected, getString(Res.string.movimiento_detected_label), locale)
      }
    } finally {
      Locale.setDefault(original)
    }
  }

  @Test
  fun extraCategoryCountAgreesInNumberForEveryLocale() = runBlocking {
    val original = Locale.getDefault()
    try {
      val cases =
          listOf(
              Triple("es-CO", "1 categoría más", "2 categorías más"),
              Triple("es-AR", "1 categoría más", "2 categorías más"),
              Triple("en-US", "1 more category", "2 more categories"),
              Triple("pt-BR", "mais 1 categoria", "mais 2 categorias"),
          )
      cases.forEach { (locale, singular, plural) ->
        Locale.setDefault(Locale.forLanguageTag(locale))
        val one = getPluralString(Res.plurals.movimiento_row_extra_categories, 1, 1)
        val two = getPluralString(Res.plurals.movimiento_row_extra_categories, 2, 2)
        assertEquals(singular, one, locale)
        assertEquals(plural, two, locale)
      }
    } finally {
      Locale.setDefault(original)
    }
  }
}
