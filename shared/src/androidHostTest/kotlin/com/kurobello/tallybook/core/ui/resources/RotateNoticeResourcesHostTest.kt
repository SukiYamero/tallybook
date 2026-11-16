package com.kurobello.tallybook.core.ui.resources

import java.util.Locale
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.rotate_notice_body
import tallybook.shared.generated.resources.rotate_notice_dismiss
import tallybook.shared.generated.resources.rotate_notice_title

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RotateNoticeResourcesHostTest {
  @Test
  fun noticeCopyResolvesForEverySupportedLocale() = runBlocking {
    val original = Locale.getDefault()
    try {
      val cases =
          mapOf(
              "es-CO" to listOf("Mejor en vertical", "Entendido"),
              "es-AR" to listOf("Mejor en vertical", "Entendido"),
              "en-US" to listOf("Best in portrait", "Got it"),
              "pt-BR" to listOf("Melhor na vertical", "Entendi"),
          )
      cases.forEach { (locale, expected) ->
        Locale.setDefault(Locale.forLanguageTag(locale))
        assertEquals(expected[0], getString(Res.string.rotate_notice_title), locale)
        assertEquals(expected[1], getString(Res.string.rotate_notice_dismiss), locale)
        assertEquals(true, getString(Res.string.rotate_notice_body).isNotBlank(), locale)
      }
    } finally {
      Locale.setDefault(original)
    }
  }
}
