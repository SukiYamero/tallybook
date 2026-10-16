package com.kurobello.tallybook.core.ui.resources

import com.kurobello.tallybook.core.data.CategorySeedProvider
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import java.util.Locale
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CategoryResourcesHostTest {
  @Test
  fun otrosIsLocalizedForEverySupportedLocale() = runBlocking {
    val original = Locale.getDefault()
    try {
      val cases =
          mapOf("es-CO" to "Otros", "es-AR" to "Otros", "en-US" to "Other", "pt-BR" to "Outros")
      cases.forEach { (locale, expected) ->
        Locale.setDefault(Locale.forLanguageTag(locale))
        val otros = CategorySeedProvider().getCategories().single { it.id == CATEGORIA_OTROS_ID }
        assertEquals(expected, otros.nombre, locale)
      }
    } finally {
      Locale.setDefault(original)
    }
  }
}
