package com.kurobello.tallybook.core.ui.resources

import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.feature.profile.ui.nameResource
import java.util.Locale
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.profile_export_body
import tallybook.shared.generated.resources.profile_theme_dark

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProfileResourcesHostTest {
  @Test
  fun everyCurrencyHasADistinctLocalizedNameInEverySupportedLocale() = runBlocking {
    val original = Locale.getDefault()
    try {
      val dollar =
          mapOf(
              "es-CO" to "Dólar estadounidense",
              "es-AR" to "Dólar estadounidense",
              "en-US" to "US dollar",
              "pt-BR" to "Dólar americano",
          )
      dollar.forEach { (locale, expected) ->
        Locale.setDefault(Locale.forLanguageTag(locale))
        val names = Moneda.entries.map { getString(it.nameResource()) }
        assertEquals(Moneda.entries.size, names.toSet().size, locale)
        assertEquals(expected, getString(Moneda.USD.nameResource()), locale)
      }
      Locale.setDefault(Locale.forLanguageTag("es-AR"))
      assertEquals(
          "Obtené una copia de tus movimientos para abrirla en una hoja de cálculo.",
          getString(Res.string.profile_export_body),
      )
      Locale.setDefault(Locale.forLanguageTag("pt-BR"))
      assertEquals("Escuro", getString(Res.string.profile_theme_dark))
    } finally {
      Locale.setDefault(original)
    }
  }
}
